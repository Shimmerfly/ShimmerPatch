package moe.shimmerfly.shimmerpatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import moe.shimmerfly.shimmerpatch.ui.component.m3.SettingsDialog
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private class LoadingDialogLabel(var value: String)

@Composable
fun LoadingDialog(show: MutableState<Boolean> = mutableStateOf(true), title: String = "") {
    LoadingDialog(visible = show.value, title = title)
}

/** Progress content follows InstallerX-Revived's InstallingDialog / PreparingDialog. */
@Composable
fun LoadingDialog(visible: Boolean, title: String) {
    val lastVisibleTitle = remember { LoadingDialogLabel(title) }
    if (visible) lastVisibleTitle.value = title
    SettingsDialog(
        show = visible,
        title = lastVisibleTitle.value,
        onDismissRequest = {},
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        dismissButton = {},
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {},
    )
}
