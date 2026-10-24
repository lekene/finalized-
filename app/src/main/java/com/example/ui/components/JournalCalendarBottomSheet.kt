package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.text.SimpleDateFormat
import java.util.*

private val CardBg = Color(0xFF151921)
private val CardBorderColor = Color(0xFF262C3A)
private val SubLabelColor = Color(0xFF94A3B8)
private val BentoGreen = Color(0xFF22C55E)
private val DarkEmptyCellBg = Color(0xFF1E232E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalCalendarBottomSheet(
  selectedDate: String, // "yyyy-MM-dd"
  todayDate: String, // "yyyy-MM-dd"
  recordedDates: Set<String>, // Dates that contain meals, water, weight, etc.
  onSelectDate: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Parse today and selected date to calendars
  val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
  val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("tr", "TR")) }

  val todayCal = remember(todayDate) {
    Calendar.getInstance().apply {
      try {
        time = isoFormat.parse(todayDate) ?: Date()
      } catch (_: Exception) {}
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
  }

  // Active month being viewed in calendar (defaults to selectedDate's month)
  var displayedYear by remember {
    mutableIntStateOf(
      try {
        val cal = Calendar.getInstance()
        cal.time = isoFormat.parse(selectedDate) ?: Date()
        cal.get(Calendar.YEAR)
      } catch (_: Exception) {
        Calendar.getInstance().get(Calendar.YEAR)
      }
    )
  }

  var displayedMonth by remember {
    mutableIntStateOf(
      try {
        val cal = Calendar.getInstance()
        cal.time = isoFormat.parse(selectedDate) ?: Date()
        cal.get(Calendar.MONTH) // 0-based
      } catch (_: Exception) {
        Calendar.getInstance().get(Calendar.MONTH)
      }
    )
  }

  // 3-year limit calculation (36 months before today)
  val minYear = todayCal.get(Calendar.YEAR) - 3
  val minMonth = todayCal.get(Calendar.MONTH)

  val maxYear = todayCal.get(Calendar.YEAR)
  val maxMonth = todayCal.get(Calendar.MONTH)

  val canGoBack = (displayedYear > minYear) || (displayedYear == minYear && displayedMonth > minMonth)
  val canGoForward = (displayedYear < maxYear) || (displayedYear == maxYear && displayedMonth < maxMonth)

  // Title string for month & year (e.g. "Ekim 2026")
  val displayedMonthTitle = remember(displayedYear, displayedMonth) {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, displayedYear)
      set(Calendar.MONTH, displayedMonth)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    val raw = monthYearFormat.format(cal.time)
    raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() }
  }

  // Calculate days in month and starting offset (Monday-first)
  val (firstDayOffset, daysInMonth) = remember(displayedYear, displayedMonth) {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, displayedYear)
      set(Calendar.MONTH, displayedMonth)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    // Calendar.DAY_OF_WEEK: Sunday = 1, Monday = 2, ..., Saturday = 7
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // Map to Monday-first: Monday -> 0, Tuesday -> 1, ..., Sunday -> 6
    val offset = (dayOfWeek + 5) % 7
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    Pair(offset, maxDays)
  }

  val dayHeaders = listOf(
    stringResource(R.string.day_mon),
    stringResource(R.string.day_tue),
    stringResource(R.string.day_wed),
    stringResource(R.string.day_thu),
    stringResource(R.string.day_fri),
    stringResource(R.string.day_sat),
    stringResource(R.string.day_sun)
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor = CardBg,
    dragHandle = {
      BottomSheetDefaults.DragHandle(color = SubLabelColor.copy(alpha = 0.4f))
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. ÜST BAŞLIK & AY DEĞİŞTİRME OKLARI
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Önceki Ay Oku
        IconButton(
          onClick = {
            if (canGoBack) {
              if (displayedMonth == 0) {
                displayedMonth = 11
                displayedYear -= 1
              } else {
                displayedMonth -= 1
              }
            }
          },
          enabled = canGoBack,
          modifier = Modifier
            .size(40.dp)
            .testTag("calendar_prev_month_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.calendar_prev_month),
            tint = if (canGoBack) BentoGreen else SubLabelColor.copy(alpha = 0.3f)
          )
        }

        // Ay ve Yıl Başlığı (ortalanmış)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.testTag("calendar_month_title")
        ) {
          Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = BentoGreen,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = displayedMonthTitle,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        // Sonraki Ay Oku
        IconButton(
          onClick = {
            if (canGoForward) {
              if (displayedMonth == 11) {
                displayedMonth = 0
                displayedYear += 1
              } else {
                displayedMonth += 1
              }
            }
          },
          enabled = canGoForward,
          modifier = Modifier
            .size(40.dp)
            .testTag("calendar_next_month_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.calendar_next_month),
            tint = if (canGoForward) BentoGreen else SubLabelColor.copy(alpha = 0.3f)
          )
        }
      }

      // 2. HAFTANIN GÜNLERİ BAŞLIKLARI (Pzt - Paz)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        dayHeaders.forEach { header ->
          Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = header,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = SubLabelColor
            )
          }
        }
      }

      // 3. AYLIK KARE IZGARA (Pazartesi ile başlayan)
      // Toplam hücre sayısı = firstDayOffset + daysInMonth
      val totalSlots = firstDayOffset + daysInMonth
      val numRows = (totalSlots + 6) / 7

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        for (rowIndex in 0 until numRows) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            for (colIndex in 0 until 7) {
              val slotIndex = rowIndex * 7 + colIndex
              if (slotIndex in firstDayOffset until totalSlots) {
                val dayNum = slotIndex - firstDayOffset + 1
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", displayedYear, displayedMonth + 1, dayNum)

                val isToday = dateStr == todayDate
                val isSelected = dateStr == selectedDate
                val isFuture = dateStr > todayDate
                val hasData = recordedDates.contains(dateStr)

                // Renk mantığı (Kural 2):
                // - Veri varsa: vurgu renginde dolu kare
                // - Veri yoksa: koyu gri boş kare
                // - Bugün: ince vurgu çerçeveli
                // - Seçili gün: beyaz çerçeveli
                // - Gelecek günler: soluk ve seçilemez
                val cellBg = when {
                  isFuture -> DarkEmptyCellBg.copy(alpha = 0.4f)
                  hasData -> BentoGreen
                  else -> DarkEmptyCellBg
                }

                val borderStroke: BorderStroke? = when {
                  isFuture -> null
                  isSelected -> BorderStroke(2.dp, Color.White)
                  isToday -> BorderStroke(1.5.dp, BentoGreen)
                  else -> null
                }

                val textColor = when {
                  isFuture -> SubLabelColor.copy(alpha = 0.35f)
                  hasData -> Color.White
                  isSelected -> Color.White
                  isToday -> BentoGreen
                  else -> SubLabelColor
                }

                Box(
                  modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cellBg)
                    .then(
                      if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(8.dp))
                      else Modifier
                    )
                    .then(
                      if (!isFuture) {
                        Modifier.clickable {
                          onSelectDate(dateStr)
                          onDismiss()
                        }
                      } else {
                        Modifier
                      }
                    )
                    .testTag("calendar_day_$dateStr"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "$dayNum",
                    fontSize = 13.sp,
                    fontWeight = if (hasData || isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                  )
                }
              } else {
                // Ay dışındaki boş hücre
                Spacer(
                  modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                )
              }
            }
          }
        }
      }

      HorizontalDivider(color = CardBorderColor, modifier = Modifier.padding(vertical = 4.dp))

      // 4. AÇIKLAMA (LEJANT): Veri var / Veri yok / Bugün
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Veri var
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(12.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(BentoGreen)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.legend_has_data),
            fontSize = 11.sp,
            color = SubLabelColor
          )
        }

        // Veri yok
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(12.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(DarkEmptyCellBg)
              .border(0.5.dp, CardBorderColor, RoundedCornerShape(3.dp))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.legend_no_data),
            fontSize = 11.sp,
            color = SubLabelColor
          )
        }

        // Bugün
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(12.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(DarkEmptyCellBg)
              .border(1.5.dp, BentoGreen, RoundedCornerShape(3.dp))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.legend_today),
            fontSize = 11.sp,
            color = SubLabelColor
          )
        }

        // Seçili
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(12.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(DarkEmptyCellBg)
              .border(1.5.dp, Color.White, RoundedCornerShape(3.dp))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.legend_selected),
            fontSize = 11.sp,
            color = SubLabelColor
          )
        }
      }
    }
  }
}
