package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.ui.CalculatedMetrics
import com.example.ui.theme.AppTheme
import java.util.Locale

enum class MetricInfoType {
  BMI,
  BMR,
  TDEE,
  TARGET_CALORIES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricInfoBottomSheet(
  metricType: MetricInfoType,
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scrollState = rememberScrollState()

  val cardBg = AppTheme.colors.surface
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border
  val subLabelColor = AppTheme.colors.textSecondary
  val accentColor = AppTheme.colors.accent

  val bentoGreen = Color(0xFF22C55E)
  val bentoBlue = Color(0xFF38BDF8)
  val bentoAmber = Color(0xFFFBBF24)
  val bentoPink = Color(0xFFFB7185)
  val bentoPurple = Color(0xFFA855F7)
  val bentoRed = Color(0xFFEF4444)

  val (headerTitle, headerBadge, themeColor, headerIcon) = when (metricType) {
    MetricInfoType.BMI -> {
      val bmiColor = when (metrics.bmiCategory) {
        "Normal" -> bentoGreen
        "Zayıf" -> bentoBlue
        "Kilolu" -> bentoAmber
        else -> bentoRed
      }
      Quadruple(
        "Vücut Kitle İndeksi (BMI / VKİ)",
        "${String.format(Locale.US, "%.1f", metrics.bmi)} • ${metrics.bmiCategory}",
        bmiColor,
        Icons.Default.Speed
      )
    }
    MetricInfoType.BMR -> Quadruple(
      "Bazal Metabolizma Hızı (BMR)",
      "${metrics.bmr} kcal / gün",
      bentoBlue,
      Icons.Default.BatteryChargingFull
    )
    MetricInfoType.TDEE -> Quadruple(
      "Günlük Enerji İhtiyacı (TDEE)",
      "${metrics.tdee} kcal / gün",
      bentoAmber,
      Icons.Default.Bolt
    )
    MetricInfoType.TARGET_CALORIES -> Quadruple(
      "Günlük Kalori Hedefi",
      "${metrics.targetCalories} kcal / gün",
      bentoGreen,
      Icons.Default.TrackChanges
    )
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = cardBg,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 8.dp)
          .width(42.dp)
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(borderColor)
      )
    },
    modifier = Modifier.testTag("metric_info_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp)
        .padding(bottom = 36.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header: İkon, Başlık, Rozet ve Kapatma Butonu
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
            color = themeColor.copy(alpha = 0.16f),
            modifier = Modifier.size(44.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = headerIcon,
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.size(24.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = headerTitle,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = themeColor.copy(alpha = 0.16f),
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Text(
                text = headerBadge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(36.dp)
            .background(itemBg, CircleShape)
            .testTag("metric_info_close_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Kapat",
            tint = subLabelColor,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // İçerik Bölümleri (Türe Göre)
      when (metricType) {
        MetricInfoType.BMI -> BmiDetailContent(userProfile, metrics, themeColor, itemBg, borderColor, subLabelColor)
        MetricInfoType.BMR -> BmrDetailContent(userProfile, metrics, themeColor, itemBg, borderColor, subLabelColor)
        MetricInfoType.TDEE -> TdeeDetailContent(userProfile, metrics, themeColor, itemBg, borderColor, subLabelColor)
        MetricInfoType.TARGET_CALORIES -> TargetCaloriesDetailContent(userProfile, metrics, themeColor, itemBg, borderColor, subLabelColor)
      }

      // Alt Buton
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("metric_info_dismiss_button")
      ) {
        Text(
          text = "Anladım",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = Color.White
        )
      }
    }
  }
}

// ==========================================
// 1. BMI (VÜCUT KİTLE İNDEKSİ) DETAY İÇERİĞİ
// ==========================================
@Composable
private fun BmiDetailContent(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  themeColor: Color,
  itemBg: Color,
  borderColor: Color,
  subLabelColor: Color
) {
  val heightM = userProfile.height / 100f
  val minHealthyWeight = 18.5f * heightM * heightM
  val maxHealthyWeight = 24.9f * heightM * heightM

  // 1. Nedir?
  InfoSectionCard(
    title = "Vücut Kitle İndeksi (BMI) Nedir?",
    icon = Icons.Default.Info,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Text(
      text = "Vücut Kitle İndeksi (VKİ / BMI), Dünya Sağlık Örgütü (WHO) tarafından boy uzunluğunuza kıyasla kilonuzun sağlıklı, dengeli bir aralıkta olup olmadığını belirlemek için kullanılan uluslararası temel standarttır.",
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurface,
      lineHeight = 19.sp
    )
  }

  // 2. Nasıl Hesaplanır?
  InfoSectionCard(
    title = "Nasıl Hesaplanır / Tahmin Edilir?",
    icon = Icons.Default.Calculate,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    FormulaBox(
      formula = "BMI = Kilo (kg) ÷ [Boy (m)]²",
      borderColor = borderColor
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "Sizin Değerlerinizle Hesaplama:",
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = subLabelColor
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "${String.format(Locale.US, "%.1f", userProfile.currentWeight)} kg ÷ (${String.format(Locale.US, "%.2f", heightM)} m × ${String.format(Locale.US, "%.2f", heightM)} m) = ${String.format(Locale.US, "%.1f", metrics.bmi)}",
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = themeColor,
      fontFamily = FontFamily.Monospace
    )
  }

  // 3. Ne Olmalıdır? (İdeal Aralıklar)
  InfoSectionCard(
    title = "BMI Değeriniz Ne Olmalıdır?",
    icon = Icons.Default.CheckCircle,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      BmiCategoryRow("< 18.5", "Zayıf", "Düşük kilo / kas kütlesi", Color(0xFF38BDF8), metrics.bmi < 18.5f)
      BmiCategoryRow("18.5 – 24.9", "Normal (İdeal)", "En sağlıklı ve dengeli aralık", Color(0xFF22C55E), metrics.bmi in 18.5f..24.99f)
      BmiCategoryRow("25.0 – 29.9", "Fazla Kilolu", "Hafif risk, kilo verme önerilir", Color(0xFFFBBF24), metrics.bmi in 25.0f..29.99f)
      BmiCategoryRow("≥ 30.0", "Obezite", "Sağlık riskleri artabilir", Color(0xFFEF4444), metrics.bmi >= 30.0f)
    }

    Spacer(modifier = Modifier.height(12.dp))
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = themeColor.copy(alpha = 0.12f),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(themeColor.copy(alpha = 0.4f), themeColor.copy(alpha = 0.2f)))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "Boyunuza Göre İdeal Kilo Aralığı",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = themeColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${userProfile.height.toInt()} cm boy için ideal sağlıklı kilo: ${String.format(Locale.US, "%.1f", minHealthyWeight)} kg – ${String.format(Locale.US, "%.1f", maxHealthyWeight)} kg arasındadır.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface,
          lineHeight = 17.sp
        )
      }
    }
  }

  // 4. Uzman İpucu
  InfoTipCard(
    tip = "BMI genel bir kılavuzdur; kas ve yağ dokusunu ayırt etmez. Sporcularda yüksek çıkması kas kütlesinden kaynaklanabilir. Bel çevresi ve ayna görüntünüzle birlikte değerlendirilmelidir.",
    subLabelColor = subLabelColor
  )
}

