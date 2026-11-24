package com.versity.launcher.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import com.versity.launcher.data.AppEntry

/** Launching and icons go through LauncherApps with the app's own profile, so clone/work apps work. */
class AppLauncher(context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private fun userFor(serial: Long): UserHandle? = userManager?.getUserForSerialNumber(serial)

    /** False when the app could not be started (uninstalled, paused or locked profile, no permission). */
    fun launch(app: AppEntry): Boolean = try {
        val user = userFor(app.userSerial)
        if (user == null || launcherApps == null) false
        else {
            launcherApps.startMainActivity(ComponentName(app.packageName, app.activityName), user, null, null)
            true
        }
    } catch (_: Exception) {
        false
    }

    /** The launcher icon with the profile badge (for example the clone-app marker). */
    fun icon(app: AppEntry): Drawable? = try {
        val user = userFor(app.userSerial)
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
        if (user == null) null else launcherApps?.resolveActivity(intent, user)?.getBadgedIcon(0)
    } catch (_: Exception) {
        null
    }
}
