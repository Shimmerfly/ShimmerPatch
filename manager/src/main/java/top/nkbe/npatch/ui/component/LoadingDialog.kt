package top.nkbe.npatch.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LoadingDialog(
    show: MutableState<Boolean> = mutableStateOf(true),
    title: String = ""
) {
    if (!show.value) return
    AlertDialog(
        onDismissRequest = {},
        title = title.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
        confirmButton = {},
        text = {
            Box(
                modifier = Modifier.padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
        },
    )
}

@Preview
@Composable
private fun LoadingDialogPreview() {
    LoadingDialog()
}
