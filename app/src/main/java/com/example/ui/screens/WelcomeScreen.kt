package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedAccent

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF1E1E1E), Color(0xFF121212), Color(0xFF0A0A0A))
        )
      )
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth().maximumWidth(480.dp)
    ) {
      // App Icon representation: Red apple with scale
      Box(
        modifier = Modifier
          .size(120.dp)
          .scale(scale)
          .background(
            brush = Brush.radialGradient(
              colors = listOf(RedAccent, Color(0xFF8B0000))
            ),
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Scale,
          contentDescription = "KiloCAL Logo",
          tint = Color.White,
          modifier = Modifier.size(64.dp)
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

      Text(
        text = "KiloCAL",
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Akıllı Kalori Takibi & Kilo Verme Asistanı",
        fontSize = 16.sp,
        color = Color(0xFFB0BEC5),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(48.dp))

      Button(
        onClick = onGetStarted,
        modifier = Modifier
          .testTag("get_started_button")
          .fillMaxWidth()
          .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text(
          text = "Hemen Başla",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}

private fun Modifier.maximumWidth(dp: androidx.compose.ui.unit.Dp): Modifier = this.then(
  Modifier.widthIn(max = dp)
)
