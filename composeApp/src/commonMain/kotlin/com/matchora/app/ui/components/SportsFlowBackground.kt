package com.matchora.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import com.matchora.app.domain.Sport
import com.matchora.app.state.emoji
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.currentCoroutineContext
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// SPORTS FLOW BACKGROUND
// Hafif açık turuncu-beyaz zeminde canlı turuncu tonlu, yavaşça yukarı süzülen
// spor emojileri ve vektör çizgi ikonları (tenis kortu, futbol kalesi, basketbol potası).
// ═══════════════════════════════════════════════════════════════════════════════

private enum class GlyphKind { EMOJI, TENNIS_COURT, GOAL, HOOP, GLOVE, RACKET, DUMBBELL, GOGGLES, SHUTTLE, SKATE }

private data class FlowGlyph(
    val kind: GlyphKind,
    val emojiIndex: Int,
    val baseX: Float,
    val baseY: Float,
    val speed: Float,        // ekran yüksekliği / saniye
    val sizeScale: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val phase: Float,
    val baseRotation: Float,
    val spin: Float
)

private val FlowEmojis = Sport.entries.map { it.emoji }.distinct() + listOf("🏆", "👟", "🥅", "🧤")

@Composable
fun SportsFlowBackground(
    modifier: Modifier = Modifier.fillMaxSize(),
    glyphCount: Int = 42,
    protectCenter: Boolean = false,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val measurer = rememberTextMeasurer()
    val emojiLayouts: List<TextLayoutResult> = remember(measurer) {
        FlowEmojis.map { measurer.measure(AnnotatedString(it), TextStyle(fontSize = 30.sp)) }
    }

    val glyphs = remember(glyphCount) {
        val rnd = Random(20261017)
        List(glyphCount) { i ->
            // Alternate recognizable equipment outlines with every sport family.
            val kind = if (i % 2 == 0) GlyphKind.EMOJI else GlyphKind.entries[1 + (i / 2) % (GlyphKind.entries.size - 1)]
            FlowGlyph(
                kind = kind,
                emojiIndex = (i / 2) % FlowEmojis.size,
                baseX = ((i * 0.618034f) % 1f) * 0.94f + 0.03f,
                baseY = (i + rnd.nextFloat() * 0.6f) / glyphCount,
                speed = 0.022f + rnd.nextFloat() * 0.026f,
                sizeScale = 0.65f + rnd.nextFloat() * 0.65f,
                swayAmp = 12f + rnd.nextFloat() * 24f,
                swayFreq = 0.15f + rnd.nextFloat() * 0.25f,
                phase = rnd.nextFloat() * (2 * PI).toFloat(),
                baseRotation = -20f + rnd.nextFloat() * 40f,
                spin = (rnd.nextFloat() - 0.5f) * 10f
            )
        }.shuffled(Random(7))
    }

    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val motionScale = currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f
        if (motionScale == 0f) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> time.floatValue = (now - start) / 1_000_000_000f / motionScale }
        }
    }

    // Turuncu akan emojiler için sıcak filtre
    val orangeFlowPaint = remember {
        Paint().apply {
            colorFilter = ColorFilter.tint(Color(0xFFFF5500), BlendMode.SrcIn)
            alpha = 0.19f
        }
    }

    val bgGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFCF8),
                Color(0xFFFBFAF7),
                Color(0xFFF8F8F6)
            )
        )
    }

    Box(modifier = modifier.background(bgGradient)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val t = time.floatValue
            val w = size.width
            val h = size.height
            val widthFactor = (w / density / 400f).coerceIn(0.85f, 1.4f)
            val margin = 60f * density

            // Üst sağda sıcak güneş/enerji ışıltısı
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x28FF6600), Color.Transparent),
                    center = Offset(w * 0.90f, h * 0.05f),
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = Offset(w * 0.90f, h * 0.05f)
            )

            // Sol altta hafif sıcak turuncu derinlik
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1CFF8800), Color.Transparent),
                    center = Offset(w * 0.10f, h * 0.92f),
                    radius = w * 0.70f
                ),
                radius = w * 0.70f,
                center = Offset(w * 0.10f, h * 0.92f)
            )

            fun position(g: FlowGlyph): Triple<Float, Float, Float> {
                val raw = g.baseY + t * g.speed
                val p = raw - floor(raw)
                val y = h + margin - p * (h + margin * 2)
                val x = g.baseX * w + sin(t * g.swayFreq + g.phase) * g.swayAmp * density
                val rot = g.baseRotation + sin(t * 0.25f + g.phase) * 14f + t * g.spin
                return Triple(x, y, rot)
            }

            fun visibility(x: Float, y: Float): Float {
                if (!protectCenter) return 0.72f
                // Soft fade prevents the old abrupt disappearance around the logo.
                val distance = kotlin.math.hypot((x - w * 0.5f), (y - h * 0.46f) * 0.75f)
                val progress = ((distance / density - 90f) / 90f).coerceIn(0f, 1f)
                return 0.12f + 0.88f * progress * progress * (3f - 2f * progress)
            }

            // 1) Emoji katmanı — turuncu tonunda süzülen spor emojileri
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), orangeFlowPaint)
                glyphs.forEach { g ->
                    if (g.kind != GlyphKind.EMOJI) return@forEach
                    val (x, y, rot) = position(g)
                    // Merkezdeki logo ve ikon bölgesini temiz tut (çizgi temasını önler)

                    val layout = emojiLayouts[g.emojiIndex]
                    val s = g.sizeScale * widthFactor
                    translate(x, y) {
                        rotate(rot, pivot = Offset.Zero) {
                            scaleDraw(s) {
                                drawText(
                                    textLayoutResult = layout,
                                    alpha = visibility(x, y),
                                    topLeft = Offset(-layout.size.width / 2f, -layout.size.height / 2f)
                                )
                            }
                        }
                    }
                }
                canvas.restore()
            }

            // 2) Çizgi ikon katmanı — turuncu tenis kortu, kale, pota
            val stroke = 1.3f * density
            glyphs.forEach { g ->
                if (g.kind == GlyphKind.EMOJI) return@forEach
                val (x, y, rot) = position(g)
                // Merkezdeki app icon ve logo bölgesini temiz tut (çizgi temasını engeller)
                val lineColor = Color(0xFFED672E).copy(alpha = 0.22f * visibility(x, y))
                val s = g.sizeScale * widthFactor * density
                translate(x, y) {
                    rotate(rot * 0.6f, pivot = Offset.Zero) {
                        when (g.kind) {
                            GlyphKind.TENNIS_COURT -> drawTennisCourt(s, stroke, lineColor)
                            GlyphKind.GOAL -> drawGoal(s, stroke, lineColor)
                            GlyphKind.HOOP -> drawHoop(s, stroke, lineColor)
                            GlyphKind.GLOVE -> drawGoalkeeperGlove(s, stroke, lineColor)
                            GlyphKind.RACKET -> drawRacket(s, stroke, lineColor)
                            GlyphKind.DUMBBELL -> drawDumbbell(s, stroke, lineColor)
                            GlyphKind.GOGGLES -> drawGoggles(s, stroke, lineColor)
                            GlyphKind.SHUTTLE -> drawShuttle(s, stroke, lineColor)
                            GlyphKind.SKATE -> drawSkate(s, stroke, lineColor)
                            GlyphKind.EMOJI -> Unit
                        }
                    }
                }
            }
        }
        content()
    }
}

