package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DayWaterItem
import kotlin.math.max

@Composable
fun WaterBarChart(
  dailyWater: List<DayWaterItem>,
  targetMl: Int,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
    ) {
      val width = size.width
      val height = size.height
      val paddingBottom = 16.dp.toPx()
      val paddingTop = 12.dp.toPx()
      val chartHeight = height - paddingBottom - paddingTop

      val maxVal = max(targetMl.toFloat() * 1.25f, (dailyWater.maxOfOrNull { it.totalMl } ?: 2500).toFloat() * 1.1f)

      // Target dashed line
      val targetY = paddingTop + chartHeight * (1f - (targetMl / maxVal).coerceIn(0f, 1f))
      val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
      drawLine(
        color = Color(0xFF29B6F6).copy(alpha = 0.6f),
        start = Offset(0f, targetY),
        end = Offset(width, targetY),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = dashEffect
      )

      val count = if (dailyWater.isNotEmpty()) dailyWater.size else 7
      val barWidth = (width / count) * 0.45f
      val spacing = width / count

      dailyWater.forEachIndexed { idx, item ->
        val centerX = spacing * idx + spacing / 2f
        val barH = chartHeight * (item.totalMl / maxVal).coerceIn(0f, 1f)
        val barTop = paddingTop + (chartHeight - barH)

        val barColor = if (item.totalMl >= targetMl) Color(0xFF29B6F6) else Color(0xFF0288D1).copy(alpha = 0.7f)

        // Track background
        drawRoundRect(
          color = Color(0xFF242C38),
          topLeft = Offset(centerX - barWidth / 2f, paddingTop),
          size = Size(barWidth, chartHeight),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Water fill
        if (item.totalMl > 0) {
          drawRoundRect(
            color = barColor,
            topLeft = Offset(centerX - barWidth / 2f, barTop),
            size = Size(barWidth, barH),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
          )
        }
      }
    }

    // Day labels
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      dailyWater.forEach { item ->
        Text(
          text = item.dayLabel,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = if (item.isTargetReached) Color(0xFF29B6F6) else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
