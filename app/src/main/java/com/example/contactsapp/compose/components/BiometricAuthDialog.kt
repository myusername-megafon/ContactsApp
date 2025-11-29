package com.example.contactsapp.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BiometricAuthDialog(
    onAuthSuccess: () -> Unit,
    onAuthFailure: () -> Unit,
    contactName: String
) {
    var showDialog by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        // Простая имитация биометрии - показываем сообщение о необходимости биометрии
        // В реальном приложении здесь был бы вызов системного API
        kotlinx.coroutines.delay(1000)
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                onAuthFailure()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Защищенный контакт",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Для доступа к контакту \"$contactName\" требуется подтверждение.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Используйте отпечаток пальца или PIN-код для доступа.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onAuthSuccess()
                    }
                ) {
                    Text("Разблокировать")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onAuthFailure()
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

