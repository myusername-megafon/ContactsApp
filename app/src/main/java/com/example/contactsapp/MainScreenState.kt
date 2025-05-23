package com.example.contactsapp

data class MainScreenState(
    val isLoading: Boolean = false,
    val permissionGranted: Boolean = false,
    val contactsList: List<Contact> = emptyList(),
    val cleanupStatus: CleanedStatusModel? = null
)
