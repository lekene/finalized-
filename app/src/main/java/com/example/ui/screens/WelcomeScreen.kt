package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppLogo
import com.example.ui.theme.AppTheme

private val BentoGreen: Color
  @Composable get() = AppTheme.colors.accent

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
  // Logo açılışta hafifçe büyüyerek ve belirginleşerek görünsün (yaklaşık 400 ms animasyon)
  val animProgress = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    animProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )
  }

  val logoScale = 0.82f + 0.18f * animProgress.value
  val logoAlpha = animProgress.value

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
      modifier = Modifier
        .fillMaxWidth()
        .maximumWidth(480.dp)
    ) {
      // Uygulama Logosu: Yuvarlak köşeli kare dijital baskül ve alev (yaklaşık 120dp, köşe yarıçapı 28dp)
      Box(
        modifier = Modifier
          .graphicsLayer {
            scaleX = logoScale
            scaleY = logoScale
            alpha = logoAlpha
          },
        contentAlignment = Alignment.Center
      ) {
        AppLogo(
          size = 120.dp,
          cornerRadius = 28.dp
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
        text = androidx.compose.ui.res.stringResource(com.example.R.string.welcome_tagline),
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
        colors = ButtonDefaults.buttonColors(containerColor = BentoGreen),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text(
          text = androidx.compose.ui.res.stringResource(com.example.R.string.get_started),
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
