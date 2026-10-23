package app.suran

/** In-memory, process-lifetime state shared between SURAN's activities (and the M2 service). */
object Session {
    /** Packages opened with "Open once". Cleared whenever SURAN Home is shown or the screen turns off. */
    val onceAllowed: MutableSet<String> = mutableSetOf()

    /** Last package the Outside sheet was shown for (M2 loop guard). Cleared when SURAN Home is shown. */
    @Volatile var lastOutside: String? = null

    fun reset() {
        onceAllowed.clear()
        lastOutside = null
    }
}
