package top.nkbe.npatch.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import top.nkbe.npatch.ui.component.compat.OverlayDialog

/**
 * Vector-styled alert dialog. Delegates to [OverlayDialog] with Material 3 [AlertDialog] fallback
 * for complex custom content scenarios.
 */
@Composable
fun VectorAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = confirmButton,
    modifier = modifier,
    dismissButton = dismissButton,
    icon = icon,
    title = title,
    text = text,
    properties = properties,
)
