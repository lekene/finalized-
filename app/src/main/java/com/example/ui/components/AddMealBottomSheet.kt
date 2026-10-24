package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FoodItemAnalysis
import com.example.data.GeminiNutritionService
import com.example.data.Meal
import com.example.data.NutritionResult
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealBottomSheet(
  targetDate: String,
  mealToEdit: Meal? = null,
  initialMealType: String = "Kahvaltı",
  onDismiss: () -> Unit,
  onSaveMeal: (Meal) -> Unit,
  onCalculateNutrition: suspend (String, String, (GeminiNutritionService.CalculationState) -> Unit) -> NutritionResult?
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  var mealType by remember { mutableStateOf(mealToEdit?.mealType ?: initialMealType) }
  var foodName by remember { mutableStateOf(mealToEdit?.foodName ?: "") }
  var portion by remember { mutableStateOf(mealToEdit?.portion ?: "") }
  var caloriesText by remember { mutableStateOf(mealToEdit?.calories?.toString() ?: "") }
  var proteinText by remember {
    mutableStateOf(
      if (mealToEdit != null && mealToEdit.protein > 0f) String.format(Locale.US, "%.1f", mealToEdit.protein) else ""
    )
  }
  var carbsText by remember {
    mutableStateOf(
      if (mealToEdit != null && mealToEdit.carbs > 0f) String.format(Locale.US, "%.1f", mealToEdit.carbs) else ""
    )
  }
  var fatText by remember {
    mutableStateOf(
      if (mealToEdit != null && mealToEdit.fat > 0f) String.format(Locale.US, "%.1f", mealToEdit.fat) else ""
    )
  }
  var fiberText by remember {
    mutableStateOf(
      if (mealToEdit != null && mealToEdit.fiber > 0f) String.format(Locale.US, "%.1f", mealToEdit.fiber) else ""
    )
  }
  var note by remember { mutableStateOf(mealToEdit?.note ?: "") }

  // Candidate selection state (up to 3 items)
  var candidateList by remember { mutableStateOf<List<FoodItemAnalysis>>(emptyList()) }
  var selectedCandidateIndex by remember { mutableStateOf<Int?>(null) }

  // Base values for proportional scaling on portion change
  var baseAmount by remember { mutableStateOf<Float?>(null) }
  var baseCalories by remember { mutableStateOf<Int?>(null) }
  var baseProtein by remember { mutableStateOf<Float?>(null) }
  var baseCarbs by remember { mutableStateOf<Float?>(null) }
  var baseFat by remember { mutableStateOf<Float?>(null) }
  var baseFiber by remember { mutableStateOf<Float?>(null) }
  var baseUnit by remember { mutableStateOf<String?>(null) }

  var errorMessage by remember { mutableStateOf<String?>(null) }
  var aiStatusMessage by remember { mutableStateOf<String?>(null) }
  var isAiSuccess by remember { mutableStateOf(false) }
  var isNotFoodError by remember { mutableStateOf(false) }

  // AI calculation state
  var isCalculating by remember { mutableStateOf(false) }
  var isRetrying by remember { mutableStateOf(false) }

  val context = androidx.compose.ui.platform.LocalContext.current
  val mealTypeOptions = remember {
    listOf(
      "Kahvaltı" to com.example.R.string.meal_breakfast,
      "Öğle" to com.example.R.string.meal_lunch,
      "Akşam" to com.example.R.string.meal_dinner,
      "Ara Öğün" to com.example.R.string.meal_snack
    )
  }
  val scrollState = rememberScrollState()

  // Helper to extract number from portion string (e.g. "2 kase" -> 2.0f, "0,5 dilim" -> 0.5f)
  fun parseNumberFromPortion(str: String): Float? {
    val clean = str.trim().replace(",", ".")
    val match = Regex("([0-9]+(?:\\.[0-9]+)?)").find(clean) ?: return null
    return match.value.toFloatOrNull()
  }

  fun formatMacro(value: Float): String {
    return if (value <= 0f) ""
    else if (value % 1.0f == 0f) value.toInt().toString()
    else String.format(Locale.US, "%.1f", value)
  }

  fun formatAmount(value: Float): String {
    return if (value % 1.0f == 0f) value.toInt().toString()
    else String.format(Locale("tr", "TR"), "%.1f", value)
  }

  // Applies candidate values and sets base values for proportional scaling
  fun applyCandidate(candidate: FoodItemAnalysis) {
    foodName = candidate.ad
    val mStr = formatAmount(candidate.miktar)
    portion = "$mStr ${candidate.birim}"
    caloriesText = candidate.kcal.toString()
    proteinText = formatMacro(candidate.protein)
    carbsText = formatMacro(candidate.karbonhidrat)
    fatText = formatMacro(candidate.yag)
    fiberText = formatMacro(candidate.lif)

    baseAmount = if (candidate.miktar > 0f) candidate.miktar else 1f
    baseCalories = candidate.kcal
    baseProtein = candidate.protein
    baseCarbs = candidate.karbonhidrat
    baseFat = candidate.yag
    baseFiber = candidate.lif
    baseUnit = candidate.birim

    isAiSuccess = true
    aiStatusMessage = null
    errorMessage = null
  }

  // Proportional scaling when portion changes
  fun onPortionChange(newPortion: String) {
    portion = newPortion
    aiStatusMessage = null

    val bAmt = baseAmount
    val bCal = baseCalories
    if (bAmt != null && bAmt > 0f && bCal != null && bCal > 0) {
      val parsedNum = parseNumberFromPortion(newPortion)
      if (parsedNum != null && parsedNum > 0f) {
        val ratio = parsedNum / bAmt
        caloriesText = maxOf(0, (bCal * ratio).roundToInt()).toString()
        if (baseProtein != null) proteinText = formatMacro(baseProtein!! * ratio)
        if (baseCarbs != null) carbsText = formatMacro(baseCarbs!! * ratio)
        if (baseFat != null) fatText = formatMacro(baseFat!! * ratio)
        if (baseFiber != null) fiberText = formatMacro(baseFiber!! * ratio)
      }
    }
  }

  // Step button adjustment (0.5 delta)
  fun adjustPortionStep(delta: Float) {
    val currentNum = parseNumberFromPortion(portion) ?: (baseAmount ?: 1.0f)
    val newNum = maxOf(0.5f, currentNum + delta)
    val numStr = formatAmount(newNum)
    val unit = baseUnit ?: run {
      val parts = portion.trim().split(Regex("\\s+"), limit = 2)
      if (parts.size > 1 && parts[1].isNotBlank()) parts[1] else "porsiyon"
    }
    val newPortion = "$numStr $unit"
    onPortionChange(newPortion)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(scrollState)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (mealToEdit == null) androidx.compose.ui.res.stringResource(com.example.R.string.add_meal) else androidx.compose.ui.res.stringResource(com.example.R.string.edit_meal),
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.close),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Meal Type Selection Chips
      Text(
        text = androidx.compose.ui.res.stringResource(com.example.R.string.meal_type_label),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        mealTypeOptions.forEach { (typeKey, titleRes) ->
          val isSelected = mealType == typeKey
          FilterChip(
            selected = isSelected,
            onClick = { mealType = typeKey },
            label = {
              Text(
                text = androidx.compose.ui.res.stringResource(titleRes),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 1. Food Name (Mandatory)
      OutlinedTextField(
        value = foodName,
        onValueChange = {
          foodName = it
          errorMessage = null
          aiStatusMessage = null
          isAiSuccess = false
          isNotFoodError = false
        },
        label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.food_name_required)) },
        placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.food_name_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = errorMessage != null && foodName.isBlank()
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Portion with Step Buttons (- 0,5 +)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = portion,
          onValueChange = { onPortionChange(it) },
          label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.portion_label)) },
          placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.portion_placeholder)) },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        // Step Buttons (- / +)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          IconButton(
            onClick = { adjustPortionStep(-0.5f) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Remove,
              contentDescription = "Porsiyon Azalt",
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          Text(
            text = "0,5",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
          )

          IconButton(
            onClick = { adjustPortionStep(0.5f) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Porsiyon Artır",
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // AI "Kalori Hesapla" Button
      Button(
        onClick = {
          if (foodName.isBlank()) {
            aiStatusMessage = context.getString(com.example.R.string.please_enter_food_name)
            return@Button
          }

          coroutineScope.launch {
            isCalculating = true
            isRetrying = false
            aiStatusMessage = null
            isAiSuccess = false
            isNotFoodError = false
            candidateList = emptyList()
            selectedCandidateIndex = null

            try {
              val result = onCalculateNutrition(foodName, portion) { state ->
                when (state) {
                  is GeminiNutritionService.CalculationState.Loading -> {
                    isCalculating = true
                    isRetrying = false
                  }
                  is GeminiNutritionService.CalculationState.Retrying -> {
                    isCalculating = true
                    isRetrying = true
                  }
                  is GeminiNutritionService.CalculationState.Error -> {
                    isCalculating = false
                    isRetrying = false
                    aiStatusMessage = "Bulunamadı, adı daha açık yazmayı dene ya da kaloriyi elle gir"
                  }
                  is GeminiNutritionService.CalculationState.Success -> {
                    isCalculating = false
                    isRetrying = false
                  }
                  else -> {}
                }
              }

              if (result != null && !result.isFood) {
                isNotFoodError = true
                candidateList = emptyList()
                selectedCandidateIndex = null
                aiStatusMessage = null
                // 2. Alanlara dokunma, kullanıcı elle girebilsin
              } else if (result != null && result.candidates.isNotEmpty()) {
                isNotFoodError = false
                candidateList = result.candidates
                selectedCandidateIndex = 0
                applyCandidate(result.candidates.first())
              } else if (result != null && result.item != null && result.item.yiyecek_mi) {
                isNotFoodError = false
                candidateList = listOf(result.item)
                selectedCandidateIndex = 0
                applyCandidate(result.item)
              } else {
                isNotFoodError = false
                aiStatusMessage = "Bulunamadı, adı daha açık yazmayı dene ya da kaloriyi elle gir"
              }
            } catch (e: Exception) {
              aiStatusMessage = "Bulunamadı, adı daha açık yazmayı dene ya da kaloriyi elle gir"
            } finally {
              // Spinner asla sonsuza kadar dönmez
              isCalculating = false
              isRetrying = false
              // 3. Sonuç gelince klavyeyi kapat ve aday listesini görünür alana kaydır
              focusManager.clearFocus()
              keyboardController?.hide()
              coroutineScope.launch {
                kotlinx.coroutines.delay(100)
                scrollState.animateScrollTo(scrollState.maxValue)
              }
            }
          }
        },
        enabled = !isCalculating,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
          disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        if (isCalculating) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color.White,
            strokeWidth = 2.5.dp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = if (isRetrying) androidx.compose.ui.res.stringResource(com.example.R.string.retrying) else androidx.compose.ui.res.stringResource(com.example.R.string.calculating),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
          )
        } else {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.calculate_calories),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }

      // 2. Yiyecek Olmayan Girdi Kırmızı Çerçeveli Uyarı Mesajı
      AnimatedVisibility(visible = isNotFoodError) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
          border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = "Bu bir yiyecek ya da içecek gibi görünmüyor. Adı yeniden yazmayı dene ya da kaloriyi elle gir.",
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.error,
              lineHeight = 18.sp
            )
          }
        }
      }

      // 3. Aday Yiyecekler Listesi & "Elle Gireceğim" Butonu
      AnimatedVisibility(visible = candidateList.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Gerçek aday sayısına göre başlık (1 ise "1 öneri", >1 ise "N seçenek")
          val candidateTitle = if (candidateList.size == 1) {
            "Önerilen Aday (1 öneri):"
          } else {
            "Önerilen Adaylar (${candidateList.size} seçenek - birini seçin):"
          }

          Text(
            text = candidateTitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          candidateList.forEachIndexed { idx, candidate ->
            val isSelected = selectedCandidateIndex == idx
            val isLowConf = candidate.guven == "dusuk" || candidate.isTutarsiz
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              border = BorderStroke(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedCandidateIndex = idx
                  applyCandidate(candidate)
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                  )

                  Column {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(
                        text = candidate.ad,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      // 5. guven "dusuk" adaylarda "Tahmini" etiketi kalsın
                      if (isLowConf) {
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                          border = BorderStroke(0.8.dp, Color(0xFFF59E0B))
                        ) {
                          Text(
                            text = "Tahmini",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                          )
                        }
                      }
                    }
                    val mStr = formatAmount(candidate.miktar)
                    Text(
                      text = "$mStr ${candidate.birim} (~${candidate.gram_tahmini}g) • P: ${candidate.protein.toInt()}g  K: ${candidate.karbonhidrat.toInt()}g  Y: ${candidate.yag.toInt()}g",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Text(
                  text = "${candidate.kcal} kcal",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          // "Elle gireceğim" seçeneği (alanları boş bıraksın)
          OutlinedButton(
            onClick = {
              selectedCandidateIndex = null
              candidateList = emptyList()
              // Alanları boş bırak
              caloriesText = ""
              proteinText = ""
              carbsText = ""
              fatText = ""
              fiberText = ""
              baseAmount = null
              baseCalories = null
              baseProtein = null
              baseCarbs = null
              baseFat = null
              baseFiber = null
              baseUnit = null
              isAiSuccess = false
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(38.dp)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Hiçbiri uymuyor, elle gireceğim",
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // 3. Türkçe Hata Mesajı
      if (aiStatusMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = aiStatusMessage!!,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.error,
          lineHeight = 17.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 3. Calories (Mandatory, can be edited manually)
      OutlinedTextField(
        value = caloriesText,
        onValueChange = {
          caloriesText = it.filter { char -> char.isDigit() }
          errorMessage = null
        },
        label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.calories_kcal_required)) },
        placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.calories_placeholder)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = errorMessage != null && caloriesText.isBlank()
      )

      Spacer(modifier = Modifier.height(16.dp))

      // 5. Macros (2x2 Izgara, "Protein (g)" bölünmez, hepsi aynı yükseklikte)
      Text(
        text = androidx.compose.ui.res.stringResource(com.example.R.string.macros_optional_label),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))

      // 2x2 Izgara: Satır 1 -> Protein (g) & Karb (g)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = proteinText,
          onValueChange = { proteinText = it },
          label = {
            Text(
              text = "Protein (g)",
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier
            .weight(1f)
            .height(60.dp),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = carbsText,
          onValueChange = { carbsText = it },
          label = {
            Text(
              text = "Karb (g)",
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier
            .weight(1f)
            .height(60.dp),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2x2 Izgara: Satır 2 -> Yağ (g) & Lif (g)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = fatText,
          onValueChange = { fatText = it },
          label = {
            Text(
              text = "Yağ (g)",
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier
            .weight(1f)
            .height(60.dp),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = fiberText,
          onValueChange = { fiberText = it },
          label = {
            Text(
              text = "Lif (g)",
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier
            .weight(1f)
            .height(60.dp),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 6. Note (Optional)
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.note_optional)) },
        placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.note_meal_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        maxLines = 2
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = errorMessage!!,
          color = MaterialTheme.colorScheme.error,
          fontSize = 13.sp
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 7. Action Buttons (İptal ve Kaydet)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier
            .weight(1f)
            .height(50.dp),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.cancel), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Button(
          onClick = {
            if (foodName.isBlank()) {
              errorMessage = context.getString(com.example.R.string.error_enter_food_name)
              return@Button
            }
            val cal = caloriesText.toIntOrNull()
            if (cal == null || cal < 0) {
              errorMessage = context.getString(com.example.R.string.error_invalid_calories)
              return@Button
            }

            val p = proteinText.replace(",", ".").toFloatOrNull() ?: 0f
            val c = carbsText.replace(",", ".").toFloatOrNull() ?: 0f
            val f = fatText.replace(",", ".").toFloatOrNull() ?: 0f
            val fib = fiberText.replace(",", ".").toFloatOrNull() ?: 0f

            val meal = Meal(
              id = mealToEdit?.id ?: 0L,
              mealType = mealType,
              foodName = foodName.trim(),
              portion = portion.trim(),
              calories = cal,
              protein = p,
              carbs = c,
              fat = f,
              fiber = fib,
              note = note.trim(),
              date = mealToEdit?.date ?: targetDate,
              createdAt = mealToEdit?.createdAt ?: System.currentTimeMillis()
            )
            onSaveMeal(meal)
          },
          modifier = Modifier
            .weight(1f)
            .height(50.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.save),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
          )
        }
      }
    }
  }
}
