package app.suran

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local research log. JSON lines, capped at 5,000 lines. Never transmitted.
 * File: <external files dir>/events.jsonl  (adb pull /sdcard/Android/data/app.suran/files/events.jsonl)
 */
class EventLog(context: Context) {
    private val file = File(context.getExternalFilesDir(null) ?: context.filesDir, "events.jsonl")
    private var writes = 0

    @Synchronized
    fun log(type: String, vararg fields: Pair<String, Any?>) {
        try {
            val o = JSONObject()
            o.put("t", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()))
            o.put("e", type)
            fields.forEach { (k, v) ->
                o.put(k, if (v is Collection<*>) JSONArray(v) else (v ?: JSONObject.NULL))
            }
            file.appendText(o.toString() + "\n")
            if (++writes % 100 == 0) trim()
        } catch (e: Exception) {
            // Logging must never break the product.
        }
    }

    private fun trim() {
        val lines = file.readLines()
        if (lines.size > MAX_LINES) {
            file.writeText(lines.takeLast(MAX_LINES).joinToString("\n") + "\n")
        }
    }

    companion object { const val MAX_LINES = 5000 }
}