private inline fun DrawScope.scaleDraw(scale: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.scale(scale, scale, Offset.Zero)
    block()
    drawContext.transform.scale(1f / scale, 1f / scale, Offset.Zero)
}

private fun DrawScope.drawTennisCourt(s: Float, stroke: Float, color: Color) {
    val cw = 34f * s
    val ch = 54f * s
    val left = -cw / 2
    val top = -ch / 2
    val st = Stroke(width = stroke, cap = StrokeCap.Round)
    drawRoundRect(color, Offset(left, top), Size(cw, ch), CornerRadius(3f * s), style = st)
    // Tekler çizgileri
    val inset = cw * 0.14f
    drawLine(color, Offset(left + inset, top), Offset(left + inset, top + ch), stroke)
    drawLine(color, Offset(left + cw - inset, top), Offset(left + cw - inset, top + ch), stroke)
    // File
    drawLine(color, Offset(left - 3f * s, 0f), Offset(left + cw + 3f * s, 0f), stroke * 1.4f)
    // Servis çizgileri
    val sl = ch * 0.23f
    drawLine(color, Offset(left + inset, -sl), Offset(left + cw - inset, -sl), stroke)
    drawLine(color, Offset(left + inset, sl), Offset(left + cw - inset, sl), stroke)
    drawLine(color, Offset(0f, -sl), Offset(0f, sl), stroke)
}

