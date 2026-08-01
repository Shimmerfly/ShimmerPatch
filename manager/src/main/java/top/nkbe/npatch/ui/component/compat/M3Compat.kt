package top.nkbe.npatch.ui.component.compat

import androidx.compose.material.icons.automirrored.rounded.*

import androidx.compose.material.icons.rounded.*

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.nkbe.npatch.ui.component.NPatchTopAppBar

@Composable
fun Card(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    insideMargin: PaddingValues = PaddingValues(0.dp),
    showIndication: Boolean = true,
    pressFeedbackType: Any? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongPress: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactive = if (onClick != null) {
        modifier.combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongPress)
    } else modifier
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        Column(
            modifier = interactive.clip(shape).padding(insideMargin),
            content = content,
        )
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    insideMargin: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit = { text?.let { Text(it) } },
) = androidx.compose.material3.TextButton(onClick, modifier, enabled, colors = colors, contentPadding = insideMargin) {
    content()
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    insideMargin: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit = { text?.let { Text(it) } },
) = androidx.compose.material3.Button(onClick, modifier, enabled, colors = colors, contentPadding = insideMargin) {
    content()
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) = androidx.compose.material3.OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    label = label.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
    enabled = enabled,
    readOnly = readOnly,
    interactionSource = interactionSource,
    singleLine = singleLine,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
)

@Composable
fun InputField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onSearch: (String) -> Unit = {},
    enabled: Boolean = true,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
) = androidx.compose.material3.TextField(
    value = query,
    onValueChange = { onQueryChange(it); onExpandedChange(true) },
    modifier = modifier,
    placeholder = label.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    enabled = enabled,
    singleLine = true,
    shape = RoundedCornerShape(28.dp),
)

@Composable
fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) = NPatchTopAppBar(title, modifier, color, navigationIcon = navigationIcon, actions = actions, scrollBehavior = scrollBehavior)

@Composable
fun SmallTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun OverlayDialog(
    title: String,
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String = "",
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    summaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    insideMargin: DpSize = DpSize(24.dp, 20.dp),
    renderInRootScaffold: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = {
            Column {
                Text(title, color = titleColor)
                if (summary.isNotEmpty()) Text(summary, color = summaryColor, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = content,
        confirmButton = {},
        containerColor = backgroundColor,
    )
}

@Composable
private fun PreferenceRow(
    title: String,
    summary: String? = null,
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    insideMargin: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
) {
    Surface(onClick = onClick ?: {}, enabled = onClick != null, color = Color.Transparent, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(insideMargin), verticalAlignment = Alignment.CenterVertically) {
            if (startAction != null) { startAction(); Spacer(Modifier.width(14.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (!summary.isNullOrEmpty()) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            endAction?.invoke()
        }
    }
}

@Composable
fun ArrowPreference(
    title: String,
    summary: String? = null,
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    startAction: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) = PreferenceRow(title, summary, modifier, startAction, {
    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}, onClick, insideMargin)

@Composable
fun SwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    startAction: @Composable (() -> Unit)? = null,
) = PreferenceRow(title, summary, modifier, startAction, {
    Switch(checked, onCheckedChange = null, enabled = enabled)
}, if (enabled) ({ onCheckedChange(!checked) }) else null)

@Composable
fun BasicComponent(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) = PreferenceRow(title, summary, modifier, startAction, endActions, onClick)

data class DropdownItem(
    val text: String,
    val summary: String? = null,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)
data class DropdownEntry(val items: List<DropdownItem>)

@Composable
fun OverlayDropdownPreference(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    items: List<String> = emptyList(),
    selectedIndex: Int = 0,
    startAction: @Composable (() -> Unit)? = null,
    onSelectedIndexChange: (Int) -> Unit = {},
    entries: List<DropdownEntry> = emptyList(),
) {
    var expanded by remember { mutableStateOf(false) }
    val flatItems = if (entries.isEmpty()) items.mapIndexed { index, text ->
        DropdownItem(text, selected = index == selectedIndex) { onSelectedIndexChange(index) }
    } else entries.flatMap { it.items }
    Box {
        PreferenceRow(title, summary ?: flatItems.firstOrNull { it.selected }?.text, modifier, startAction, onClick = { expanded = true })
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            flatItems.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(item.text)
                            item.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    },
                    onClick = { item.onClick(); expanded = false },
                    trailingIcon = if (item.selected) ({ Icon(Icons.Rounded.Check, null) }) else null,
                )
            }
        }
    }
}

@Composable
fun TabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: Any? = null,
    height: androidx.compose.ui.unit.Dp = 48.dp,
) {
    androidx.compose.material3.PrimaryTabRow(selectedTabIndex = selectedTabIndex, modifier = modifier.height(height)) {
        tabs.forEachIndexed { index, label ->
            Tab(
                selected = index == selectedTabIndex,
                onClick = { onTabSelected(index) },
                text = { Text(label, maxLines = 1) },
            )
        }
    }
}

object PopupPositionProvider {
    enum class Align { End, TopEnd }
}

@Composable
fun OverlayListPopup(
    show: Boolean,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.End,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    DropdownMenu(expanded = show, onDismissRequest = onDismissRequest) { content() }
}

@Composable
fun ListPopupColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(content = content)
}

@Composable
fun PopupHost() = Unit
