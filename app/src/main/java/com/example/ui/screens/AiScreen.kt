package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.CalculatedMetrics
import com.example.ui.DailyNutritionSummary
import com.example.ui.components.ParsedMealEditorCard
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

import com.example.ui.theme.AppTheme

// Modern Bento Color Palette
private val BentoGreen: Color
  @Composable get() = AppTheme.colors.accent
private val BentoBlue = Color(0xFF38BDF8)
private val BentoAmber = Color(0xFFFBBF24)
private val BentoPink = Color(0xFFFB7185)
private val BentoTeal = Color(0xFF2DD4BF)
private val BentoPurple = Color(0xFFA855F7)
private val BentoPurpleLight = Color(0xFFC084FC)

private val BentoCardBg: Color
  @Composable get() = AppTheme.colors.surface
private val BentoCardBorder: Color
  @Composable get() = AppTheme.colors.border
private val BentoSubBg: Color
  @Composable get() = AppTheme.colors.itemBg

enum class AiModule(val id: Int, val title: String, val shortTitle: String, val subtitle: String, val icon: ImageVector, val color: Color) {
  OVERVIEW(0, "Yapay Zeka Merkezi", "Genel Bakış", "Tüm AI beslenme araçları", Icons.Default.AutoAwesome, BentoPurple),
  VISION(1, "Fotoğraftan Yemek Tanıma", "Fotoğraf Tanıma", "Tabaktaki yemekleri & kalorileri tespit et", Icons.Default.CameraAlt, BentoTeal),
  NATURAL_TEXT(2, "Doğal Dille Öğün Ekle", "Metinle Ekle", "Ne yediğini yaz, kalorileri otomatik ayrıştır", Icons.Default.EditNote, BentoBlue),
  COACH(3, "Kişisel AI Beslenme Koçu", "AI Koç", "Hedeflerine ve kalan kalorine göre tavsiyeler", Icons.Default.Psychology, BentoPurple),
  PACKAGE_SCAN(4, "Paket & İçerik Analizi", "Paket Analizi", "Katkı maddeleri ve Temiz İçerik Puanı", Icons.Default.QrCodeScanner, BentoAmber),
  INSIGHTS(5, "Akıllı Tahmin & İçgörüler", "Tahmin & İçgörü", "Kilo hedefi süresi ve hafta analizi", Icons.Default.Insights, Color(0xFF22C55E)),
  RECIPES(6, "Tarif & Menü Planlayıcı", "Tarif & Menü", "Kalan kalorine uygun tarif & alışveriş listesi", Icons.Default.RestaurantMenu, BentoPink)
}

@Composable
fun AiScreen(
  viewModel: MainViewModel,
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  todayNutrition: DailyNutritionSummary,
  todayWaterMl: Int,
  streakCount: Int,
  modifier: Modifier = Modifier
) {
  var selectedModule by remember { mutableStateOf(AiModule.OVERVIEW) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header Bar
    AiTopBar(
      selectedModule = selectedModule,
      onBackToOverview = { selectedModule = AiModule.OVERVIEW }
    )

    // Module Switcher Chips (Horizontal Row)
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(AiModule.values()) { module ->
        val isSelected = selectedModule == module
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = if (isSelected) module.color.copy(alpha = 0.2f) else BentoCardBg,
          border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, module.color) else androidx.compose.foundation.BorderStroke(1.dp, BentoCardBorder),
          modifier = Modifier.clickable { selectedModule = module }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = module.icon,
              contentDescription = module.shortTitle,
              tint = if (isSelected) module.color else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = module.shortTitle,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) module.color else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    // Main Content
    Box(
      modifier = Modifier
        .fillMaxSize()
        .weight(1f)
    ) {
      when (selectedModule) {
        AiModule.OVERVIEW -> {
          AiOverviewContent(
            userProfile = userProfile,
            metrics = metrics,
            todayNutrition = todayNutrition,
            todayWaterMl = todayWaterMl,
            streakCount = streakCount,
            onSelectModule = { selectedModule = it }
          )
        }
        AiModule.VISION -> {
          AiVisionSection(
            viewModel = viewModel,
            onSavedToJournal = { selectedModule = AiModule.OVERVIEW }
          )
        }
        AiModule.NATURAL_TEXT -> {
          AiNaturalLanguageSection(
            viewModel = viewModel,
            onSavedToJournal = { selectedModule = AiModule.OVERVIEW }
          )
        }
        AiModule.COACH -> {
          AiCoachSection(
            service = viewModel.aiAssistantService,
            userProfile = userProfile,
            metrics = metrics,
            todayNutrition = todayNutrition,
            todayWaterMl = todayWaterMl
          )
        }
        AiModule.PACKAGE_SCAN -> {
          AiPackageScanSection(service = viewModel.aiAssistantService)
        }
        AiModule.INSIGHTS -> {
          AiInsightsSection(
            service = viewModel.aiAssistantService,
            userProfile = userProfile,
            metrics = metrics,
            todayNutrition = todayNutrition,
            todayWaterMl = todayWaterMl,
            streakCount = streakCount
          )
        }
        AiModule.RECIPES -> {
          AiRecipesSection(
            service = viewModel.aiAssistantService,
            metrics = metrics,
            todayNutrition = todayNutrition
          )
        }
      }
    }
  }
}

