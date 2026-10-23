package app.suran

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class InstalledApp(val pkg: String, val label: String)

/** Launchable apps, minus SURAN itself, sorted by label. */
fun queryInstalled(ctx: Context): List<InstalledApp> {
    val pm = ctx.packageManager
    val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(main, 0)
        .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .filter { it.pkg != ctx.packageName }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

fun launchApp(ctx: Context, pkg: String): Boolean {
    val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        ctx.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

fun isDefaultHome(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 29) {
        val rm = ctx.getSystemService(RoleManager::class.java) ?: return false
        return rm.isRoleHeld(RoleManager.ROLE_HOME)
    }
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val ri = ctx.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
    return ri?.activityInfo?.packageName == ctx.packageName
}

fun appLabelOf(ctx: Context, pkg: String): String = try {
    val pm = ctx.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
} catch (e: Exception) {
    pkg.substringAfterLast('.')
}
