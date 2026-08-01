package top.nkbe.npatch.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/** Generates a complete Material 3 tonal scheme from a single seed in perceptual CIE LCh. */
object SeedScheme {
    const val DEFAULT_SEED: Int = 0xFF6ABFCF.toInt()

    fun of(seed: Int, dark: Boolean): ColorScheme {
        val (_, chroma, hue) = Color(seed).toLch()
        val primary = Ramp(hue, chroma.coerceAtLeast(48f))
        val secondary = Ramp(hue, 16f)
        val tertiary = Ramp(hue + 60f, 24f)
        val neutral = Ramp(hue, 4f)
        val variant = Ramp(hue, 8f)
        val error = Ramp(25f, 84f)

        return if (dark) darkColorScheme(
            primary = primary[80], onPrimary = primary[20],
            primaryContainer = primary[30], onPrimaryContainer = primary[90],
            inversePrimary = primary[40],
            secondary = secondary[80], onSecondary = secondary[20],
            secondaryContainer = secondary[30], onSecondaryContainer = secondary[90],
            tertiary = tertiary[80], onTertiary = tertiary[20],
            tertiaryContainer = tertiary[30], onTertiaryContainer = tertiary[90],
            error = error[80], onError = error[20],
            errorContainer = error[30], onErrorContainer = error[90],
            background = neutral[6], onBackground = neutral[90],
            surface = neutral[6], onSurface = neutral[90],
            surfaceVariant = variant[30], onSurfaceVariant = variant[80],
            surfaceTint = primary[80], inverseSurface = neutral[90],
            inverseOnSurface = neutral[20], outline = variant[60],
            outlineVariant = variant[30], surfaceBright = neutral[24],
            surfaceDim = neutral[6], surfaceContainerLowest = neutral[4],
            surfaceContainerLow = neutral[10], surfaceContainer = neutral[12],
            surfaceContainerHigh = neutral[17], surfaceContainerHighest = neutral[22],
        ) else lightColorScheme(
            primary = primary[40], onPrimary = primary[100],
            primaryContainer = primary[90], onPrimaryContainer = primary[10],
            inversePrimary = primary[80],
            secondary = secondary[40], onSecondary = secondary[100],
            secondaryContainer = secondary[90], onSecondaryContainer = secondary[10],
            tertiary = tertiary[40], onTertiary = tertiary[100],
            tertiaryContainer = tertiary[90], onTertiaryContainer = tertiary[10],
            error = error[40], onError = error[100],
            errorContainer = error[90], onErrorContainer = error[10],
            background = neutral[98], onBackground = neutral[10],
            surface = neutral[98], onSurface = neutral[10],
            surfaceVariant = variant[90], onSurfaceVariant = variant[30],
            surfaceTint = primary[40], inverseSurface = neutral[20],
            inverseOnSurface = neutral[95], outline = variant[50],
            outlineVariant = variant[80], surfaceBright = neutral[98],
            surfaceDim = neutral[87], surfaceContainerLowest = neutral[100],
            surfaceContainerLow = neutral[96], surfaceContainer = neutral[94],
            surfaceContainerHigh = neutral[92], surfaceContainerHighest = neutral[90],
        )
    }

    private class Ramp(private val hue: Float, private val chroma: Float) {
        private val cache = HashMap<Int, Color>()
        operator fun get(tone: Int) = cache.getOrPut(tone) { lchToColor(tone.toFloat(), chroma, hue) }
    }

    private const val WHITE_X = 95.047f
    private const val WHITE_Y = 100f
    private const val WHITE_Z = 108.883f

    private fun Color.toLch(): Triple<Float, Float, Float> {
        val r = linearize(red); val g = linearize(green); val b = linearize(blue)
        val x = (0.4124f * r + 0.3576f * g + 0.1805f * b) * 100f
        val y = (0.2126f * r + 0.7152f * g + 0.0722f * b) * 100f
        val z = (0.0193f * r + 0.1192f * g + 0.9505f * b) * 100f
        val fx = labF(x / WHITE_X); val fy = labF(y / WHITE_Y); val fz = labF(z / WHITE_Z)
        val l = 116f * fy - 16f; val a = 500f * (fx - fy); val bb = 200f * (fy - fz)
        var hue = Math.toDegrees(atan2(bb, a).toDouble()).toFloat()
        if (hue < 0f) hue += 360f
        return Triple(l, hypot(a, bb), hue)
    }

    private fun lchToColor(lightness: Float, chroma: Float, hueDegrees: Float): Color {
        val l = lightness.coerceIn(0f, 100f)
        val hue = Math.toRadians(hueDegrees.toDouble())
        val cosH = cos(hue).toFloat(); val sinH = sin(hue).toFloat()
        fun fits(c: Float): Boolean {
            val (r, g, b) = labToLinear(l, c * cosH, c * sinH)
            return r in -0.0001f..1.0001f && g in -0.0001f..1.0001f && b in -0.0001f..1.0001f
        }
        var low = 0f; var high = chroma
        if (fits(chroma)) low = chroma else repeat(12) {
            val mid = (low + high) / 2f
            if (fits(mid)) low = mid else high = mid
        }
        val (r, g, b) = labToLinear(l, low * cosH, low * sinH)
        return Color(delinearize(r), delinearize(g), delinearize(b))
    }

    private fun labToLinear(l: Float, a: Float, b: Float): Triple<Float, Float, Float> {
        val fy = (l + 16f) / 116f; val fx = fy + a / 500f; val fz = fy - b / 200f
        val x = labInverseF(fx) * WHITE_X / 100f
        val y = labInverseF(fy) * WHITE_Y / 100f
        val z = labInverseF(fz) * WHITE_Z / 100f
        return Triple(
            3.2406f * x - 1.5372f * y - 0.4986f * z,
            -0.9689f * x + 1.8758f * y + 0.0415f * z,
            0.0557f * x - 0.2040f * y + 1.0570f * z,
        )
    }

    private fun labF(t: Float) = if (t > 0.008856f) cbrt(t.toDouble()).toFloat() else 7.787f * t + 16f / 116f
    private fun labInverseF(t: Float): Float {
        val cube = t * t * t
        return if (cube > 0.008856f) cube else (t - 16f / 116f) / 7.787f
    }
    private fun linearize(c: Float) = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    private fun delinearize(value: Float): Float {
        val c = value.coerceIn(0f, 1f)
        return if (c <= 0.0031308f) c * 12.92f else (1.055f * c.toDouble().pow(1.0 / 2.4) - 0.055f).toFloat()
    }
}