@Composable
private fun AiTopBar(
  selectedModule: AiModule,
  onBackToOverview: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (selectedModule != AiModule.OVERVIEW) {
        IconButton(
          onClick = onBackToOverview,
          modifier = Modifier
            .size(38.dp)
            .background(BentoCardBg, CircleShape)
            .border(1.dp, BentoCardBorder, CircleShape)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Geri",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
          )
        }
      } else {
        Box(
          modifier = Modifier
            .size(38.dp)
            .background(
              Brush.radialGradient(listOf(BentoPurple, BentoPurpleLight.copy(alpha = 0.5f))),
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Yapay Zeka",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (selectedModule == AiModule.OVERVIEW) "Asistan" else selectedModule.title,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = if (selectedModule == AiModule.OVERVIEW) "Gemini Destekli Beslenme Asistanı" else selectedModule.subtitle,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Gemini Badge (Tek satır, asla taşmaz veya alt başlıkla çakışmaz)
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = BentoPurple.copy(alpha = 0.15f),
      border = androidx.compose.foundation.BorderStroke(1.dp, BentoPurple.copy(alpha = 0.35f))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(BentoGreen, CircleShape)
        )
        Text(
          text = "Gemini AI",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = BentoPurpleLight,
          maxLines = 1,
          softWrap = false
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 0. OVERVIEW: BENTO GRID OF ALL 6 MODULES
// -------------------------------------------------------------
@Composable
private fun AiOverviewContent(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  todayNutrition: DailyNutritionSummary,
  todayWaterMl: Int,
  streakCount: Int,
  onSelectModule: (AiModule) -> Unit
) {
  val remainingCals = metrics.targetCalories - todayNutrition.totalCalories

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    // Top Hero Bento Card: AI Live Summary
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = BentoCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoPurple.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .background(BentoPurple.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.SmartToy,
                  contentDescription = null,
                  tint = BentoPurpleLight,
                  modifier = Modifier.size(18.dp)
                )
              }
              Column {
                Text(
                  text = "Akıllı Asistan Özeti",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Bugünkü durumuna göre anlık öneri",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = (if (remainingCals >= 0) BentoGreen else BentoAmber).copy(alpha = 0.15f)
            ) {
              Text(
                text = "${remainingCals} kcal kaldı",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (remainingCals >= 0) BentoGreen else BentoAmber,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Text(
            text = if (remainingCals > 400) {
              "Hedefine ulaşmak için akşam öğününde yüksek proteinli ve lifli bir tabak tercih edebilirsin. Fotoğrafını çek veya yaz, kalorisini anında hesaplayalım!"
            } else if (remainingCals in 0..400) {
              "Bugünkü kalori hedefine çok yakınsın! Hafif bir çorba veya salata ile günü mükemmel kapatabilirsin."
            } else {
              "Günlük hedefini biraz aştın, sorun değil! AI Koçuna danışarak yarın için pratik dengeleme tüyoları alabilirsin."
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            lineHeight = 17.sp
          )

          // Quick Action Button
          Button(
            onClick = { onSelectModule(AiModule.COACH) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoPurple),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.align(Alignment.End)
          ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("AI Koça Sor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Grid of the 6 Features
    val modules = listOf(
      AiModule.VISION,
      AiModule.NATURAL_TEXT,
      AiModule.COACH,
      AiModule.PACKAGE_SCAN,
      AiModule.INSIGHTS,
      AiModule.RECIPES
    )

    items(modules.chunked(2)) { pair ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        pair.forEach { module ->
          Box(modifier = Modifier.weight(1f)) {
            AiFeatureBentoCard(
              module = module,
              onClick = { onSelectModule(module) }
            )
          }
        }
        if (pair.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun AiFeatureBentoCard(
  module: AiModule,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = BentoCardBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, BentoCardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .height(160.dp)
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(module.color.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .border(1.dp, module.color.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = module.icon,
            contentDescription = module.title,
            tint = module.color,
            modifier = Modifier.size(22.dp)
          )
        }

        Icon(
          imageVector = Icons.Default.ArrowOutward,
          contentDescription = "Aç",
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.size(16.dp)
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = module.title,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = module.subtitle,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 13.sp
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 1. FOTOĞRAFTAN YEMEK TANIMA (VISION)
// -------------------------------------------------------------
@Composable
private fun AiVisionSection(
  viewModel: MainViewModel,
  onSavedToJournal: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var samplePlateName by remember { mutableStateOf<String?>(null) }
  var isScanning by remember { mutableStateOf(false) }
  var scanResult by remember { mutableStateOf<FoodScanResult?>(null) }
  var selectedMealCategory by remember { mutableStateOf("Öğle") }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      samplePlateName = null
      try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val source = ImageDecoder.createSource(context.contentResolver, uri)
          ImageDecoder.decodeBitmap(source)
        } else {
          @Suppress("DEPRECATION")
          MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        selectedBitmap = bitmap
      } catch (e: Exception) {
        Toast.makeText(context, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show()
      }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    // Instructions Card
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = BentoCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoTeal.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = BentoTeal)
            Text(
              text = "Yemek Tabağını Tara",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Text(
            text = "Tabağın fotoğrafını seçin veya aşağıdaki hazır örnek tabaklardan birini deneyin. Gemini AI tabaktaki yiyecekleri, porsiyonları ve besin değerlerini anında ayrıştırır.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )

          // Gallery Pick Button
          Button(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoTeal),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Galeriden Fotoğraf Seç", color = Color.Black, fontWeight = FontWeight.Bold)
          }

          // Sample Plates Row
          Text(
            text = "Hızlı Deneme Tabakları:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("🍗 Izgara Tavuk", "🍳 Türk Kahvaltısı", "🧆 Köfte & Bulgur").forEach { sample ->
              val isChosen = samplePlateName == sample
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isChosen) BentoTeal.copy(alpha = 0.2f) else BentoSubBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isChosen) BentoTeal else BentoCardBorder),
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    samplePlateName = sample
                    selectedBitmap = null
                  }
              ) {
                Text(
                  text = sample,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = if (isChosen) BentoTeal else MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Scan Trigger Button
          if (selectedBitmap != null || samplePlateName != null) {
            Button(
              onClick = {
                isScanning = true
                coroutineScope.launch {
                  val result = viewModel.aiAssistantService.analyzeFoodPhoto(
                    bitmap = selectedBitmap,
                    sampleType = samplePlateName
                  )
                  scanResult = result
                  selectedMealCategory = result.suggestedMealType
                  isScanning = false
                }
              },
              enabled = !isScanning,
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BentoGreen),
              modifier = Modifier.fillMaxWidth()
            ) {
              if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini Analiz Ediyor...", color = Color.Black, fontWeight = FontWeight.Bold)
              } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tabaktaki Besinleri Çözümle", color = Color.Black, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // Results Card (Ortak Düzenlenebilir Liste)
    if (scanResult != null) {
      val res = scanResult!!
      item {
        ParsedMealEditorCard(
          initialItems = res.foodItems,
          initialMealType = res.suggestedMealType,
          generalWarnings = res.warnings,
          initialUnmatched = res.unmatchedExpressions,
          aiAssistantService = viewModel.aiAssistantService,
          onSaveToJournal = { selectedItems, mealType ->
            viewModel.saveScannedFoodItems(
              mealType = mealType,
              date = viewModel.getTodayDateString(),
              items = selectedItems
            )
            Toast.makeText(context, "$mealType öğününe ${selectedItems.size} besin eklendi! ✨", Toast.LENGTH_LONG).show()
            onSavedToJournal()
          }
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 2. DOĞAL DİLLE ÖĞÜN EKLE
// -------------------------------------------------------------
@Composable
private fun AiNaturalLanguageSection(
  viewModel: MainViewModel,
  onSavedToJournal: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var inputText by remember { mutableStateOf("") }
  var isParsing by remember { mutableStateOf(false) }
  var parseResult by remember { mutableStateOf<NaturalLanguageMealResult?>(null) }
  var targetMealCategory by remember { mutableStateOf("Öğle") }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = BentoCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBlue.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.EditNote, contentDescription = null, tint = BentoBlue)
            Text(
              text = "Ne Yediğinizi Yazın",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Text(
            text = "Günlük konuşma diliyle yazın. Gemini öğün türünü, yiyecekleri, porsiyonları ve makro değerleri otomatik ayrıştırır.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text("Örn: 1 kase mercimek çorbası, 1 dilim kepek ekmeği ve salata yedim...", fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BentoBlue,
              unfocusedBorderColor = BentoCardBorder,
              focusedContainerColor = BentoSubBg,
              unfocusedContainerColor = BentoSubBg
            ),
            minLines = 3,
            maxLines = 5
          )

          // Quick Preset Prompts
          Text(text = "Hızlı Örnekler:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          val presets = listOf(
            "Sabah 2 yumurta, 1 dilim peynir ve domates",
            "Öğle 1 kase mercimek çorbası ve mevsim salata",
            "Akşam 150g ızgara tavuk ve 4 kaşık bulgur pilavı"
          )
          presets.forEach { preset ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = BentoSubBg,
              border = androidx.compose.foundation.BorderStroke(1.dp, BentoCardBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { inputText = preset }
            ) {
              Text(
                text = "💬 \"$preset\"",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
              )
            }
          }

          // Parse Button
          Button(
            onClick = {
              if (inputText.isBlank()) return@Button
              isParsing = true
              coroutineScope.launch {
                val res = viewModel.aiAssistantService.parseNaturalLanguageMeal(inputText)
                parseResult = res
                targetMealCategory = res.mealType
                isParsing = false
              }
            },
            enabled = !isParsing && inputText.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoBlue),
            modifier = Modifier.fillMaxWidth()
          ) {
            if (isParsing) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Gemini Ayrıştırıyor...", color = Color.Black, fontWeight = FontWeight.Bold)
            } else {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Yapay Zeka ile Ayrıştır", color = Color.Black, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Parsed Result Card (Ortak Düzenlenebilir Liste)
    if (parseResult != null) {
      val res = parseResult!!
      item {
        ParsedMealEditorCard(
          initialItems = res.foodItems,
          initialMealType = res.mealType,
          generalWarnings = res.warnings,
          initialUnmatched = res.unmatchedExpressions,
          aiAssistantService = viewModel.aiAssistantService,
          onSaveToJournal = { selectedItems, mealType ->
            viewModel.saveScannedFoodItems(
              mealType = mealType,
              date = viewModel.getTodayDateString(),
              items = selectedItems
            )
            Toast.makeText(context, "$mealType öğününe ${selectedItems.size} besin eklendi! ✨", Toast.LENGTH_LONG).show()
            onSavedToJournal()
          }
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 3. KİŞİSEL AI BESLENME KOÇU (CANLI SOHBET)
// -------------------------------------------------------------
@Composable
private fun AiCoachSection(
  service: AiAssistantService,
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  todayNutrition: DailyNutritionSummary,
  todayWaterMl: Int
) {
  val coroutineScope = rememberCoroutineScope()
  var inputMessage by remember { mutableStateOf("") }
  var isTyping by remember { mutableStateOf(false) }

  val chatMessages = remember {
    mutableStateListOf(
      CoachChatMessage(
        isFromUser = false,
        text = "Merhaba ${if (userProfile.name != "Kullanıcı") userProfile.name else ""}! 👋 Ben senin kişisel beslenme koçunum. Bugünkü hedeflerine ve kalan kalorine göre sana özel tavsiyeler verebilirim. Nasıl yardımcı olabilirim?"
      )
    )
  }

  val listState = rememberLazyListState()

  val userContext = remember(userProfile, metrics, todayNutrition, todayWaterMl) {
    """
      Kullanıcı: ${userProfile.name}, Yaş: ${userProfile.age}, Cinsiyet: ${userProfile.gender}
      Kilo: ${userProfile.currentWeight} kg, Hedef Kilo: ${userProfile.targetWeight} kg
      Günlük Kalori Hedefi: ${metrics.targetCalories} kcal
      Bugün Alınan Kalori: ${todayNutrition.totalCalories} kcal
      Kalan Kalori: ${metrics.targetCalories - todayNutrition.totalCalories} kcal
      Bugün Alınan Protein: ${todayNutrition.totalProtein}g (Hedef: ${metrics.proteinGrams}g)
      İçilen Su: $todayWaterMl ml (Hedef: ${userProfile.dailyWaterTarget} ml)
    """.trimIndent()
  }

  fun sendMessage(text: String) {
    if (text.isBlank()) return
    val userMsg = CoachChatMessage(isFromUser = true, text = text)
    chatMessages.add(userMsg)
    inputMessage = ""
    isTyping = true

    coroutineScope.launch {
      listState.animateScrollToItem(chatMessages.size - 1)
      val reply = service.askNutritionCoach(text, chatMessages.toList(), userContext)
      chatMessages.add(CoachChatMessage(isFromUser = false, text = reply))
      isTyping = false
      listState.animateScrollToItem(chatMessages.size - 1)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Quick Info Card
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = BentoCardBg,
      border = androidx.compose.foundation.BorderStroke(1.dp, BentoPurple.copy(alpha = 0.3f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val rem = metrics.targetCalories - todayNutrition.totalCalories
        Text(
          text = "Kalan: ${rem} kcal",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = if (rem >= 0) BentoGreen else BentoAmber
        )
        Text(
          text = "Protein: ${todayNutrition.totalProtein.toInt()}/${metrics.proteinGrams}g",
          fontSize = 11.sp,
          color = BentoBlue
        )
        Text(
          text = "Su: ${todayWaterMl}/${userProfile.dailyWaterTarget}ml",
          fontSize = 11.sp,
          color = BentoTeal
        )
      }
    }

    // Chat History
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(vertical = 8.dp)
    ) {
      items(chatMessages) { msg ->
        CoachChatBubble(message = msg)
      }

      if (isTyping) {
        item {
          Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = BentoPurple, strokeWidth = 2.dp)
            Text(text = "AI Koç düşünüyor...", fontSize = 11.sp, color = BentoPurpleLight)
          }
        }
      }
    }

    // Suggested Fast Questions
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val suggestions = listOf(
        "Akşam ne yemeliyim?",
        "Protein açığımı nasıl kapatırım?",
        "Tatlı krizini nasıl bastırırım?",
        "Su içmeyi unutuyorum, ne yapayım?"
      )
      items(suggestions) { q ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = BentoSubBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, BentoCardBorder),
          modifier = Modifier.clickable { sendMessage(q) }
        ) {
          Text(
            text = q,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }

    // Input Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = inputMessage,
        onValueChange = { inputMessage = it },
        placeholder = { Text("AI Koça soru sor...", fontSize = 13.sp) },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = BentoPurple,
          unfocusedBorderColor = BentoCardBorder,
          focusedContainerColor = BentoCardBg,
          unfocusedContainerColor = BentoCardBg
        ),
        modifier = Modifier.weight(1f),
        maxLines = 3
      )

      IconButton(
        onClick = { sendMessage(inputMessage) },
        enabled = inputMessage.isNotBlank() && !isTyping,
        modifier = Modifier
          .size(48.dp)
          .background(if (inputMessage.isNotBlank()) BentoPurple else BentoCardBg, CircleShape)
          .border(1.dp, BentoCardBorder, CircleShape)
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Gönder",
          tint = if (inputMessage.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun CoachChatBubble(message: CoachChatMessage) {
  val isUser = message.isFromUser
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    if (!isUser) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .background(BentoPurple.copy(alpha = 0.2f), CircleShape)
          .border(1.dp, BentoPurple.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BentoPurpleLight, modifier = Modifier.size(14.dp))
      }
      Spacer(modifier = Modifier.width(6.dp))
    }

    Surface(
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
      ),
      color = if (isUser) BentoPurple else BentoCardBg,
      border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, BentoCardBorder),
      modifier = Modifier.widthIn(max = 280.dp)
    ) {
      Text(
        text = message.text,
        fontSize = 13.sp,
        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
        lineHeight = 18.sp,
        modifier = Modifier.padding(12.dp)
      )
    }
  }
}

// -------------------------------------------------------------
// 4. PAKET & İÇERİK ANALİZİ (CLEAN SCORE)
// -------------------------------------------------------------
@Composable
private fun AiPackageScanSection(service: AiAssistantService) {
  val coroutineScope = rememberCoroutineScope()
  var productQuery by remember { mutableStateOf("") }
  var isAnalyzing by remember { mutableStateOf(false) }
  var analysisResult by remember { mutableStateOf<PackageAnalysisResult?>(null) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = BentoCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoAmber.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = BentoAmber)
            Text(
              text = "Paketli Ürün İçerik Analizi",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Text(
            text = "Ambalajlı ürünün adını veya içindekiler etiketini girin. Gemini zararlı katkı maddelerini (E kodları), ilave şeker ve palm yağını tarayarak 100 üzerinden Temiz İçerik Puanı çıkarır.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = productQuery,
            onValueChange = { productQuery = it },
            placeholder = { Text("Örn: Fıstık Ezmeli Protein Bar, Çikolatalı Gofret...", fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BentoAmber,
              unfocusedBorderColor = BentoCardBorder,
              focusedContainerColor = BentoSubBg,
              unfocusedContainerColor = BentoSubBg
            ),
            minLines = 2
          )

          // Quick Preset Chips
          Text(text = "Hızlı İnceleme Örnekleri:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("Protein Bar", "Doğal Granola", "Kakaolu Bisküvi").forEach { p ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = BentoSubBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, BentoCardBorder),
                modifier = Modifier
                  .weight(1f)
                  .clickable { productQuery = p }
              ) {
                Text(
                  text = p,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(vertical = 8.dp)
                )
              }
            }
          }

          Button(
            onClick = {
              if (productQuery.isBlank()) return@Button
              isAnalyzing = true
              coroutineScope.launch {
                val res = service.analyzeProductPackage(productQuery)
                analysisResult = res
                isAnalyzing = false
              }
            },
            enabled = !isAnalyzing && productQuery.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoAmber),
            modifier = Modifier.fillMaxWidth()
          ) {
            if (isAnalyzing) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("İçerik İnceleniyor...", color = Color.Black, fontWeight = FontWeight.Bold)
            } else {
              Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Temiz İçerik Puanını Hesapla", color = Color.Black, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    if (analysisResult != null) {
      val res = analysisResult!!
      item {
        val scoreColor = when {
          res.cleanScore >= 75 -> BentoGreen
          res.cleanScore >= 50 -> BentoAmber
          else -> BentoPink
        }

        Surface(
          shape = RoundedCornerShape(24.dp),
          color = BentoCardBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(text = res.productName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = res.ratingLabel, fontSize = 12.sp, color = scoreColor, fontWeight = FontWeight.SemiBold)
              }

              // Score Dial
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .background(scoreColor.copy(alpha = 0.15f), CircleShape)
                  .border(2.dp, scoreColor, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(text = "${res.cleanScore}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                  Text(text = "/100", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }

            Text(text = res.verdictText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)

            // Pros
            if (res.pros.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "✅ Artı Yönler", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoGreen)
                res.pros.forEach { p ->
                  Text(text = "• $p", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }

            // Cons
            if (res.cons.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "⚠️ Dikkat Edilmesi Gerekenler", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoPink)
                res.cons.forEach { c ->
                  Text(text = "• $c", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }

            // Additives
            if (res.additivesFound.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "🔬 Tespit Edilen Katkı Maddeleri", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoAmber)
                res.additivesFound.forEach { a ->
                  Text(text = "• $a", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 5. AKILLI TAHMİN & İÇGÖRÜLER
// -------------------------------------------------------------
@Composable
private fun AiInsightsSection(
  service: AiAssistantService,
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  todayNutrition: DailyNutritionSummary,
  todayWaterMl: Int,
  streakCount: Int
) {
  val coroutineScope = rememberCoroutineScope()
  var isGenerating by remember { mutableStateOf(false) }
  var insightsResult by remember { mutableStateOf<SmartInsightsResult?>(null) }

  LaunchedEffect(Unit) {
    isGenerating = true
    insightsResult = service.generateSmartInsights(
      currentWeight = userProfile.currentWeight,
      targetWeight = userProfile.targetWeight,
      dailyCalories = todayNutrition.totalCalories,
      targetCalories = metrics.targetCalories,
      waterMl = todayWaterMl,
      waterTarget = userProfile.dailyWaterTarget,
      streakDays = streakCount
    )
    isGenerating = false
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = BentoCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoGreen.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Insights, contentDescription = null, tint = BentoGreen)
              Text(
                text = "Akıllı İlerleme Tahmini",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            IconButton(
              onClick = {
                isGenerating = true
                coroutineScope.launch {
                  insightsResult = service.generateSmartInsights(
                    currentWeight = userProfile.currentWeight,
                    targetWeight = userProfile.targetWeight,
                    dailyCalories = todayNutrition.totalCalories,
                    targetCalories = metrics.targetCalories,
                    waterMl = todayWaterMl,
                    waterTarget = userProfile.dailyWaterTarget,
                    streakDays = streakCount
                  )
                  isGenerating = false
                }
              }
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = BentoGreen)
            }
          }

          if (isGenerating) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = BentoGreen)
            }
          } else if (insightsResult != null) {
            val res = insightsResult!!

            // Forecast Highlight Box
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = BentoGreen.copy(alpha = 0.12f),
              border = androidx.compose.foundation.BorderStroke(1.dp, BentoGreen.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = BentoGreen)
                Text(
                  text = res.forecastText,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  lineHeight = 18.sp
                )
              }
            }

            // Calorie & Trend Review
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(text = "📊 Kalori & Alışkanlık Trendi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoBlue)
              Text(text = res.calorieBalanceNote, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(text = res.weeklyTrendReview, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Water review
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(text = "💧 Hidrasyon Durumu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoTeal)
              Text(text = res.waterStreakNote, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(color = BentoCardBorder)

            // 3 Action items for tomorrow
            Text(text = "🎯 Yarın İçin 3 Öncelikli Aksiyon", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              res.actionItems.forEachIndexed { idx, action ->
                var isChecked by remember { mutableStateOf(false) }
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(BentoSubBg, RoundedCornerShape(12.dp))
                    .clickable { isChecked = !isChecked }
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isChecked) BentoGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Text(
                    text = action,
                    fontSize = 12.sp,
                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 6. TARİF & MENÜ PLANLAYICI (RECIPES)
// -------------------------------------------------------------
@Composable
private fun AiRecipesSection(
  service: AiAssistantService,
  metrics: CalculatedMetrics,
  todayNutrition: DailyNutritionSummary
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()

  var selectedDiet by remember { mutableStateOf("Dengeli") }
  var isGenerating by remember { mutableStateOf(false) }
  var recipeResult by remember { mutableStateOf<RecipePlanResult?>(null) }

  val remainingCalories = (metrics.targetCalories - todayNutrition.totalCalories).coerceAtLeast(350)

  LaunchedEffect(Unit) {
    isGenerating = true
    recipeResult = service.generateRecipeAndMealPlan(
      goalType = "Kilo Kontrolü",
      targetCalories = remainingCalories,
      preference = selectedDiet
    )
    isGenerating = false
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = BentoCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoPink.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = BentoPink)
            Text(
              text = "Kalan Kalorine Uygun Akıllı Tarif",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Text(
            text = "Kalan kalori hedefinize (~$remainingCalories kcal) göre kişiselleştirilmiş sağlıklı yemek tarifleri ve anında kopyalanabilir alışveriş listesi.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // Diet Filter Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("Dengeli", "Yüksek Protein", "Düşük Karb", "Vejetaryen").forEach { diet ->
              val isSel = selectedDiet == diet
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSel) BentoPink.copy(alpha = 0.2f) else BentoSubBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BentoPink else BentoCardBorder),
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    selectedDiet = diet
                    isGenerating = true
                    coroutineScope.launch {
                      recipeResult = service.generateRecipeAndMealPlan(
                        goalType = "Kilo Kontrolü",
                        targetCalories = remainingCalories,
                        preference = diet
                      )
                      isGenerating = false
                    }
                  }
              ) {
                Text(
                  text = diet,
                  fontSize = 11.sp,
                  color = if (isSel) BentoPink else MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(vertical = 8.dp)
                )
              }
            }
          }
        }
      }
    }

    if (isGenerating) {
      item {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = BentoPink)
        }
      }
    } else if (recipeResult != null) {
      val r = recipeResult!!
      item {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = BentoCardBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, BentoPink.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(text = r.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(top = 4.dp)
                ) {
                  Surface(shape = RoundedCornerShape(8.dp), color = BentoPink.copy(alpha = 0.15f)) {
                    Text(text = r.dietBadge, fontSize = 10.sp, color = BentoPink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                  Text(text = "⏱️ ${r.prepTimeMinutes} dk", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = BentoGreen.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "${r.calories} kcal",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = BentoGreen,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }

            // Macros
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              MacroMiniBadge("Protein", "${r.protein.toInt()}g", BentoBlue, Modifier.weight(1f))
              MacroMiniBadge("Karb", "${r.carbs.toInt()}g", BentoAmber, Modifier.weight(1f))
              MacroMiniBadge("Yağ", "${r.fat.toInt()}g", BentoPink, Modifier.weight(1f))
            }

            // Ingredients
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = "🛒 Malzemeler", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                TextButton(
                  onClick = {
                    val shoppingText = "${r.title} Alışveriş Listesi:\n" + r.ingredients.joinToString("\n") { "• $it" }
                    clipboardManager.setText(AnnotatedString(shoppingText))
                    Toast.makeText(context, "Malzemeler panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                  },
                  contentPadding = PaddingValues(0.dp)
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = BentoBlue)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Listeyi Kopyala", fontSize = 11.sp, color = BentoBlue)
                }
              }

              r.ingredients.forEach { ing ->
                Text(text = "• $ing", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            HorizontalDivider(color = BentoCardBorder)

            // Instructions
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(text = "👨‍🍳 Hazırlanışı", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              r.instructions.forEachIndexed { i, step ->
                Row(
                  modifier = Modifier.padding(vertical = 2.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(text = "${i + 1}.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoPink)
                  Text(text = step, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Helper Component: Mini Macro Badge
// -------------------------------------------------------------
@Composable
private fun MacroMiniBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = BentoSubBg,
    border = androidx.compose.foundation.BorderStroke(1.dp, BentoCardBorder),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
      Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