private fun DrawScope.drawGoal(s: Float, stroke: Float, color: Color) {
    val gw = 52f * s
    val gh = 30f * s
    val left = -gw / 2
    val top = -gh / 2
    // Direkler ve üst direk
    drawLine(color, Offset(left, top + gh), Offset(left, top), stroke * 1.5f, StrokeCap.Round)
    drawLine(color, Offset(left, top), Offset(left + gw, top), stroke * 1.5f, StrokeCap.Round)
    drawLine(color, Offset(left + gw, top), Offset(left + gw, top + gh), stroke * 1.5f, StrokeCap.Round)
    // Ağ
    val net = color.copy(alpha = color.alpha * 0.6f)
    val cols = 6
    for (i in 1 until cols) {
        val x = left + gw * i / cols
        drawLine(net, Offset(x, top), Offset(x, top + gh), stroke * 0.6f)
    }
    for (j in 1..3) {
        val y = top + gh * j / 4
        drawLine(net, Offset(left, y), Offset(left + gw, y), stroke * 0.6f)
    }
    // Zemin
    drawLine(color, Offset(left - 6f * s, top + gh), Offset(left + gw + 6f * s, top + gh), stroke, StrokeCap.Round)
}

private fun DrawScope.drawHoop(s: Float, stroke: Float, color: Color) {
    val bw = 38f * s
    val bh = 26f * s
    val left = -bw / 2
    val top = -bh / 2 - 10f * s
    val st = Stroke(width = stroke, cap = StrokeCap.Round)
    // Panya
    drawRoundRect(color, Offset(left, top), Size(bw, bh), CornerRadius(3f * s), style = st)
    drawRect(color, Offset(-bw * 0.18f, top + bh * 0.38f), Size(bw * 0.36f, bh * 0.36f), style = st)
    // Çember
    val rimY = top + bh + 2f * s
    val rimW = bw * 0.5f
    drawOval(color, Offset(-rimW / 2, rimY - 3f * s), Size(rimW, 6f * s), style = Stroke(stroke * 1.3f))
    // File
    val net = color.copy(alpha = color.alpha * 0.7f)
    val netDepth = 14f * s
    for (i in 0..4) {
        val fx = -rimW / 2 + rimW * i / 4
        val tx = -rimW * 0.3f + rimW * 0.6f * i / 4
        drawLine(net, Offset(fx, rimY), Offset(tx, rimY + netDepth), stroke * 0.7f)
    }
    drawLine(net, Offset(-rimW * 0.3f, rimY + netDepth), Offset(rimW * 0.3f, rimY + netDepth), stroke * 0.7f)
}

// Vector equipment keeps the same visual weight across Android and iOS.
private fun DrawScope.drawGoalkeeperGlove(s: Float, stroke: Float, color: Color) {
    val outline = Path().apply {
        moveTo(-13*s, 24*s); lineTo(-17*s, 5*s); lineTo(-27*s, -7*s)
        cubicTo(-31*s, -15*s, -24*s, -19*s, -18*s, -11*s)
        lineTo(-14*s, -6*s); lineTo(-16*s, -26*s)
        cubicTo(-16*s, -34*s, -8*s, -34*s, -8*s, -26*s)
        lineTo(-6*s, -10*s); lineTo(-6*s, -32*s)
        cubicTo(-6*s, -39*s, 2*s, -39*s, 2*s, -32*s)
        lineTo(3*s, -10*s); lineTo(5*s, -29*s)
        cubicTo(6*s, -36*s, 14*s, -34*s, 13*s, -27*s)
        lineTo(12*s, -7*s); lineTo(16*s, -22*s)
        cubicTo(18*s, -28*s, 25*s, -25*s, 23*s, -19*s)
        lineTo(19*s, 8*s); lineTo(13*s, 24*s); close()
    }
    drawPath(outline, color.copy(alpha = color.alpha * 0.2f))
    drawPath(outline, color, style = Stroke(stroke, cap = StrokeCap.Round))
    drawRoundRect(color, Offset(-14*s, 20*s), Size(28*s, 10*s), CornerRadius(3*s), style = Stroke(stroke))
    drawLine(color, Offset(-7*s, 7*s), Offset(10*s, 7*s), stroke, StrokeCap.Round)
}

