package app.suran

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One thing this phone is for. intentId is null for apps picked from "Everything else". */
data class Entry(val pkg: String, val intentId: String?, val appLabel: String)

data class Config(val setupDone: Boolean, val entries: List<Entry>)

/** Single JSON file, written atomically (temp file + rename). */
class ConfigStore(dir: File) {
    private val file = File(dir, "config.json")
    private val tmp = File(dir, "config.json.tmp")

    fun load(): Config? {
        if (!file.exists()) return null
        return try {
            val o = JSONObject(file.readText())
            val arr = o.getJSONArray("entries")
            val entries = (0 until arr.length()).map { i ->
                val e = arr.getJSONObject(i)
                val pkg = e.getString("pkg")
                Entry(
                    pkg = pkg,
                    intentId = if (e.isNull("intentId")) null else e.getString("intentId"),
                    appLabel = e.optString("appLabel", pkg),
                )
            }
            Config(o.optBoolean("setupDone", false), entries)
        } catch (e: Exception) {
            null
        }
    }

    fun save(c: Config): Boolean = try {
        val arr = JSONArray()
        c.entries.forEach {
            arr.put(
                JSONObject()
                    .put("pkg", it.pkg)
                    .put("intentId", it.intentId ?: JSONObject.NULL)
                    .put("appLabel", it.appLabel)
            )
        }
        val o = JSONObject().put("schema", 1).put("setupDone", c.setupDone).put("entries", arr)
        tmp.writeText(o.toString())
        tmp.renameTo(file)
    } catch (e: Exception) {
        false
    }
}

/** Catalog entries first (catalog order), then everything else in the order added. */
fun normalize(draft: List<Entry>): List<Entry> {
    val idx = CATALOG.withIndex().associate { it.value.id to it.index }
    val unique = draft.distinctBy { it.pkg }
    val cat = unique.filter { it.intentId != null && idx.containsKey(it.intentId) }
        .sortedBy { idx[it.intentId] }
    val rest = unique.filter { it.intentId == null || !idx.containsKey(it.intentId) }
    return cat + rest
}
