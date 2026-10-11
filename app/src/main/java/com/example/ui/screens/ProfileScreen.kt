package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.UserProfile
import com.example.reminder.ReminderSettings
import com.example.ui.CalculatedMetrics
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedAccent
import java.util.*

@Composable
fun ProfileScreen(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  reminderSettings: ReminderSettings = ReminderSettings(),
  onSaveReminderSettings: (ReminderSettings) -> Unit = {},
  onUpdateWaterTarget: (Int) -> Unit = {},
  onEditProfile: () -> Unit,
  onLogout: () -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  var showWaterTargetDialog by remember { mutableStateOf(false) }
  var customWaterInput by remember { mutableStateOf(if (userProfile.customWaterTarget > 0) userProfile.customWaterTarget.toString() else "") }

  // Permission handling
  var showPermissionRationaleDialog by remember { mutableStateOf(false) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (!isGranted) {
      showPermissionRationaleDialog = true
    }
  }

  fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val isGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!isGranted) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  // Helper for opening native TimePickerDialog
  fun openTimePicker(currentTimeStr: String, onTimeSelected: (String) -> Unit) {
    val parts = currentTimeStr.split(":")
    val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0

    TimePickerDialog(
      context,
      { _, hourOfDay, minute ->
        val formatted = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
        onTimeSelected(formatted)
      },
      initialH,
      initialM,
      true
    ).show()
  }

  // Weight day dropdown state
  var showWeightDayDialog by remember { mutableStateOf(false) }
  val daysOfWeek = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp)
        .padding(bottom = 80.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // 1. Avatar & Name Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Surface(
            modifier = Modifier.size(76.dp),
            shape = CircleShape,
            color = GreenPrimary
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = userProfile.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = userProfile.email,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = onEditProfile,
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Profili Düzenle", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 2. Personal Stats Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
        ) {
          Text(
            text = "Kişisel Bilgiler",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(14.dp))

          ProfileInfoRow(label = "Cinsiyet", value = userProfile.gender)
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
          ProfileInfoRow(label = "Yaş", value = "${userProfile.age}")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
          ProfileInfoRow(label = "Boy", value = "${userProfile.height.toInt()} cm")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
          ProfileInfoRow(label = "Mevcut Kilo", value = String.format(Locale.getDefault(), "%.1f kg", userProfile.currentWeight))
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
          ProfileInfoRow(label = "Hedef Kilo", value = String.format(Locale.getDefault(), "%.1f kg", userProfile.targetWeight))
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
          val actLabel = when (userProfile.activityLevel) {
            "hareketsiz" -> "Hareketsiz"
            "hafif_aktif" -> "Hafif Aktif"
            "orta_aktif" -> "Orta Aktif"
            "cok_aktif" -> "Çok Aktif"
            else -> "Hafif Aktif"
          }
          ProfileInfoRow(label = "Aktivite Seviyesi", value = actLabel)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 3. Su Hedefi Ayarı Kartı (Water Target Card)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = Color(0xFF0288D1).copy(alpha = 0.2f),
                modifier = Modifier.size(34.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF29B6F6),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Günlük Su Hedefi",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (userProfile.customWaterTarget > 0) "Elle belirlendi" else "Otomatik (kilo x 33 ml)",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF0288D1).copy(alpha = 0.15f),
              modifier = Modifier.clickable {
                customWaterInput = if (userProfile.customWaterTarget > 0) userProfile.customWaterTarget.toString() else userProfile.dailyWaterTarget.toString()
                showWaterTargetDialog = true
              }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "${userProfile.dailyWaterTarget} ml",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF29B6F6)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = null,
                  tint = Color(0xFF29B6F6),
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 4. Hatırlatıcılar Bölümü (Reminders Settings Card)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = null,
              tint = GreenPrimary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Hatırlatıcılar",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 💧 Su Hatırlatıcısı
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Su Hatırlatıcısı",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Gün boyu su içmenizi nazikçe hatırlatır",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = reminderSettings.waterReminderEnabled,
              onCheckedChange = { enabled ->
                if (enabled) requestNotificationPermissionIfNeeded()
                onSaveReminderSettings(reminderSettings.copy(waterReminderEnabled = enabled))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GreenPrimary
              )
            )
          }

          AnimatedVisibility(visible = reminderSettings.waterReminderEnabled) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              // Time Range
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = "Saat Aralığı", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clickable {
                      openTimePicker(reminderSettings.waterStartTime) { newTime ->
                        onSaveReminderSettings(reminderSettings.copy(waterStartTime = newTime))
                      }
                    }
                  ) {
                    Text(
                      text = reminderSettings.waterStartTime,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }

                  Text(text = " - ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clickable {
                      openTimePicker(reminderSettings.waterEndTime) { newTime ->
                        onSaveReminderSettings(reminderSettings.copy(waterEndTime = newTime))
                      }
                    }
                  ) {
                    Text(
                      text = reminderSettings.waterEndTime,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Interval Selector
              Text(text = "Hatırlatma Sıklığı", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf(1, 2, 3).forEach { intervalHours ->
                  val isSelected = reminderSettings.waterIntervalHours == intervalHours
                  Surface(
                    onClick = {
                      onSaveReminderSettings(reminderSettings.copy(waterIntervalHours = intervalHours))
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) GreenPrimary else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f).height(34.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = "$intervalHours saatte bir",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)

          // 🥗 Öğün Hatırlatıcısı
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Öğün Hatırlatıcıları",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Kahvaltı, öğle ve akşam yemeği saatleri",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = reminderSettings.mealReminderEnabled,
              onCheckedChange = { enabled ->
                if (enabled) requestNotificationPermissionIfNeeded()
                onSaveReminderSettings(reminderSettings.copy(mealReminderEnabled = enabled))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GreenPrimary
              )
            )
          }

          AnimatedVisibility(visible = reminderSettings.mealReminderEnabled) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              MealTimeRow(title = "Kahvaltı Saati", time = reminderSettings.breakfastTime) { newTime ->
                onSaveReminderSettings(reminderSettings.copy(breakfastTime = newTime))
              }
              MealTimeRow(title = "Öğle Yemeği Saati", time = reminderSettings.lunchTime) { newTime ->
                onSaveReminderSettings(reminderSettings.copy(lunchTime = newTime))
              }
              MealTimeRow(title = "Akşam Yemeği Saati", time = reminderSettings.dinnerTime) { newTime ->
                onSaveReminderSettings(reminderSettings.copy(dinnerTime = newTime))
              }
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)

          // ⚖️ Haftalık Tartılma Hatırlatıcısı
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Haftalık Tartılma Hatırlatıcısı",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Kilo takibini aksatmamak için haftada bir gün",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = reminderSettings.weightReminderEnabled,
              onCheckedChange = { enabled ->
                if (enabled) requestNotificationPermissionIfNeeded()
                onSaveReminderSettings(reminderSettings.copy(weightReminderEnabled = enabled))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GreenPrimary
              )
            )
          }

          AnimatedVisibility(visible = reminderSettings.weightReminderEnabled) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = "Tartılma Günü", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.clickable { showWeightDayDialog = true }
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = reminderSettings.weightReminderDay,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = GreenPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = GreenPrimary)
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = "Tartılma Saati", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.clickable {
                    openTimePicker(reminderSettings.weightReminderTime) { newTime ->
                      onSaveReminderSettings(reminderSettings.copy(weightReminderTime = newTime))
                    }
                  }
                ) {
                  Text(
                    text = reminderSettings.weightReminderTime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 5. Logout Button
      Button(
        onClick = onLogout,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RedAccent.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = RedAccent)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Çıkış Yap",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = RedAccent
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }

    // Water Target Edit Dialog
    if (showWaterTargetDialog) {
      AlertDialog(
        onDismissRequest = { showWaterTargetDialog = false },
        title = { Text("Günlük Su Hedefi") },
        text = {
          Column {
            Text(
              text = "Kilonuza göre önerilen otomatik hedef: ${(userProfile.currentWeight * 33f).toInt().coerceIn(1500, 3500)} ml",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = customWaterInput,
              onValueChange = { customWaterInput = it.filter { c -> c.isDigit() } },
              label = { Text("Özel Hedef (ml)") },
              placeholder = { Text("ör. 2500") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
              onClick = {
                onUpdateWaterTarget(0) // 0 resets to auto
                showWaterTargetDialog = false
              }
            ) {
              Text("Otomatik Hesaplamaya Dön (kilo x 33 ml)", color = GreenPrimary, fontSize = 12.sp)
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val customVal = customWaterInput.toIntOrNull()
              if (customVal != null && customVal > 0) {
                onUpdateWaterTarget(customVal)
              }
              showWaterTargetDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
          ) {
            Text("Kaydet", color = Color.White)
          }
        },
        dismissButton = {
          TextButton(onClick = { showWaterTargetDialog = false }) {
            Text("Vazgeç")
          }
        }
      )
    }

    // Weight Reminder Day Selection Dialog
    if (showWeightDayDialog) {
      AlertDialog(
        onDismissRequest = { showWeightDayDialog = false },
        title = { Text("Tartılma Günü Seçin") },
        text = {
          Column {
            daysOfWeek.forEach { day ->
              val isSelected = reminderSettings.weightReminderDay.equals(day, ignoreCase = true)
              Surface(
                onClick = {
                  onSaveReminderSettings(reminderSettings.copy(weightReminderDay = day))
                  showWeightDayDialog = false
                },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) GreenPrimary.copy(alpha = 0.15f) else Color.Transparent,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = day,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) GreenPrimary else MaterialTheme.colorScheme.onSurface
                  )
                  if (isSelected) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = GreenPrimary)
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showWeightDayDialog = false }) {
            Text("Kapat")
          }
        }
      )
    }

    // Notification Permission Rationale Dialog
    if (showPermissionRationaleDialog) {
      AlertDialog(
        onDismissRequest = { showPermissionRationaleDialog = false },
        title = { Text("Bildirim İzni Gerekli") },
        text = {
          Text(
            text = "Hatırlatıcıları zamanında alabilmeniz ve günlük su/öğün takibinizi aksatmamak için bildirim iznine ihtiyaç duyuyoruz. Dilediğiniz zaman cihaz ayarlarından değiştirebilirsiniz.",
            fontSize = 14.sp
          )
        },
        confirmButton = {
          Button(
            onClick = { showPermissionRationaleDialog = false },
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
          ) {
            Text("Anladım", color = Color.White)
          }
        }
      )
    }
  }
}

@Composable
fun MealTimeRow(title: String, time: String, onTimeChange: (String) -> Unit) {
  val context = LocalContext.current
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.clickable {
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
          context,
          { _, selectedH, selectedM ->
            val formatted = String.format(Locale.getDefault(), "%02d:%02d", selectedH, selectedM)
            onTimeChange(formatted)
          },
          h,
          m,
          true
        ).show()
      }
    ) {
      Text(
        text = time,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
      )
    }
  }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
  }
}
