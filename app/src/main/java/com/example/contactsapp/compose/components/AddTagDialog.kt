package com.example.contactsapp.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AddTagDialog(
    availableTags: List<String> = listOf("Друзья", "Работа", "Семья", "Коллеги", "Важные"),
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit
) {
    var selectedTags by remember { mutableStateOf(emptySet<String>()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Добавить теги",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Text(
                    text = "Выберите теги или создайте новый",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Существующие теги
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableTags) { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilterChip(
                                selected = selectedTags.contains(tag),
                                onClick = { 
                                    selectedTags = if (selectedTags.contains(tag)) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                Text(
                    text = "Добавлено тегов: ${selectedTags.size}",
                    style = MaterialTheme.typography.bodySmall
                )
                
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
                            selectedTags.forEach { tag ->
                                onAddTag(tag)
                            }
                            onDismiss()
                        }
                    ) {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}

