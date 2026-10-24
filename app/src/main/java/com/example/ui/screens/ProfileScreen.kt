package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.UserProfile
import com.example.reminder.ReminderSettings
import com.example.ui.CalculatedMetrics
import com.example.ui.components.MetricInfoBottomSheet
import com.example.ui.components.MetricInfoType
import com.example.ui.components.AcilirKart
import com.example.ui.avatar.UserAvatarView
import java.util.*

import com.example.ui.theme.AppTheme

// Bento Palette matching Summary, Journal, and Progress:
// Kalori/Vurgu: Dinamik tema vurgusu, Protein: mavi, Karbonhidrat: amber, Yağ: pembe-kırmızı, Su: turkuaz, Kilo: mor
private val BentoGreen: Color
  @Composable get() = AppTheme.colors.accent
private val BentoBlue = Color(0xFF38BDF8)
private val BentoAmber = Color(0xFFFBBF24)
private val BentoPink = Color(0xFFFB7185)
private val BentoTeal = Color(0xFF2DD4BF)
private val BentoPurple = Color(0xFFA855F7)
private val BentoOverRed = Color(0xFFEF4444)

// Dark Bento Card Colors (24dp rounded corners, distinctive elevated container surfaces)
private val CardBg: Color
  @Composable get() = AppTheme.colors.surface
private val CardBorderColor: Color
  @Composable get() = AppTheme.colors.border
private val SubLabelColor: Color
  @Composable get() = AppTheme.colors.textSecondary
private val ItemBg: Color
  @Composable get() = AppTheme.colors.itemBg