// ==========================================
// 2. BMR (BAZAL METABOLİZMA) DETAY İÇERİĞİ
// ==========================================
@Composable
private fun BmrDetailContent(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  themeColor: Color,
  itemBg: Color,
  borderColor: Color,
  subLabelColor: Color
) {
  // 1. Nedir?
  InfoSectionCard(
    title = "Bazal Metabolizma (BMR) Nedir?",
    icon = Icons.Default.Info,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Text(
      text = "Bazal Metabolizma Hızı (BMR), tüm gün yatakta hiç hareket etmeden dursanız dahi vücudunuzun sadece hayatta kalmak (nefes alma, organların çalışması, kan dolaşımı, vücut ısısı) için yaktığı minimum temel kalori miktarıdır.",
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurface,
      lineHeight = 19.sp
    )
  }

  // 2. Nasıl Hesaplanır?
  InfoSectionCard(
    title = "Nasıl Hesaplanır / Formülü Nedir?",
    icon = Icons.Default.Calculate,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Text(
      text = "Klinik araştırmalarda doğruluğu en yüksek kabul edilen Mifflin-St Jeor formülü kullanılır:",
      fontSize = 12.sp,
      color = subLabelColor
    )
    Spacer(modifier = Modifier.height(8.dp))
    FormulaBox(
      formula = if (userProfile.gender == "Erkek") {
        "BMR (Erkek) = (10 × Kilo) + (6.25 × Boy) - (5 × Yaş) + 5"
      } else {
        "BMR (Kadın) = (10 × Kilo) + (6.25 × Boy) - (5 × Yaş) - 161"
      },
      borderColor = borderColor
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "Sizin Parametrelerinizle Hesap:",
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = subLabelColor
    )
    Spacer(modifier = Modifier.height(4.dp))
    val genderAdjust = if (userProfile.gender == "Erkek") "+ 5" else "- 161"
    Text(
      text = "(10 × ${userProfile.currentWeight.toInt()}) + (6.25 × ${userProfile.height.toInt()}) - (5 × ${userProfile.age}) $genderAdjust = ${metrics.bmr} kcal",
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = themeColor,
      fontFamily = FontFamily.Monospace
    )
  }

  // 3. Ne Olmalıdır & Neden Önemli?
  InfoSectionCard(
    title = "Ne Olmalıdır ve Neden Önemlidir?",
    icon = Icons.Default.Shield,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      BulletItem(
        title = "Günlük Enerjinin %60-%75'i:",
        desc = "Günlük yaktığınız toplam kalorinin büyük kısmını BMR oluşturur; fiziksel hareket sadece üzerine eklenir."
      )
      BulletItem(
        title = "Kritik Güvenlik Kuralı:",
        desc = "Kalori alımınız uzun süre asla BMR değerinizin (${metrics.bmr} kcal) çok altına düşmemelidir! Aksi takdirde metabolizma korumaya geçer, tiroit fonksiyonları yavaşlar ve kas yıkımı başlar."
      )
      BulletItem(
        title = "Kas Kütlesiyle Artar:",
        desc = "Kas dokusu dinlenirken yağdan yaklaşık 3 kat fazla kalori yakar. Kas kütlesi arttıkça BMR değeri de yükselir."
      )
    }
  }

  // 4. Uzman İpucu
  InfoTipCard(
    tip = "Metabolizmanızı hızlandırmak için ağır açlık diyetleri yerine yeterli protein alın ve haftada 2-3 gün direnç (kuvvet) egzersizleri yapın.",
    subLabelColor = subLabelColor
  )
}

