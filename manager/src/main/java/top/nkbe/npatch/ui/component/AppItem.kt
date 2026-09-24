// SPDX-License-Identifier: GPL-3.0-only
// Adapted from InstallerX-Revived ui/page/main/settings/config/apply/ApplyItemWidget.kt.
package top.nkbe.npatch.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.nkbe.npatch.ui.util.backgroundAwareCardColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppItem(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    packageName: String,
    labelTrailingContent: (@Composable RowScope.() -> Unit)? = null,
    summaryRow: (@Composable RowScope.() -> Unit)? = null,
    topRightContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    description: String = "",
    warningText: String? = null,
    isEnabled: Boolean = true,
    cardColors: CardColors = backgroundAwareCardColors(),
    shape: Shape = RoundedCornerShape(16.dp),
    checked: Boolean? = null,
    onClick: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
) {
    var descriptionExpanded by rememberSaveable(packageName, description) { mutableStateOf(false) }
    var descriptionOverflows by remember(packageName, description) { mutableStateOf(false) }
    val interactionModifier = if (checked == null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress, role = Role.Button)
    } else {
        Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardColors.containerColor, shape)
            .clip(shape)
            .then(interactionModifier)
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).alpha(if (isEnabled) 1f else 0.45f)) { icon() }
            Column(
                modifier = Modifier.weight(1f).alpha(if (isEnabled) 1f else 0.45f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = cardColors.contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    labelTrailingContent?.invoke(this)
                }
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summaryRow != null || topRightContent != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        summaryRow?.let { summary ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                content = summary,
                            )
                        }
                        topRightContent?.invoke()
                    }
                }
            }
            trailingContent?.invoke()
        }
        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (!descriptionExpanded) descriptionOverflows = result.hasVisualOverflow
                },
                modifier = Modifier.fillMaxWidth().then(
                    if (descriptionOverflows || descriptionExpanded) {
                        Modifier.clickable { descriptionExpanded = !descriptionExpanded }
                    } else {
                        Modifier
                    }
                ),
            )
        }
        warningText?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
