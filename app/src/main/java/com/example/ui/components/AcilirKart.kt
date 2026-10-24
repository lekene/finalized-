package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme

/**
 * Yeniden kullanılabilir akordeon kart bileşeni.
 * - Başlık, sol ikon, isteğe bağlı kısa özet satırı, sağda dönen ok
 * - Başlık satırına dokununca içerik yaklaşık 200 ms yumuşak animasyonla açılıp kapanır, ok döner
 * - Varsayılan durum kapalı
 * - Aynı anda birden fazla kart açık kalabilir (her kart kendi durumunu yönetir)
 * - Başlık alanı en az 56dp yüksekliğinde ve kolay dokunulabilir
 * - Bildirim izni kapalı olduğunda başlıkta kırmızı uyarı noktası gösterilebilir
 * - Tema rengi değiştiğinde ikonlar ve oklar yeni vurgu rengini alır
 */
@Composable
fun AcilirKart(
  title: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  iconTint: Color = AppTheme.colors.accent,
  summary: String? = null,
  showWarningBadge: Boolean = false,
  initiallyExpanded: Boolean = false,
  testTag: String = "",
  content: @Composable ColumnScope.() -> Unit
) {
  var isExpanded by remember { mutableStateOf(initiallyExpanded) }
  val arrowRotation by animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    animationSpec = tween(durationMillis = 200),
    label = "accordion_arrow_rotation"
  )

  val cardBg = AppTheme.colors.surface
  val cardBorderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary

  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = AppTheme.colors.cardBorderBrush
    )
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Başlık Alanı (En az 56dp yükseklik, kolay dokunulabilir erişilebilir alan)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 56.dp)
          .clip(RoundedCornerShape(24.dp))
          .clickable { isExpanded = !isExpanded }
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .testTag(if (testTag.isNotEmpty()) "${testTag}_header" else "accordion_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.16f),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              if (showWarningBadge) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .testTag("notification_permission_warning_dot")
                )
              }
            }
            if (!summary.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = summary,
                fontSize = 12.sp,
                color = subLabelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dönen Ok (200 ms animasyon, tema vurgulu)
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = if (isExpanded) "Daralt" else "Genişlet",
          tint = iconTint,
          modifier = Modifier
            .size(24.dp)
            .rotate(arrowRotation)
        )
      }

      // Açılır / Kapanır İçerik (200 ms yumuşak animasyon)
      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(
          animationSpec = tween(durationMillis = 200)
        ) + fadeIn(
          animationSpec = tween(durationMillis = 200)
        ),
        exit = shrinkVertically(
          animationSpec = tween(durationMillis = 200)
        ) + fadeOut(
          animationSpec = tween(durationMillis = 200)
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          HorizontalDivider(
            color = cardBorderColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 2.dp)
          )
          content()
        }
      }
    }
  }
}