// ==========================================
// 3. TDEE (GÜNLÜK ENERJİ İHTİYACI) DETAY İÇERİĞİ
// ==========================================
@Composable
private fun TdeeDetailContent(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  themeColor: Color,
  itemBg: Color,
  borderColor: Color,
  subLabelColor: Color
) {
  val (multiplier, actLabel) = when (userProfile.activityLevel) {
    "hareketsiz" -> 1.200f to "Hareketsiz (Masa başı)"
    "hafif_aktif" -> 1.375f to "Hafif Aktif (Haftada 1-3 gün spor)"
    "orta_aktif" -> 1.550f to "Orta Aktif (Haftada 3-5 gün spor)"
    "cok_aktif" -> 1.725f to "Çok Aktif (Haftada 6-7 gün spor)"
    else -> 1.200f to "Hareketsiz"
  }

  // 1. Nedir?
  InfoSectionCard(
    title = "Günlük Enerji İhtiyacı (TDEE) Nedir?",
    icon = Icons.Default.Info,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Text(
      text = "TDEE (Total Daily Energy Expenditure), bazal metabolizmanıza gün içindeki tüm hareketleriniz, adımlarınız, egzersizleriniz ve besinlerin sindirilmesi (Termik Etki) dahil olmak üzere 24 saatte yaktığınız TOPLAM kalori miktarıdır.",
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurface,
      lineHeight = 19.sp
    )
  }

  // 2. Nasıl Hesaplanır?
  InfoSectionCard(
    title = "Nasıl Hesaplanır / Aktivite Çarpanı",
    icon = Icons.Default.Calculate,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    FormulaBox(
      formula = "TDEE = BMR × Fiziksel Aktivite Katsayısı",
      borderColor = borderColor
    )
    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      ActivityLevelRow("Hareketsiz", "1.200", "Masa başı iş, az hareket", userProfile.activityLevel == "hareketsiz", themeColor)
      ActivityLevelRow("Hafif Aktif", "1.375", "Haftada 1-3 gün hafif spor", userProfile.activityLevel == "hafif_aktif", themeColor)
      ActivityLevelRow("Orta Aktif", "1.550", "Haftada 3-5 gün orta spor", userProfile.activityLevel == "orta_aktif", themeColor)
      ActivityLevelRow("Çok Aktif", "1.725", "Haftada 6-7 gün ağır spor/fiziksel iş", userProfile.activityLevel == "cok_aktif", themeColor)
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "Sizin Seçiminiz: $actLabel (×$multiplier)",
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = subLabelColor
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "${metrics.bmr} kcal × $multiplier = ${metrics.tdee} kcal / gün",
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = themeColor,
      fontFamily = FontFamily.Monospace
    )
  }

  // 3. Ne Olmalıdır?
  InfoSectionCard(
    title = "Kilo Dengesi İçin Anlamı Nedir?",
    icon = Icons.Default.CheckCircle,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      BulletItem(
        title = "Kilonuzu Korumak İçin:",
        desc = "Günde tam olarak ${metrics.tdee} kcal tüketirseniz kilonuz sabit kalır (Kalori dengesi)."
      )
      BulletItem(
        title = "Kilo Vermek İçin:",
        desc = "TDEE'nin altında kalori alarak (${metrics.tdee - 500} kcal civarı) vücudun yağ depolarını yakması sağlanır."
      )
      BulletItem(
        title = "Kilo Almak İçin:",
        desc = "TDEE'nin üzerinde kalori alarak vücuda yeni doku ve kas inşası için enerji sağlanır."
      )
    }
  }

  // 4. Uzman İpucu
  InfoTipCard(
    tip = "Aktivite seviyenizi Profil sekmesinden istediğiniz an güncelleyebilirsiniz. Günlük adımlarınız ve antrenman yoğunluğunuz değiştikçe TDEE otomatik olarak yenilenir.",
    subLabelColor = subLabelColor
  )
}

