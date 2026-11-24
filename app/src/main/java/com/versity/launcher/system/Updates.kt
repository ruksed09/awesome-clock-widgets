package com.versity.launcher.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** A published Versity release with its APK. */
data class Release(
    val version: String,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    /** SHA-256 of the APK as GitHub reports it, when it does. */
    val sha256: String?,
    val pageUrl: String,
)

/** Where an update stands. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class Ready(val release: Release, val file: File) : UpdateState
    data class Failed(val message: String, val release: Release? = null) : UpdateState
}

/** Reads releases from GitHub and compares versions. No account or token: the repository is public. */
object GitHubReleases {
    const val REPO = "ruksed09/awesome-clock-widgets"
    private const val LATEST = "https://api.github.com/repos/$REPO/releases/latest"

    fun parse(json: String): Release? {
        val o = JSONObject(json)
        val tag = o.optString("tag_name").ifBlank { return null }
        val assets = o.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (!a.optString("name").endsWith(".apk")) continue
            return Release(
                version = tag.removePrefix("v"),
                tag = tag,
                notes = o.optString("body"),
                apkUrl = a.optString("browser_download_url"),
                apkSize = a.optLong("size"),
                sha256 = a.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:"),
                pageUrl = o.optString("html_url"),
            )
        }
        return null
    }

    /** True when [remote] is a later version than [local], comparing dotted numbers ("0.10.0" > "0.9.2"). */
    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** The latest release's JSON as GitHub sends it, or null when there is none yet. */
    suspend fun latestJson(): String? = withContext(Dispatchers.IO) {
        val conn = URL(LATEST).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            // GitHub asks API clients to name themselves; nothing personal is sent.
            conn.setRequestProperty("User-Agent", "Versity-Updater")
            when (conn.responseCode) {
                200 -> conn.inputStream.bufferedReader().use { it.readText() }
                404 -> null
                else -> error("GitHub answered ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Checks for, downloads and installs Versity updates from GitHub releases. One instance lives in the
 * app container, so progress survives leaving and re-entering screens.
 */
class UpdateManager(private val app: Context, private val scope: CoroutineScope) {
    private val prefs = app.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String =
        runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull().orEmpty()

    init {
        // An update found earlier is shown straight away on every launch until it is installed, rather
        // than only in the session that happened to find it.
        known()?.let { _state.value = UpdateState.Available(it) }
    }

    /** The last release GitHub reported, if it is still newer than this install. */
    private fun known(): Release? = prefs.getString(LATEST_JSON, null)
        ?.let { runCatching { GitHubReleases.parse(it) }.getOrNull() }
        ?.takeIf { GitHubReleases.isNewer(it.version, currentVersion) }

    /**
     * Asks GitHub for the latest release, at most once an hour unless [force] (the "Check now" button).
     * Called each time Versity comes to the front, so a new release shows within the hour.
     */
    fun check(force: Boolean = false) {
        val busy = _state.value is UpdateState.Checking || _state.value is UpdateState.Downloading || _state.value is UpdateState.Ready
        val recent = System.currentTimeMillis() - prefs.getLong(LAST_CHECK, 0L) < CHECK_EVERY
        if (busy || (!force && recent)) return
        scope.launch {
            val before = _state.value
            if (force || before !is UpdateState.Available) _state.value = UpdateState.Checking
            _state.value = runCatching { GitHubReleases.latestJson() }.fold(
                onSuccess = { json ->
                    val release = json?.let { runCatching { GitHubReleases.parse(it) }.getOrNull() }
                    prefs.edit().putLong(LAST_CHECK, System.currentTimeMillis()).putString(LATEST_JSON, json).apply()
                    when {
                        release != null && GitHubReleases.isNewer(release.version, currentVersion) -> UpdateState.Available(release)
                        else -> UpdateState.UpToDate(currentVersion)
                    }
                },
                // A quiet background check that fails keeps showing what was already known.
                onFailure = {
                    if (!force && before is UpdateState.Available) before
                    else UpdateState.Failed("Couldn't reach GitHub. Check your connection and try again.")
                },
            )
        }
    }

    /** Downloads the APK into the app's cache, then verifies it before offering to install. */
    fun download(release: Release) {
        if (_state.value is UpdateState.Downloading) return
        scope.launch {
            _state.value = UpdateState.Downloading(release, 0f)
            _state.value = runCatching { fetch(release) }.fold(
                onSuccess = { UpdateState.Ready(release, it) },
                onFailure = { UpdateState.Failed(it.message ?: "The download failed.", release) },
            )
        }
    }

    private suspend fun fetch(release: Release): File = withContext(Dispatchers.IO) {
        val dir = File(app.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "versity-${release.version}.apk")
        val conn = URL(release.apkUrl).openConnection() as HttpURLConnection
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Versity-Updater")
            check(conn.responseCode == 200) { "The download failed (GitHub answered ${conn.responseCode})." }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastShown = 0f
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        val p = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
                        if (p - lastShown >= 0.01f) {
                            lastShown = p
                            _state.value = UpdateState.Downloading(release, p)
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        check(release.sha256 == null || release.sha256.equals(sha, ignoreCase = true)) { "The download was damaged. Please try again." }
        verify(file)
        file
    }

    /** The APK must be Versity, newer than this install, and signed with the same key. */
    private fun verify(file: File) {
        val pm = app.packageManager
        val archive = archiveInfo(pm, file.path) ?: error("The download isn't a valid app.")
        check(archive.packageName == app.packageName) { "The download isn't Versity." }
        val installed = installedInfo(pm)
        check(versionCode(archive) > versionCode(installed)) { "The download isn't newer than this version." }
        check(signatures(archive) == signatures(installed) && signatures(installed).isNotEmpty()) { "The download isn't signed by Versity's key." }
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, path: String): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return if (Build.VERSION.SDK_INT >= 33) pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(flags.toLong()))
        else pm.getPackageArchiveInfo(path, flags)
    }

    @Suppress("DEPRECATION")
    private fun installedInfo(pm: PackageManager): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        else pm.getPackageInfo(app.packageName, flags)
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signatures(info: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return sigs.orEmpty().map { it.toCharsString() }.toSet()
    }

    /**
     * Hands the verified APK to Android's installer. The first time, Android asks the user to allow
     * Versity to install apps; that settings screen opens instead, and they tap Install again after.
     */
    fun install(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settings) }
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            _state.value = UpdateState.Failed("Couldn't open the installer.", (state.value as? UpdateState.Ready)?.release)
        }
    }

    private companion object {
        const val LAST_CHECK = "last_check"
        const val LATEST_JSON = "latest_json"
        /** GitHub allows 60 anonymous requests an hour; one an hour is far inside that. */
        const val CHECK_EVERY = 60 * 60 * 1000L
    }
}
