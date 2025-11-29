package com.example.contactsapp.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.contactsapp.data.model.ReminderType

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        type: ReminderType,
        daysFromNow: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ReminderType.CALL_BACK) }
    var daysFromNow by remember { mutableStateOf(7) }
    
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
                    text = "Добавить напоминание",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                
                // Type selection
                Text(
                    text = "Тип напоминания",
                    style = MaterialTheme.typography.labelMedium
                )
                
                ReminderType.values().forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                
                // Days selection
                Text(
                    text = "Через сколько дней",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Slider(
                    value = daysFromNow.toFloat(),
                    onValueChange = { daysFromNow = it.toInt() },
                    valueRange = 1f..30f,
                    steps = 28
                )
                
                Text("$daysFromNow дней")
                
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
                            if (title.isNotEmpty()) {
                                onConfirm(title, description, selectedType, daysFromNow)
                                onDismiss()
                            }
                        }
                    ) {
                        Text("Создать")
                    }
                }
            }
        }
    }
}

