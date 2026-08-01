package top.nkbe.npatch.ui.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.nkbe.npatch.ui.util.backgroundAwareCardColors

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
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    var descriptionExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .alpha(if (isEnabled) 1f else 0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (isEnabled) 1f else 0.45f)
                        .align(Alignment.CenterVertically)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight(600),
                            color = when {
                                warningText != null -> MaterialTheme.colorScheme.error
                                isEnabled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                        labelTrailingContent?.invoke(this)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = packageName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight(500),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )

                    if (summaryRow != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            content = summaryRow
                        )
                    }
                }

                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                        trailingContent()
                    }
                }

                if (topRightContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.align(Alignment.Top)) {
                        topRightContent()
                    }
                }
            }

            if (description.isNotEmpty() || warningText != null) {
                Spacer(modifier = Modifier.height(4.dp))

                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight(500),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { descriptionExpanded = !descriptionExpanded }
                    )
                }

                if (warningText != null) {
                    if (description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = warningText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight(550),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp, end = 20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
