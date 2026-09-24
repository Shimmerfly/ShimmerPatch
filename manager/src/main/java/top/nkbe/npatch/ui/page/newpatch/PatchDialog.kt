package top.nkbe.npatch.ui.page.newpatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private class DialogSnapshot<T>(var value: T?)

/** Keeps dialog content arguments stable while its visibility changes. */
@Composable
fun <T : Any> RetainedPatchDialog(value: T?, content: @Composable (T, Boolean) -> Unit) {
    val snapshot = remember { DialogSnapshot(value) }
    if (value != null) snapshot.value = value
    snapshot.value?.let { content(it, value != null) }
}
