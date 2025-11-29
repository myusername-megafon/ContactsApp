package com.example.contactsapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_tags")
data class ContactTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val tagName: String
)

