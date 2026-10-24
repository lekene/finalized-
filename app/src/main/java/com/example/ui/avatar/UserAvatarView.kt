package com.example.ui.avatar

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import java.io.File

/**
 * Uygulamanın tüm ekranlarında (Kimlik kartı, Özet Merhaba satırı,
 * Profil geçiş menüsü, Ayarlar profil listesi) kullanılan birleşik Avatar bileşeni.
 *
 * - avatarUri == null: Baş harfli yeşil/vurgulu daire gösterilir.
 * - avatarUri.startsWith("preset:"): 12 hazır vektör avatardan biri Canvas ile çizilir.
 * - avatarUri dosya yolu ise: 512x512 yuvarlak kırpılmış özel fotoğraf gösterilir.
 */
@Composable
fun UserAvatarView(
  avatarUri: String?,
  name: String,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  border: BorderStroke? = null,
  testTag: String = "user_avatar_view",
  onClick: (() -> Unit)? = null
) {
  val initialLetter = (name.trim().firstOrNull() ?: 'P').uppercase()
  val accentColor = AppTheme.colors.accent

  // Özel yüklenen fotoğraf bitmap'i (eğer dosya yoluysa)
  val customBitmap = remember(avatarUri) {
    if (!avatarUri.isNullOrBlank() && !avatarUri.startsWith("preset:")) {
      try {
        val cleanPath = avatarUri.removePrefix("file://")
        val file = File(cleanPath)
        if (file.exists() && file.canRead()) {
          BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } else {
          null
        }
      } catch (_: Exception) {
        null
      }
    } else {
      null
    }
  }

  val baseModifier = modifier
    .size(size)
    .clip(CircleShape)
    .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
    .then(
      if (onClick != null) {
        Modifier.clickable(onClick = onClick)
      } else {
        Modifier
      }
    )
    .testTag(testTag)

  Box(
    modifier = baseModifier,
    contentAlignment = Alignment.Center
  ) {
    when {
      // 1. Özel Fotoğraf
      customBitmap != null -> {
        Image(
          bitmap = customBitmap,
          contentDescription = name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      // 2. Hazır Vektör Avatar
      !avatarUri.isNullOrBlank() && avatarUri.startsWith("preset:") -> {
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawPresetAvatar(avatarUri)
        }
      }

      // 3. Varsayılan Baş Harfli Daire
      else -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(accentColor),
          contentAlignment = Alignment.Center
        ) {
          val fontSize = (size.value * 0.44f).sp
          Text(
            text = initialLetter,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }
}
