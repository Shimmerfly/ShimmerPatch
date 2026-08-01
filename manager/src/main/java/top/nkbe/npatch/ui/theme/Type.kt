package top.nkbe.npatch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

val VectorMono = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    letterSpacing = 0.sp,
    textDirection = TextDirection.Ltr,
)

val VectorLogLine = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.sp,
    textDirection = TextDirection.Ltr,
)

@Composable
fun sectionHeaderStyle(base: Typography): TextStyle = remember(base) {
    base.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
}
