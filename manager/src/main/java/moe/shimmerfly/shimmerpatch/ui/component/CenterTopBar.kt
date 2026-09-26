package moe.shimmerfly.shimmerpatch.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import moe.shimmerfly.shimmerpatch.ui.util.SampleStringProvider

@Preview
@Composable
fun CenterTopBar(@PreviewParameter(SampleStringProvider::class, 1) text: String) {
    ShimmerPatchTopAppBar(
        title = text
    )
}
