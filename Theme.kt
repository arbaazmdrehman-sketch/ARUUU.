package com.aruuu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aruuu.app.R

// ═══════════════════════════════════════════════════════════════════════════
// Color palette — Deep navy vault aesthetic + electric cyan accent
// ═══════════════════════════════════════════════════════════════════════════

object ARUUUColors {
    // ── Dark palette ──
    val NavyDeep        = Color(0xFF060B14)
    val NavyMid         = Color(0xFF0D1526)
    val NavySurface     = Color(0xFF111E33)
    val NavyCard        = Color(0xFF172440)
    val NavyBorder      = Color(0xFF1F3050)
    val CyanPrimary     = Color(0xFF00D4FF)
    val CyanSecondary   = Color(0xFF0099CC)
    val CyanGlow        = Color(0x4000D4FF)
    val CyanDim         = Color(0xFF004E66)

    // ── Light palette ──
    val LightBg         = Color(0xFFF0F6FF)
    val LightSurface    = Color(0xFFFFFFFF)
    val LightCard       = Color(0xFFF5F9FF)
    val LightBorder     = Color(0xFFDEEBFF)
    val LightText       = Color(0xFF0A1628)
    val LightSubtext    = Color(0xFF4A6080)

    // ── Semantic ──
    val Success         = Color(0xFF00E676)
    val Warning         = Color(0xFFFFAB40)
    val Danger          = Color(0xFFFF5252)
    val Locked          = Color(0xFFFF6B6B)
    val Unlocked        = Color(0xFF69F0AE)
    val Purple          = Color(0xFFBB86FC)

    // ── Text on dark ──
    val TextPrimary     = Color(0xFFE8F0FE)
    val TextSecondary   = Color(0xFF8A9AB5)
    val TextMuted       = Color(0xFF3A4A60)
}

// ═══════════════════════════════════════════════════════════════════════════
// Material 3 Color Schemes
// ═══════════════════════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    primary             = ARUUUColors.CyanPrimary,
    onPrimary           = ARUUUColors.NavyDeep,
    primaryContainer    = ARUUUColors.CyanDim,
    onPrimaryContainer  = ARUUUColors.CyanPrimary,
    secondary           = ARUUUColors.CyanSecondary,
    onSecondary         = ARUUUColors.NavyDeep,
    secondaryContainer  = Color(0xFF003340),
    onSecondaryContainer= ARUUUColors.CyanPrimary,
    tertiary            = ARUUUColors.Purple,
    background          = ARUUUColors.NavyDeep,
    onBackground        = ARUUUColors.TextPrimary,
    surface             = ARUUUColors.NavySurface,
    onSurface           = ARUUUColors.TextPrimary,
    surfaceVariant      = ARUUUColors.NavyCard,
    onSurfaceVariant    = ARUUUColors.TextSecondary,
    outline             = ARUUUColors.NavyBorder,
    error               = ARUUUColors.Danger,
    onError             = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary             = ARUUUColors.CyanSecondary,
    onPrimary           = Color.White,
    primaryContainer    = ARUUUColors.LightBorder,
    onPrimaryContainer  = Color(0xFF003347),
    secondary           = Color(0xFF007A99),
    onSecondary         = Color.White,
    background          = ARUUUColors.LightBg,
    onBackground        = ARUUUColors.LightText,
    surface             = ARUUUColors.LightSurface,
    onSurface           = ARUUUColors.LightText,
    surfaceVariant      = ARUUUColors.LightCard,
    onSurfaceVariant    = ARUUUColors.LightSubtext,
    outline             = ARUUUColors.LightBorder,
    error               = ARUUUColors.Danger,
    onError             = Color.White,
)

// ═══════════════════════════════════════════════════════════════════════════
// Typography — Space Grotesk display + Inter body
// ═══════════════════════════════════════════════════════════════════════════

// Note: add font files to res/font/ in the real project.
// Here we fall back to system sans-serif.
val ARUUUTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1.5).sp),
    displaySmall  = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 20.sp, lineHeight = 24.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 18.sp, lineHeight = 22.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, lineHeight = 20.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 14.sp, lineHeight = 18.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 13.sp, lineHeight = 17.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 12.sp, lineHeight = 15.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)

// ═══════════════════════════════════════════════════════════════════════════
// Shape overrides
// ═══════════════════════════════════════════════════════════════════════════

val ARUUUShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

// ═══════════════════════════════════════════════════════════════════════════
// ARUUUTheme composable
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ARUUUTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = ARUUUTypography,
        shapes      = ARUUUShapes,
        content     = content,
    )
}
