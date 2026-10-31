package com.matchora.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.ui.components.SportsFlowBackground
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import matchora.composeapp.generated.resources.Res
import matchora.composeapp.generated.resources.splash_icon
import org.jetbrains.compose.resources.painterResource

// ═══════════════════════════════════════════════════════════════════════════════
// BIÇAK KESİĞİ (KNIFE SLICE) SHAPE
// Ekranı çapraz bir bıçak kesiği çizgisiyle (üst sağdan alt sola) ikiye böler:
// Sol Parça (isLeftHalf = true) ve Sağ Parça (isLeftHalf = false)
// ═══════════════════════════════════════════════════════════════════════════════
private const val TOP_CUT_RATIO = 0.65f
private const val BOTTOM_CUT_RATIO = 0.35f

class KnifeSliceShape(
    private val isLeftHalf: Boolean,
    private val topRatio: Float = TOP_CUT_RATIO,
    private val bottomRatio: Float = BOTTOM_CUT_RATIO
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val topCutX = size.width * topRatio
        val bottomCutX = size.width * bottomRatio
        val path = Path().apply {
            if (isLeftHalf) {
                moveTo(0f, 0f)
                lineTo(topCutX, 0f)
                lineTo(bottomCutX, size.height)
                lineTo(0f, size.height)
                close()
            } else {
                moveTo(topCutX, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(bottomCutX, size.height)
                close()
            }
        }
        return Outline.Generic(path)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MATCHORA SPLASH EKRANI
// • Canlı akan SportsFlowBackground (spor emojileri, tenis kortu, kale, pota)
// • 360° kesintisiz dönen ve nabız gibi atan App Icon
// • MATCHORA başlığı + canlı yön arayan animasyonlu Pusula simgesi
// • Bıçakla kesilmiş gibi sağa ve sola ayrılarak açılan ultra dinamik kapanış
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MatchoraSplashScreen(
    modifier: Modifier = Modifier,
    displayDurationMillis: Long = 2000L,
    onSplashFinished: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSlicing by remember { mutableStateOf(false) }

    // Animasyon durumları
    val sliceProgress = remember { Animatable(0f) }
    val slashFlashAlpha = remember { Animatable(0f) }
    val entryScale = remember { Animatable(0.85f) }

    // Sonsuz geçiş animasyonları (İkon dönüşü, pusula salınımı, ışık nabzı)
    val infiniteTransition = rememberInfiniteTransition(label = "SplashLoop")

    // İkonun kesintisiz 360 derece akıcı dönüşü
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "IconRotation"
    )

    // Pusula iğnesinin canlı yön arama salınımı
    val compassRotation by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CompassRotation"
    )

    // İkonun hafif nabız gibi büyüme küçülmesi
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Halka genişleme efekti
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuraScale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuraAlpha"
    )

    // Bıçak kesiği başlatma fonksiyonu
    fun startKnifeSlice() {
        if (isSlicing) return
        isSlicing = true
        coroutineScope.launch {
            // 1. Aşama: Bıçağın lazer gibi parlama efekti (Slash Flash)
            launch {
                slashFlashAlpha.animateTo(1f, tween(100, easing = LinearEasing))
                slashFlashAlpha.animateTo(0f, tween(180, easing = FastOutLinearInEasing))
            }
            delay(40)
            // 2. Aşama: Ekranın bıçak kesiği hattından sağa ve sola ayrılması
            sliceProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 620,
                    easing = CubicBezierEasing(0.18f, 1.0f, 0.28f, 1.0f)
                )
            )
            onSplashFinished()
        }
    }

    // Başlangıç yaylanma girişi ve otomatik zamanlayıcı
    LaunchedEffect(Unit) {
        launch {
            entryScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        delay(displayDurationMillis)
        startKnifeSlice()
    }

    val leftSliceShape = remember { KnifeSliceShape(isLeftHalf = true) }
    val rightSliceShape = remember { KnifeSliceShape(isLeftHalf = false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Kullanıcı ekrana dokunursa beklemeden bıçak kesiğiyle açılır
                startKnifeSlice()
            }
    ) {
        if (!isSlicing) {
            // Kesilmeden önceki tek parça pürüzsüz splash görünümü
            SplashContent(
                iconRotation = iconRotation,
                compassRotation = compassRotation,
                pulseScale = pulseScale,
                auraScale = auraScale,
                auraAlpha = auraAlpha,
                entryScale = entryScale.value
            )
        } else {
            // ═══════════════════════════════════════════════════════════════════
            // BIÇAKLA KESİLMİŞ ÇİFT PARÇA (SOL VE SAĞ AYRILMA KATMANLARI)
            // ═══════════════════════════════════════════════════════════════════
            val progress = sliceProgress.value

            // 1) SOL KESİLMİŞ PARÇA (Sola ve hafif yukarı doğru fırlar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -size.width * progress * 1.35f
                        translationY = -size.height * 0.12f * progress
                        rotationZ = -4.2f * progress
                        clip = true
                        shape = leftSliceShape
                    }
            ) {
                SplashContent(
                    iconRotation = iconRotation,
                    compassRotation = compassRotation,
                    pulseScale = pulseScale,
                    auraScale = auraScale,
                    auraAlpha = auraAlpha,
                    entryScale = 1f
                )

                // Kesik kenarındaki parlak turuncu-beyaz bıçak hattı (Sol taraf)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val topCutX = size.width * TOP_CUT_RATIO
                    val bottomCutX = size.width * BOTTOM_CUT_RATIO
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF5500),
                                Color.White,
                                Color(0xFFFF8833),
                                Color(0xFFFF3300)
                            ),
                            start = Offset(topCutX, 0f),
                            end = Offset(bottomCutX, size.height)
                        ),
                        start = Offset(topCutX, 0f),
                        end = Offset(bottomCutX, size.height),
                        strokeWidth = 3.5f * density,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2) SAĞ KESİLMİŞ PARÇA (Sağa ve hafif aşağı doğru fırlar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = size.width * progress * 1.35f
                        translationY = size.height * 0.12f * progress
                        rotationZ = 4.2f * progress
                        clip = true
                        shape = rightSliceShape
                    }
            ) {
                SplashContent(
                    iconRotation = iconRotation,
                    compassRotation = compassRotation,
                    pulseScale = pulseScale,
                    auraScale = auraScale,
                    auraAlpha = auraAlpha,
                    entryScale = 1f
                )

                // Kesik kenarındaki parlak turuncu-beyaz bıçak hattı (Sağ taraf)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val topCutX = size.width * TOP_CUT_RATIO
                    val bottomCutX = size.width * BOTTOM_CUT_RATIO
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF3300),
                                Color.White,
                                Color(0xFFFF8833),
                                Color(0xFFFF5500)
                            ),
                            start = Offset(topCutX, 0f),
                            end = Offset(bottomCutX, size.height)
                        ),
                        start = Offset(topCutX, 0f),
                        end = Offset(bottomCutX, size.height),
                        strokeWidth = 3.5f * density,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3) BIÇAK KESİĞİ PARILTI VE LAZER IŞIK PATLAMASI EFEKTİ (Laser Slash Streak)
            if (slashFlashAlpha.value > 0.01f) {
                val alpha = slashFlashAlpha.value
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val topCutX = size.width * TOP_CUT_RATIO
                    val bottomCutX = size.width * BOTTOM_CUT_RATIO

                    // Geniş turuncu enerji parıltısı
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCCFF4D00),
                                Color(0xEEFFFFFF),
                                Color(0xCCFF7A33),
                                Color.Transparent
                            )
                        ),
                        start = Offset(topCutX + 45f, -30f),
                        end = Offset(bottomCutX - 45f, size.height + 30f),
                        strokeWidth = 28f * density * alpha,
                        cap = StrokeCap.Round
                    )

                    // Keskin beyaz lazer çekirdeği
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = Offset(topCutX, 0f),
                        end = Offset(bottomCutX, size.height),
                        strokeWidth = 6f * density * alpha,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SPLASH İÇERİK BİLEŞENİ
