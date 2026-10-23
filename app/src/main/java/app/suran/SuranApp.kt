package app.suran

import android.app.Application

class SuranApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EventLog(this).log("app_open", "defaultHome" to isDefaultHome(this))
    }
}
