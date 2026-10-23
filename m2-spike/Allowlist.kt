package app.suran

/** Packages that never trigger the Outside sheet (M2). Edit freely during the spike. */
object Allowlist {
    val packages: Set<String> = setOf(
        "app.suran",
        "android",
        "com.android.systemui",
        "com.android.settings",
        // Telecom / emergency: never intercepted.
        "com.android.server.telecom", "com.android.phone", "com.android.incallui",
        "com.android.emergency", "com.huawei.android.incallui",
        // Permission and install dialogs.
        "com.android.packageinstaller", "com.google.android.packageinstaller",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller",
        // EMUI system surfaces seen on Honor/Huawei devices (extend after observing the log).
        "com.huawei.systemmanager", "com.huawei.android.internal.app",
    )
}
