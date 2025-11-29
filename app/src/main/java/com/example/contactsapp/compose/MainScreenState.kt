package com.example.contactsapp.compose

import com.example.contactsapp.Contact
import com.example.contactsapp.data.model.ExtendedContact
import com.example.contactsapp.data.model.Reminder

data class MainScreenState(
    val isLoading: Boolean = false,
    val permissionGranted: Boolean = false,
    val contactsList: List<Contact> = emptyList(),
    val extendedContacts: List<ExtendedContact> = emptyList(),
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val showTagFilter: Boolean = false,
    val navigationState: NavigationState = NavigationState.ContactsList,
    val selectedContactId: Long? = null
)

sealed class NavigationState {
    object ContactsList : NavigationState()
    object ContactDetails : NavigationState()
}
