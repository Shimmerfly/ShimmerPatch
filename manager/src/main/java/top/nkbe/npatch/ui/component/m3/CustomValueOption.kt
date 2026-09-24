package top.nkbe.npatch.ui.component.m3

import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun CustomValueOption(
    title: String,
    value: String,
    selected: Boolean,
    selectionEnabled: Boolean = value.isNotBlank(),
    onEdit: () -> Unit,
    onSelect: () -> Unit,
) {
    BaseWidget(
        title = title,
        description = value.takeIf { it.isNotBlank() },
        trailingDivider = true,
        onClick = onEdit,
    ) { interactionSource ->
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = selectionEnabled,
            modifier = Modifier.semantics { contentDescription = title },
            interactionSource = interactionSource,
        )
    }
}
