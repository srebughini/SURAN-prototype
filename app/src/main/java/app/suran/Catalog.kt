package app.suran

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore

enum class Group(val title: String) {
    TALK("Talk"),
    GET_AROUND("Get around"),
    CAPTURE("Capture"),
    LISTEN("Listen"),
    PAY("Pay and prove"),
}

/** A curated "thing this phone can be for". Matched by known packages, or by an intent. */
class CatalogItem(
    val id: String,
    val label: String,
    val group: Group,
    val packages: List<String> = emptyList(),
    val intent: (() -> Intent)? = null,
)

val CATALOG: List<CatalogItem> = listOf(
    CatalogItem("phone", "Phone", Group.TALK, intent = { Intent(Intent.ACTION_DIAL) }),
    CatalogItem("messages", "Messages", Group.TALK, intent = { Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")) }),
    CatalogItem("whatsapp", "WhatsApp", Group.TALK, packages = listOf("com.whatsapp")),
    CatalogItem("maps", "Maps", Group.GET_AROUND, intent = { Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0")) }),
    CatalogItem("camera", "Camera", Group.CAPTURE, intent = { Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA) }),
    CatalogItem("music", "Music", Group.LISTEN, packages = listOf("com.spotify.music", "com.google.android.apps.youtube.music")),
    CatalogItem("authenticator", "Authenticator", Group.PAY, packages = listOf("com.google.android.apps.authenticator2", "com.azure.authenticator")),
    CatalogItem("wallet", "Wallet", Group.PAY, packages = listOf("com.google.android.apps.walletnfcrel")),
)

fun catalogItem(id: String?): CatalogItem? = CATALOG.firstOrNull { it.id == id }

/** Installed candidates per catalog item. Items with no installed match are dropped. */
fun catalogCandidates(ctx: Context, installed: List<InstalledApp>): List<Pair<CatalogItem, List<InstalledApp>>> {
    val byPkg = installed.associateBy { it.pkg }
    val pm = ctx.packageManager
    return CATALOG.mapNotNull { item ->
        val pkgs: List<String> = if (item.packages.isNotEmpty()) {
            item.packages
        } else {
            val i = item.intent?.invoke()
            if (i == null) emptyList()
            else pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo.packageName }
        }
        val apps = pkgs.distinct().mapNotNull { byPkg[it] }
        if (apps.isEmpty()) null else item to apps
    }
}
