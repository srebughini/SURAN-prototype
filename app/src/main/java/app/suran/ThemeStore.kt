package app.suran

import java.io.File

/**
 * Remembers the chosen theme in a tiny text file next to config.json ("dark" or "light").
 * Missing, unreadable or unknown content means light. Never transmitted.
 */
class ThemeStore(dir: File) {
    private val file = File(dir, "theme")

    fun loadDark(): Boolean = try {
        file.exists() && file.readText().trim() == "dark"
    } catch (e: Exception) {
        false
    }

    fun saveDark(dark: Boolean): Boolean = try {
        file.writeText(if (dark) "dark" else "light")
        true
    } catch (e: Exception) {
        false
    }
}
