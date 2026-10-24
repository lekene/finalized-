package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.reminder.ReminderPreferences
import com.example.reminder.ReminderSettings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.data.BackupDataHelper
import com.example.ui.theme.ACCENT_PRESETS
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalThemePreferences
import com.example.ui.theme.ThemeState
import com.example.ui.components.AcilirKart
import com.example.ui.avatar.UserAvatarView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Color palette matching Summary, Profile & Journal Bento style
private val BentoGreen: Color
  @Composable get() = AppTheme.colors.accent
private val BentoBlue = Color(0xFF38BDF8)
private val BentoAmber = Color(0xFFFBBF24)
private val BentoPurple = Color(0xFFA855F7)
private val BentoCyan = Color(0xFF06B6D4)
private val CardBg: Color
  @Composable get() = AppTheme.colors.surface
private val ItemBg: Color
  @Composable get() = AppTheme.colors.itemBg
private val CardBorderColor: Color
  @Composable get() = AppTheme.colors.border
private val SubLabelColor: Color
  @Composable get() = AppTheme.colors.textSecondary

@Composable
fun SettingsScreen(
  reminderSettings: ReminderSettings = ReminderSettings(),
  onSetNotificationMaster: (Boolean) -> Unit = {},
  onSetTemporarySnooze: (Long) -> Unit = {},
  onClearTemporarySnooze: () -> Unit = {},
  onSetQuietHours: (Boolean, String, String) -> Unit = { _, _, _ -> },
  onSaveReminderSettings: (ReminderSettings) -> Unit = {},
  allProfiles: List<com.example.data.UserProfile> = emptyList(),
  activeProfileId: Int = 1,
  onSwitchProfile: (Int) -> Unit = {},
  onAddNewProfile: () -> Unit = {},
  onUpdateProfileName: (Int, String) -> Unit = { _, _ -> },
  onDeleteProfile: (Int, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
  themeState: ThemeState? = null,
  onSetThemeAccent: (Int) -> Unit = {},
  onSetOledEnabled: (Boolean) -> Unit = {},
  isAddStepCaloriesToTargetEnabled: Boolean = false,
  onSetAddStepCaloriesToTarget: (Boolean) -> Unit = {},
  lastBackupDateText: String = "Henüz yedek alınmadı",
  onExportBackup: suspend () -> String = { "" },
  onRestoreBackup: suspend (String) -> Result<Unit> = { Result.success(Unit) },
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  val themePrefs = runCatching { LocalThemePreferences.current }.getOrNull()
  val fallbackThemeState by (themePrefs?.themeState ?: remember { kotlinx.coroutines.flow.MutableStateFlow(ThemeState()) }).collectAsState()
  val activeTheme = themeState ?: fallbackThemeState
  val currentAccentIndex = activeTheme.accentIndex
  val isOledActive = activeTheme.isOled

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  // Backup dialog states
  var showExportPreInfoDialog by remember { mutableStateOf(false) }
  var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
  var pendingRestoreSummary by remember { mutableStateOf<String?>(null) }
  var errorMessageDialog by remember { mutableStateOf<String?>(null) }
  var successMessageDialog by remember { mutableStateOf<String?>(null) }
  var isExporting by remember { mutableStateOf(false) }
  var isRestoring by remember { mutableStateOf(false) }

  // SAF Create Document launcher for Export
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        isExporting = true
        try {
          val json = onExportBackup()
          context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(json.toByteArray(Charsets.UTF_8))
            os.flush()
          }
          successMessageDialog = "Yedek dosyanız başarıyla kaydedildi."
        } catch (t: Throwable) {
          errorMessageDialog = "Yedek kaydedilirken bir hata oluştu: ${t.localizedMessage ?: "Bilinmeyen hata"}"
        } finally {
          isExporting = false
        }
      }
    }
  }

  // SAF Open Document launcher for Restore
  val openDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        isRestoring = true
        try {
          val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
          if (json.isNullOrBlank()) {
            errorMessageDialog = "Seçilen dosya boş veya okunamadı."
          } else {
            val parseResult = BackupDataHelper.parseAndValidate(json)
            if (parseResult.isFailure) {
              errorMessageDialog = parseResult.exceptionOrNull()?.localizedMessage ?: "Bozuk ya da uyumsuz yedek dosyası."
            } else {
              val payload = parseResult.getOrThrow()
              val summaryText = buildString {
                append("• ${payload.profiles.size} Profil\n")
                append("• ${payload.meals.size} Öğün kaydı\n")
                append("• ${payload.waterLogs.size} Su kaydı\n")
                append("• ${payload.weightLogs.size} Kilo kaydı\n")
                append("• ${payload.stepLogs.size} Adım kaydı\n")
                if (payload.nutritionCache.isNotEmpty()) {
                  append("• ${payload.nutritionCache.size} Besin ve favori kaydı\n")
                }
                if (payload.exportedDate.isNotBlank()) {
                  append("Yedek Tarihi: ${payload.exportedDate}")
                }
              }
              pendingRestoreJson = json
              pendingRestoreSummary = summaryText
            }
          }
        } catch (t: Throwable) {
          errorMessageDialog = "Dosya okunurken hata oluştu: ${t.localizedMessage ?: "Bilinmeyen hata"}"
        } finally {
          isRestoring = false
        }
      }
    }
  }

  // Multi-profile dialog states
  var editingProfileForName by remember { mutableStateOf<com.example.data.UserProfile?>(null) }
  var newProfileNameInput by remember { mutableStateOf("") }
  var deletingProfile by remember { mutableStateOf<com.example.data.UserProfile?>(null) }

  // App version resolution
  val appVersion = try {
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    pInfo.versionName ?: "1.0.0"
  } catch (_: Exception) {
    "1.0.0"
  }

  // Notification permission check (including Android 13+ and system notification channel state)
  var hasNotificationPermission by remember {
    mutableStateOf(checkNotificationPermission(context))
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasNotificationPermission = checkNotificationPermission(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasNotificationPermission = isGranted || checkNotificationPermission(context)
  }

  // Periodic clock state to live-update snooze countdown
  var currentMillis by remember { mutableStateOf(System.currentTimeMillis()) }
  LaunchedEffect(reminderSettings.snoozeUntilMillis) {
    while (reminderSettings.snoozeUntilMillis > System.currentTimeMillis()) {
      delay(1000L)
      currentMillis = System.currentTimeMillis()
    }
    currentMillis = System.currentTimeMillis()
  }

  val isSnoozed = reminderSettings.snoozeUntilMillis > currentMillis

  // Temporary mute inputs
  var hoursInput by remember { mutableStateOf("") }
  var minutesInput by remember { mutableStateOf("") }
  var showWeightDayDialog by remember { mutableStateOf(false) }
  val daysOfWeek = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

  // Helper for native 24-hr TimePickerDialog
  fun openTimePicker(currentTimeStr: String, onTimeSelected: (String) -> Unit) {
    val parts = currentTimeStr.split(":")
    val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 23
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

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // 1. Üst Çubuk (Geri Oku & Başlık)
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(40.dp)
            .testTag("settings_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Geri",
            tint = MaterialTheme.colorScheme.onBackground
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Uygulama Ayarları",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    }

    // 2. Kaydırılabilir Ayarlar İçeriği
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .padding(bottom = 36.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. TEMA KARTI
      ThemeSettingCard(
        accentIndex = currentAccentIndex,
        isOled = isOledActive,
        onSelectAccent = { idx ->
          onSetThemeAccent(idx)
          themePrefs?.setAccentIndex(idx)
        },
        onToggleOled = { enabled ->
          onSetOledEnabled(enabled)
          themePrefs?.setOledEnabled(enabled)
        }
      )

      val notificationsSummary = if (isSnoozed) {
        val formattedTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(reminderSettings.snoozeUntilMillis))
        "${formattedTime}'e kadar sessiz"
      } else if (reminderSettings.masterReminderEnabled) {
        "Tümü açık"
      } else {
        "Kapalı"
      }

      // 3. BİLDİRİMLER KARTI (Açılır Kart)
      AcilirKart(
        title = "Bildirimler",
        icon = Icons.Default.Notifications,
        summary = notificationsSummary,
        showWarningBadge = !hasNotificationPermission,
        iconTint = AppTheme.colors.accent,
        testTag = "settings_notifications_card"
      ) {

          // UYARI: Android 13+ veya sistem bildirim izni kapalıysa uyarı ve sistem ayarlarını açan buton
          if (!hasNotificationPermission) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color(0xFFEF4444).copy(alpha = 0.12f),
              border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                  listOf(Color(0xFFEF4444).copy(alpha = 0.45f), Color(0xFFEF4444).copy(alpha = 0.2f))
                )
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("notification_permission_warning_box")
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Bildirim izni kapalı",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCA5A5)
                  )
                }

                Text(
                  text = "Uygulama hatırlatıcılarının (su, öğün, tartılma) iletilebilmesi için cihazınızdan bildirim iznini açmanız gerekir.",
                  fontSize = 11.sp,
                  color = SubLabelColor,
                  lineHeight = 15.sp
                )

                Button(
                  onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                      permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    openSystemNotificationSettings(context)
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("settings_notification_permission_button"),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.85f),
                    contentColor = Color.White
                  ),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Bildirim Ayarlarını Aç",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          // 1. "TÜM BİLDİRİMLER" ANA ANAHTARI (Aç/Kapa Switch)
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Surface(
                  shape = CircleShape,
                  color = if (reminderSettings.masterReminderEnabled) BentoGreen.copy(alpha = 0.16f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                  modifier = Modifier.size(34.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = if (reminderSettings.masterReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                      contentDescription = null,
                      tint = if (reminderSettings.masterReminderEnabled) BentoGreen else Color(0xFFEF4444),
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.padding(end = 6.dp)) {
                  Text(
                    text = "Tüm bildirimler",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = if (reminderSettings.masterReminderEnabled) {
                      "Açık • Hatırlatıcılar kendi ayarlarına göre çalışır"
                    } else {
                      "Kapalı • Hiçbir bildirim gönderilmez"
                    },
                    fontSize = 11.sp,
                    color = SubLabelColor
                  )
                }
              }

              Switch(
                checked = reminderSettings.masterReminderEnabled,
                onCheckedChange = { isChecked ->
                  if (isChecked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                  }
                  onSetNotificationMaster(isChecked)
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = BentoGreen,
                  uncheckedThumbColor = SubLabelColor,
                  uncheckedTrackColor = Color(0xFF151921),
                  uncheckedBorderColor = CardBorderColor
                ),
                modifier = Modifier.testTag("settings_master_notifications_switch")
              )
            }
          }

          // 2. "GEÇİCİ OLARAK SESSİZE AL" KUTUSU
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(
                if (isSnoozed) {
                  listOf(BentoAmber.copy(alpha = 0.6f), BentoAmber.copy(alpha = 0.3f))
                } else {
                  listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f))
                }
              )
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("settings_snooze_section")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = CircleShape,
                  color = BentoAmber.copy(alpha = 0.16f),
                  modifier = Modifier.size(32.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.HourglassBottom,
                      contentDescription = null,
                      tint = BentoAmber,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Geçici olarak sessize al",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Belirlenen süre boyunca bildirimler sessizce atlanır.",
                    fontSize = 11.sp,
                    color = SubLabelColor
                  )
                }
              }

              if (isSnoozed) {
                // Aktif Sessize Alma Durumu: "Bildirimler HH:MM'e kadar kapalı" ve "Şimdi aç" butonu
                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reminderSettings.snoozeUntilMillis))
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = BentoAmber.copy(alpha = 0.12f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = BentoAmber,
                        modifier = Modifier.size(20.dp)
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Column {
                        Text(
                          text = "Bildirimler $formattedTime'e kadar kapalı",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = BentoAmber,
                          modifier = Modifier.testTag("settings_snoozed_status_text")
                        )
                        Text(
                          text = "Süre dolunca bildirimler otomatik devam eder",
                          fontSize = 11.sp,
                          color = SubLabelColor
                        )
                      }
                    }

                    Button(
                      onClick = {
                        onClearTemporarySnooze()
                        Toast.makeText(context, "Bildirimler yeniden açıldı", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier
                        .height(38.dp)
                        .testTag("settings_snooze_unmute_button"),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = BentoGreen,
                        contentColor = Color.Black
                      ),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Şimdi aç",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              } else {
                // Sessize alma süresi girme formu (Saat ve Dakika)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Saat Girişi
                  OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { input ->
                      if (input.all { it.isDigit() } && input.length <= 2) {
                        hoursInput = input
                      }
                    },
                    label = { Text("Saat", fontSize = 11.sp) },
                    placeholder = { Text("0", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = BentoAmber,
                      unfocusedBorderColor = CardBorderColor,
                      focusedLabelColor = BentoAmber,
                      unfocusedLabelColor = SubLabelColor,
                      cursorColor = BentoAmber
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("settings_snooze_hours_input")
                  )

                  // Dakika Girişi
                  OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { input ->
                      if (input.all { it.isDigit() } && input.length <= 3) {
                        minutesInput = input
                      }
                    },
                    label = { Text("Dakika", fontSize = 11.sp) },
                    placeholder = { Text("30", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = BentoAmber,
                      unfocusedBorderColor = CardBorderColor,
                      focusedLabelColor = BentoAmber,
                      unfocusedLabelColor = SubLabelColor,
                      cursorColor = BentoAmber
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("settings_snooze_minutes_input")
                  )

                  // "Sessize al" Butonu
                  Button(
                    onClick = {
                      val h = hoursInput.trim().toIntOrNull() ?: 0
                      val m = minutesInput.trim().toIntOrNull() ?: 0
                      val totalMinutes = h * 60 + m

                      if (hoursInput.isBlank() && minutesInput.isBlank()) {
                        Toast.makeText(context, "Lütfen bir süre girin (saat veya dakika).", Toast.LENGTH_SHORT).show()
                      } else if (totalMinutes <= 0) {
                        Toast.makeText(context, "Süre 0 olamaz. En az 1 dakika girin.", Toast.LENGTH_SHORT).show()
                      } else if (totalMinutes > 72 * 60) {
                        Toast.makeText(context, "Süre en fazla 72 saat (3 gün) olabilir.", Toast.LENGTH_SHORT).show()
                      } else {
                        val untilMillis = System.currentTimeMillis() + (totalMinutes * 60 * 1000L)
                        onSetTemporarySnooze(untilMillis)
                        hoursInput = ""
                        minutesInput = ""
                        val endStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(untilMillis))
                        Toast.makeText(context, "Bildirimler $endStr'e kadar sessize alındı.", Toast.LENGTH_SHORT).show()
                      }
                    },
                    modifier = Modifier
                      .height(54.dp)
                      .testTag("settings_snooze_submit_button"),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = BentoAmber,
                      contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.NotificationsPaused,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Sessize al",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                // Hızlı seçim hazır süre butonları (Kullanım kolaylığı)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  listOf("30 dk" to 30, "1 saat" to 60, "2 saat" to 120, "8 saat" to 480).forEach { (label, mins) ->
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = CardBorderColor.copy(alpha = 0.5f),
                      modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                          hoursInput = (mins / 60).let { if (it > 0) it.toString() else "" }
                          minutesInput = (mins % 60).let { if (it > 0) it.toString() else "" }
                        }
                    ) {
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                      ) {
                        Text(
                          text = label,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Medium,
                          color = SubLabelColor
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // 3. "SESSİZ SAATLER" SATIRI (Aç/Kapa Anahtarı & Başlangıç/Bitiş Seçici)
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Sessiz saatler aç/kapa satırı
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = BentoBlue.copy(alpha = 0.16f),
                    modifier = Modifier.size(34.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = BentoBlue,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.padding(end = 6.dp)) {
                    Text(
                      text = "Sessiz saatler",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (reminderSettings.quietHoursEnabled) {
                        "${reminderSettings.quietStartTime} - ${reminderSettings.quietEndTime} arasında bildirim gönderilmez"
                      } else {
                        "Kapalı • Gece aralıklarında bildirim gönderilir"
                      },
                      fontSize = 11.sp,
                      color = SubLabelColor
                    )
                  }
                }

                Switch(
                  checked = reminderSettings.quietHoursEnabled,
                  onCheckedChange = { isChecked ->
                    onSetQuietHours(
                      isChecked,
                      reminderSettings.quietStartTime,
                      reminderSettings.quietEndTime
                    )
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BentoBlue,
                    uncheckedThumbColor = SubLabelColor,
                    uncheckedTrackColor = Color(0xFF151921),
                    uncheckedBorderColor = CardBorderColor
                  ),
                  modifier = Modifier.testTag("settings_quiet_hours_switch")
                )
              }

              // Başlangıç ve bitiş saati seçici çipleri (varsayılan 23:00 - 07:00)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBorderColor.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Başlangıç saati
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                      brush = Brush.horizontalGradient(listOf(BentoBlue.copy(alpha = 0.5f), BentoBlue.copy(alpha = 0.2f)))
                    ),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.quietStartTime) { newStart ->
                          onSetQuietHours(
                            reminderSettings.quietHoursEnabled,
                            newStart,
                            reminderSettings.quietEndTime
                          )
                        }
                      }
                      .testTag("settings_quiet_start_chip")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text(
                        text = "Başlangıç: ",
                        fontSize = 11.sp,
                        color = SubLabelColor
                      )
                      Text(
                        text = reminderSettings.quietStartTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoBlue
                      )
                    }
                  }

                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SubLabelColor,
                    modifier = Modifier.size(16.dp)
                  )

                  // Bitiş saati
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                      brush = Brush.horizontalGradient(listOf(BentoBlue.copy(alpha = 0.5f), BentoBlue.copy(alpha = 0.2f)))
                    ),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.quietEndTime) { newEnd ->
                          onSetQuietHours(
                            reminderSettings.quietHoursEnabled,
                            reminderSettings.quietStartTime,
                            newEnd
                          )
                        }
                      }
                      .testTag("settings_quiet_end_chip")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text(
                        text = "Bitiş: ",
                        fontSize = 11.sp,
                        color = SubLabelColor
                      )
                      Text(
                        text = reminderSettings.quietEndTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoBlue
                      )
                    }
                  }
                }
              }

              Text(
                text = "Gece yarısını aşan aralıklar (ör. 23:00 - 07:00) otomatik olarak desteklenir.",
                fontSize = 10.sp,
                color = SubLabelColor,
                modifier = Modifier.padding(horizontal = 4.dp)
              )
            }
          }

          // 4. "ADIM HATIRLATICISI" (Aç/Kapa Anahtarı & Saat Seçici - Varsayılan Açık ve 21:00)
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("settings_step_reminder_row")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = BentoGreen.copy(alpha = 0.16f),
                    modifier = Modifier.size(34.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = BentoGreen,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.padding(end = 6.dp)) {
                    Text(
                      text = "Adım hatırlatıcısı",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (reminderSettings.stepReminderEnabled) {
                        "Her gün saat ${reminderSettings.stepReminderTime}'de bildirim gönderilir"
                      } else {
                        "Kapalı • Adım hatırlatma bildirimi gönderilmez"
                      },
                      fontSize = 11.sp,
                      color = if (reminderSettings.stepReminderEnabled) BentoGreen else SubLabelColor
                    )
                  }
                }

                Switch(
                  checked = reminderSettings.stepReminderEnabled,
                  onCheckedChange = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                      permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onSaveReminderSettings(
                      reminderSettings.copy(stepReminderEnabled = isChecked)
                    )
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BentoGreen,
                    uncheckedThumbColor = SubLabelColor,
                    uncheckedTrackColor = Color(0xFF151921),
                    uncheckedBorderColor = CardBorderColor
                  ),
                  modifier = Modifier.testTag("settings_step_reminder_switch")
                )
              }

              // Saat seçici satırı (Varsayılan 21:00)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBorderColor.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Hatırlatma saati",
                    fontSize = 11.sp,
                    color = SubLabelColor
                  )

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                      brush = Brush.horizontalGradient(
                        listOf(BentoGreen.copy(alpha = 0.5f), BentoGreen.copy(alpha = 0.2f))
                      )
                    ),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.stepReminderTime) { newTime ->
                          onSaveReminderSettings(
                            reminderSettings.copy(stepReminderTime = newTime)
                          )
                        }
                      }
                      .testTag("settings_step_reminder_time_chip")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = BentoGreen,
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = reminderSettings.stepReminderTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoGreen
                      )
                    }
                  }
                }
              }
            }
          }

          // 5. "SU HATIRLATICISI"
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
            ),
            modifier = Modifier.fillMaxWidth().testTag("settings_water_reminder_row")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = BentoBlue.copy(alpha = 0.16f),
                    modifier = Modifier.size(34.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = BentoBlue,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.padding(end = 6.dp)) {
                    Text(
                      text = "Su hatırlatıcısı",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (reminderSettings.waterReminderEnabled) {
                        "${reminderSettings.waterStartTime} - ${reminderSettings.waterEndTime} • ${reminderSettings.waterIntervalHours}s ara ile"
                      } else {
                        "Kapalı • Su hatırlatması gönderilmez"
                      },
                      fontSize = 11.sp,
                      color = if (reminderSettings.waterReminderEnabled) BentoBlue else SubLabelColor
                    )
                  }
                }

                Switch(
                  checked = reminderSettings.waterReminderEnabled,
                  onCheckedChange = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                      permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onSaveReminderSettings(
                      reminderSettings.copy(waterReminderEnabled = isChecked)
                    )
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BentoBlue,
                    uncheckedThumbColor = SubLabelColor,
                    uncheckedTrackColor = Color(0xFF151921),
                    uncheckedBorderColor = CardBorderColor
                  ),
                  modifier = Modifier.testTag("settings_water_reminder_switch")
                )
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBorderColor.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.waterStartTime) { newStart ->
                          onSaveReminderSettings(reminderSettings.copy(waterStartTime = newStart))
                        }
                      }
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text("Başlangıç: ", fontSize = 11.sp, color = SubLabelColor)
                      Text(reminderSettings.waterStartTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoBlue)
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.waterEndTime) { newEnd ->
                          onSaveReminderSettings(reminderSettings.copy(waterEndTime = newEnd))
                        }
                      }
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text("Bitiş: ", fontSize = 11.sp, color = SubLabelColor)
                      Text(reminderSettings.waterEndTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoBlue)
                    }
                  }
                }
              }
            }
          }

          // 6. "ÖĞÜN HATIRLATICILARI"
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
            ),
            modifier = Modifier.fillMaxWidth().testTag("settings_meal_reminder_row")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = BentoAmber.copy(alpha = 0.16f),
                    modifier = Modifier.size(34.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = BentoAmber,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.padding(end = 6.dp)) {
                    Text(
                      text = "Öğün hatırlatıcıları",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (reminderSettings.mealReminderEnabled) {
                        "Kahvaltı (${reminderSettings.breakfastTime}), Öğle (${reminderSettings.lunchTime}), Akşam (${reminderSettings.dinnerTime})"
                      } else {
                        "Kapalı • Öğün hatırlatmaları gönderilmez"
                      },
                      fontSize = 11.sp,
                      color = if (reminderSettings.mealReminderEnabled) BentoAmber else SubLabelColor
                    )
                  }
                }

                Switch(
                  checked = reminderSettings.mealReminderEnabled,
                  onCheckedChange = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                      permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onSaveReminderSettings(
                      reminderSettings.copy(mealReminderEnabled = isChecked)
                    )
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BentoAmber,
                    uncheckedThumbColor = SubLabelColor,
                    uncheckedTrackColor = Color(0xFF151921),
                    uncheckedBorderColor = CardBorderColor
                  ),
                  modifier = Modifier.testTag("settings_meal_reminder_switch")
                )
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBorderColor.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.breakfastTime) { newTime ->
                          onSaveReminderSettings(reminderSettings.copy(breakfastTime = newTime))
                        }
                      }
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Text("Kahvaltı", fontSize = 10.sp, color = SubLabelColor)
                      Text(reminderSettings.breakfastTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoAmber)
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.lunchTime) { newTime ->
                          onSaveReminderSettings(reminderSettings.copy(lunchTime = newTime))
                        }
                      }
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Text("Öğle", fontSize = 10.sp, color = SubLabelColor)
                      Text(reminderSettings.lunchTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoAmber)
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.dinnerTime) { newTime ->
                          onSaveReminderSettings(reminderSettings.copy(dinnerTime = newTime))
                        }
                      }
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Text("Akşam", fontSize = 10.sp, color = SubLabelColor)
                      Text(reminderSettings.dinnerTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoAmber)
                    }
                  }
                }
              }
            }
          }

          // 7. "HAFTALIK TARTILMA"
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
            ),
            modifier = Modifier.fillMaxWidth().testTag("settings_weight_reminder_row")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = BentoPurple.copy(alpha = 0.16f),
                    modifier = Modifier.size(34.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = BentoPurple,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.padding(end = 6.dp)) {
                    Text(
                      text = "Haftalık tartılma",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (reminderSettings.weightReminderEnabled) {
                        "${reminderSettings.weightReminderDay} günü, saat ${reminderSettings.weightReminderTime}"
                      } else {
                        "Kapalı • Tartılma hatırlatması gönderilmez"
                      },
                      fontSize = 11.sp,
                      color = if (reminderSettings.weightReminderEnabled) BentoPurple else SubLabelColor
                    )
                  }
                }

                Switch(
                  checked = reminderSettings.weightReminderEnabled,
                  onCheckedChange = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                      permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onSaveReminderSettings(
                      reminderSettings.copy(weightReminderEnabled = isChecked)
                    )
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BentoPurple,
                    uncheckedThumbColor = SubLabelColor,
                    uncheckedTrackColor = Color(0xFF151921),
                    uncheckedBorderColor = CardBorderColor
                  ),
                  modifier = Modifier.testTag("settings_weight_reminder_switch")
                )
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBorderColor.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable { showWeightDayDialog = true }
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text("Gün: ", fontSize = 11.sp, color = SubLabelColor)
                      Text(reminderSettings.weightReminderDay, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoPurple)
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ItemBg,
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.weightReminderTime) { newTime ->
                          onSaveReminderSettings(reminderSettings.copy(weightReminderTime = newTime))
                        }
                      }
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text("Saat: ", fontSize = 11.sp, color = SubLabelColor)
                      Text(reminderSettings.weightReminderTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoPurple)
                    }
                  }
                }
              }
            }
          }

          // 8. "AYLIK RAPOR BİLDİRİMİ" (Aç/Kapa Anahtarı & Saat Seçici - Varsayılan Açık ve 10:00)
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("settings_monthly_report_reminder_row")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = BentoGreen.copy(alpha = 0.16f),
                    modifier = Modifier.size(36.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = BentoGreen,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.padding(end = 6.dp)) {
                    Text(
                      text = "Aylık rapor bildirimi",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (reminderSettings.monthlyReportReminderEnabled) {
                        "Her ayın 1'inde saat ${reminderSettings.monthlyReportReminderTime}'da geçen ayın özetini hatırlatır"
                      } else {
                        "Kapalı • Aylık rapor bildirimi gönderilmez"
                      },
                      fontSize = 11.sp,
                      color = if (reminderSettings.monthlyReportReminderEnabled) BentoGreen else SubLabelColor
                    )
                  }
                }

                Switch(
                  checked = reminderSettings.monthlyReportReminderEnabled,
                  onCheckedChange = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                      permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onSaveReminderSettings(
                      reminderSettings.copy(monthlyReportReminderEnabled = isChecked)
                    )
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BentoGreen,
                    uncheckedThumbColor = SubLabelColor,
                    uncheckedTrackColor = Color(0xFF151921),
                    uncheckedBorderColor = CardBorderColor
                  ),
                  modifier = Modifier.testTag("settings_monthly_report_reminder_switch")
                )
              }

              // Saat seçici satırı (Varsayılan 10:00)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBorderColor.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Bildirim Saati (Ayın 1'i)",
                    fontSize = 12.sp,
                    color = SubLabelColor
                  )

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoGreen.copy(alpha = 0.16f),
                    border = BorderStroke(
                      1.dp,
                      BentoGreen.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        openTimePicker(reminderSettings.monthlyReportReminderTime) { newTime ->
                          onSaveReminderSettings(
                            reminderSettings.copy(monthlyReportReminderTime = newTime)
                          )
                        }
                      }
                      .testTag("settings_monthly_report_time_chip")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = BentoGreen,
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = reminderSettings.monthlyReportReminderTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoGreen
                      )
                    }
                  }
                }
              }
            }
          }
        }

      // 4. ADIM & KALORİ HEDEFİ BÖLÜMÜ
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("settings_step_calorie_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f)))
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Surface(
              shape = CircleShape,
              color = BentoGreen.copy(alpha = 0.16f),
              modifier = Modifier.size(34.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.DirectionsWalk,
                  contentDescription = null,
                  tint = BentoGreen,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Adım & Kalori Hedefi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Günlük aktivite ve kalori dengesi",
                fontSize = 11.sp,
                color = SubLabelColor
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                  Text(
                    text = "Adımdan yakılan kaloriyi günlük hedefe ekle",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = if (isAddStepCaloriesToTargetEnabled) "Açık (Maksimum %25 eklenir)" else "Kapalı (Varsayılan)",
                    fontSize = 11.sp,
                    color = if (isAddStepCaloriesToTargetEnabled) BentoGreen else SubLabelColor
                  )
                }
                Switch(
                  checked = isAddStepCaloriesToTargetEnabled,
                  onCheckedChange = { onSetAddStepCaloriesToTarget(it) },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BentoGreen,
                    uncheckedThumbColor = SubLabelColor,
                    uncheckedTrackColor = Color(0xFF262C3A)
                  ),
                  modifier = Modifier.testTag("settings_step_calories_switch")
                )
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Açıldığında, adımlarınızdan yakılan kalori günlük kalori hedefinize eklenir. Aktivite seviyesi zaten temel hedefe dahil olduğundan ve çift sayımı önlemek için hedefin en fazla %25'i kadar ekleme yapılır.",
                fontSize = 11.sp,
                color = SubLabelColor,
                lineHeight = 15.sp
              )
            }
          }
        }
      }

      // 5. PROFİLLER BÖLÜMÜ
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("settings_profiles_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.5f)))
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Kart Başlığı
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = Color(0xFFF97316).copy(alpha = 0.16f),
                modifier = Modifier.size(34.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Profiller",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${allProfiles.size} profil mevcut • Dokunarak geçiş yapın",
                  fontSize = 11.sp,
                  color = SubLabelColor
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = BentoGreen.copy(alpha = 0.14f)
            ) {
              Text(
                text = "${allProfiles.size} Profil",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BentoGreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          // Profil Listesi
          allProfiles.forEach { profile ->
            val isCurrent = profile.id == activeProfileId
            val initialLetter = (profile.name.firstOrNull() ?: 'P').uppercase()

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = ItemBg,
              border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                  if (isCurrent) {
                    listOf(BentoGreen.copy(alpha = 0.7f), BentoGreen.copy(alpha = 0.3f))
                  } else {
                    listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f))
                  }
                )
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                  if (!isCurrent) {
                    onSwitchProfile(profile.id)
                    Toast.makeText(context, "${profile.name} profiline geçildi", Toast.LENGTH_SHORT).show()
                  }
                }
                .testTag("settings_profile_item_${profile.id}")
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
                  // Avatar
                  UserAvatarView(
                    avatarUri = profile.avatarUri,
                    name = profile.name,
                    size = 38.dp,
                    testTag = "settings_profile_avatar_${profile.id}"
                  )

                  Spacer(modifier = Modifier.width(12.dp))

                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = profile.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) BentoGreen else MaterialTheme.colorScheme.onSurface
                      )
                      if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = BentoGreen.copy(alpha = 0.18f)
                        ) {
                          Text(
                            text = "Aktif",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${profile.gender} • ${profile.currentWeight.toInt()} kg • Hedef: ${profile.targetWeight.toInt()} kg",
                      fontSize = 11.sp,
                      color = SubLabelColor
                    )
                  }
                }

                // Eylem Butonları: Adı Düzenle & Sil
                Row(verticalAlignment = Alignment.CenterVertically) {
                  // Adı Düzenle
                  IconButton(
                    onClick = {
                      editingProfileForName = profile
                      newProfileNameInput = profile.name
                    },
                    modifier = Modifier
                      .size(36.dp)
                      .testTag("edit_profile_name_${profile.id}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = "Adı Düzenle",
                      tint = BentoBlue,
                      modifier = Modifier.size(17.dp)
                    )
                  }

                  // Sil Butonu (Son kalan profil silinemez)
                  IconButton(
                    onClick = {
                      if (allProfiles.size <= 1) {
                        Toast.makeText(context, "Son kalan profil silinemez.", Toast.LENGTH_SHORT).show()
                      } else {
                        deletingProfile = profile
                      }
                    },
                    modifier = Modifier
                      .size(36.dp)
                      .testTag("delete_profile_${profile.id}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Profili Sil",
                      tint = if (allProfiles.size > 1) Color(0xFFEF4444) else SubLabelColor.copy(alpha = 0.35f),
                      modifier = Modifier.size(17.dp)
                    )
                  }
                }
              }
            }
          }

          // "Yeni Profil Ekle" Butonu
          Button(
            onClick = onAddNewProfile,
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("settings_add_profile_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFF97316),
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PersonAdd,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Yeni Profil Ekle",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      val backupSummary = if (lastBackupDateText == "Henüz yedek alınmadı" || lastBackupDateText.isBlank()) {
        "Henüz yedek yok"
      } else {
        "Son yedek: $lastBackupDateText"
      }

      // 6. YEDEKLEME BÖLÜMÜ (Açılır Kart)
      AcilirKart(
        title = "Yedekleme",
        icon = Icons.Default.CloudSync,
        summary = backupSummary,
        iconTint = AppTheme.colors.accent,
        testTag = "settings_backup_card"
      ) {

          // Son Yedek Tarihi
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = ItemBg,
            border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = null,
                  tint = SubLabelColor,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Son Yedek",
                  fontSize = 12.sp,
                  color = SubLabelColor
                )
              }
              Text(
                text = lastBackupDateText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (lastBackupDateText == "Henüz yedek alınmadı") BentoAmber else BentoCyan
              )
            }
          }

          // Bilgi Metni (Samsung Smart Switch & Drive)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = BentoCyan.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, BentoCyan.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.CloudUpload,
                  contentDescription = null,
                  tint = BentoCyan,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Bulut ve Depolama Yedeklemesi",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = BentoCyan
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tüm profiller, öğünler, su, kilo, adım ve favori kayıtların tek bir JSON dosyasında toplanır. Yedek dosyasını Google Drive, OneDrive, Dropbox gibi bir buluta ya da telefon belleğine kaydedebilirsin.\n\nSamsung telefonlarda Smart Switch ile de taşıyabilirsin.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 15.sp
              )
            }
          }

          // Butonlar: Yedeği Dışa Aktar ve Yedekten Geri Yükle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Yedeği Dışa Aktar Butonu
            Button(
              onClick = { showExportPreInfoDialog = true },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = BentoCyan,
                contentColor = Color.Black
              ),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("export_backup_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.FileDownload,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Yedeği Dışa Aktar",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Yedekten Geri Yükle Butonu
            OutlinedButton(
              onClick = {
                openDocumentLauncher.launch(
                  arrayOf(
                    "application/json",
                    "application/octet-stream",
                    "text/*",
                    "*/*"
                  )
                )
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BentoCyan
              ),
              border = BorderStroke(1.dp, BentoCyan.copy(alpha = 0.6f)),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("restore_backup_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.FileUpload,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Yedekten Geri Yükle",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

      // 5. DESTEK BÖLÜMÜ
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
              color = BentoGreen.copy(alpha = 0.16f),
              modifier = Modifier.size(32.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Email,
                  contentDescription = null,
                  tint = BentoGreen,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Destek",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          // "Öneride bulun" satırı (zarf ikonlu, dokunulabilir)
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ItemBg,
            border = CardDefaults.outlinedCardBorder().copy(
              brush = Brush.horizontalGradient(listOf(CardBorderColor, CardBorderColor.copy(alpha = 0.4f)))
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable {
                sendFeedbackEmail(context, appVersion)
              }
              .testTag("support_feedback_row")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Surface(
                  shape = CircleShape,
                  color = BentoGreen.copy(alpha = 0.16f),
                  modifier = Modifier.size(36.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Email,
                      contentDescription = null,
                      tint = BentoGreen,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Öneride bulun",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Görüş, öneri ve hata bildirimlerinizi iletin",
                    fontSize = 11.sp,
                    color = SubLabelColor
                  )
                }
              }

              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = SubLabelColor,
                modifier = Modifier.size(13.dp)
              )
            }
          }
        }
      }

      // 6. HAKKINDA BÖLÜMÜ
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
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = BentoBlue.copy(alpha = 0.16f),
              modifier = Modifier.size(32.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = BentoBlue,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Hakkında",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          // Sürüm Numarası Satırı
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Uygulama Sürümü",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = CardBorderColor.copy(alpha = 0.6f)
              ) {
                Text(
                  text = "v$appVersion",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = BentoGreen,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }

          // Veri Gizliliği & Yerel Saklama Bilgisi Satırı
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
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = CircleShape,
                color = BentoGreen.copy(alpha = 0.16f),
                modifier = Modifier.size(34.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = BentoGreen,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Verilerin yalnızca bu cihazda tutulur",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Öğün, su ve kilo kayıtlarınız harici sunuculara iletilmez, tamamen cihazınızda güvendedir.",
                  fontSize = 11.sp,
                  color = SubLabelColor
                )
              }
            }
          }
        }
      }
    }
  }

  // Profil Adı Düzenleme Dialogu
  editingProfileForName?.let { profile ->
    AlertDialog(
      onDismissRequest = { editingProfileForName = null },
      title = {
        Text("Profil Adını Düzenle", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
      },
      text = {
        OutlinedTextField(
          value = newProfileNameInput,
          onValueChange = { newProfileNameInput = it },
          label = { Text("Profil Adı") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_name_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BentoBlue,
            focusedLabelColor = BentoBlue,
            cursorColor = BentoBlue
          )
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newProfileNameInput.isNotBlank()) {
              onUpdateProfileName(profile.id, newProfileNameInput.trim())
              Toast.makeText(context, "Profil adı güncellendi", Toast.LENGTH_SHORT).show()
              editingProfileForName = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = BentoBlue),
          modifier = Modifier.testTag("save_profile_name_button")
        ) {
          Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { editingProfileForName = null }) {
          Text("İptal", color = SubLabelColor)
        }
      },
      containerColor = CardBg
    )
  }

  // Profil Silme Onay Dialogu (Kırmızı Onay)
  deletingProfile?.let { profile ->
    AlertDialog(
      onDismissRequest = { deletingProfile = null },
      icon = {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = Color(0xFFEF4444),
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(
          text = "Profili Sil",
          fontWeight = FontWeight.Bold,
          color = Color(0xFFEF4444),
          fontSize = 17.sp
        )
      },
      text = {
        Text(
          text = "\"${profile.name}\" profili ve bu profile ait tüm veriler (öğün, su ve kilo kayıtları) kalıcı olarak silinecektir. Emin misin?",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteProfile(profile.id) { success, msg ->
              Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            deletingProfile = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEF4444),
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("confirm_delete_profile_button")
        ) {
          Text("Sil", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingProfile = null }) {
          Text("Vazgeç", color = SubLabelColor)
        }
      },
      containerColor = CardBg
    )
  }

  if (showWeightDayDialog) {
    AlertDialog(
      onDismissRequest = { showWeightDayDialog = false },
      shape = RoundedCornerShape(24.dp),
      containerColor = CardBg,
      title = {
        Text(text = "Tartılma Günü Seçin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) BentoPurple.copy(alpha = 0.2f) else ItemBg,
              border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BentoPurple, BentoPurple.copy(alpha = 0.5f)))) else null,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = day,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) BentoPurple else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                  Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = BentoPurple, modifier = Modifier.size(16.dp))
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

  // Yedek Dışa Aktarma Öncesi Bilgi Dialogu
  if (showExportPreInfoDialog) {
    AlertDialog(
      onDismissRequest = { showExportPreInfoDialog = false },
      shape = RoundedCornerShape(24.dp),
      containerColor = CardBg,
      icon = {
        Icon(
          imageVector = Icons.Default.CloudUpload,
          contentDescription = null,
          tint = BentoCyan,
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(
          text = "Yedeği Dışa Aktar",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Yedek dosyasını Drive gibi bir buluta kaydetmek için açılan listeden seç.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Samsung telefonlarda Smart Switch ile de taşıyabilirsin.",
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showExportPreInfoDialog = false
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            createDocumentLauncher.launch("kilocal-yedek-$todayStr.json")
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = BentoCyan,
            contentColor = Color.Black
          )
        ) {
          Text("Devam Et", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showExportPreInfoDialog = false }) {
          Text("İptal", color = SubLabelColor)
        }
      }
    )
  }

  // Yedek Geri Yükleme Onay Dialogu
  if (pendingRestoreJson != null) {
    AlertDialog(
      onDismissRequest = {
        pendingRestoreJson = null
        pendingRestoreSummary = null
      },
      shape = RoundedCornerShape(24.dp),
      containerColor = CardBg,
      icon = {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = BentoAmber,
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(
          text = "Mevcut veriler değiştirilecek, emin misin?",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Geri yükleme işlemi mevcut verilerinizin üzerine yazacaktır. Bu işlem geri alınamaz.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (!pendingRestoreSummary.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = ItemBg,
              border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = pendingRestoreSummary!!,
                fontSize = 11.sp,
                color = SubLabelColor,
                lineHeight = 16.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val toRestore = pendingRestoreJson
            pendingRestoreJson = null
            pendingRestoreSummary = null
            if (toRestore != null) {
              coroutineScope.launch {
                isRestoring = true
                val result = onRestoreBackup(toRestore)
                isRestoring = false
                if (result.isSuccess) {
                  successMessageDialog = "Yedek başarıyla geri yüklendi!"
                } else {
                  errorMessageDialog = "Geri yükleme başarısız oldu: ${result.exceptionOrNull()?.localizedMessage ?: "Bilinmeyen hata"}"
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = BentoAmber,
            contentColor = Color.Black
          )
        ) {
          Text("Geri Yükle", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = {
          pendingRestoreJson = null
          pendingRestoreSummary = null
        }) {
          Text("Vazgeç", color = SubLabelColor)
        }
      }
    )
  }

  // Hata Dialogu
  errorMessageDialog?.let { msg ->
    AlertDialog(
      onDismissRequest = { errorMessageDialog = null },
      shape = RoundedCornerShape(24.dp),
      containerColor = CardBg,
      icon = {
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(
          text = "Yedekleme Hatası",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      text = {
        Text(text = msg, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
      },
      confirmButton = {
        Button(
          onClick = { errorMessageDialog = null },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Text("Tamam")
        }
      }
    )
  }

  // Başarı Dialogu
  successMessageDialog?.let { msg ->
    AlertDialog(
      onDismissRequest = { successMessageDialog = null },
      shape = RoundedCornerShape(24.dp),
      containerColor = CardBg,
      icon = {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = BentoGreen,
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(
          text = "İşlem Başarılı",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      text = {
        Text(text = msg, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
      },
      confirmButton = {
        Button(
          onClick = { successMessageDialog = null },
          colors = ButtonDefaults.buttonColors(
            containerColor = BentoGreen,
            contentColor = Color.Black
          )
        ) {
          Text("Tamam")
        }
      }
    )
  }
}

/**
 * Reusable "Yakında" Setting Card for Language, Theme & Profiles
 */
@Composable
private fun ComingSoonSettingCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color
) {
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
              modifier = Modifier.size(18.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(end = 8.dp)) {
          Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            fontSize = 11.sp,
            color = SubLabelColor,
            maxLines = 1
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBorderColor.copy(alpha = 0.6f)
      ) {
        Text(
          text = "Yakında",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = SubLabelColor,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
    }
  }
}

/**
 * Interactive Theme Setting Card supporting 6 vibrant dark accents, OLED black, and live preview.
 */
@Composable
private fun ThemeSettingCard(
  accentIndex: Int,
  isOled: Boolean,
  onSelectAccent: (Int) -> Unit,
  onToggleOled: (Boolean) -> Unit
) {
  val activeAccent = ACCENT_PRESETS.getOrNull(accentIndex)?.color ?: ACCENT_PRESETS[0].color
  val themeAccentName = ACCENT_PRESETS.getOrNull(accentIndex)?.name ?: "Yeşil"
  val themeSummary = if (isOled) "OLED" else "Koyu Tema · $themeAccentName"

  AcilirKart(
    title = "Tema",
    icon = Icons.Default.Palette,
    summary = themeSummary,
    iconTint = AppTheme.colors.accent,
    testTag = "settings_theme_card"
  ) {

      // 7 Adet Dolu Daire (En az 48dp dokunma alanı, isimsiz, seçili olanda beyaz halka + onay işareti)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        ACCENT_PRESETS.forEachIndexed { idx, preset ->
          val isSelected = (idx == accentIndex)
          Box(
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .semantics {
                contentDescription = "Tema ${idx + 1}"
              }
              .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
              ) {
                onSelectAccent(idx)
              }
              .testTag("theme_circle_${idx + 1}"),
            contentAlignment = Alignment.Center
          ) {
            // Dairenin kendisi (32dp)
            val circleBgModifier = if (preset.gradientColors != null) {
              Modifier.background(Brush.linearGradient(preset.gradientColors))
            } else {
              Modifier.background(preset.color)
            }
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(circleBgModifier)
                .then(
                  if (isSelected) {
                    Modifier.border(2.dp, Color.White, CircleShape)
                  } else {
                    Modifier
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
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

      HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f))

      // OLED Siyah Aç/Kapa Anahtarı
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = "OLED siyah",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "OLED ekranlarda pil tasarrufu sağlar.",
            fontSize = 12.sp,
            color = SubLabelColor
          )
        }

        Switch(
          checked = isOled,
          onCheckedChange = { onToggleOled(it) },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = activeAccent,
            uncheckedThumbColor = SubLabelColor,
            uncheckedTrackColor = CardBorderColor
          ),
          modifier = Modifier.testTag("oled_switch")
        )
      }

      // Canlı Küçük Önizleme Kartı (Halka, buton ve çip örneği)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(ItemBg)
          .border(
            1.dp,
            if (AppTheme.colors.isGradient) AppTheme.colors.cardBorderBrush else SolidColor(CardBorderColor.copy(alpha = 0.6f)),
            RoundedCornerShape(16.dp)
          )
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // 1. Örnek Mini Kalori Halkası (34dp)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier.size(34.dp),
              contentAlignment = Alignment.Center
            ) {
              val isThemeGradient = AppTheme.colors.isGradient
              val sweepColors = AppTheme.colors.sweepGradientColors
              if (isThemeGradient) {
                Canvas(modifier = Modifier.size(32.dp)) {
                  val stroke = 3.5.dp.toPx()
                  val diameter = size.minDimension - stroke
                  val topLeft = Offset(stroke / 2f, stroke / 2f)
                  val arcSize = Size(diameter, diameter)
                  drawArc(
                    color = activeAccent.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                  )
                  drawArc(
                    brush = Brush.sweepGradient(sweepColors),
                    startAngle = -90f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                  )
                }
              } else {
                CircularProgressIndicator(
                  progress = { 0.75f },
                  color = activeAccent,
                  trackColor = activeAccent.copy(alpha = 0.22f),
                  strokeWidth = 3.5.dp,
                  modifier = Modifier.size(32.dp)
                )
              }
              Text(
                text = "75%",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
            Text(
              text = "Halka",
              fontSize = 11.sp,
              color = SubLabelColor
            )
          }

          // 2. Örnek Buton
          Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(
              containerColor = if (AppTheme.colors.isGradient) Color.Transparent else activeAccent
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier
              .height(30.dp)
              .then(
                if (AppTheme.colors.isGradient) {
                  Modifier.background(AppTheme.colors.accentBrush, RoundedCornerShape(10.dp))
                } else {
                  Modifier
                }
              )
          ) {
            Text(
              text = "Kaydet",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          }

          // 3. Örnek Çip
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = activeAccent.copy(alpha = 0.18f),
            border = androidx.compose.foundation.BorderStroke(1.dp, activeAccent.copy(alpha = 0.5f)),
            modifier = Modifier.height(28.dp)
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Aktif Çip",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = activeAccent
              )
            }
          }
        }
      }
    }
}