// ==========================================
// 4. KALORİ HEDEFİ DETAY İÇERİĞİ
// ==========================================
@Composable
private fun TargetCaloriesDetailContent(
  userProfile: UserProfile,
  metrics: CalculatedMetrics,
  themeColor: Color,
  itemBg: Color,
  borderColor: Color,
  subLabelColor: Color
) {
  val isDeficit = userProfile.targetWeight < userProfile.currentWeight
  val isSurplus = userProfile.targetWeight > userProfile.currentWeight

  // 1. Nedir?
  InfoSectionCard(
    title = "Günlük Kalori Hedefi Nedir?",
    icon = Icons.Default.Info,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Text(
      text = "Hedef kilonuza (${userProfile.targetWeight} kg) kas kütlenizi koruyarak ve metabolizmanızı yavaşlatmadan ulaşmanız için her gün tüketmeniz gereken net kalori miktarıdır.",
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurface,
      lineHeight = 19.sp
    )
  }

  // 2. Nasıl Belirlenir?
  InfoSectionCard(
    title = "Nasıl Belirlenir & Hesaplanır?",
    icon = Icons.Default.Calculate,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Text(
      text = "Bilimsel Kriter: 1 kg vücut yağı yaklaşık 7.700 kcal enerji depolar.",
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = themeColor
    )
    Spacer(modifier = Modifier.height(6.dp))

    if (isDeficit) {
      FormulaBox(
        formula = "Kalori Hedefi = TDEE - 500 kcal",
        borderColor = borderColor
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "• Günde 500 kcal açık, haftada 3.500 kcal açık oluşturur.\n• Bu da haftada yaklaşık 0.5 kg (ayda ~2 kg) saf yağ yakımı sağlar.\n• Hızlı kilo verme yerine kalıcı ve sarkmasız kilo vermenin altın anahtarıdır.",
        fontSize = 12.sp,
        color = subLabelColor,
        lineHeight = 18.sp
      )
    } else if (isSurplus) {
      FormulaBox(
        formula = "Kalori Hedefi = TDEE + 300 kcal",
        borderColor = borderColor
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "• Günde +300 kcal fazlalık, gereksiz yağlanma olmadan temiz kas kütlesi (Clean Bulk) artışı sağlar.",
        fontSize = 12.sp,
        color = subLabelColor,
        lineHeight = 18.sp
      )
    } else {
      FormulaBox(
        formula = "Kalori Hedefi = TDEE (Kilo Koruma)",
        borderColor = borderColor
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Sizin Kalori Hedefiniz: ${metrics.targetCalories} kcal / gün",
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = themeColor,
      fontFamily = FontFamily.Monospace
    )
  }

  // 3. Güvenlik Sınırları ve Makrolar
  InfoSectionCard(
    title = "Güvenli Sınırlar ve Makro Dağılımı",
    icon = Icons.Default.Shield,
    iconTint = themeColor,
    itemBg = itemBg,
    borderColor = borderColor
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val minThreshold = if (userProfile.gender == "Erkek") "1.500 kcal" else "1.200 kcal"
      BulletItem(
        title = "Güvenli Minimum Eşik:",
        desc = "Hormonal denge ve organ sağlığı için kalori hedefiniz ${userProfile.gender}lerde asla $minThreshold eşiğinin altına düşürülmez."
      )
      BulletItem(
        title = "Protein (${metrics.proteinGrams}g - %30):",
        desc = "Kas dokusunu korur, onarır ve tokluk hormonu salgılatır."
      )
      BulletItem(
        title = "Karbonhidrat (${metrics.carbGrams}g - %40):",
        desc = "Günlük enerjinizi ve antrenman performansınızı sağlar."
      )
      BulletItem(
        title = "Sağlıklı Yağ (${metrics.fatGrams}g - %30):",
        desc = "Hormon sentezi ve A, D, E, K vitaminlerinin emilimi için zorunludur."
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = themeColor.copy(alpha = 0.12f),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Tahmini Süre: ${metrics.estimatedText}",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }

  // 4. Uzman İpucu
  InfoTipCard(
    tip = "Günlük kalori hedefinizi karşılarken besinlerin kalitesine (tam tahıllar, kaliteli proteinler, bol sebze ve su) özen göstermek açlık krizlerini önler.",
    subLabelColor = subLabelColor
  )
}

// ==========================================
// YARDIMCI KOMPONENTLER (Bento Stili Kartlar)
// ==========================================
@Composable
private fun InfoSectionCard(
  title: String,
  icon: ImageVector,
  iconTint: Color,
  itemBg: Color,
  borderColor: Color,
  content: @Composable () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = itemBg,
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(borderColor, borderColor.copy(alpha = 0.4f)))
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      content()
    }
  }
}

