package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WeightLog
import com.example.ui.theme.GreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWeightBottomSheet(
  initialDate: String,
  weightLogToEdit: WeightLog? = null,
  currentWeightPlaceholder: Float = 75f,
  onDismiss: () -> Unit,
  onSaveWeight: (weight: Float, date: String, note: String, id: Long) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var weightText by remember {
    mutableStateOf(
      weightLogToEdit?.weight?.toString() ?: ""
    )
  }
  var dateText by remember {
    mutableStateOf(
      weightLogToEdit?.date ?: initialDate
    )
  }
  var noteText by remember {
    mutableStateOf(
      weightLogToEdit?.note ?: ""
    )
  }

  var errorMessage by remember { mutableStateOf<String?>(null) }
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
        .padding(horizontal = 24.dp)
        .padding(bottom = 36.dp)
        .verticalScroll(scrollState)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.MonitorWeight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          val context = androidx.compose.ui.platform.LocalContext.current
          Text(
            text = if (weightLogToEdit == null) androidx.compose.ui.res.stringResource(com.example.R.string.add_weight) else androidx.compose.ui.res.stringResource(com.example.R.string.edit_weight),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.close),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Weight Input
      OutlinedTextField(
        value = weightText,
        onValueChange = {
          weightText = it
          errorMessage = null
        },
        label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.weight_kg_required)) },
        placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.weight_placeholder)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        isError = errorMessage != null
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Date Input
      OutlinedTextField(
        value = dateText,
        onValueChange = { dateText = it },
        label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.date_label)) },
        placeholder = { Text("yyyy-MM-dd") },
        trailingIcon = {
          IconButton(onClick = {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dateText = today
          }) {
            Icon(
              imageVector = Icons.Default.CalendarToday,
              contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.today),
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Note Input
      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.note_optional)) },
        placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.note_weight_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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

      val context = androidx.compose.ui.platform.LocalContext.current
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
            val parsedWeight = weightText.replace(",", ".").toFloatOrNull()
            if (parsedWeight == null || parsedWeight <= 20f || parsedWeight >= 300f) {
              errorMessage = context.getString(com.example.R.string.error_invalid_weight)
              return@Button
            }

            val validDate = if (dateText.isBlank()) initialDate else dateText.trim()
            onSaveWeight(
              parsedWeight,
              validDate,
              noteText.trim(),
              weightLogToEdit?.id ?: 0L
            )
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
