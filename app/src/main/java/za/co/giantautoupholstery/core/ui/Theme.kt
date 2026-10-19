package za.co.giantautoupholstery.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Central brand tokens: warm leather-workshop identity.
object Brand {
    val Rust = Color(0xFF7A2A1C)
    val Amber = Color(0xFFE0A23B)
    val AmberDark = Color(0xFF8A5A00) // amber with enough contrast on light backgrounds
    val OffWhite = Color(0xFFFBF6EF)
    val Ink = Color(0xFF2B2320)
}

object Spacing {
    val xs = 4.dp; val s = 8.dp; val m = 16.dp; val l = 24.dp; val xl = 32.dp
    val screen = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
}

private val Light = lightColorScheme(
    primary = Brand.Rust, onPrimary = Color.White,
    primaryContainer = Color(0xFFF4DCD4), onPrimaryContainer = Color(0xFF3A0F08),
    secondary = Brand.AmberDark, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3B0), onSecondaryContainer = Color(0xFF3B2600),
    background = Brand.OffWhite, onBackground = Brand.Ink,
    surface = Brand.OffWhite, onSurface = Brand.Ink,
    surfaceVariant = Color(0xFFF1E6DA), onSurfaceVariant = Color(0xFF52443D),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB4A2), onPrimary = Color(0xFF4A1409),
    primaryContainer = Color(0xFF6A2417), onPrimaryContainer = Color(0xFFFFDAD1),
    secondary = Brand.Amber, onSecondary = Color(0xFF3B2600),
    secondaryContainer = Color(0xFF5A3C00), onSecondaryContainer = Color(0xFFFFDEA6),
    background = Color(0xFF1C1512), onBackground = Color(0xFFEDE0DA),
    surface = Color(0xFF1C1512), onSurface = Color(0xFFEDE0DA),
    surfaceVariant = Color(0xFF3A2F2A), onSurfaceVariant = Color(0xFFD7C2B9),
    error = Color(0xFFF2B8B5),
)

// Fraunces/Manrope are not bundled (font files must be licensed and added under res/font).
// Documented native fallbacks: Serif for headings, default sans for body.
private val Heading = FontFamily.Serif
private val Base = Typography()
private val AppTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontFamily = Heading, fontWeight = FontWeight.Bold),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Heading, fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Heading, fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Heading, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontFamily = Heading, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(lineHeight = 24.sp),
)

@Composable
fun GiantTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = AppTypography, content = content)
}
