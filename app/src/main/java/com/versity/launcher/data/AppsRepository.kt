package com.versity.launcher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A row as reported by the system, before we decide whether it is launchable. */
data class RawApp(
    val label: String,
    val packageName: String,
    val launchActivity: String?,
    /** Which profile the app lives in (main, work, clone/dual apps). 0 is the main user. */
    val userSerial: Long = 0,
)

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val userSerial: Long = 0,
) {
    /** Stable id used for favourites and list keys. */
    val key: String get() = "$packageName/$activityName#$userSerial"
}

/** Thin seam over PackageManager so the repository is testable on the JVM. */
fun interface InstalledAppSource {
    fun queryInstalled(): List<RawApp>
}

class AppsRepository(
    private val source: InstalledAppSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /** Launchable apps only, de-duplicated by component and profile, sorted by label ignoring case. */
    suspend fun loadApps(): List<AppEntry> = withContext(ioDispatcher) {
        source.queryInstalled()
            .mapNotNull { raw ->
                val activity = raw.launchActivity
                if (activity.isNullOrBlank() || raw.packageName.isBlank() || raw.label.isBlank()) null
                else AppEntry(raw.label.trim(), raw.packageName, activity, raw.userSerial)
            }
            .distinctBy { it.key }
            .sortedWith(compareBy<AppEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.label }.thenBy { it.userSerial })
    }
}

/** Case-insensitive label filter; a blank query returns everything unchanged. */
fun filterApps(apps: List<AppEntry>, query: String): List<AppEntry> {
    val q = query.trim()
    if (q.isEmpty()) return apps
    return apps.filter { it.label.contains(q, ignoreCase = true) }
}

/** Keys to apps, in key order, dropping any that are no longer installed. */
fun resolveFavourites(keys: List<String>, apps: List<AppEntry>): List<AppEntry> {
    val byKey = apps.associateBy { it.key }
    return keys.mapNotNull { byKey[it] }
}

/**
 * Reads launchable activities from every profile of the user (main, work, cloned/dual apps)
 * through LauncherApps, which is the API launchers are meant to use.
 */
class LauncherAppsSource(private val context: Context) : InstalledAppSource {
    override fun queryInstalled(): List<RawApp> {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        val userManager = context.getSystemService(UserManager::class.java) ?: return emptyList()
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            runCatching {
                launcherApps.getActivityList(null, user).map { info ->
                    RawApp(
                        label = info.label?.toString().orEmpty(),
                        packageName = info.componentName.packageName,
                        launchActivity = info.componentName.className,
                        userSerial = serial,
                    )
                }
            }.getOrDefault(emptyList())
        }
    }
}
