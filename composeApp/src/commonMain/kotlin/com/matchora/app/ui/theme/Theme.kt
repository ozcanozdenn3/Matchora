package com.matchora.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════════
// MATCHORA DESIGN SYSTEM — LIGHT ONLY
// Kırık beyaz zemin, mürekkep tonlu tipografi, enerji turuncusu tek baskın aksiyon rengi.
// Yardımcı renkler yalnızca anlam taşır: mint=güven, lila=kimya, mavi=bilgi,
// amber=uyarı, gül=risk, altın=statü.
// ═══════════════════════════════════════════════════════════════════════════════
object MColors {
    // Brand
    val Brand = Color(0xFFFF4D00)
    val BrandLight = Color(0xFFFF7A33)
    val BrandDeep = Color(0xFFBC3909)
    val BrandTint = Color(0xFFFFF1EA)
    val BrandTintStrong = Color(0xFFFFE2D3)

    // Neutrals
    val Canvas = Color(0xFFFFFCF8)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF6F7F9)
    val SurfaceSunken = Color(0xFFF0F2F5)
    val Line = Color(0xFFE8EBF1)
    val LineSoft = Color(0xFFF0F2F6)

    val Ink900 = Color(0xFF0B1220)
    val Ink800 = Color(0xFF1A2333)
    val Ink700 = Color(0xFF2E3850)
    val Ink600 = Color(0xFF475467)
    val Ink500 = Color(0xFF5E6A82)
    val Ink400 = Color(0xFF69758B)
    val Ink300 = Color(0xFFB7BECC)
    val Ink200 = Color(0xFFD5DAE3)

    // Semantic
    val Mint = Color(0xFF0FA968)
    val MintTint = Color(0xFFE6F7EF)
    val Lilac = Color(0xFF7457F5)
    val LilacTint = Color(0xFFF0ECFF)
    val Sky = Color(0xFF2A7FF0)
    val SkyTint = Color(0xFFE8F1FE)
    val Amber = Color(0xFFE88A00)
    val AmberTint = Color(0xFFFFF4E0)
    val Rose = Color(0xFFE5484D)
    val RoseTint = Color(0xFFFDECEC)
    val Gold = Color(0xFFD9A21B)
    val GoldTint = Color(0xFFFFF6DC)

    // Background flow glyphs
    val Flow = Color(0xFF9AA2B1)
}

object MGradients {
    val Brand = Brush.linearGradient(listOf(MColors.BrandLight, MColors.Brand, MColors.BrandDeep))
    val BrandSoft = Brush.linearGradient(listOf(Color(0xFFFFF4EE), Color(0xFFFFE6D9)))
    val Ink = Brush.linearGradient(listOf(Color(0xFF1B2436), Color(0xFF0B1220)))
    val Lilac = Brush.linearGradient(listOf(Color(0xFF8B73FF), Color(0xFF6243EE)))
    val Mint = Brush.linearGradient(listOf(Color(0xFF2CC584), Color(0xFF0B9A5D)))
    val Gold = Brush.linearGradient(listOf(Color(0xFFF5C84B), Color(0xFFD39812)))
    val Sky = Brush.linearGradient(listOf(Color(0xFF5BA2FF), Color(0xFF2468E0)))
}

private val MatchoraColorScheme = lightColorScheme(
    primary = MColors.BrandDeep,
    onPrimary = Color.White,
    primaryContainer = MColors.BrandTint,
    onPrimaryContainer = MColors.BrandDeep,
    secondary = MColors.Ink900,
    onSecondary = Color.White,
    background = MColors.Canvas,
    onBackground = MColors.Ink900,
    surface = MColors.Surface,
    onSurface = MColors.Ink900,
    surfaceVariant = MColors.SurfaceMuted,
    onSurfaceVariant = MColors.Ink500,
    surfaceContainer = MColors.Surface,
    surfaceContainerHigh = MColors.Surface,
    surfaceContainerLow = MColors.Surface,
    outline = MColors.Line,
    outlineVariant = MColors.LineSoft,
    error = MColors.Rose,
    onError = Color.White
)

private val MatchoraTypography = Typography(
    displayLarge = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    titleLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.15).sp),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp)
)

@Composable
fun MatchoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MatchoraColorScheme,
        typography = MatchoraTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