@Composable
private fun FormulaBox(
  formula: String,
  borderColor: Color
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF0F172A).copy(alpha = 0.8f),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(borderColor.copy(alpha = 0.8f), borderColor.copy(alpha = 0.3f)))
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(modifier = Modifier.padding(10.dp)) {
      Text(
        text = formula,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFF38BDF8)
      )
    }
  }
}

@Composable
private fun BmiCategoryRow(
  range: String,
  category: String,
  desc: String,
  color: Color,
  isSelected: Boolean
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) color.copy(alpha = 0.18f) else Color.Transparent,
    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.5f)))) else null,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = category,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
              color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = color
              ) {
                Text(
                  text = "Siz",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
          }
          Text(
            text = desc,
            fontSize = 10.sp,
            color = AppTheme.colors.textSecondary
          )
        }
      }
      Text(
        text = range,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = if (isSelected) color else AppTheme.colors.textSecondary
      )
    }
  }
}

@Composable
private fun ActivityLevelRow(
  title: String,
  multiplier: String,
  desc: String,
  isSelected: Boolean,
  themeColor: Color
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) themeColor.copy(alpha = 0.16f) else Color.Transparent,
    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(themeColor, themeColor.copy(alpha = 0.5f)))) else null,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface
          )
          if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = themeColor
            ) {
              Text(
                text = "Aktif",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }
        Text(
          text = desc,
          fontSize = 10.sp,
          color = AppTheme.colors.textSecondary
        )
      }
      Text(
        text = "×$multiplier",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = if (isSelected) themeColor else AppTheme.colors.textSecondary
      )
    }
  }
}

@Composable
private fun BulletItem(
  title: String,
  desc: String
) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "•",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = AppTheme.colors.accent,
      modifier = Modifier.padding(end = 6.dp)
    )
    Column {
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = desc,
        fontSize = 11.sp,
        color = AppTheme.colors.textSecondary,
        lineHeight = 16.sp
      )
    }
  }
}

@Composable
private fun InfoTipCard(
  tip: String,
  subLabelColor: Color
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0xFFFBBF24).copy(alpha = 0.10f),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(Color(0xFFFBBF24).copy(alpha = 0.35f), Color(0xFFFBBF24).copy(alpha = 0.15f)))
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      Icon(
        imageVector = Icons.Default.Lightbulb,
        contentDescription = null,
        tint = Color(0xFFFBBF24),
        modifier = Modifier
          .size(18.dp)
          .padding(top = 1.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = tip,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 16.sp
      )
    }
  }
}

private data class Quadruple<A, B, C, D>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D
)
