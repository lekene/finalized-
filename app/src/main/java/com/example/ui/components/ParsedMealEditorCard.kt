package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AiAssistantService
import com.example.data.FoodItemAnalysis
import com.example.data.ScannedFoodItem
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Düzenlenebilir Yiyecek Satırı Modeli
 */
data class EditableParsedFoodItem(
  val id: String = java.util.UUID.randomUUID().toString(),
  var isSelected: Boolean = true,
  var name: String,
  var amount: Float,
  var unit: String,
  var calories: Int,
  var protein: Float,
  var carbs: Float,
  var fat: Float,
  var fiber: Float,
  val baseCaloriesPerUnit: Float,
  val baseProteinPerUnit: Float,
  val baseCarbsPerUnit: Float,
  val baseFatPerUnit: Float,
  val baseFiberPerUnit: Float,
  var isManuallyEdited: Boolean = false,
  var isExpanded: Boolean = false,
  val guven: String = "yuksek",
  val varsayim: Boolean = false,
  val isTutarsiz: Boolean = false,
  val isOlasilikDisi: Boolean = false,
  val warnings: List<String> = emptyList()
)

val PARSED_MEAL_AVAILABLE_UNITS = listOf(
  "porsiyon",
  "adet",
  "dilim",
  "kase",
  "tabak",
  "yemek kaşığı",
  "çay bardağı",
  "su bardağı",
  "gr",
  "kg",
  "ml",
  "litre"
)

fun FoodItemAnalysis.toEditableParsedFoodItem(): EditableParsedFoodItem {
  val safeAmount = if (this.miktar > 0f) this.miktar else 1f
  val safeUnit = if (this.birim.isNotBlank()) this.birim else "porsiyon"
  val baseCal = this.kcal / safeAmount
  val baseProt = this.protein / safeAmount
  val baseCarb = this.karbonhidrat / safeAmount
  val baseFat = this.yag / safeAmount
  val baseFib = this.lif / safeAmount

  return EditableParsedFoodItem(
    name = this.ad,
    amount = safeAmount,
    unit = safeUnit,
    calories = this.kcal,
    protein = this.protein,
    carbs = this.karbonhidrat,
    fat = this.yag,
    fiber = this.lif,
    baseCaloriesPerUnit = baseCal,
    baseProteinPerUnit = baseProt,
    baseCarbsPerUnit = baseCarb,
    baseFatPerUnit = baseFat,
    baseFiberPerUnit = baseFib,
    guven = this.guven,
    varsayim = this.varsayim,
    isTutarsiz = this.isTutarsiz,
    isOlasilikDisi = this.isOlasilikDisi,
    warnings = this.warnings
  )
}

fun ScannedFoodItem.toEditableParsedFoodItem(): EditableParsedFoodItem {
  val safeAmount = if (this.miktar > 0f) this.miktar else 1f
  val safeUnit = if (this.birim.isNotBlank()) this.birim else "porsiyon"
  val baseCal = this.calories / safeAmount
  val baseProt = this.protein / safeAmount
  val baseCarb = this.carbs / safeAmount
  val baseFat = this.fat / safeAmount
  val baseFib = this.fiber / safeAmount

  return EditableParsedFoodItem(
    name = this.name,
    amount = safeAmount,
    unit = safeUnit,
    calories = this.calories,
    protein = this.protein,
    carbs = this.carbs,
    fat = this.fat,
    fiber = this.fiber,
    baseCaloriesPerUnit = baseCal,
    baseProteinPerUnit = baseProt,
    baseCarbsPerUnit = baseCarb,
    baseFatPerUnit = baseFat,
    baseFiberPerUnit = baseFib,
    guven = this.guven,
    varsayim = this.varsayim,
    isTutarsiz = this.isTutarsiz,
    isOlasilikDisi = this.isOlasilikDisi,
    warnings = this.warnings
  )
}