/**
 * Checks whether notification permission is granted on the device.
 */
private fun checkNotificationPermission(context: Context): Boolean {
  val areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
  if (!areEnabled) return false
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    return ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
  }
  return true
}

/**
 * Opens system notification settings for this application.
 */
private fun openSystemNotificationSettings(context: Context) {
  try {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    } else {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    }
    context.startActivity(intent)
  } catch (_: Exception) {
    try {
      val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(fallback)
    } catch (_: Exception) {
      Toast.makeText(context, "Sistem bildirim ayarları açılamadı", Toast.LENGTH_SHORT).show()
    }
  }
}

/**
 * Handles sending feedback email with intent ACTION_SENDTO.
 * Falls back to copying email address to clipboard if no email client is available.
 */
private fun sendFeedbackEmail(context: Context, appVersion: String) {
  val targetEmail = "ilekeine@yandex.com"
  val subject = "KiloCAL Önerisi"
  val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
  val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
  val bodyText = "\n\n\n\n---\nUygulama sürümü: $appVersion, Android: $androidVersion, Cihaz: $deviceModel"

  try {
    val mailtoUri = Uri.parse(
      "mailto:$targetEmail?subject=${Uri.encode(subject)}&body=${Uri.encode(bodyText)}"
    )
    val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri)
    context.startActivity(emailIntent)
  } catch (_: Exception) {
    // Fallback: Copy address to clipboard and show toast
    try {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      val clip = ClipData.newPlainText("E-posta Adresi", targetEmail)
      clipboard?.setPrimaryClip(clip)
      Toast.makeText(
        context,
        "E-posta uygulaması bulunamadı, adres panoya kopyalandı",
        Toast.LENGTH_LONG
      ).show()
    } catch (_: Exception) {
      Toast.makeText(
        context,
        "E-posta adresi: $targetEmail",
        Toast.LENGTH_LONG
      ).show()
    }
  }
}
