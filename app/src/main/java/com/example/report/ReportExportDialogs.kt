package com.example.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppTheme

@Composable
fun ExportProgressDialog(
  isPdf: Boolean,
  onDismiss: () -> Unit = {}
) {
  val bentoGreen = AppTheme.colors.accent
  val cardBg = AppTheme.colors.surface

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = cardBg,
      border = BorderStroke(1.dp, AppTheme.colors.border),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("export_progress_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        CircularProgressIndicator(
          color = bentoGreen,
          strokeWidth = 3.5.dp,
          modifier = Modifier.size(48.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = if (isPdf) "PDF Hazırlanıyor..." else "JPG Hazırlanıyor...",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "İndirilenler klasörüne kaydediliyor, lütfen bekleyin.",
            fontSize = 12.sp,
            color = AppTheme.colors.textSecondary
          )
        }
      }
    }
  }
}

@Composable
fun ExportSuccessDialog(
  result: ReportExportHelper.ExportResult,
  onOpen: () -> Unit,
  onShare: () -> Unit,
  onDismiss: () -> Unit
) {
  val bentoGreen = AppTheme.colors.accent
  val cardBg = AppTheme.colors.surface
  val itemBg = AppTheme.colors.itemBg
  val borderColor = AppTheme.colors.border

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("export_success_dialog"),
    shape = RoundedCornerShape(24.dp),
    containerColor = cardBg,
    icon = {
      Surface(
        shape = CircleShape,
        color = bentoGreen.copy(alpha = 0.16f),
        modifier = Modifier.size(54.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = bentoGreen,
            modifier = Modifier.size(32.dp)
          )
        }
      }
    },
    title = {
      Text(
        text = "Rapor İndirildi!",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Dosya İndirilenler klasörünüze başarıyla kaydedildi.",
          fontSize = 13.5.sp,
          color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = itemBg,
          border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "📁 ${result.fileName}",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = bentoGreen,
            modifier = Modifier.padding(10.dp)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onOpen,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = bentoGreen,
          contentColor = Color.Black
        ),
        modifier = Modifier.testTag("export_success_open_button")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Aç",
            fontWeight = FontWeight.Bold
          )
        }
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onShare,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, bentoGreen),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = bentoGreen),
          modifier = Modifier.testTag("export_success_share_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Paylaş")
          }
        }

        TextButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Kapat", color = AppTheme.colors.textSecondary)
        }
      }
    }
  )
}

@Composable
fun ExportErrorDialog(
  errorMessage: String,
  onDismiss: () -> Unit
) {
  val cardBg = AppTheme.colors.surface
  val errorColor = Color(0xFFFB7185)

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("export_error_dialog"),
    shape = RoundedCornerShape(24.dp),
    containerColor = cardBg,
    icon = {
      Surface(
        shape = CircleShape,
        color = errorColor.copy(alpha = 0.16f),
        modifier = Modifier.size(54.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = errorColor,
            modifier = Modifier.size(32.dp)
          )
        }
      }
    },
    title = {
      Text(
        text = "İndirme Başarısız",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      Text(
        text = errorMessage,
        fontSize = 13.5.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = errorColor,
          contentColor = Color.White
        )
      ) {
        Text("Tamam", fontWeight = FontWeight.Bold)
      }
    }
  )
}