@Composable
fun ProfileScreen(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  reminderSettings: ReminderSettings = ReminderSettings(),
  onSaveReminderSettings: (ReminderSettings) -> Unit = {},
  onUpdateWaterTarget: (Int) -> Unit = {},
  onUpdateActivityLevel: (String) -> Unit = {},
  onUpdateHeight: (Float) -> Unit = {},
  onUpdateCurrentWeight: (Float) -> Unit = {},
  onUpdateTargetWeight: (Float) -> Unit = {},
  onResetAllData: () -> Unit = {},
  onEditProfile: () -> Unit,
  onLogout: () -> Unit,
  onOpenSettings: () -> Unit = {},
  onOpenReport: (com.example.report.ReportPeriod, String?, String?) -> Unit = { _, _, _ -> },
  onExportReport: (com.example.report.ReportPeriod, String?, String?, Boolean) -> Unit = { _, _, _, _ -> },
  isExporting: Boolean = false,
  exportResult: com.example.report.ReportExportHelper.ExportResult? = null,
  exportError: String? = null,
  onDismissExportResult: () -> Unit = {},
  onDismissExportError: () -> Unit = {}
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  // Monthly Report Sheet States
  var showReportInfoSheet by remember { mutableStateOf(false) }
  var selectedReportPeriod by remember { mutableStateOf(com.example.report.ReportPeriod.THIS_MONTH) }
  val defaultEndStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }
  val defaultStartStr = remember {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, -13)
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
  }
  var customStartDate by remember { mutableStateOf(defaultStartStr) }
  var customEndDate by remember { mutableStateOf(defaultEndStr) }

  // Dialog States
  var showWaterTargetDialog by remember { mutableStateOf(false) }
  var customWaterInput by remember {
    mutableStateOf(if (userProfile.customWaterTarget > 0) userProfile.customWaterTarget.toString() else "")
  }

  // Quick edit single body metric modal
  var editMetricType by remember { mutableStateOf<String?>(null) } // "height", "weight", "targetWeight"
  var editMetricValue by remember { mutableStateOf("") }

  // Metabolizma ve Kalori Analizi bilgi kartı seçimi
  var selectedMetricInfo by remember { mutableStateOf<MetricInfoType?>(null) }

  // Reset all data dialog state
  var showResetConfirmDialog by remember { mutableStateOf(false) }

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

  // Avatar initial letter
  val avatarInitial = remember(userProfile.name) {
    userProfile.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "K"
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // UYGULAMA AYARLARI BUTONU (Tam genişlikte ince, 44dp, dişli ikonlu, sağında ok)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .clip(RoundedCornerShape(24.dp))
          .clickable { onOpenSettings() }
          .testTag("profile_settings_button"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f)))
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Ayarlar",
              tint = BentoGreen,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Uygulama Ayarları",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = SubLabelColor,
            modifier = Modifier.size(13.dp)
          )
        }
      }

      // RAPOR OLUŞTUR BUTONU (Belirgin, tam genişlikte, ikonlu, Bento stili)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .height(58.dp)
          .clip(RoundedCornerShape(24.dp))
          .clickable { showReportInfoSheet = true }
          .testTag("profile_generate_report_button"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(
            listOf(BentoGreen.copy(alpha = 0.85f), Color(0xFF38BDF8).copy(alpha = 0.55f))
          )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = BentoGreen.copy(alpha = 0.18f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Assessment,
                  contentDescription = null,
                  tint = BentoGreen,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Rapor Oluştur",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Kalori, makro, su, adım & hedef varış analizi",
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = BentoGreen.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, BentoGreen.copy(alpha = 0.35f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "İncele",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = BentoGreen
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = BentoGreen,
                modifier = Modifier.size(11.dp)
              )
            }
          }
        }
      }

      // 1. KİMLİK KARTI (Tam genişlik Bento)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f)))
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            // Profil Avatarı
            UserAvatarView(
              avatarUri = userProfile.avatarUri,
              name = userProfile.name,
              size = 54.dp,
              onClick = onEditProfile,
              testTag = "profile_identity_avatar"
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
              Text(
                text = userProfile.name.ifBlank { "Kullanıcı" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${userProfile.age} Yaş • ${userProfile.gender}",
                fontSize = 12.sp,
                color = SubLabelColor
              )
            }
          }

          // Sağda küçük "Düzenle" butonu
          Button(
            onClick = onEditProfile,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ItemBg),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = null,
              tint = BentoGreen,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Düzenle",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = BentoGreen
            )
          }
        }
      }

      // 2. VÜCUT BİLGİLERİ (Üç küçük kart yan yana): Boy, güncel kilo, hedef kilo
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Boy Kartı
        BodyMetricTile(
          label = "Boy",
          value = "${userProfile.height.toInt()}",
          unit = "cm",
          icon = Icons.Default.Height,
          color = BentoBlue,
          modifier = Modifier.weight(1f),
          onClick = {
            editMetricType = "height"
            editMetricValue = userProfile.height.toInt().toString()
          }
        )

        // Güncel Kilo Kartı
        BodyMetricTile(
          label = "Güncel Kilo",
          value = String.format(Locale.getDefault(), "%.1f", userProfile.currentWeight),
          unit = "kg",
          icon = Icons.Default.MonitorWeight,
          color = BentoPurple,
          modifier = Modifier.weight(1f),
          onClick = {
            editMetricType = "weight"
            editMetricValue = String.format(Locale.getDefault(), "%.1f", userProfile.currentWeight)
          }
        )

        // Hedef Kilo Kartı
        BodyMetricTile(
          label = "Hedef Kilo",
          value = String.format(Locale.getDefault(), "%.1f", userProfile.targetWeight),
          unit = "kg",
          icon = Icons.Default.Flag,
          color = BentoGreen,
          modifier = Modifier.weight(1f),
          onClick = {
            editMetricType = "targetWeight"
            editMetricValue = String.format(Locale.getDefault(), "%.1f", userProfile.targetWeight)
          }
        )
      }

      // 3. HESAPLANAN DEĞERLER KARTI (2x2 mini kutu): BMI, Bazal Metabolizma, Günlük Enerji İhtiyacı, Kalori Hedefi
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f)))
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = BentoAmber.copy(alpha = 0.16f),
              modifier = Modifier.size(32.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Calculate,
                  contentDescription = null,
                  tint = BentoAmber,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Metabolizma & Kalori Analizi",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          // 2x2 Mini Kutu Izgarası
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // 1. BMI (Vücut Kitle İndeksi)
              val bmiColor = when (metrics.bmiCategory) {
                "Normal" -> BentoGreen
                "Zayıf" -> BentoBlue
                "Kilolu" -> BentoAmber
                else -> BentoOverRed
              }
              CalculatedMetricBox(
                title = "BMI (VKİ)",
                value = String.format(Locale.getDefault(), "%.1f", metrics.bmi),
                badgeText = metrics.bmiCategory,
                badgeColor = bmiColor,
                desc = "Vücut Kitle İndeksi",
                icon = Icons.Default.Speed,
                iconTint = bmiColor,
                testTag = "metric_box_bmi",
                onClick = { selectedMetricInfo = MetricInfoType.BMI },
                modifier = Modifier.weight(1f)
              )

              // 2. Bazal Metabolizma (BMR)
              CalculatedMetricBox(
                title = "Bazal Metab.",
                value = "${metrics.bmr}",
                badgeText = "BMR",
                badgeColor = BentoBlue,
                desc = "Dinlenirken yakılan kalori",
                icon = Icons.Default.BatteryChargingFull,
                iconTint = BentoBlue,
                testTag = "metric_box_bmr",
                onClick = { selectedMetricInfo = MetricInfoType.BMR },
                modifier = Modifier.weight(1f)
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // 3. Günlük Enerji İhtiyacı (TDEE)
              CalculatedMetricBox(
                title = "Günlük Enerji",
                value = "${metrics.tdee}",
                badgeText = "TDEE",
                badgeColor = BentoAmber,
                desc = "Mevcut kiloyu koruma",
                icon = Icons.Default.Bolt,
                iconTint = BentoAmber,
                testTag = "metric_box_tdee",
                onClick = { selectedMetricInfo = MetricInfoType.TDEE },
                modifier = Modifier.weight(1f)
              )

              // 4. Günlük Kalori Hedefi
              CalculatedMetricBox(
                title = "Kalori Hedefi",
                value = "${metrics.targetCalories}",
                badgeText = "Hedef",
                badgeColor = BentoGreen,
                desc = "Hedefe ulaşmak için alım",
                icon = Icons.Default.TrackChanges,
                iconTint = BentoGreen,
                testTag = "metric_box_target_calories",
                onClick = { selectedMetricInfo = MetricInfoType.TARGET_CALORIES },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      val activitySummaryLabel = when (userProfile.activityLevel) {
        "hareketsiz" -> "Hareketsiz"
        "hafif_aktif" -> "Hafif aktif"
        "orta_aktif" -> "Orta aktif"
        "cok_aktif" -> "Çok aktif"
        else -> "Hafif aktif"
      }
      val activityNutritionSummary = "$activitySummaryLabel · Su ${userProfile.dailyWaterTarget} ml · P${metrics.proteinGrams} K${metrics.carbGrams} Y${metrics.fatGrams}"

      // 4. HEDEFLER KARTI: Aktivite seviyesi seçici (çip düğmeler), Su hedefi, Makro hedefleri (Açılır Kart)
      AcilirKart(
        title = "Aktivite ve Besin Hedefleri",
        icon = Icons.Default.FitnessCenter,
        summary = activityNutritionSummary,
        iconTint = AppTheme.colors.accent,
        testTag = "profile_activity_nutrition_card"
      ) {

          // Aktivite Seviyesi Başlığı & Çipler
          Text(
            text = "Aktivite Seviyesi",
            fontSize = 12.sp,
            color = SubLabelColor,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(6.dp))

          val activities = listOf(
            "hareketsiz" to "Hareketsiz",
            "hafif_aktif" to "Hafif",
            "orta_aktif" to "Orta",
            "cok_aktif" to "Çok Aktif"
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            activities.forEach { (key, label) ->
              val isSelected = userProfile.activityLevel == key
              Surface(
                onClick = { onUpdateActivityLevel(key) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) BentoGreen else ItemBg,
                border = if (!isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor))) else null,
                modifier = Modifier
                  .weight(1f)
                  .height(32.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else SubLabelColor
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Günlük Su Hedefi (Elle değiştirilebilir satır)
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                customWaterInput = if (userProfile.customWaterTarget > 0) userProfile.customWaterTarget.toString() else userProfile.dailyWaterTarget.toString()
                showWaterTargetDialog = true
              }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = BentoTeal.copy(alpha = 0.16f),
                  modifier = Modifier.size(36.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.WaterDrop,
                      contentDescription = null,
                      tint = BentoTeal,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Günlük Su Hedefi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (userProfile.customWaterTarget > 0) "Elle belirlendi" else "Otomatik (kilo x 33 ml)",
                    fontSize = 11.sp,
                    color = SubLabelColor
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = BentoTeal.copy(alpha = 0.14f)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = "${userProfile.dailyWaterTarget} ml",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTeal
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = BentoTeal,
                    modifier = Modifier.size(13.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Makro Hedefleri (Renkli etiketlerle: Protein mavi, Karbonhidrat amber, Yağ pembe-kırmızı)
          Text(
            text = "Günlük Makro Hedefleri",
            fontSize = 12.sp,
            color = SubLabelColor,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Protein
            MacroTargetPill(
              title = "Protein",
              grams = "${metrics.proteinGrams}g",
              percent = "%30",
              color = BentoBlue,
              modifier = Modifier.weight(1f)
            )

            // Karbonhidrat
            MacroTargetPill(
              title = "Karb",
              grams = "${metrics.carbGrams}g",
              percent = "%40",
              color = BentoAmber,
              modifier = Modifier.weight(1f)
            )

            // Yağ
            MacroTargetPill(
              title = "Yağ",
              grams = "${metrics.fatGrams}g",
              percent = "%30",
              color = BentoPink,
              modifier = Modifier.weight(1f)
            )
          }
        }

      val remindersSummary = if (!reminderSettings.masterReminderEnabled) {
        "Tüm bildirimler kapalı"
      } else {
        val activeList = mutableListOf<String>()
        if (reminderSettings.waterReminderEnabled) activeList.add("su")
        if (reminderSettings.mealReminderEnabled) activeList.add("öğün")
        if (reminderSettings.weightReminderEnabled) activeList.add("tartılma")
        if (reminderSettings.stepReminderEnabled) activeList.add("adım")

        if (activeList.isEmpty()) {
          "Tümü kapalı"
        } else {
          val formatted = activeList.mapIndexed { idx, item ->
            if (idx == 0) item.replaceFirstChar { it.uppercase() } else item
          }.joinToString(", ")
          "$formatted açık"
        }
      }

      // 5. HATIRLATICILAR KARTI (Açılır Kart)
      AcilirKart(
        title = "Bildirimler ve Hatırlatıcılar",
        icon = Icons.Default.Notifications,
        summary = remindersSummary,
        iconTint = AppTheme.colors.accent,
        testTag = "profile_reminders_card"
      ) {
        val isMasterActive = reminderSettings.masterReminderEnabled

        // Tüm Bildirimler Ana Satırı (Aç/Kapa Switch)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = if (isMasterActive) BentoGreen.copy(alpha = 0.16f) else Color(0xFFEF4444).copy(alpha = 0.12f),
              modifier = Modifier.size(32.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isMasterActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                  contentDescription = null,
                  tint = if (isMasterActive) BentoGreen else Color(0xFFEF4444),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Tüm Bildirimler",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (!isMasterActive) {
                  "Tüm bildirimler kapalı"
                } else if (reminderSettings.quietHoursEnabled) {
                  "Açık • Sessiz saatler (${reminderSettings.quietStartTime} - ${reminderSettings.quietEndTime})"
                } else {
                  "Tüm bildirimler açık"
                },
                fontSize = 11.sp,
                color = if (isMasterActive) BentoGreen else SubLabelColor
              )
            }
          }

            // Tüm Bildirimleri Aç / Kapat (Master on-off button)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = if (isMasterActive) "Açık" else "Kapalı",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isMasterActive) BentoGreen else SubLabelColor,
                modifier = Modifier.padding(end = 6.dp)
              )
              Switch(
                checked = isMasterActive,
                onCheckedChange = { enableAll ->
                  if (enableAll) requestNotificationPermissionIfNeeded()
                  onSaveReminderSettings(
                    reminderSettings.copy(
                      masterReminderEnabled = enableAll
                    )
                  )
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = BentoGreen,
                  uncheckedThumbColor = SubLabelColor,
                  uncheckedTrackColor = Color(0xFF151921),
                  uncheckedBorderColor = CardBorderColor
                ),
                modifier = Modifier.testTag("master_notification_switch")
              )
            }
          }

          if (!isMasterActive) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFEF4444).copy(alpha = 0.12f),
              border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.4f), Color(0xFFEF4444).copy(alpha = 0.2f)))
              ),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.NotificationsOff,
                  contentDescription = null,
                  tint = Color(0xFFEF4444),
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Tüm bildirimler kapatıldı. Bildirim almak için anahtarı açabilirsiniz.",
                  fontSize = 11.sp,
                  color = Color(0xFFFCA5A5)
                )
              }
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ⏰ 1. Saat Ayarlı Kapat (Sessiz Saatler - on-off button)
            ReminderInlineRow(
              title = "Saat Ayarlı Kapat (Sessiz Saatler)",
              subtitle = if (reminderSettings.quietHoursEnabled) {
                "${reminderSettings.quietStartTime} - ${reminderSettings.quietEndTime} arasında bildirimler kapatılır"
              } else {
                "Belirli saat aralığında tüm bildirimleri kapatın"
              },
              icon = Icons.Default.Bedtime,
              iconTint = Color(0xFF818CF8),
              isEnabled = isMasterActive && reminderSettings.quietHoursEnabled,
              timeLabel = "${reminderSettings.quietStartTime} - ${reminderSettings.quietEndTime}",
              onToggle = { enabled ->
                onSaveReminderSettings(reminderSettings.copy(quietHoursEnabled = enabled))
              },
              onTimeClick = {
                openTimePicker(reminderSettings.quietStartTime) { newStart ->
                  openTimePicker(reminderSettings.quietEndTime) { newEnd ->
                    onSaveReminderSettings(
                      reminderSettings.copy(quietStartTime = newStart, quietEndTime = newEnd)
                    )
                  }
                }
              }
            )

            // 💧 2. Su Hatırlatıcısı (Tek satırda toggle ve saat aralığı)
            ReminderInlineRow(
              title = "Su Hatırlatıcısı",
              subtitle = "${reminderSettings.waterStartTime} - ${reminderSettings.waterEndTime} • ${reminderSettings.waterIntervalHours}s ara",
              icon = Icons.Default.WaterDrop,
              iconTint = BentoTeal,
              isEnabled = isMasterActive && reminderSettings.waterReminderEnabled,
              timeLabel = "${reminderSettings.waterStartTime} - ${reminderSettings.waterEndTime}",
              onToggle = { enabled ->
                if (enabled) requestNotificationPermissionIfNeeded()
                onSaveReminderSettings(reminderSettings.copy(waterReminderEnabled = enabled))
              },
              onTimeClick = {
                openTimePicker(reminderSettings.waterStartTime) { newStart ->
                  openTimePicker(reminderSettings.waterEndTime) { newEnd ->
                    onSaveReminderSettings(
                      reminderSettings.copy(waterStartTime = newStart, waterEndTime = newEnd)
                    )
                  }
                }
              }
            )

            // 🥗 3. Öğün Hatırlatıcıları (Tek satırda toggle ve saatler)
            ReminderInlineRow(
              title = "Öğün Hatırlatıcıları",
              subtitle = "Kahvaltı (${reminderSettings.breakfastTime}), Öğle (${reminderSettings.lunchTime}), Akşam (${reminderSettings.dinnerTime})",
              icon = Icons.Default.Restaurant,
              iconTint = BentoAmber,
              isEnabled = isMasterActive && reminderSettings.mealReminderEnabled,
              timeLabel = reminderSettings.lunchTime,
              onToggle = { enabled ->
                if (enabled) requestNotificationPermissionIfNeeded()
                onSaveReminderSettings(reminderSettings.copy(mealReminderEnabled = enabled))
              },
              onTimeClick = {
                openTimePicker(reminderSettings.lunchTime) { newLunch ->
                  onSaveReminderSettings(reminderSettings.copy(lunchTime = newLunch))
                }
              }
            )

            // ⚖️ 4. Haftalık Tartılma Hatırlatıcısı (Tek satırda toggle, gün ve saat)
            ReminderInlineRow(
              title = "Haftalık Tartılma",
              subtitle = "${reminderSettings.weightReminderDay} günü, saat ${reminderSettings.weightReminderTime}",
              icon = Icons.Default.MonitorWeight,
              iconTint = BentoPurple,
              isEnabled = isMasterActive && reminderSettings.weightReminderEnabled,
              timeLabel = "${reminderSettings.weightReminderDay.take(3)} ${reminderSettings.weightReminderTime}",
              onToggle = { enabled ->
                if (enabled) requestNotificationPermissionIfNeeded()
                onSaveReminderSettings(reminderSettings.copy(weightReminderEnabled = enabled))
              },
              onTimeClick = {
                showWeightDayDialog = true
              }
            )

            // 🚶 5. Adım Hatırlatıcısı (Tek satırda toggle ve saat)
            ReminderInlineRow(
              title = "Adım Hatırlatıcısı",
              subtitle = "Her gün saat ${reminderSettings.stepReminderTime}'de",
              icon = Icons.Default.DirectionsWalk,
              iconTint = BentoGreen,
              isEnabled = isMasterActive && reminderSettings.stepReminderEnabled,
              timeLabel = reminderSettings.stepReminderTime,
              onToggle = { enabled ->
                if (enabled) requestNotificationPermissionIfNeeded()
                onSaveReminderSettings(reminderSettings.copy(stepReminderEnabled = enabled))
              },
              onTimeClick = {
                openTimePicker(reminderSettings.stepReminderTime) { newTime ->
                  onSaveReminderSettings(reminderSettings.copy(stepReminderTime = newTime))
                }
              }
            )
          }
        }

      // 6. UYGULAMA KARTI: Sürüm bilgisi ve "Tüm verileri sıfırla" butonu
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f)))
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = SubLabelColor.copy(alpha = 0.16f),
                modifier = Modifier.size(32.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SubLabelColor,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Uygulama Bilgisi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Text(
              text = "Sürüm 1.0.0",
              fontSize = 12.sp,
              color = SubLabelColor
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Kırmızı tonlu, onay isteyen "Tüm verileri sıfırla" butonu
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = BentoOverRed.copy(alpha = 0.12f),
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(BentoOverRed.copy(alpha = 0.4f), BentoOverRed.copy(alpha = 0.2f)))
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showResetConfirmDialog = true }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = BentoOverRed,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Tüm Verileri Sıfırla",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BentoOverRed
              )
            }
          }
        }
      }

      // Alt gezinme çubuğu üstünde ferah boşluk
      Spacer(modifier = Modifier.height(72.dp))
    }

    // 1. Günlük Su Hedefi Dialogu
    if (showWaterTargetDialog) {
      AlertDialog(
        onDismissRequest = { showWaterTargetDialog = false },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
          Text(
            text = "Günlük Su Hedefi",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        text = {
          Column {
            Text(
              text = "Kilonuza göre önerilen otomatik hedef: ${(userProfile.currentWeight * 33f).toInt().coerceIn(1500, 3500)} ml",
              fontSize = 12.sp,
              color = SubLabelColor
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
                onUpdateWaterTarget(0)
                showWaterTargetDialog = false
              }
            ) {
              Text("Otomatik Hesaplamaya Dön (kilo x 33 ml)", color = BentoGreen, fontSize = 12.sp)
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
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoTeal)
          ) {
            Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showWaterTargetDialog = false }) {
            Text("Vazgeç", color = SubLabelColor)
          }
        }
      )
    }

    // 2. Vücut Ölçüsü Düzenleme Dialogu (Boy, Güncel Kilo, Hedef Kilo)
    editMetricType?.let { type ->
      val (dialogTitle, unitText) = when (type) {
        "height" -> "Boy Düzenle" to "cm"
        "weight" -> "Güncel Kilo Düzenle" to "kg"
        else -> "Hedef Kilo Düzenle" to "kg"
      }
      AlertDialog(
        onDismissRequest = { editMetricType = null },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
          Text(text = dialogTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
          Column {
            OutlinedTextField(
              value = editMetricValue,
              onValueChange = { editMetricValue = it.replace(',', '.') },
              label = { Text("Yeni Değer ($unitText)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val parsed = editMetricValue.toFloatOrNull()
              if (parsed != null && parsed > 0f) {
                when (type) {
                  "height" -> onUpdateHeight(parsed)
                  "weight" -> onUpdateCurrentWeight(parsed)
                  "targetWeight" -> onUpdateTargetWeight(parsed)
                }
              }
              editMetricType = null
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
          ) {
            Text("Güncelle", color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { editMetricType = null }) {
            Text("Vazgeç", color = SubLabelColor)
          }
        }
      )
    }

    // 3. Tartılma Günü Seçim Dialogu
    if (showWeightDayDialog) {
      AlertDialog(
        onDismissRequest = { showWeightDayDialog = false },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = {
          Text(text = "Tartılma Günü Seçin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
          Column {
            daysOfWeek.forEach { day ->
              val isSelected = reminderSettings.weightReminderDay.equals(day, ignoreCase = true)
              Surface(
                onClick = {
                  onSaveReminderSettings(reminderSettings.copy(weightReminderDay = day))
                  showWeightDayDialog = false
                  openTimePicker(reminderSettings.weightReminderTime) { newTime ->
                    onSaveReminderSettings(
                      reminderSettings.copy(weightReminderDay = day, weightReminderTime = newTime)
                    )
                  }
                },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) BentoPurple.copy(alpha = 0.16f) else Color.Transparent,
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
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) BentoPurple else MaterialTheme.colorScheme.onSurface
                  )
                  if (isSelected) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = BentoPurple)
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showWeightDayDialog = false }) {
            Text("Kapat", color = SubLabelColor)
          }
        }
      )
    }

    // 4. "Tüm Verileri Sıfırla" Onay Dialogu ("Emin misin?" penceresi)
    if (showResetConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showResetConfirmDialog = false },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        icon = {
          Surface(
            shape = CircleShape,
            color = BentoOverRed.copy(alpha = 0.2f),
            modifier = Modifier.size(52.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = BentoOverRed,
                modifier = Modifier.size(28.dp)
              )
            }
          }
        },
        title = {
          Text(
            text = "Emin misiniz?",
            fontWeight = FontWeight.Bold,
            color = BentoOverRed,
            textAlign = TextAlign.Center
          )
        },
        text = {
          Text(
            text = "Tüm öğün kayıtlarınız, kilo geçmişiniz, su takipleriniz ve profil bilgileriniz kalıcı olarak silinecek ve uygulama fabrika ayarlarına dönecektir. Bu işlem geri alınamaz.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showResetConfirmDialog = false
              onResetAllData()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoOverRed)
          ) {
            Text("Evet, Her Şeyi Sil", color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showResetConfirmDialog = false }) {
            Text("Vazgeç", color = SubLabelColor)
          }
        }
      )
    }

    // 5. Bildirim İzni Gerekli Dialogu
    if (showPermissionRationaleDialog) {
      AlertDialog(
        onDismissRequest = { showPermissionRationaleDialog = false },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBg,
        title = { Text("Bildirim İzni Gerekli", fontWeight = FontWeight.Bold) },
        text = {
          Text(
            text = "Hatırlatıcıları zamanında alabilmeniz için bildirim iznine ihtiyaç duyuyoruz. Dilediğiniz zaman cihaz ayarlarından değiştirebilirsiniz.",
            fontSize = 13.sp,
            color = SubLabelColor
          )
        },
        confirmButton = {
          Button(
            onClick = { showPermissionRationaleDialog = false },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
          ) {
            Text("Anladım", color = Color.White)
          }
        }
      )
    }

    // 6. Rapor Bilgilendirme ve Dönem Seçim BottomSheet'i
    if (showReportInfoSheet) {
      com.example.report.ReportInfoBottomSheet(
        selectedPeriod = selectedReportPeriod,
        customStartDate = customStartDate,
        customEndDate = customEndDate,
        onSelectPeriod = { selectedReportPeriod = it },
        onUpdateCustomDates = { start, end ->
          customStartDate = start
          customEndDate = end
        },
        onConfirmShowReport = {
          showReportInfoSheet = false
          val s = if (selectedReportPeriod == com.example.report.ReportPeriod.CUSTOM) customStartDate else null
          val e = if (selectedReportPeriod == com.example.report.ReportPeriod.CUSTOM) customEndDate else null
          onOpenReport(selectedReportPeriod, s, e)
        },
        onDownloadPdf = {
          val s = if (selectedReportPeriod == com.example.report.ReportPeriod.CUSTOM) customStartDate else null
          val e = if (selectedReportPeriod == com.example.report.ReportPeriod.CUSTOM) customEndDate else null
          onExportReport(selectedReportPeriod, s, e, true)
        },
        onDownloadJpg = {
          val s = if (selectedReportPeriod == com.example.report.ReportPeriod.CUSTOM) customStartDate else null
          val e = if (selectedReportPeriod == com.example.report.ReportPeriod.CUSTOM) customEndDate else null
          onExportReport(selectedReportPeriod, s, e, false)
        },
        isExporting = isExporting,
        onDismiss = { showReportInfoSheet = false }
      )
    }

    if (isExporting) {
      com.example.report.ExportProgressDialog(isPdf = true)
    }

    exportResult?.let { res ->
      com.example.report.ExportSuccessDialog(
        result = res,
        onOpen = { com.example.report.ReportExportHelper.openFile(context, res.uri, res.mimeType) },
        onShare = { com.example.report.ReportExportHelper.shareFile(context, res.uri, res.mimeType) },
        onDismiss = onDismissExportResult
      )
    }

    exportError?.let { err ->
      com.example.report.ExportErrorDialog(
        errorMessage = err,
        onDismiss = onDismissExportError
      )
    }

    // 7. Metabolizma ve Kalori Analizi Bilgi Kartı BottomSheet'i
    selectedMetricInfo?.let { metricType ->
      MetricInfoBottomSheet(
        metricType = metricType,
        userProfile = userProfile,
        metrics = metrics,
        onDismiss = { selectedMetricInfo = null }
      )
    }
  }
}

