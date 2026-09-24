package moe.shimmerfly.shimmerpatch.ui.util

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class SampleStringProvider : PreviewParameterProvider<String> {
    override val values: Sequence<String> = sequenceOf("Hello", "World")
}
