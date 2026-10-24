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
import com.example.data.DayStepItem
import kotlin.math.max

private val BentoGreen = Color(0xFF22C55E)
private val BarTrackBg = Color(0xFF1E232E)

@Composable
fun StepBarChart(
  dailySteps: List<DayStepItem>,
  targetSteps: Int = 10_000,
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

      val maxVal = max(targetSteps.toFloat() * 1.2f, (dailySteps.maxOfOrNull { it.stepCount } ?: 10_000).toFloat() * 1.15f)

      // Target dashed line (10.000 steps)
      val targetY = paddingTop + chartHeight * (1f - (targetSteps / maxVal).coerceIn(0f, 1f))
      val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
      drawLine(
        color = BentoGreen.copy(alpha = 0.5f),
        start = Offset(0f, targetY),
        end = Offset(width, targetY),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = dashEffect
      )

      val count = if (dailySteps.isNotEmpty()) dailySteps.size else 7
      val barWidth = (width / count) * 0.45f
      val spacing = width / count

      dailySteps.forEachIndexed { idx, item ->
        val centerX = spacing * idx + spacing / 2f
        val barH = chartHeight * (item.stepCount / maxVal).coerceIn(0f, 1f)
        val barTop = paddingTop + (chartHeight - barH)

        val isTargetReached = item.stepCount >= targetSteps
        val barColor = if (isTargetReached) BentoGreen else BentoGreen.copy(alpha = 0.7f)

        // Track background
        drawRoundRect(
          color = BarTrackBg,
          topLeft = Offset(centerX - barWidth / 2f, paddingTop),
          size = Size(barWidth, chartHeight),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Step fill
        if (item.stepCount > 0) {
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
      dailySteps.forEach { item ->
        val isTargetReached = item.stepCount >= targetSteps
        Text(
          text = item.dayLabel,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = if (isTargetReached) BentoGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