// Canlı akan arka plan + 360° Dönen App Icon + MATCHORA + Canlı Pusula Rozeti
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SplashContent(
    iconRotation: Float,
    compassRotation: Float,
    pulseScale: Float,
    auraScale: Float,
    auraAlpha: Float,
    entryScale: Float
) {
    // Canlı akan SportsFlowBackground (kullanıcının istediği canlı hareketli spor zemini)
    SportsFlowBackground(glyphCount = 68, protectCenter = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(entryScale),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ═══════════ 1. MERKEZİ DÖNEN APP ICON BÖLÜMÜ ═══════════
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Dışa doğru genişleyen parıltı aurası
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .scale(auraScale)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = Color(0xFFFF5500).copy(alpha = auraAlpha),
                                shape = CircleShape
                            )
                    )

                    // Sıcak turuncu enerji aurası
                    Canvas(modifier = Modifier.size(170.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x44FF4D00),
                                    Color(0x18FF7A33),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 2f
                            ),
                            radius = (size.minDimension / 2f) * pulseScale
                        )
                    }

                    // Dönen İnce Enerji Halkası (Ters yönde hafif salınım)
                    Canvas(
                        modifier = Modifier
                            .size(142.dp)
                            .rotate(-iconRotation * 0.45f)
                    ) {
                        drawCircle(
                            color = Color(0x35FF6600),
                            radius = size.minDimension / 2f,
                            style = Stroke(
                                width = 1.5f * density,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // ═══════════ APP ICON (360° KESİNTİSİZ DÖNÜŞ) ═══════════
                    // Beyaz kare çizgi hiçbir yere temas etmeden, hava içinde serbestçe süzülen 3D ikon
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .scale(pulseScale)
                            .rotate(iconRotation),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            // Dedicated transparent artwork: app_icon.png is flattened (opaque
                            // corners) for store use and shows an orange square in release builds.
                            painter = painterResource(Res.drawable.splash_icon),
                            contentDescription = "Matchora App Icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // ═══════════ 2. MATCHORA BAŞLIĞI ═══════════
                Text(
                    text = "MATCHORA",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = MColors.Ink900
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ═══════════ 3. PUSULA SİMGESİ VE ETİKET ROZETİ ═══════════
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, Color(0x33FF4D00)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Canlı yön arayan pusula simgesi (salınım animasyonlu)
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = "Pusula Simgesi",
                            tint = MColors.Brand,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(compassRotation)
                        )

                        Text(
                            text = "SPORUN PUSULASI",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = MColors.Ink800
                        )

                        // Canlı hazır sinyali (Mint yeşili)
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MColors.Mint)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(44.dp))

                // Alt minik yükleniyor / senkronizasyon çizgisi
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0x18FF4D00))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.55f)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFF7A33),
                                        Color(0xFFFF3D00)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
