package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenPrimary

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
  var isLoginTab by remember { mutableStateOf(true) }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 440.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (isLoginTab) androidx.compose.ui.res.stringResource(com.example.R.string.auth_title_login) else androidx.compose.ui.res.stringResource(com.example.R.string.auth_title_register),
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = androidx.compose.ui.res.stringResource(com.example.R.string.auth_subtitle),
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(4.dp)
        ) {
          TabButton(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.auth_tab_login),
            selected = isLoginTab,
            modifier = Modifier.weight(1f)
          ) { isLoginTab = true }

          TabButton(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.auth_tab_register),
            selected = !isLoginTab,
            modifier = Modifier.weight(1f)
          ) { isLoginTab = false }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isLoginTab) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.auth_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.auth_email_label)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.auth_password_label)) },
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onAuthenticated,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(
            text = if (isLoginTab) androidx.compose.ui.res.stringResource(com.example.R.string.auth_title_login) else androidx.compose.ui.res.stringResource(com.example.R.string.auth_register_and_continue),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
          )
        }
      }
    }
  }
}

@Composable
fun TabButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
  val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
  val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
    onClick = onClick,
    modifier = modifier.height(40.dp),
    shape = RoundedCornerShape(10.dp),
    color = backgroundColor
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = text,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = textColor,
        fontSize = 14.sp
      )
    }
  }
}
