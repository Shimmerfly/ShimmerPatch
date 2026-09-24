package moe.shimmerfly.shimmerpatch.ui.page.newpatch

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager

/** The patch destination retains dialog data while an app picker is above it or configuration changes. */
class PatchFlowViewModel : ViewModel() {
    val requestStorage = mutableStateOf(false)
    val pendingPatchedApp = mutableStateOf<NeoPackageManager.AppInfo?>(null)
    val pendingPatchedType = mutableStateOf(NeoPackageManager.PatchedType.NONE)
    val isExtracting = mutableStateOf(false)
    val missingOriginalDialog = mutableStateOf<NeoPackageManager.AppInfo?>(null)
    val packageMismatchDialog = mutableStateOf<Pair<NeoPackageManager.AppInfo, NeoPackageManager.ExtractResult.PackageMismatch>?>(null)
}