fun EditableParsedFoodItem.toScannedFoodItem(): ScannedFoodItem {
  val amountStr = if (amount % 1.0f == 0f) amount.toInt().toString() else String.format(Locale("tr", "TR"), "%.1f", amount)
  val portionStr = "$amountStr $unit"
  return ScannedFoodItem(
    name = name,
    portion = portionStr,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    miktar = amount,
    birim = unit,
    guven = guven,
    varsayim = varsayim,
    isTutarsiz = isTutarsiz,
    isOlasilikDisi = isOlasilikDisi,
    warnings = warnings
  )
}

/**
 * Asistan Metin ve Fotoğraf Tanıma sekmelerinde ortak kullanılan,
 * düzenlenebilir ayrıştırılmış öğün sonuç kartı.
 */
@Composable
fun ParsedMealEditorCard(
  initialItems: List<ScannedFoodItem>,
  initialMealType: String = "Öğle",
  generalWarnings: List<String> = emptyList(),
  initialUnmatched: List<String> = emptyList(),
  aiAssistantService: AiAssistantService? = null,
  onSaveToJournal: (selectedItems: List<ScannedFoodItem>, mealType: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()

  // State
  var items by remember(initialItems) {
    mutableStateOf(initialItems.map { it.toEditableParsedFoodItem() })
  }
  var unmatchedList by remember(initialUnmatched) {
    mutableStateOf(initialUnmatched)
  }
  var selectedMealCategory by remember(initialMealType) {
    mutableStateOf(
      when (initialMealType.lowercase(Locale("tr", "TR"))) {
        "kahvalti", "kahvaltı" -> "Kahvaltı"
        "ogle", "öğle" -> "Öğle"
        "aksam", "akşam" -> "Akşam"
        "ara_ogun", "ara öğün", "ara" -> "Ara Öğün"
        else -> "Öğle"
      }
    )
  }

  // Modals & temporary editing states
  var editingItemNameId by remember { mutableStateOf<String?>(null) }
  var nameEditText by remember { mutableStateOf("") }
  var isCalculatingUnmatched by remember { mutableStateOf<String?>(null) }

  // Add Item Dialog / Inline State
  var showAddDialog by remember { mutableStateOf(false) }
  var addQueryText by remember { mutableStateOf("") }
  var isCalculatingNewItem by remember { mutableStateOf(false) }
  var addErrorMessage by remember { mutableStateOf<String?>(null) }

  // Manual Add Fields
  var manualMode by remember { mutableStateOf(false) }
  var manualName by remember { mutableStateOf("") }
  var manualAmountText by remember { mutableStateOf("1") }
  var manualUnit by remember { mutableStateOf("porsiyon") }
  var manualCaloriesText by remember { mutableStateOf("") }
  var manualProteinText by remember { mutableStateOf("") }
  var manualCarbsText by remember { mutableStateOf("") }
  var manualFatText by remember { mutableStateOf("") }
  var manualFiberText by remember { mutableStateOf("") }

  // Dynamic calculated totals (ONLY for selected items)
  val selectedItems = items.filter { it.isSelected }
  val totalCalories = selectedItems.sumOf { it.calories }
  val totalProtein = selectedItems.map { it.protein }.sum()
  val totalCarbs = selectedItems.map { it.carbs }.sum()
  val totalFat = selectedItems.map { it.fat }.sum()
  val totalFiber = selectedItems.map { it.fiber }.sum()

  // Theme colors
  val bentoGreen = AppTheme.colors.accent
  val bentoCardBg = AppTheme.colors.surface
  val bentoCardBorder = AppTheme.colors.border
  val bentoSubBg = AppTheme.colors.itemBg
  val bentoAmber = Color(0xFFFBBF24)
  val bentoBlue = Color(0xFF38BDF8)
  val bentoPink = Color(0xFFFB7185)
  val bentoTeal = Color(0xFF2DD4BF)
  val bentoPurple = Color(0xFFA855F7)

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = bentoCardBg,
    border = BorderStroke(1.dp, bentoGreen.copy(alpha = 0.4f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Header: Bulunan: N yiyecek & Total Calories Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Bulunan: ${items.size} yiyecek",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${selectedItems.size} seçili",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = bentoGreen.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, bentoGreen.copy(alpha = 0.35f))
        ) {
          Text(
            text = "$totalCalories kcal",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = bentoGreen,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }

      // 4. Kırmızı Uyarı: Eşleşmeyen İfadeler (eslesmeyen_ifadeler)
      if (unmatchedList.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          unmatchedList.forEach { unmatchedExpr ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFEF4444).copy(alpha = 0.12f),
              border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
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
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "Şu kısım için sonuç bulunamadı: \"$unmatchedExpr\"",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                  onClick = {
                    if (aiAssistantService != null) {
                      isCalculatingUnmatched = unmatchedExpr
                      coroutineScope.launch {
                        try {
                          val analysis = aiAssistantService.calculateFoodFromText(unmatchedExpr)
                          val newItem = analysis.ogeler.firstOrNull()
                          if (newItem != null) {
                            val editable = newItem.toEditableParsedFoodItem()
                            items = items + editable
                            unmatchedList = unmatchedList - unmatchedExpr
                          } else {
                            // If AI couldn't calculate, prefill manual add dialog
                            manualName = unmatchedExpr
                            manualMode = true
                            showAddDialog = true
                          }
                        } catch (e: Exception) {
                          manualName = unmatchedExpr
                          manualMode = true
                          showAddDialog = true
                        } finally {
                          isCalculatingUnmatched = null
                        }
                      }
                    } else {
                      manualName = unmatchedExpr
                      manualMode = true
                      showAddDialog = true
                    }
                  },
                  enabled = isCalculatingUnmatched != unmatchedExpr,
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                  modifier = Modifier.height(34.dp)
                ) {
                  if (isCalculatingUnmatched == unmatchedExpr) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                  } else {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekle", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            }
          }
        }
      }

      val notFoodWarnings = generalWarnings.filter { it.startsWith("Şu kısım yiyecek olarak tanınmadı") }
      val otherWarnings = generalWarnings.filter { !it.startsWith("Şu kısım yiyecek olarak tanınmadı") }

      // 4. Yiyecek Olarak Tanınmayan Öğeler Uyarısı
      if (notFoodWarnings.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          notFoodWarnings.forEach { warnMsg ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFEF4444).copy(alpha = 0.12f),
              border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = Color(0xFFEF4444),
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = warnMsg,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                  lineHeight = 16.sp
                )
              }
            }
          }
        }
      }

      // Diğer Genel Uyarılar (Varsa)
      if (otherWarnings.isNotEmpty()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = bentoAmber.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, bentoAmber.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = bentoAmber, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Uyarılar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = bentoAmber)
            }
            otherWarnings.forEach { warn ->
              Text("• $warn", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      // 1. Food Items List (Her bulunan yiyecek ayrı bir satır)
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, item ->
          val isAmberBorder = item.guven == "dusuk" || item.isTutarsiz || item.isOlasilikDisi
          val itemBorder = if (isAmberBorder) {
            BorderStroke(1.2.dp, bentoAmber)
          } else {
            BorderStroke(1.dp, bentoCardBorder)
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = bentoSubBg,
            border = itemBorder,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              // Main Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Checkbox
                Checkbox(
                  checked = item.isSelected,
                  onCheckedChange = { checked ->
                    items = items.mapIndexed { i, old ->
                      if (i == index) old.copy(isSelected = checked) else old
                    }
                  },
                  colors = CheckboxDefaults.colors(
                    checkedColor = bentoGreen,
                    checkmarkColor = Color.Black
                  ),
                  modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Name & Badges
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      editingItemNameId = item.id
                      nameEditText = item.name
                    }
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = item.name,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = "Düzenle",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                      modifier = Modifier.size(12.dp)
                    )
                  }

                  // Rozetler: Tahmini, Varsayılan porsiyon, Elle Düzenlendi
                  Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    if (isAmberBorder) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = bentoAmber.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, bentoAmber)
                      ) {
                        Text(
                          text = "Tahmini",
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = bentoAmber,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                    }

                    if (item.varsayim) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = bentoBlue.copy(alpha = 0.15f)
                      ) {
                        Text(
                          text = "Varsayılan porsiyon",
                          fontSize = 9.sp,
                          color = bentoBlue,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                    }

                    if (item.isManuallyEdited) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = bentoPurple.copy(alpha = 0.2f)
                      ) {
                        Text(
                          text = "Elle Düzenlendi",
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Medium,
                          color = bentoPurple,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Kcal text
                Text(
                  text = "${item.calories} kcal",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (item.isSelected) bentoGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Expand Button
                IconButton(
                  onClick = {
                    items = items.mapIndexed { i, old ->
                      if (i == index) old.copy(isExpanded = !old.isExpanded) else old
                    }
                  },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = if (item.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Detay",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                }

                // Delete Button
                IconButton(
                  onClick = {
                    items = items.filterIndexed { i, _ -> i != index }
                  },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              // Secondary Row: Step Buttons (- sayı +) & Unit Selector
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 42.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                // Step Buttons
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, bentoCardBorder, RoundedCornerShape(8.dp))
                ) {
                  // Minus Button (0.5 step down, min 0.5)
                  IconButton(
                    onClick = {
                      if (item.amount > 0.5f) {
                        val newAmt = (item.amount - 0.5f)
                        val updated = recalculateItemForNewAmount(item, newAmt)
                        items = items.mapIndexed { i, old -> if (i == index) updated else old }
                      }
                    },
                    modifier = Modifier.size(30.dp),
                    enabled = item.amount > 0.5f
                  ) {
                    Icon(
                      imageVector = Icons.Default.Remove,
                      contentDescription = "Azalt",
                      modifier = Modifier.size(14.dp),
                      tint = if (item.amount > 0.5f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                  }

                  // Amount Display
                  val amtStr = if (item.amount % 1.0f == 0f) item.amount.toInt().toString() else String.format(Locale("tr", "TR"), "%.1f", item.amount)
                  Text(
                    text = amtStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp)
                  )

                  // Plus Button (0.5 step up)
                  IconButton(
                    onClick = {
                      val newAmt = (item.amount + 0.5f)
                      val updated = recalculateItemForNewAmount(item, newAmt)
                      items = items.mapIndexed { i, old -> if (i == index) updated else old }
                    },
                    modifier = Modifier.size(30.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = "Artır",
                      modifier = Modifier.size(14.dp),
                      tint = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }

                // Birim Seçici (Dropdown)
                var unitMenuExpanded by remember { mutableStateOf(false) }
                Box {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, bentoCardBorder),
                    modifier = Modifier.clickable { unitMenuExpanded = true }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Text(
                        text = item.unit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Birim Seç",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  DropdownMenu(
                    expanded = unitMenuExpanded,
                    onDismissRequest = { unitMenuExpanded = false }
                  ) {
                    PARSED_MEAL_AVAILABLE_UNITS.forEach { unitOption ->
                      DropdownMenuItem(
                        text = {
                          Text(
                            text = unitOption,
                            fontWeight = if (unitOption == item.unit) FontWeight.Bold else FontWeight.Normal,
                            color = if (unitOption == item.unit) bentoGreen else MaterialTheme.colorScheme.onSurface
                          )
                        },
                        onClick = {
                          unitMenuExpanded = false
                          val updated = recalculateItemForNewUnit(item, unitOption)
                          items = items.mapIndexed { i, old -> if (i == index) updated else old }
                        }
                      )
                    }
                  }
                }
              }

              // Expanded Macro & Kcal Editors (Satırı genişletince)
              AnimatedVisibility(visible = item.isExpanded) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(
                    text = "Besin Değerlerini Düzenle (Kcal veya Makrolar)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  // Editable Calories field (Manual change adapts macros proportionately)
                  var kcalInput by remember(item.calories) { mutableStateOf(item.calories.toString()) }
                  OutlinedTextField(
                    value = kcalInput,
                    onValueChange = { newVal ->
                      val digits = newVal.filter { it.isDigit() }
                      kcalInput = digits
                      val newKcal = digits.toIntOrNull()
                      if (newKcal != null && newKcal >= 0) {
                        val updated = recalculateItemForManualCalories(item, newKcal)
                        items = items.mapIndexed { i, old -> if (i == index) updated else old }
                      }
                    },
                    label = { Text("Kalori (kcal)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                  )

                  // Editable Macros (Protein, Karb, Yağ, Lif)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    var protInput by remember(item.protein) {
                      mutableStateOf(if (item.protein % 1f == 0f) item.protein.toInt().toString() else String.format(Locale.US, "%.1f", item.protein))
                    }
                    var carbInput by remember(item.carbs) {
                      mutableStateOf(if (item.carbs % 1f == 0f) item.carbs.toInt().toString() else String.format(Locale.US, "%.1f", item.carbs))
                    }
                    var fatInput by remember(item.fat) {
                      mutableStateOf(if (item.fat % 1f == 0f) item.fat.toInt().toString() else String.format(Locale.US, "%.1f", item.fat))
                    }
                    var fiberInput by remember(item.fiber) {
                      mutableStateOf(if (item.fiber % 1f == 0f) item.fiber.toInt().toString() else String.format(Locale.US, "%.1f", item.fiber))
                    }

                    OutlinedTextField(
                      value = protInput,
                      onValueChange = {
                        protInput = it
                        val f = it.replace(",", ".").toFloatOrNull()
                        if (f != null && f >= 0f) {
                          items = items.mapIndexed { i, old ->
                            if (i == index) old.copy(protein = f, isManuallyEdited = true) else old
                          }
                        }
                      },
                      label = { Text("Prot", fontSize = 10.sp) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      singleLine = true,
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                      value = carbInput,
                      onValueChange = {
                        carbInput = it
                        val f = it.replace(",", ".").toFloatOrNull()
                        if (f != null && f >= 0f) {
                          items = items.mapIndexed { i, old ->
                            if (i == index) old.copy(carbs = f, isManuallyEdited = true) else old
                          }
                        }
                      },
                      label = { Text("Karb", fontSize = 10.sp) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      singleLine = true,
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                      value = fatInput,
                      onValueChange = {
                        fatInput = it
                        val f = it.replace(",", ".").toFloatOrNull()
                        if (f != null && f >= 0f) {
                          items = items.mapIndexed { i, old ->
                            if (i == index) old.copy(fat = f, isManuallyEdited = true) else old
                          }
                        }
                      },
                      label = { Text("Yağ", fontSize = 10.sp) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      singleLine = true,
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                      value = fiberInput,
                      onValueChange = {
                        fiberInput = it
                        val f = it.replace(",", ".").toFloatOrNull()
                        if (f != null && f >= 0f) {
                          items = items.mapIndexed { i, old ->
                            if (i == index) old.copy(fiber = f, isManuallyEdited = true) else old
                          }
                        }
                      },
                      label = { Text("Lif", fontSize = 10.sp) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                      singleLine = true,
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(8.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 5. "+ Yiyecek ekle" Satırı
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = bentoSubBg,
        border = BorderStroke(1.dp, bentoGreen.copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            addQueryText = ""
            addErrorMessage = null
            manualMode = false
            showAddDialog = true
          }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = bentoGreen, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "+ Yiyecek Ekle",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = bentoGreen
          )
        }
      }

      // 5. Toplam Kcal ve Toplam Makrolar (Seçili satırlara göre anında güncellenir)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        MacroMiniBadge("Protein", "${totalProtein.toInt()}g", bentoBlue, Modifier.weight(1f))
        MacroMiniBadge("Karb", "${totalCarbs.toInt()}g", bentoAmber, Modifier.weight(1f))
        MacroMiniBadge("Yağ", "${totalFat.toInt()}g", bentoPink, Modifier.weight(1f))
        MacroMiniBadge("Lif", "${totalFiber.toInt()}g", bentoTeal, Modifier.weight(1f))
      }

      HorizontalDivider(color = bentoCardBorder)

      // 6. Öğün Türü Çipleri
      Text(
        text = "Öğün Türü:",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("Kahvaltı", "Öğle", "Akşam", "Ara Öğün").forEach { cat ->
          val isSelected = selectedMealCategory == cat
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) bentoGreen.copy(alpha = 0.2f) else bentoSubBg,
            border = BorderStroke(1.dp, if (isSelected) bentoGreen else bentoCardBorder),
            modifier = Modifier
              .weight(1f)
              .clickable { selectedMealCategory = cat }
          ) {
            Text(
              text = cat,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) bentoGreen else MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        }
      }

      // 6. "Günlüğe Kaydet" Butonu (Yalnızca seçili satırları kaydeder, seçili yoksa pasiftir)
      val hasSelection = selectedItems.isNotEmpty()
      Button(
        onClick = {
          if (hasSelection) {
            val scannedItems = selectedItems.map { it.toScannedFoodItem() }
            onSaveToJournal(scannedItems, selectedMealCategory)
          }
        },
        enabled = hasSelection,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = bentoGreen,
          disabledContainerColor = bentoGreen.copy(alpha = 0.3f),
          contentColor = Color.Black,
          disabledContentColor = Color.Black.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (hasSelection) "Seçili ${selectedItems.size} Yiyeceği Günlüğe Kaydet" else "En Az 1 Yiyecek Seçin",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }
    }
  }

  // Edit Food Name Dialog
  if (editingItemNameId != null) {
    Dialog(onDismissRequest = { editingItemNameId = null }) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = bentoCardBg,
        border = BorderStroke(1.dp, bentoCardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Yiyecek Adını Düzenle", fontSize = 15.sp, fontWeight = FontWeight.Bold)

          OutlinedTextField(
            value = nameEditText,
            onValueChange = { nameEditText = it },
            label = { Text("Yiyecek Adı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = { editingItemNameId = null }) {
              Text("İptal")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val newName = nameEditText.trim()
                if (newName.isNotBlank()) {
                  items = items.map { old ->
                    if (old.id == editingItemNameId) old.copy(name = newName) else old
                  }
                }
                editingItemNameId = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = bentoGreen)
            ) {
              Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  // Add Food Item Dialog (AI ile Hesapla veya Tamamen Elle Gir)
  if (showAddDialog) {
    Dialog(onDismissRequest = { showAddDialog = false }) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = bentoCardBg,
        border = BorderStroke(1.dp, bentoCardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Yiyecek Ekle", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              FilterChip(
                selected = !manualMode,
                onClick = { manualMode = false },
                label = { Text("AI ile", fontSize = 11.sp) },
                shape = RoundedCornerShape(8.dp)
              )
              FilterChip(
                selected = manualMode,
                onClick = { manualMode = true },
                label = { Text("Elle", fontSize = 11.sp) },
                shape = RoundedCornerShape(8.dp)
              )
            }
          }

          if (!manualMode) {
            // AI Mode
            Text(
              text = "Yiyecek ve miktarını yazın (örn: 1 elma, 1 kase yoğurt, 100g tavuk):",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
              value = addQueryText,
              onValueChange = {
                addQueryText = it
                addErrorMessage = null
              },
              placeholder = { Text("örn: 1 kase yoğurt veya 5 zeytin") },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              singleLine = true
            )

            if (addErrorMessage != null) {
              Text(addErrorMessage!!, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(onClick = { showAddDialog = false }) {
                Text("İptal")
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  val query = addQueryText.trim()
                  if (query.isBlank()) {
                    addErrorMessage = "Lütfen bir yiyecek adı girin."
                    return@Button
                  }
                  if (aiAssistantService != null) {
                    isCalculatingNewItem = true
                    coroutineScope.launch {
                      try {
                        val analysis = aiAssistantService.calculateFoodFromText(query)
                        if (analysis.ogeler.isNotEmpty()) {
                          val newItems = analysis.ogeler.map { it.toEditableParsedFoodItem() }
                          items = items + newItems
                          showAddDialog = false
                        } else {
                          addErrorMessage = "Besin hesaplanamadı, 'Elle' sekmesinden ekleyebilirsiniz."
                        }
                      } catch (e: Exception) {
                        addErrorMessage = "Hata oluştu. Lütfen elle ekleyin."
                      } finally {
                        isCalculatingNewItem = false
                      }
                    }
                  } else {
                    addErrorMessage = "AI servisine ulaşılamıyor."
                  }
                },
                enabled = !isCalculatingNewItem,
                colors = ButtonDefaults.buttonColors(containerColor = bentoGreen)
              ) {
                if (isCalculatingNewItem) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                  Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Hesapla ve Ekle", color = Color.Black, fontWeight = FontWeight.Bold)
              }
            }
          } else {
            // Manual Mode
            OutlinedTextField(
              value = manualName,
              onValueChange = { manualName = it },
              label = { Text("Yiyecek Adı *") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = manualAmountText,
                onValueChange = { manualAmountText = it },
                label = { Text("Miktar") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
              )

              var unitExpanded by remember { mutableStateOf(false) }
              Box(modifier = Modifier.weight(1.2f)) {
                OutlinedTextField(
                  value = manualUnit,
                  onValueChange = {},
                  readOnly = true,
                  label = { Text("Birim") },
                  trailingIcon = {
                    IconButton(onClick = { unitExpanded = true }) {
                      Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { unitExpanded = true },
                  shape = RoundedCornerShape(10.dp)
                )

                DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                  PARSED_MEAL_AVAILABLE_UNITS.forEach { u ->
                    DropdownMenuItem(
                      text = { Text(u) },
                      onClick = {
                        manualUnit = u
                        unitExpanded = false
                      }
                    )
                  }
                }
              }
            }

            OutlinedTextField(
              value = manualCaloriesText,
              onValueChange = { manualCaloriesText = it.filter { c -> c.isDigit() } },
              label = { Text("Kalori (kcal) *") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = manualProteinText,
                onValueChange = { manualProteinText = it },
                label = { Text("Prot (g)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              )
              OutlinedTextField(
                value = manualCarbsText,
                onValueChange = { manualCarbsText = it },
                label = { Text("Karb (g)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              )
              OutlinedTextField(
                value = manualFatText,
                onValueChange = { manualFatText = it },
                label = { Text("Yağ (g)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              )
              OutlinedTextField(
                value = manualFiberText,
                onValueChange = { manualFiberText = it },
                label = { Text("Lif (g)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(onClick = { showAddDialog = false }) {
                Text("İptal")
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  val name = manualName.trim()
                  val cal = manualCaloriesText.toIntOrNull()
                  if (name.isBlank() || cal == null) {
                    return@Button
                  }
                  val amt = manualAmountText.replace(",", ".").toFloatOrNull() ?: 1f
                  val prot = manualProteinText.replace(",", ".").toFloatOrNull() ?: 0f
                  val carb = manualCarbsText.replace(",", ".").toFloatOrNull() ?: 0f
                  val fat = manualFatText.replace(",", ".").toFloatOrNull() ?: 0f
                  val fib = manualFiberText.replace(",", ".").toFloatOrNull() ?: 0f

                  val baseCal = cal / amt
                  val baseProt = prot / amt
                  val baseCarb = carb / amt
                  val baseFat = fat / amt
                  val baseFib = fib / amt

                  val newItem = EditableParsedFoodItem(
                    name = name,
                    amount = amt,
                    unit = manualUnit,
                    calories = cal,
                    protein = prot,
                    carbs = carb,
                    fat = fat,
                    fiber = fib,
                    baseCaloriesPerUnit = baseCal,
                    baseProteinPerUnit = baseProt,
                    baseCarbsPerUnit = baseCarb,
                    baseFatPerUnit = baseFat,
                    baseFiberPerUnit = baseFib,
                    isManuallyEdited = true
                  )
                  items = items + newItem
                  showAddDialog = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = bentoGreen)
              ) {
                Text("Ekle", color = Color.Black, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Miktar değiştiğinde kcal ve makroları orantılı olarak yeniden hesaplar.
 */
private fun recalculateItemForNewAmount(item: EditableParsedFoodItem, newAmount: Float): EditableParsedFoodItem {
  val safeAmt = maxOf(0.1f, newAmount)
  return if (item.isManuallyEdited) {
    val ratio = safeAmt / maxOf(0.1f, item.amount)
    item.copy(
      amount = safeAmt,
      calories = maxOf(0, (item.calories * ratio).roundToInt()),
      protein = maxOf(0f, item.protein * ratio),
      carbs = maxOf(0f, item.carbs * ratio),
      fat = maxOf(0f, item.fat * ratio),
      fiber = maxOf(0f, item.fiber * ratio)
    )
  } else {
    item.copy(
      amount = safeAmt,
      calories = maxOf(0, (item.baseCaloriesPerUnit * safeAmt).roundToInt()),
      protein = maxOf(0f, item.baseProteinPerUnit * safeAmt),
      carbs = maxOf(0f, item.baseCarbsPerUnit * safeAmt),
      fat = maxOf(0f, item.baseFatPerUnit * safeAmt),
      fiber = maxOf(0f, item.baseFiberPerUnit * safeAmt)
    )
  }
}

/**
 * Birim değiştiğinde birim dönüşümü (gr <-> kg, ml <-> litre gibi) veya doğrudan birim güncellemesi yapar.
 */
private fun recalculateItemForNewUnit(item: EditableParsedFoodItem, newUnit: String): EditableParsedFoodItem {
  val oldUnit = item.unit.lowercase(Locale("tr", "TR")).trim()
  val target = newUnit.lowercase(Locale("tr", "TR")).trim()

  var updatedAmount = item.amount
  if (oldUnit == "gr" && target == "kg") {
    updatedAmount = item.amount / 1000f
  } else if (oldUnit == "kg" && target == "gr") {
    updatedAmount = item.amount * 1000f
  } else if (oldUnit == "ml" && target == "litre") {
    updatedAmount = item.amount / 1000f
  } else if (oldUnit == "litre" && target == "ml") {
    updatedAmount = item.amount * 1000f
  }

  return item.copy(
    unit = newUnit,
    amount = updatedAmount
  )
}

/**
 * Kalori elle değiştirildiğinde makroları orantılı olarak uyarlar ve "elle düzenlendi" işaretler.
 */
private fun recalculateItemForManualCalories(item: EditableParsedFoodItem, newCalories: Int): EditableParsedFoodItem {
  val oldCal = maxOf(1, item.calories)
  val ratio = newCalories.toFloat() / oldCal.toFloat()

  return item.copy(
    calories = newCalories,
    protein = item.protein * ratio,
    carbs = item.carbs * ratio,
    fat = item.fat * ratio,
    fiber = item.fiber * ratio,
    isManuallyEdited = true,
    isTutarsiz = false
  )
}

@Composable
private fun MacroMiniBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = color.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
  }
}
