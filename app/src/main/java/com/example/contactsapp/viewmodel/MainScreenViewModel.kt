package com.example.contactsapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactsapp.Contact
import com.example.contactsapp.compose.MainScreenState
import com.example.contactsapp.compose.NavigationState
import com.example.contactsapp.data.model.ExtendedContact
import com.example.contactsapp.data.model.Reminder
import com.example.contactsapp.data.model.ReminderType
import com.example.contactsapp.data.repository.ContactsRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainScreenViewModel(private val context: Context) : ViewModel() {

    private val repository = ContactsRepository(context)
    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    fun updatePermission(granted: Boolean) {
        _state.value = _state.value.copy(permissionGranted = granted)
        if (granted) {
            loadContacts()
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                val contacts = withContext(Dispatchers.IO) {
                    repository.loadSystemContacts(context)
                }

                // Сохраняем/обновляем расширенные контакты, но не теряем уже сохранённые поля (в т.ч. isLocked)
                withContext(Dispatchers.IO) {
                    val existingExtended = repository.getAllExtendedContacts().first()
                    val existingById = existingExtended.associateBy { it.contactId }

                    val extendedContacts = contacts.map { contact ->
                        val base = repository.convertToExtendedContact(contact)
                        val existing = existingById[contact.id]

                        if (existing != null) {
                            base.copy(
                                emails = existing.emails,
                                birthday = existing.birthday,
                                socialNetworks = existing.socialNetworks,
                                tags = existing.tags,
                                biography = existing.biography,
                                notes = existing.notes,
                                lastCallDate = existing.lastCallDate,
                                lastMessageDate = existing.lastMessageDate,
                                lastMeetingDate = existing.lastMeetingDate,
                                reminderToCall = existing.reminderToCall,
                                reminderToCongratulate = existing.reminderToCongratulate,
                                reminderReason = existing.reminderReason,
                                isLocked = existing.isLocked,
                                dateAdded = existing.dateAdded
                            )
                        } else {
                            base
                        }
                    }

                    repository.saveExtendedContacts(extendedContacts)
                }

                _state.value = _state.value.copy(
                    contactsList = contacts,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun toggleTagFilter(tag: String) {
        val currentTags = _state.value.selectedTags.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _state.value = _state.value.copy(selectedTags = currentTags)
    }

    fun clearTagFilters() {
        _state.value = _state.value.copy(selectedTags = emptySet())
    }

    fun navigateTo(state: NavigationState) {
        _state.value = _state.value.copy(navigationState = state)
    }

    fun selectContact(contactId: Long) {
        _state.value = _state.value.copy(
            selectedContactId = contactId,
            navigationState = NavigationState.ContactDetails
        )
    }

    fun navigateBack() {
        _state.value = _state.value.copy(
            navigationState = NavigationState.ContactsList,
            selectedContactId = null
        )
    }

    fun getGroupedContactsList(contacts: List<Contact>): Map<Char, List<Contact>> {
        val filtered = if (_state.value.searchQuery.isNotEmpty()) {
            contacts.filter { 
                it.name.contains(_state.value.searchQuery, ignoreCase = true) ||
                it.phones.any { phone -> phone.contains(_state.value.searchQuery, ignoreCase = true) }
            }
        } else {
            contacts
        }
        return filtered.sortedBy { it.name }.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
    }
    
    suspend fun getExtendedContact(contactId: Long): ExtendedContact? {
        return withContext(Dispatchers.IO) {
            repository.getContactById(contactId)
        }
    }
    
    fun updateExtendedContact(
        contactId: Long,
        biography: String? = null,
        notes: String? = null,
        socialNetworks: Map<String, String>? = null
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val contact = repository.getContactById(contactId) ?: return@withContext
                val gson = Gson()
                val updatedContact = contact.copy(
                    biography = biography ?: contact.biography,
                    notes = notes ?: contact.notes,
                    socialNetworks = socialNetworks?.let { gson.toJson(it) } ?: contact.socialNetworks,
                    dateModified = System.currentTimeMillis()
                )
                repository.updateExtendedContact(updatedContact)
            }
        }
    }
    
    fun addTag(contactId: Long, tagName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.addTag(contactId, tagName)
            }
        }
    }
    
    fun removeTag(contactId: Long, tagName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.removeTag(contactId, tagName)
            }
        }
    }
    
    suspend fun getTagsForContact(contactId: Long): List<String> {
        return withContext(Dispatchers.IO) {
            repository.getTagsForContact(contactId).first().map { it.tagName }
        }
    }
    
    suspend fun getRemindersForContact(contactId: Long): List<Reminder> {
        return withContext(Dispatchers.IO) {
            repository.getRemindersForContact(contactId).first()
        }
    }
    
    fun createReminder(
        contactId: Long,
        type: ReminderType,
        title: String,
        description: String?,
        daysFromNow: Int
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val scheduledDate = System.currentTimeMillis() + (daysFromNow * 24 * 60 * 60 * 1000L)
                repository.createReminder(contactId, type, title, description, scheduledDate)
            }
        }
    }
    
    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteReminder(reminderId)
            }
        }
    }
    
    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteExtendedContact(contactId)
            }
            navigateBack()
        }
    }
    
    fun toggleContactLock(contactId: Long, isLocked: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val contact = repository.getContactById(contactId) ?: return@withContext
                val updatedContact = contact.copy(isLocked = isLocked)
                repository.updateExtendedContact(updatedContact)
            }
        }
    }
}