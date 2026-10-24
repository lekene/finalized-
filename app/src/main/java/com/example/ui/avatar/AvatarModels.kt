package com.example.ui.avatar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 12 adet düz renkli (flat) hazır vektör avatar tanımı.
 * Genç, orta yaş ve yaşlı; kadın ve erkek gruplarında en az 2'şer farklı görünüm.
 */
data class PresetAvatar(
  val id: String,
  val title: String,
  val gender: String, // "Kadın" or "Erkek"
  val ageGroup: String, // "Genç", "Orta Yaş", "Yaşlı"
  val bgColor: Color,
  val skinColor: Color,
  val hairColor: Color,
  val clothColor: Color
)

val PRESET_AVATARS: List<PresetAvatar> = listOf(
  // 1. GENÇ KADIN 1: Sıcak ten, uzun koyu saç, pembe üst
  PresetAvatar(
    id = "preset:w_young_1",
    title = "Genç Kadın (Uzun Koyu Saç)",
    gender = "Kadın",
    ageGroup = "Genç",
    bgColor = Color(0xFF332030),
    skinColor = Color(0xFFFFDFBF),
    hairColor = Color(0xFF2B1810),
    clothColor = Color(0xFFF43F5E)
  ),
  // 2. GENÇ KADIN 2: Açık ten, dalgalı sarı saç, turkuaz üst
  PresetAvatar(
    id = "preset:w_young_2",
    title = "Genç Kadın (Dalgalı Sarı Saç)",
    gender = "Kadın",
    ageGroup = "Genç",
    bgColor = Color(0xFF1E3338),
    skinColor = Color(0xFFFDE8D3),
    hairColor = Color(0xFFE2AD5F),
    clothColor = Color(0xFF06B6D4)
  ),
  // 3. ORTA YAŞ KADIN 1: Kumral küt saç, mercan üst
  PresetAvatar(
    id = "preset:w_mid_1",
    title = "Orta Yaş Kadın (Küt Saç)",
    gender = "Kadın",
    ageGroup = "Orta Yaş",
    bgColor = Color(0xFF33261E),
    skinColor = Color(0xFFF5D6BA),
    hairColor = Color(0xFF5D3A1A),
    clothColor = Color(0xFFFB923C)
  ),
  // 4. ORTA YAŞ KADIN 2: Toplu koyu saç, şık mor üst, gözlük
  PresetAvatar(
    id = "preset:w_mid_2",
    title = "Orta Yaş Kadın (Gözlüklü)",
    gender = "Kadın",
    ageGroup = "Orta Yaş",
    bgColor = Color(0xFF28203B),
    skinColor = Color(0xFFE2B488),
    hairColor = Color(0xFF1F1F1F),
    clothColor = Color(0xFF8B5CF6)
  ),
  // 5. YAŞLI KADIN 1: Gümüş dalgalı saç, lavanta üst
  PresetAvatar(
    id = "preset:w_senior_1",
    title = "Yaşlı Kadın (Gümüş Saç)",
    gender = "Kadın",
    ageGroup = "Yaşlı",
    bgColor = Color(0xFF2B2633),
    skinColor = Color(0xFFFFE0D0),
    hairColor = Color(0xFFCBD5E1),
    clothColor = Color(0xFFA78BFA)
  ),
  // 6. YAŞLI KADIN 2: Beyaz kıvırcık saç, gözlüklü, zümrüt üst
  PresetAvatar(
    id = "preset:w_senior_2",
    title = "Yaşlı Kadın (Kıvırcık Beyaz Saç)",
    gender = "Kadın",
    ageGroup = "Yaşlı",
    bgColor = Color(0xFF1C2E2A),
    skinColor = Color(0xFFE6C4A6),
    hairColor = Color(0xFFF1F5F9),
    clothColor = Color(0xFF10B981)
  ),

  // 7. GENÇ ERKEK 1: Kısa modern siyah saç, mavi polo
  PresetAvatar(
    id = "preset:m_young_1",
    title = "Genç Erkek (Kısa Koyu Saç)",
    gender = "Erkek",
    ageGroup = "Genç",
    bgColor = Color(0xFF1E283D),
    skinColor = Color(0xFFE8B88A),
    hairColor = Color(0xFF1A1A1A),
    clothColor = Color(0xFF3B82F6)
  ),
  // 8. GENÇ ERKEK 2: Kahverengi dağınık saç, yeşil tişört
  PresetAvatar(
    id = "preset:m_young_2",
    title = "Genç Erkek (Kahve Saç)",
    gender = "Erkek",
    ageGroup = "Genç",
    bgColor = Color(0xFF202F24),
    skinColor = Color(0xFFFFE2C7),
    hairColor = Color(0xFF6B4226),
    clothColor = Color(0xFF22C55E)
  ),
  // 9. ORTA YAŞ ERKEK 1: Koyu saç, hafif sakal, antrasit gömlek
  PresetAvatar(
    id = "preset:m_mid_1",
    title = "Orta Yaş Erkek (Hafif Sakallı)",
    gender = "Erkek",
    ageGroup = "Orta Yaş",
    bgColor = Color(0xFF24272D),
    skinColor = Color(0xFFE2B488),
    hairColor = Color(0xFF2C1E18),
    clothColor = Color(0xFF64748B)
  ),
  // 10. ORTA YAŞ ERKEK 2: Klasik saç, gözlüklü, lacivert kazak
  PresetAvatar(
    id = "preset:m_mid_2",
    title = "Orta Yaş Erkek (Gözlüklü)",
    gender = "Erkek",
    ageGroup = "Orta Yaş",
    bgColor = Color(0xFF1B2338),
    skinColor = Color(0xFFDEAA7A),
    hairColor = Color(0xFF452B1E),
    clothColor = Color(0xFF1E40AF)
  ),
  // 11. YAŞLI ERKEK 1: Kır bıyıklı, açık alın, amber hırka
  PresetAvatar(
    id = "preset:m_senior_1",
    title = "Yaşlı Erkek (Bıyıklı)",
    gender = "Erkek",
    ageGroup = "Yaşlı",
    bgColor = Color(0xFF352A1D),
    skinColor = Color(0xFFFFDFBF),
    hairColor = Color(0xFF94A3B8),
    clothColor = Color(0xFFF59E0B)
  ),
  // 12. YAŞLI ERKEK 2: Beyaz tam sakallı, yuvarlak gözlük, petrol yeşili
  PresetAvatar(
    id = "preset:m_senior_2",
    title = "Yaşlı Erkek (Beyaz Sakallı)",
    gender = "Erkek",
    ageGroup = "Yaşlı",
    bgColor = Color(0xFF1E2E33),
    skinColor = Color(0xFFE6C4A6),
    hairColor = Color(0xFFF8FAFC),
    clothColor = Color(0xFF0D9488)
  )
)

