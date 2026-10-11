package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiNutritionService
import com.example.data.Meal
import com.example.data.NutritionResult
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import kotlinx.coroutines.launch
import java.util.Locale

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

  var mealType by remember { mutableStateOf(mealToEdit?.mealType ?: initialMealType) }
  var foodName by remember { mutableStateOf(mealToEdit?.foodName ?: "") }
  var portion by remember { mutableStateOf(mealToEdit?.portion ?: "") }
  var caloriesText by remember { mutableStateOf(mealToEdit?.calories?.toString() ?: "") }
  var proteinText by remember { mutableStateOf(if (mealToEdit != null && mealToEdit.protein > 0f) mealToEdit.protein.toString() else "") }
  var carbsText by remember { mutableStateOf(if (mealToEdit != null && mealToEdit.carbs > 0f) mealToEdit.carbs.toString() else "") }
  var fatText by remember { mutableStateOf(if (mealToEdit != null && mealToEdit.fat > 0f) mealToEdit.fat.toString() else "") }
  var fiberText by remember { mutableStateOf(if (mealToEdit != null && mealToEdit.fiber > 0f) mealToEdit.fiber.toString() else "") }
  var note by remember { mutableStateOf(mealToEdit?.note ?: "") }

  var errorMessage by remember { mutableStateOf<String?>(null) }
  var aiStatusMessage by remember { mutableStateOf<String?>(null) }
  var isAiSuccess by remember { mutableStateOf(false) }

  // AI calculation state
  var isCalculating by remember { mutableStateOf(false) }
  var isRetrying by remember { mutableStateOf(false) }

  val mealTypes = listOf("Kahvaltı", "Öğle", "Akşam", "Ara Öğün")
  val scrollState = rememberScrollState()

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
          text = if (mealToEdit == null) "Öğün Ekle" else "Öğünü Düzenle",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Kapat",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Meal Type Selection Chips
      Text(
        text = "Öğün Türü",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        mealTypes.forEach { type ->
          val isSelected = mealType == type
          FilterChip(
            selected = isSelected,
            onClick = { mealType = type },
            label = {
              Text(
                text = type,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = GreenPrimary,
              selectedLabelColor = Color.White
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
        },
        label = { Text("Yiyecek Adı *") },
        placeholder = { Text("ör. Menemen, Tavuklu Salata, Yulaf") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = errorMessage != null && foodName.isBlank()
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Portion
      OutlinedTextField(
        value = portion,
        onValueChange = {
          portion = it
          aiStatusMessage = null
          isAiSuccess = false
        },
        label = { Text("Porsiyon Miktarı") },
        placeholder = { Text("ör. 1 porsiyon, 1 kase, 2 dilim, 150 gr") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(12.dp))

      // AI "Kalori Hesapla" Button (under Food name and Portion fields)
      Button(
        onClick = {
          if (foodName.isBlank()) {
            aiStatusMessage = "Lütfen önce yiyecek adını giriniz."
            return@Button
          }

          coroutineScope.launch {
            isCalculating = true
            isRetrying = false
            aiStatusMessage = null
            isAiSuccess = false

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
                is GeminiNutritionService.CalculationState.Success -> {
                  isCalculating = false
                  isRetrying = false
                }
                is GeminiNutritionService.CalculationState.Error -> {
                  isCalculating = false
                  isRetrying = false
                  aiStatusMessage = state.message
                }
                else -> {}
              }
            }

            // Always ensure spinner stops at the end
            isCalculating = false
            isRetrying = false

            if (result != null) {
              caloriesText = result.kalori.toString()
              proteinText = if (result.protein > 0f) String.format(Locale.US, "%.1f", result.protein) else ""
              carbsText = if (result.karbonhidrat > 0f) String.format(Locale.US, "%.1f", result.karbonhidrat) else ""
              fatText = if (result.yag > 0f) String.format(Locale.US, "%.1f", result.yag) else ""
              fiberText = if (result.lif > 0f) String.format(Locale.US, "%.1f", result.lif) else ""
              isAiSuccess = true
              aiStatusMessage = null
              errorMessage = null
            }
          }
        },
        enabled = !isCalculating,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = GreenPrimary.copy(alpha = 0.9f),
          disabledContainerColor = GreenPrimary.copy(alpha = 0.5f)
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
            text = if (isRetrying) "Tekrar deneniyor…" else "Hesaplanıyor…",
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
            text = "Kalori Hesapla",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }

      // Success feedback banner
      AnimatedVisibility(visible = isAiSuccess) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = GreenLight,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Değerler otomatik hesaplandı. Dilerseniz elle düzenleyebilirsiniz.",
            fontSize = 12.sp,
            color = GreenLight
          )
        }
      }

      // Turkish Error message display
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
        label = { Text("Kalori (kcal) *") },
        placeholder = { Text("ör. 280") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = errorMessage != null && caloriesText.isBlank()
      )

      Spacer(modifier = Modifier.height(16.dp))

      // 4. Macros (Optional, can be edited manually)
      Text(
        text = "Makro Besin Değerleri (Gram, İsteğe bağlı)",
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = proteinText,
          onValueChange = { proteinText = it },
          label = { Text("Protein (g)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = carbsText,
          onValueChange = { carbsText = it },
          label = { Text("Karb (g)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = fatText,
          onValueChange = { fatText = it },
          label = { Text("Yağ (g)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = fiberText,
          onValueChange = { fiberText = it },
          label = { Text("Lif (g)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 5. Note (Optional)
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Not (İsteğe bağlı)") },
        placeholder = { Text("Eklemek istediğiniz notlar...") },
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

      // 6. Action Buttons (İptal ve Kaydet)
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
          Text(text = "İptal", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Button(
          onClick = {
            if (foodName.isBlank()) {
              errorMessage = "Lütfen yiyecek adını giriniz."
              return@Button
            }
            val cal = caloriesText.toIntOrNull()
            if (cal == null || cal < 0) {
              errorMessage = "Lütfen geçerli bir kalori değeri giriniz."
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
          colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(
            text = "Kaydet",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }
}
