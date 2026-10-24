package com.example.ui.avatar

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * Avatar seçimi ve fotoğraf yükleme için alttan açılan pencere (ModalBottomSheet).
 *
 * 1. "Fotoğraf yükle" bölümü: Android Photo Picker ile sıfır izinle seçim, 512x512 kırpma ve kaldırma seçeneği.
 * 2. "Hazır avatarlar" bölümü: Kadın/Erkek/Tümü filtre çipleri ile 12 hazır vektör avatar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerBottomSheet(
  currentAvatarUri: String?,
  gender: String, // "Erkek" or "Kadın"
  profileId: Int,
  onAvatarSelected: (String?) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var isSavingPhoto by remember { mutableStateOf(false) }

  // Filtre çipi: Varsayılan profil cinsiyetine göre ("Kadın", "Erkek" veya "Tümü")
  var selectedFilter by remember {
    mutableStateOf(if (gender.equals("Kadın", ignoreCase = true)) "Kadın" else "Erkek")
  }

  // Android Photo Picker Launcher (Sıfır İzin, Play Policy Uyumlu)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      coroutineScope.launch {
        isSavingPhoto = true
        val savedPath = AvatarStorageHelper.saveAvatarFromUri(context, uri, profileId)
        isSavingPhoto = false
        if (savedPath != null) {
          onAvatarSelected(savedPath)
          Toast.makeText(context, "Profil fotoğrafı güncellendi", Toast.LENGTH_SHORT).show()
          onDismiss()
        } else {
          Toast.makeText(context, "Fotoğraf kaydedilemedi", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  val filteredAvatars = remember(selectedFilter) {
    when (selectedFilter) {
      "Kadın" -> PRESET_AVATARS.filter { it.gender == "Kadın" }
      "Erkek" -> PRESET_AVATARS.filter { it.gender == "Erkek" }
      else -> PRESET_AVATARS
    }
  }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = AppTheme.colors.surface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 10.dp, bottom = 4.dp)
          .size(width = 36.dp, height = 4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(AppTheme.colors.border)
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
    ) {
      // Başlık Satırı
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Avatarı Değiştir",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Kapat",
            tint = AppTheme.colors.textSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // ==========================================
      // BÖLÜM 1: FOTOĞRAF YÜKLE
      // ==========================================
      Text(
        text = "FOTOĞRAF YÜKLE",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.accent,
        letterSpacing = 0.5.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Fotoğraf Seç Butonu
        Surface(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          shape = RoundedCornerShape(14.dp),
          color = AppTheme.colors.itemBg,
          border = BorderStroke(1.dp, AppTheme.colors.border),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("avatar_pick_photo_button")
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (isSavingPhoto) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppTheme.colors.accent,
                strokeWidth = 2.dp
              )
            } else {
              Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = AppTheme.colors.accent,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Galeriden Seç",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        // Eğer mevcut avatar varsa "Fotoğrafı / Avatarı Kaldır"
        if (!currentAvatarUri.isNullOrBlank()) {
          Surface(
            onClick = {
              AvatarStorageHelper.deleteAvatarForProfile(context, profileId)
              onAvatarSelected(null)
              Toast.makeText(context, "Avatar kaldırıldı", Toast.LENGTH_SHORT).show()
              onDismiss()
            },
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFEF4444).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("avatar_remove_button")
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Avatarı Kaldır",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEF4444)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // ==========================================
      // BÖLÜM 2: HAZIR AVATARLAR
      // ==========================================
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "HAZIR AVATARLAR",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = AppTheme.colors.accent,
          letterSpacing = 0.5.sp
        )

        // Filtre Çipleri: Kadın / Erkek / Tümü
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("Kadın", "Erkek", "Tümü").forEach { filter ->
            val isSelected = selectedFilter == filter
            Surface(
              onClick = { selectedFilter = filter },
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) AppTheme.colors.accent else AppTheme.colors.itemBg,
              border = if (!isSelected) BorderStroke(1.dp, AppTheme.colors.border) else null,
              modifier = Modifier.height(28.dp)
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = filter,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color.White else AppTheme.colors.textSecondary
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 12 Hazır Avatar Grid'i (4 sütunlu ferah görünüm)
      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 280.dp)
      ) {
        items(filteredAvatars, key = { it.id }) { preset ->
          val isSelected = currentAvatarUri == preset.id
          val accent = AppTheme.colors.accent

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                onAvatarSelected(preset.id)
                onDismiss()
              }
              .padding(4.dp)
              .testTag("preset_avatar_${preset.id.removePrefix("preset:")}")
          ) {
            Box(
              modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .then(
                  if (isSelected) {
                    Modifier.border(3.dp, accent, CircleShape)
                  } else {
                    Modifier.border(1.dp, AppTheme.colors.border.copy(alpha = 0.6f), CircleShape)
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                drawPresetAvatar(preset.id)
              }

              // Seçili onay rozeti
              if (isSelected) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                  contentAlignment = Alignment.Center
                ) {
                  Surface(
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier.size(24.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = preset.ageGroup,
              fontSize = 10.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) accent else AppTheme.colors.textSecondary,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }
  }
}
