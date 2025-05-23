package com.example.contactsapp

data class Contact(
    val id: Long,
    val name: String,
    val phones: List<String>,
    val photoUri: String? = null
)