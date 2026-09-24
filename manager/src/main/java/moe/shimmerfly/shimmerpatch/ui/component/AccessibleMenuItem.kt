package moe.shimmerfly.shimmerpatch.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/** Ordinary actions never advertise selection state; only explicit choices do. */
@Composable
fun AccessibleMenuItem(
    text: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    selected: Boolean? = null,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = modifier.then(
            if (selected == null) Modifier else Modifier.semantics { this.selected = selected }
        ),
        text = {
            Column {
                Text(text)
                if (!summary.isNullOrEmpty()) {
                    Text(summary, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        trailingIcon = if (selected == true) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else null,
        onClick = onClick,
    )
}
