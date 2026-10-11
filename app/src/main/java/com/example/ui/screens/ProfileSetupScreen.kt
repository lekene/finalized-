package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
  initialName: String = "Kullanıcı",
  initialGender: String = "Erkek",
  initialAge: Int = 28,
  initialHeight: Float = 175f,
  initialCurrentWeight: Float = 80f,
  initialTargetWeight: Float = 70f,
  initialActivityLevel: String = "hafif_aktif",
  onSaveProfile: (String, String, Int, Float, Float, Float, String) -> Unit
) {
  var name by remember { mutableStateOf(initialName) }
  var gender by remember { mutableStateOf(initialGender) }
  var ageText by remember { mutableStateOf(initialAge.toString()) }
  var heightText by remember { mutableStateOf(initialHeight.toString()) }
  var currentWeightText by remember { mutableStateOf(initialCurrentWeight.toString()) }
  var targetWeightText by remember { mutableStateOf(initialTargetWeight.toString()) }
  var activityLevel by remember { mutableStateOf(initialActivityLevel) }
  var expandedActivity by remember { mutableStateOf(false) }

  val activityOptions = mapOf(
    "hareketsiz" to "Hareketsiz (Az veya hiç egzersiz)",
    "hafif_aktif" to "Hafif aktif (Haftada 1-3 gün egzersiz)",
    "orta_aktif" to "Orta aktif (Haftada 3-5 gün egzersiz)",
    "cok_aktif" to "Çok aktif (Haftada 6-7 gün ağır egzersiz)"
  )

  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 500.dp)
        .fillMaxHeight(0.92f),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Profil ve Hedef Belirleme",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Size özel kalori ve makro planını oluşturalım",
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Adınız") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gender Selection
        Text(
          text = "Cinsiyet",
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          listOf("Erkek", "Kadın").forEach { g ->
            val selected = gender == g
            Button(
              onClick = { gender = g },
              modifier = Modifier.weight(1f).height(48.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) GreenPrimary else MaterialTheme.colorScheme.surfaceVariant
              ),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = g,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = ageText,
          onValueChange = { ageText = it },
          label = { Text("Yaş") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = heightText,
          onValueChange = { heightText = it },
          label = { Text("Boy (cm)") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = currentWeightText,
          onValueChange = { currentWeightText = it },
          label = { Text("Mevcut Kilo (kg)") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = targetWeightText,
          onValueChange = { targetWeightText = it },
          label = { Text("Hedef Kilo (kg)") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Activity level dropdown
        ExposedDropdownMenuBox(
          expanded = expandedActivity,
          onExpandedChange = { expandedActivity = !expandedActivity },
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = activityOptions[activityLevel] ?: "Seçiniz",
            onValueChange = {},
            readOnly = true,
            label = { Text("Aktivite Seviyesi") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedActivity) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
          )

          ExposedDropdownMenu(
            expanded = expandedActivity,
            onDismissRequest = { expandedActivity = false }
          ) {
            activityOptions.forEach { (key, label) ->
              DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                  activityLevel = key
                  expandedActivity = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
          onClick = {
            val age = ageText.toIntOrNull() ?: 28
            val height = heightText.toFloatOrNull() ?: 175f
            val currentWeight = currentWeightText.toFloatOrNull() ?: 80f
            val targetWeight = targetWeightText.toFloatOrNull() ?: 70f
            onSaveProfile(name, gender, age, height, currentWeight, targetWeight, activityLevel)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(
            text = "Profili Kaydet ve Hesapla",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }
}