/**
 * Verilen preset ID'ye göre flat vektör avatarı Canvas üzerine çizer.
 */
fun DrawScope.drawPresetAvatar(presetId: String) {
  val preset = PRESET_AVATARS.find { it.id == presetId } ?: PRESET_AVATARS[0]
  val w = size.width
  val h = size.height

  // 1. Zemin Dairesi
  drawCircle(
    color = preset.bgColor,
    radius = w / 2f,
    center = Offset(w / 2f, h / 2f)
  )

  // 2. Arka Saç (Uzun saçlı kadın modelleri için)
  if (preset.id == "preset:w_young_1" || preset.id == "preset:w_young_2") {
    drawRoundRect(
      color = preset.hairColor,
      topLeft = Offset(w * 0.22f, h * 0.22f),
      size = Size(w * 0.56f, h * 0.65f),
      cornerRadius = CornerRadius(w * 0.25f, w * 0.25f)
    )
  } else if (preset.id == "preset:w_mid_1") {
    // Küt saç arka fon
    drawRoundRect(
      color = preset.hairColor,
      topLeft = Offset(w * 0.23f, h * 0.24f),
      size = Size(w * 0.54f, h * 0.54f),
      cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
    )
  }

  // 3. Gövde / Omuzlar (Giysi)
  val bodyPath = Path().apply {
    moveTo(w * 0.12f, h)
    cubicTo(
      w * 0.14f, h * 0.72f,
      w * 0.28f, h * 0.66f,
      w * 0.40f, h * 0.68f
    )
    lineTo(w * 0.60f, h * 0.68f)
    cubicTo(
      w * 0.72f, h * 0.66f,
      w * 0.86f, h * 0.72f,
      w * 0.88f, h
    )
    close()
  }
  drawPath(bodyPath, color = preset.clothColor)

  // Giysi yaka detayı
  drawOval(
    color = preset.clothColor.copy(alpha = 0.8f),
    topLeft = Offset(w * 0.38f, h * 0.68f),
    size = Size(w * 0.24f, h * 0.10f)
  )

  // 4. Boyun
  drawRect(
    color = preset.skinColor.copy(alpha = 0.92f),
    topLeft = Offset(w * 0.43f, h * 0.52f),
    size = Size(w * 0.14f, h * 0.18f)
  )

  // 5. Yüz (Baş Dairesi / Ovali)
  val faceTop = h * 0.25f
  val faceWidth = w * 0.44f
  val faceHeight = h * 0.42f
  drawOval(
    color = preset.skinColor,
    topLeft = Offset((w - faceWidth) / 2f, faceTop),
    size = Size(faceWidth, faceHeight)
  )

  // 6. Kulaklar
  drawOval(
    color = preset.skinColor,
    topLeft = Offset(w * 0.24f, h * 0.40f),
    size = Size(w * 0.08f, h * 0.10f)
  )
  drawOval(
    color = preset.skinColor,
    topLeft = Offset(w * 0.68f, h * 0.40f),
    size = Size(w * 0.08f, h * 0.10f)
  )

  // 7. Saç Stilleri (Ön Saç)
  when (preset.id) {
    "preset:w_young_1" -> {
      // Düz ortadan ayrılmış perçem
      val hairPath = Path().apply {
        moveTo(w * 0.26f, h * 0.34f)
        cubicTo(w * 0.35f, h * 0.20f, w * 0.65f, h * 0.20f, w * 0.74f, h * 0.34f)
        cubicTo(w * 0.65f, h * 0.28f, w * 0.52f, h * 0.32f, w * 0.50f, h * 0.36f)
        cubicTo(w * 0.48f, h * 0.32f, w * 0.35f, h * 0.28f, w * 0.26f, h * 0.34f)
        close()
      }
      drawPath(hairPath, color = preset.hairColor)
    }
    "preset:w_young_2" -> {
      // Dalgalı yandan ayrılmış sarı saç
      val hairPath = Path().apply {
        moveTo(w * 0.25f, h * 0.36f)
        cubicTo(w * 0.30f, h * 0.18f, w * 0.70f, h * 0.18f, w * 0.75f, h * 0.36f)
        cubicTo(w * 0.68f, h * 0.26f, w * 0.45f, h * 0.26f, w * 0.35f, h * 0.34f)
        close()
      }
      drawPath(hairPath, color = preset.hairColor)
    }
    "preset:w_mid_1" -> {
      // Küt saç ön perçem
      drawOval(
        color = preset.hairColor,
        topLeft = Offset(w * 0.26f, h * 0.19f),
        size = Size(w * 0.48f, h * 0.18f)
      )
    }
    "preset:w_mid_2" -> {
      // Topuz ve düz ön saç
      drawCircle(
        color = preset.hairColor,
        radius = w * 0.12f,
        center = Offset(w * 0.50f, h * 0.18f)
      )
      drawOval(
        color = preset.hairColor,
        topLeft = Offset(w * 0.26f, h * 0.22f),
        size = Size(w * 0.48f, h * 0.14f)
      )
    }
    "preset:w_senior_1" -> {
      // Gümüş kabarık kısa dalgalı saç
      drawCircle(color = preset.hairColor, radius = w * 0.12f, center = Offset(w * 0.32f, h * 0.25f))
      drawCircle(color = preset.hairColor, radius = w * 0.13f, center = Offset(w * 0.50f, h * 0.21f))
      drawCircle(color = preset.hairColor, radius = w * 0.12f, center = Offset(w * 0.68f, h * 0.25f))
    }
    "preset:w_senior_2" -> {
      // Beyaz kıvırcık saç kümesi
      drawCircle(color = preset.hairColor, radius = w * 0.10f, center = Offset(w * 0.28f, h * 0.28f))
      drawCircle(color = preset.hairColor, radius = w * 0.11f, center = Offset(w * 0.40f, h * 0.21f))
      drawCircle(color = preset.hairColor, radius = w * 0.11f, center = Offset(w * 0.60f, h * 0.21f))
      drawCircle(color = preset.hairColor, radius = w * 0.10f, center = Offset(w * 0.72f, h * 0.28f))
    }
    "preset:m_young_1" -> {
      // Modern kısa fade saç
      drawOval(
        color = preset.hairColor,
        topLeft = Offset(w * 0.27f, h * 0.20f),
        size = Size(w * 0.46f, h * 0.16f)
      )
    }
    "preset:m_young_2" -> {
      // Dağınık kahve saç
      drawCircle(color = preset.hairColor, radius = w * 0.11f, center = Offset(w * 0.38f, h * 0.23f))
      drawCircle(color = preset.hairColor, radius = w * 0.12f, center = Offset(w * 0.55f, h * 0.21f))
      drawOval(
        color = preset.hairColor,
        topLeft = Offset(w * 0.27f, h * 0.22f),
        size = Size(w * 0.46f, h * 0.14f)
      )
    }
    "preset:m_mid_1" -> {
      // Yana taranmış saç
      drawOval(
        color = preset.hairColor,
        topLeft = Offset(w * 0.27f, h * 0.21f),
        size = Size(w * 0.46f, h * 0.15f)
      )
    }
    "preset:m_mid_2" -> {
      // Düzgün kırpılmış saç
      drawOval(
        color = preset.hairColor,
        topLeft = Offset(w * 0.28f, h * 0.22f),
        size = Size(w * 0.44f, h * 0.14f)
      )
    }
    "preset:m_senior_1" -> {
      // Şakaklarda saç, ortası seyrek
      drawOval(
        color = preset.hairColor,
        topLeft = Offset(w * 0.24f, h * 0.30f),
        size = Size(w * 0.10f, h * 0.16f)
      )
      drawOval(
        color = preset.hairColor,
        topLeft = Offset(w * 0.66f, h * 0.30f),
        size = Size(w * 0.10f, h * 0.16f)
      )
    }
    "preset:m_senior_2" -> {
      // Beyaz saç çerçevesi
      drawOval(
        color = preset.hairColor,
        topLeft = Offset(w * 0.27f, h * 0.22f),
        size = Size(w * 0.46f, h * 0.14f)
      )
    }
  }

  // 8. Kaşlar
  val browColor = preset.hairColor.copy(alpha = 0.85f)
  val browStroke = Stroke(width = w * 0.022f)
  drawLine(
    color = browColor,
    start = Offset(w * 0.36f, h * 0.39f),
    end = Offset(w * 0.44f, h * 0.39f),
    strokeWidth = browStroke.width
  )
  drawLine(
    color = browColor,
    start = Offset(w * 0.56f, h * 0.39f),
    end = Offset(w * 0.64f, h * 0.39f),
    strokeWidth = browStroke.width
  )

  // 9. Gözler (Sade düz renkli dost canlısı noktalar)
  val eyeColor = Color(0xFF1E293B)
  drawCircle(color = eyeColor, radius = w * 0.028f, center = Offset(w * 0.40f, h * 0.44f))
  drawCircle(color = eyeColor, radius = w * 0.028f, center = Offset(w * 0.60f, h * 0.44f))

  // 10. Burun
  val noseColor = preset.skinColor.copy(alpha = 0.70f)
  drawCircle(color = Color(0x33000000), radius = w * 0.016f, center = Offset(w * 0.50f, h * 0.50f))

  // 11. Sakal / Bıyık (İlgili modeller için)
  if (preset.id == "preset:m_mid_1") {
    // Hafif kirli sakal gölgesi
    drawArc(
      color = preset.hairColor.copy(alpha = 0.25f),
      startAngle = 0f,
      sweepAngle = 180f,
      useCenter = true,
      topLeft = Offset(w * 0.36f, h * 0.52f),
      size = Size(w * 0.28f, h * 0.14f)
    )
  } else if (preset.id == "preset:m_senior_1") {
    // Bıyık
    drawOval(
      color = preset.hairColor,
      topLeft = Offset(w * 0.42f, h * 0.52f),
      size = Size(w * 0.16f, h * 0.05f)
    )
  } else if (preset.id == "preset:m_senior_2") {
    // Dolgun beyaz sakal
    val beardPath = Path().apply {
      moveTo(w * 0.34f, h * 0.48f)
      lineTo(w * 0.42f, h * 0.54f)
      lineTo(w * 0.58f, h * 0.54f)
      lineTo(w * 0.66f, h * 0.48f)
      cubicTo(w * 0.70f, h * 0.66f, w * 0.30f, h * 0.66f, w * 0.34f, h * 0.48f)
      close()
    }
    drawPath(beardPath, color = preset.hairColor)
  }

  // 12. Ağız (Sıcak tebessüm)
  drawArc(
    color = Color(0xFFC2410C).copy(alpha = 0.75f),
    startAngle = 10f,
    sweepAngle = 160f,
    useCenter = false,
    topLeft = Offset(w * 0.44f, h * 0.53f),
    size = Size(w * 0.12f, h * 0.06f),
    style = Stroke(width = w * 0.02f)
  )

  // 13. Gözlük (İlgili modeller için)
  if (preset.id == "preset:w_mid_2" || preset.id == "preset:w_senior_2" || preset.id == "preset:m_mid_2" || preset.id == "preset:m_senior_2") {
    val glassesColor = if (preset.id == "preset:m_mid_2") Color(0xFF94A3B8) else Color(0xFFE2E8F0)
    val glassesStroke = Stroke(width = w * 0.022f)
    // Sol cam
    drawCircle(
      color = glassesColor,
      radius = w * 0.07f,
      center = Offset(w * 0.40f, h * 0.44f),
      style = glassesStroke
    )
    // Sağ cam
    drawCircle(
      color = glassesColor,
      radius = w * 0.07f,
      center = Offset(w * 0.60f, h * 0.44f),
      style = glassesStroke
    )
    // Köprü
    drawLine(
      color = glassesColor,
      start = Offset(w * 0.47f, h * 0.44f),
      end = Offset(w * 0.53f, h * 0.44f),
      strokeWidth = glassesStroke.width
    )
  }
}
