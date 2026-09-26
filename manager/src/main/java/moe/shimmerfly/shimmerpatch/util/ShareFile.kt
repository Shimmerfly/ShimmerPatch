// The platform keeps the android:FLAG_GRANT_* constants inside Intent's flags typedef, and lint
// cannot resolve that typedef from this compile SDK: it reports the value as outside the allowed
// set while printing that very value in the set. Nothing else in this file needs the exemption.
@file:Suppress("WrongConstant")

package moe.shimmerfly.shimmerpatch.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands [file] to another app through the system chooser, granting whoever answers read access to
 * it.
 *
 * Both intents carry the grant, because the chooser is the one that is actually launched: a grant
 * left on the wrapped intent alone leaves some receivers unable to open the file.
 */
fun shareFileWithGrant(
    context: Context,
    file: File,
    chooserTitle: String,
    mimeType: String,
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val share = Intent(Intent.ACTION_SEND)
    share.type = mimeType
    share.putExtra(Intent.EXTRA_STREAM, uri)
    share.clipData = ClipData.newUri(context.contentResolver, file.name, uri)
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    val chooser = Intent.createChooser(share, chooserTitle)
    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(chooser)
}
