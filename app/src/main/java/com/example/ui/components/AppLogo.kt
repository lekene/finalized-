package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * KiloCAL Uygulama Logosu.
 * Yuvarlak köşeli bir kare (120dp, köşe yarıçapı 28dp), zemin koyu yeşil #0B3D2A.
 * Ortasında dijital baskül (#145A3E, kenarı #22C55E 3dp çerçeve).
 * Üstte hap şeklinde koyu (#0B3D2A) ekran ve kısa yeşil çizgi (#22C55E).
 * Ekranın altında yeşil (#22C55E) alev ve içinde açık yeşil (#D1FAE5) iç alev.
 */
@Composable
fun AppLogo(
  modifier: Modifier = Modifier,
  size: Dp = 120.dp,
  cornerRadius: Dp = 28.dp
) {
  Canvas(
    modifier = modifier.size(size)
  ) {
    val w = this.size.width
    val h = this.size.height
    // Virtual coordinate scale based on 120dp default design
    val s = w / 120f

    val outerCornerRadiusPx = cornerRadius.toPx()

    // 1. Dış Gövde: Yuvarlak köşeli kare zemin (#0B3D2A)
    drawRoundRect(
      color = Color(0xFF0B3D2A),
      topLeft = Offset.Zero,
      size = Size(w, h),
      cornerRadius = CornerRadius(outerCornerRadiusPx, outerCornerRadiusPx)
    )

    // 2. Dijital Baskül Gövdesi (#145A3E) - Ortalanmış 68x68dp
    val scaleW = 68f * s
    val scaleH = 68f * s
    val scaleLeft = (w - scaleW) / 2f
    val scaleTop = (h - scaleH) / 2f
    val scaleCornerRadius = 14f * s

    drawRoundRect(
      color = Color(0xFF145A3E),
      topLeft = Offset(scaleLeft, scaleTop),
      size = Size(scaleW, scaleH),
      cornerRadius = CornerRadius(scaleCornerRadius, scaleCornerRadius)
    )

    // 3. Baskülün İnce Yeşil Çerçevesi (#22C55E, ~3dp)
    val strokeWidthPx = 3.dp.toPx()
    drawRoundRect(
      color = Color(0xFF22C55E),
      topLeft = Offset(scaleLeft, scaleTop),
      size = Size(scaleW, scaleH),
      cornerRadius = CornerRadius(scaleCornerRadius, scaleCornerRadius),
      style = Stroke(width = strokeWidthPx)
    )

    // 4. Baskül Ekranı: Küçük hap şeklinde koyu (#0B3D2A) ekran
    val screenW = 20f * s
    val screenH = 8f * s
    val screenLeft = (w - screenW) / 2f
    val screenTop = scaleTop + 7f * s
    val screenCornerRadius = 4f * s

    drawRoundRect(
      color = Color(0xFF0B3D2A),
      topLeft = Offset(screenLeft, screenTop),
      size = Size(screenW, screenH),
      cornerRadius = CornerRadius(screenCornerRadius, screenCornerRadius)
    )

    // 5. Ekran İçindeki Kısa Yeşil Çizgi (#22C55E)
    val lineStartX = screenLeft + 4f * s
    val lineEndX = screenLeft + screenW - 4f * s
    val lineY = screenTop + screenH / 2f

    drawLine(
      color = Color(0xFF22C55E),
      start = Offset(lineStartX, lineY),
      end = Offset(lineEndX, lineY),
      strokeWidth = 2f * s,
      cap = StrokeCap.Round
    )

    // 6. Dış Alev (#22C55E) - Ekranın altında, orta-alt bölgede
    val centerX = w / 2f
    val outerFlame = Path().apply {
      moveTo(centerX, scaleTop + 21f * s)
      cubicTo(centerX, scaleTop + 21f * s, centerX + 5.5f * s, scaleTop + 28f * s, centerX + 7f * s, scaleTop + 33f * s)
      cubicTo(centerX + 8.5f * s, scaleTop + 37.5f * s, centerX + 11.5f * s, scaleTop + 39f * s, centerX + 11.5f * s, scaleTop + 44f * s)
      cubicTo(centerX + 11.5f * s, scaleTop + 50.5f * s, centerX + 6.5f * s, scaleTop + 55.5f * s, centerX, scaleTop + 55.5f * s)
      cubicTo(centerX - 6.5f * s, scaleTop + 55.5f * s, centerX - 11.5f * s, scaleTop + 50.5f * s, centerX - 11.5f * s, scaleTop + 44f * s)
      cubicTo(centerX - 11.5f * s, scaleTop + 39f * s, centerX - 8f * s, scaleTop + 35.5f * s, centerX - 6f * s, scaleTop + 33f * s)
      cubicTo(centerX - 4f * s, scaleTop + 30f * s, centerX - 3.5f * s, scaleTop + 26.5f * s, centerX - 3.5f * s, scaleTop + 26.5f * s)
      cubicTo(centerX - 3.5f * s, scaleTop + 26.5f * s, centerX - 1.5f * s, scaleTop + 31f * s, centerX + 1f * s, scaleTop + 32f * s)
      cubicTo(centerX + 1.2f * s, scaleTop + 29f * s, centerX, scaleTop + 25f * s, centerX, scaleTop + 21f * s)
      close()
    }
    drawPath(
      path = outerFlame,
      color = Color(0xFF22C55E)
    )

    // 7. İç Alev (#D1FAE5) - Küçük açık yeşil alev çekirdeği
    val innerFlame = Path().apply {
      moveTo(centerX, scaleTop + 41.5f * s)
      cubicTo(centerX + 1.2f * s, scaleTop + 41.5f * s, centerX + 3.8f * s, scaleTop + 44.5f * s, centerX + 3.8f * s, scaleTop + 47.5f * s)
      cubicTo(centerX + 3.8f * s, scaleTop + 50.5f * s, centerX + 2f * s, scaleTop + 52.8f * s, centerX, scaleTop + 52.8f * s)
      cubicTo(centerX - 2f * s, scaleTop + 52.8f * s, centerX - 3.8f * s, scaleTop + 50.5f * s, centerX - 3.8f * s, scaleTop + 47.5f * s)
      cubicTo(centerX - 3.8f * s, scaleTop + 44.5f * s, centerX - 1.2f * s, scaleTop + 41.5f * s, centerX, scaleTop + 41.5f * s)
      close()
    }
    drawPath(
      path = innerFlame,
      color = Color(0xFFD1FAE5)
    )
  }
}
