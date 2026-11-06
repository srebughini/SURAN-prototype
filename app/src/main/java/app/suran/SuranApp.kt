package app.suran

import android.app.Application

class SuranApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Ink.dark = ThemeStore(filesDir).loadDark()   // before any Activity draws
        EventLog(this).log("app_open", "defaultHome" to isDefaultHome(this))
    }
}
