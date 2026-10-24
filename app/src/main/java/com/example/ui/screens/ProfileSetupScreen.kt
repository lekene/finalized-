package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.avatar.AvatarPickerBottomSheet
import com.example.ui.avatar.UserAvatarView
import com.example.ui.theme.AppTheme
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
  initialAvatarUri: String? = null,
  profileId: Int = 1,
  title: String = androidx.compose.ui.res.stringResource(com.example.R.string.profile_setup_title),
  subtitle: String = androidx.compose.ui.res.stringResource(com.example.R.string.profile_setup_subtitle),
  saveButtonText: String = androidx.compose.ui.res.stringResource(com.example.R.string.save_profile),
  onCancel: (() -> Unit)? = null,
  onSaveProfile: (String, String, Int, Float, Float, Float, String, String?) -> Unit
) {
  var name by remember { mutableStateOf(initialName) }
  var gender by remember { mutableStateOf(initialGender) }
  var ageText by remember { mutableStateOf(initialAge.toString()) }
  var heightText by remember { mutableStateOf(initialHeight.toString()) }
  var currentWeightText by remember { mutableStateOf(initialCurrentWeight.toString()) }
  var targetWeightText by remember { mutableStateOf(initialTargetWeight.toString()) }
  var activityLevel by remember { mutableStateOf(initialActivityLevel) }
  var avatarUri by remember { mutableStateOf(initialAvatarUri) }
  var showAvatarPickerSheet by remember { mutableStateOf(false) }
  var expandedActivity by remember { mutableStateOf(false) }

  val sedentaryText = androidx.compose.ui.res.stringResource(com.example.R.string.activity_sedentary)
  val lightText = androidx.compose.ui.res.stringResource(com.example.R.string.activity_light)
  val moderateText = androidx.compose.ui.res.stringResource(com.example.R.string.activity_moderate)
  val veryText = androidx.compose.ui.res.stringResource(com.example.R.string.activity_very)

  val activityOptions = remember(sedentaryText, lightText, moderateText, veryText) {
    mapOf(
      "hareketsiz" to sedentaryText,
      "hafif_aktif" to lightText,
      "orta_aktif" to moderateText,
      "cok_aktif" to veryText
    )
  }

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
        .fillMaxHeight(0.94f),
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
        if (onCancel != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            IconButton(onClick = onCancel) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.close),
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        Text(
          text = title,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = subtitle,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Yuvarlak avatar ve "Avatarı değiştir" butonu
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(vertical = 4.dp)
        ) {
          Box(contentAlignment = Alignment.BottomEnd) {
            UserAvatarView(
              avatarUri = avatarUri,
              name = name.ifBlank { "Profil" },
              size = 96.dp,
              border = BorderStroke(2.5.dp, AppTheme.colors.accent),
              onClick = { showAvatarPickerSheet = true },
              testTag = "profile_setup_avatar_preview"
            )
            Surface(
              shape = CircleShape,
              color = AppTheme.colors.accent,
              modifier = Modifier
                .size(30.dp)
                .offset(x = 2.dp, y = 2.dp)
                .clickable { showAvatarPickerSheet = true }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Avatarı Değiştir",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          TextButton(
            onClick = { showAvatarPickerSheet = true },
            modifier = Modifier.testTag("change_avatar_button")
          ) {
            Icon(
              imageVector = Icons.Default.PhotoCamera,
              contentDescription = null,
              tint = AppTheme.colors.accent,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Avatarı değiştir",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = AppTheme.colors.accent
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.profile_name_label)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gender Selection
        Text(
          text = androidx.compose.ui.res.stringResource(com.example.R.string.gender_label),
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          FilterChip(
            selected = gender == "Erkek",
            onClick = { gender = "Erkek" },
            label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.male)) },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
              selectedLabelColor = MaterialTheme.colorScheme.primary
            )
          )
          FilterChip(
            selected = gender == "Kadın",
            onClick = { gender = "Kadın" },
            label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.female)) },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
              selectedLabelColor = MaterialTheme.colorScheme.primary
            )
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Age & Height
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = ageText,
            onValueChange = { if (it.all { c -> c.isDigit() }) ageText = it },
            label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.age_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )

          OutlinedTextField(
            value = heightText,
            onValueChange = { heightText = it },
            label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.height_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weight & Target Weight
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = currentWeightText,
            onValueChange = { currentWeightText = it },
            label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.current_weight_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )

          OutlinedTextField(
            value = targetWeightText,
            onValueChange = { targetWeightText = it },
            label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.target_weight_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activity level dropdown
        ExposedDropdownMenuBox(
          expanded = expandedActivity,
          onExpandedChange = { expandedActivity = !expandedActivity },
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = activityOptions[activityLevel] ?: androidx.compose.ui.res.stringResource(com.example.R.string.select_option),
            onValueChange = {},
            readOnly = true,
            label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.activity_level_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedActivity) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
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

        Spacer(modifier = Modifier.height(28.dp))

        Button(
          onClick = {
            val age = ageText.toIntOrNull() ?: 28
            val height = heightText.toFloatOrNull() ?: 175f
            val currentWeight = currentWeightText.toFloatOrNull() ?: 80f
            val targetWeight = targetWeightText.toFloatOrNull() ?: 70f
            onSaveProfile(name.ifBlank { "Yeni Profil" }, gender, age, height, currentWeight, targetWeight, activityLevel, avatarUri)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(
            text = saveButtonText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }

    if (showAvatarPickerSheet) {
      AvatarPickerBottomSheet(
        currentAvatarUri = avatarUri,
        gender = gender,
        profileId = profileId,
        onAvatarSelected = { selectedUri ->
          avatarUri = selectedUri
        },
        onDismiss = { showAvatarPickerSheet = false }
      )
    }
  }
}