@Composable
private fun BodyMetricTile(
  label: String,
  value: String,
  unit: String,
  icon: ImageVector,
  color: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Card(
    modifier = modifier
      .height(98.dp)
      .clickable { onClick() },
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CardBg),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.verticalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = label,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = SubLabelColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(15.dp)
        )
      }

      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = value,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = color,
          maxLines = 1
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
          text = unit,
          fontSize = 11.sp,
          color = SubLabelColor,
          modifier = Modifier.padding(bottom = 1.dp)
        )
      }

      Text(
        text = "Dokun: düzenle",
        fontSize = 9.sp,
        color = SubLabelColor.copy(alpha = 0.7f),
        maxLines = 1
      )
    }
  }
}

@Composable
private fun CalculatedMetricBox(
  title: String,
  value: String,
  badgeText: String,
  badgeColor: Color,
  desc: String,
  icon: ImageVector,
  iconTint: Color,
  modifier: Modifier = Modifier,
  testTag: String = "",
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = ItemBg,
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
    ),
    modifier = modifier
      .height(106.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Üst satır: Sol tarafta ikon + başlık, sağ tarafta kompakt hap etiket ve minik info ikonu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = SubLabelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = badgeColor.copy(alpha = 0.16f)
          ) {
            Text(
              text = badgeText,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = badgeColor,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.width(3.dp))
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Bilgi",
            tint = SubLabelColor.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp)
          )
        }
      }

      // Orta satır: Büyük, net sayı değeri
      Text(
        text = value,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1
      )

      // Alt satır: Açıklama ve dokunma ipucu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = desc,
          fontSize = 10.sp,
          color = SubLabelColor.copy(alpha = 0.85f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false)
        )
        Text(
          text = "Detay →",
          fontSize = 9.sp,
          fontWeight = FontWeight.SemiBold,
          color = iconTint.copy(alpha = 0.85f),
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun MacroTargetPill(
  title: String,
  grams: String,
  percent: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = ItemBg,
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
    ),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        fontSize = 11.sp,
        color = SubLabelColor
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = grams,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = color
      )
      Spacer(modifier = Modifier.height(2.dp))
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f)
      ) {
        Text(
          text = percent,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = color,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
      }
    }
  }
}

@Composable
private fun ReminderInlineRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  isEnabled: Boolean,
  timeLabel: String,
  onToggle: (Boolean) -> Unit,
  onTimeClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = ItemBg,
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
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
          modifier = Modifier.size(34.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = iconTint,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.padding(end = 8.dp)) {
          Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = subtitle,
            fontSize = 10.sp,
            color = SubLabelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isEnabled) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardBorderColor.copy(alpha = 0.7f),
            modifier = Modifier
              .clickable { onTimeClick() }
              .padding(end = 8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = timeLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(3.dp))
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = SubLabelColor,
                modifier = Modifier.size(11.dp)
              )
            }
          }
        }

        Switch(
          checked = isEnabled,
          onCheckedChange = onToggle,
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = BentoGreen
          )
        )
      }
    }
  }
}
