// Adapted from WeKit: ui/content/WeKitBasicDialog.kt.
// Dialog spacing, surface and scrolling are copied from that implementation.
package top.nkbe.npatch.ui.component.m3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@android.annotation.SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SettingsDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    scrollable: Boolean = true,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable () -> Unit = {
        TextButton(onClick = onDismissRequest) { Text(stringResource(android.R.string.cancel)) }
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!show) return

    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .heightIn(max = maxHeight),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier.weight(1f, fill = false).then(
                        if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
                Spacer(Modifier.height(24.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun SettingsErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