private fun DrawScope.drawRacket(s: Float, stroke: Float, color: Color) {
    drawOval(color, Offset(-17*s, -33*s), Size(34*s, 44*s), style = Stroke(stroke * 1.5f))
    for (i in -2..2) {
        drawLine(color.copy(alpha = color.alpha * 0.6f), Offset(i*5*s, -27*s), Offset(i*5*s, 4*s), stroke * 0.6f)
        drawLine(color.copy(alpha = color.alpha * 0.6f), Offset(-13*s, (-11+i*6)*s), Offset(13*s, (-11+i*6)*s), stroke * 0.6f)
    }
    drawLine(color, Offset(-8*s, 8*s), Offset(0f, 21*s), stroke)
    drawLine(color, Offset(8*s, 8*s), Offset(0f, 21*s), stroke)
    drawLine(color, Offset(0f, 19*s), Offset(0f, 39*s), stroke * 3f, StrokeCap.Round)
    drawCircle(color, 5*s, Offset(27*s, 5*s), style = Stroke(stroke))
}

private fun DrawScope.drawDumbbell(s: Float, stroke: Float, color: Color) {
    drawLine(color, Offset(-20*s, 0f), Offset(20*s, 0f), stroke * 2)
    listOf(-24f, -16f, 16f, 24f).forEach { x ->
        val h = if (kotlin.math.abs(x) == 16f) 30f else 20f
        drawRoundRect(color, Offset((x-3)*s, -h/2*s), Size(6*s, h*s), CornerRadius(2*s), style = Stroke(stroke))
    }
}

private fun DrawScope.drawGoggles(s: Float, stroke: Float, color: Color) {
    listOf(-24f, 4f).forEach { x ->
        drawRoundRect(color, Offset(x*s, -9*s), Size(20*s, 18*s), CornerRadius(6*s), style = Stroke(stroke))
    }
    drawLine(color, Offset(-4*s, -2*s), Offset(4*s, -2*s), stroke)
    drawArc(color, 190f, 160f, false, Offset(-33*s, -24*s), Size(66*s, 35*s), style = Stroke(stroke))
}

private fun DrawScope.drawShuttle(s: Float, stroke: Float, color: Color) {
    val fan = Path().apply {
        moveTo(-7*s, 14*s); lineTo(-23*s, -24*s); lineTo(23*s, -24*s); lineTo(7*s, 14*s); close()
    }
    drawPath(fan, color, style = Stroke(stroke))
    for (i in -1..1) drawLine(color, Offset(i*14*s, -24*s), Offset(i*4*s, 14*s), stroke)
    drawLine(color, Offset(-17*s, -10*s), Offset(17*s, -10*s), stroke)
    drawArc(color, 0f, 180f, false, Offset(-7*s, 7*s), Size(14*s, 16*s), style = Stroke(stroke*1.4f))
}

private fun DrawScope.drawSkate(s: Float, stroke: Float, color: Color) {
    val boot = Path().apply {
        moveTo(-19*s, -22*s); lineTo(-2*s, -22*s); lineTo(0f, -5*s)
        lineTo(21*s, 3*s); quadraticTo(29*s, 8*s, 22*s, 13*s)
        lineTo(-20*s, 13*s); close()
    }
    drawPath(boot, color, style = Stroke(stroke))
    drawLine(color, Offset(-23*s, 23*s), Offset(28*s, 23*s), stroke*1.5f, StrokeCap.Round)
    drawLine(color, Offset(-13*s, 13*s), Offset(-13*s, 23*s), stroke)
    drawLine(color, Offset(14*s, 13*s), Offset(14*s, 23*s), stroke)
    for (i in 0..2) drawLine(color, Offset(-9*s, (-14+i*6)*s), Offset(1*s, (-14+i*6)*s), stroke)
}
