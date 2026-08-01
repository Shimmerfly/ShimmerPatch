package top.nkbe.npatch.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PanelHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    search: (@Composable () -> Unit)? = null,
    titleOverlay: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().height(PANEL_HEADER_HEIGHT)) {
        if (titleOverlay != null) {
            Box(Modifier.fillMaxWidth().height(TitleRow + DescriptionRow)) { titleOverlay() }
        } else {
            Row(
                Modifier.fillMaxWidth().height(TitleRow).padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                actions?.invoke(this)
            }
            Box(
                Modifier.fillMaxWidth().height(DescriptionRow).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) { description?.invoke() }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) { search?.invoke() }
    }
}

private val TitleRow = 56.dp
private val DescriptionRow = 26.dp
val PANEL_HEADER_HEIGHT = TitleRow + DescriptionRow + 68.dp

/** Vector's large title row for a top-level page that does not own a search field. */
@Composable
fun VectorPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions?.invoke(this)
    }
}
