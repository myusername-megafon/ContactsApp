package com.example.contactsapp.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.contactsapp.data.model.ExtendedContact

@Composable
fun EditContactDialog(
    contact: ExtendedContact?,
    onDismiss: () -> Unit,
    onSave: (
        biography: String,
        notes: String,
        socialNetworks: Map<String, String>
    ) -> Unit
) {
    var biography by remember { mutableStateOf(contact?.biography ?: "") }
    var notes by remember { mutableStateOf(contact?.notes ?: "") }
    var socialNetworkName by remember { mutableStateOf("") }
    var socialNetworkUrl by remember { mutableStateOf("") }
    var socialNetworks by remember { 
        mutableStateOf(
            contact?.let { com.google.gson.Gson().fromJson(it.socialNetworks, Map::class.java) as? Map<String, String> } ?: emptyMap<String, String>()
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Редактировать контакт",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                OutlinedTextField(
                    value = biography,
                    onValueChange = { biography = it },
                    label = { Text("Биография") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                
                HorizontalDivider()
                
                Text(
                    text = "Социальные сети",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Список соцсетей
                socialNetworks.forEach { (platform, url) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(platform, style = MaterialTheme.typography.bodyMedium)
                            Text(url, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            socialNetworks = socialNetworks - platform
                        }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Delete, null)
                        }
                    }
                }
                
                // Добавление новой соцсети
                OutlinedTextField(
                    value = socialNetworkName,
                    onValueChange = { socialNetworkName = it },
                    label = { Text("Платформа (Telegram, Instagram и т.д.)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = socialNetworkUrl,
                    onValueChange = { socialNetworkUrl = it },
                    label = { Text("URL или username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Button(
                    onClick = {
                        if (socialNetworkName.isNotEmpty() && socialNetworkUrl.isNotEmpty()) {
                            socialNetworks = socialNetworks + (socialNetworkName to socialNetworkUrl)
                            socialNetworkName = ""
                            socialNetworkUrl = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Добавить соцсеть")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(biography, notes, socialNetworks)
                            onDismiss()
                        }
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

