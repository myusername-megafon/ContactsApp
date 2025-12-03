package com.example.contactsapp.compose

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.contactsapp.Contact
import com.example.contactsapp.R
import com.example.contactsapp.compose.components.SearchBar
import com.example.contactsapp.compose.components.BiometricAuthDialog
import com.example.contactsapp.compose.components.EditContactDialog
import com.example.contactsapp.compose.components.AddNoteDialog
import com.example.contactsapp.compose.components.AddTagDialog
import com.example.contactsapp.compose.components.AddReminderDialog
import com.example.contactsapp.viewmodel.MainScreenViewModel
import com.example.contactsapp.data.model.ReminderType
import com.example.contactsapp.data.model.Reminder
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.fragment.app.FragmentActivity
import com.example.contactsapp.utils.BiometricHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermission(isGranted)
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                        Text("Загрузка контактов...")
                    }
                }
            }

            uiState.permissionGranted -> {
                when (uiState.navigationState) {
                    is NavigationState.ContactsList -> {
                        ContactsListScreen(viewModel, uiState)
                    }
                    is NavigationState.ContactDetails -> {
                        ContactDetailsScreen(viewModel, uiState)
                    }
                    else -> ContactsListScreen(viewModel, uiState)
                }
            }

            !uiState.permissionGranted -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Доступ к контактам",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Для работы приложения необходим доступ к вашим контактам",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.READ_CONTACTS) },
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text("Предоставить доступ")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsListScreen(
    viewModel: MainScreenViewModel,
    state: MainScreenState
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Контакты") },
            actions = {
                IconButton(
                    onClick = {
                        // Обработчик нажатия на шестерёнку
                        android.widget.Toast
                            .makeText(context, "Настройки в разработке", android.widget.Toast.LENGTH_SHORT)
                            .show()
                    }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        // Search Bar
        SearchBar(
            query = state.searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            val groupedContacts = viewModel.getGroupedContactsList(state.contactsList)
            
            groupedContacts.forEach { (letter, contacts) ->
                item {
                    SectionHeader(letter = letter)
                }

                items(contacts) { contact ->
                    EnhancedContactItem(
                        contact = contact,
                        onClick = { viewModel.selectContact(contact.id) },
                        viewModel = viewModel
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    viewModel: MainScreenViewModel,
    state: MainScreenState
) {
    var showAddReminder by remember { mutableStateOf(false) }
    var showEditContact by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    var showAddTag by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var isContactUnlocked by remember { mutableStateOf(false) }
    var shouldAutoPrompt by remember { mutableStateOf(true) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var isBiometricInProgress by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val biometricHelper = remember { BiometricHelper() }
    val repository = remember { com.example.contactsapp.data.repository.ContactsRepository(context) }
    
    // Получаем текущий контакт
    val currentContact = state.contactsList.find { it.id == state.selectedContactId }
    
    // Реальные данные из базы
    var extendedContact by remember { mutableStateOf<com.example.contactsapp.data.model.ExtendedContact?>(null) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var notes by remember { mutableStateOf("") }
    var socialNetworks by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var reminders by remember { mutableStateOf<List<com.example.contactsapp.data.model.Reminder>>(emptyList()) }
    var availableTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var reminderUpdateTrigger by remember { mutableStateOf(0) }
    
    LaunchedEffect(state.selectedContactId, reminderUpdateTrigger) {
        state.selectedContactId?.let { contactId ->
            // Загружаем ExtendedContact
            extendedContact = viewModel.getExtendedContact(contactId)
            extendedContact?.let { ec ->
                notes = ec.notes ?: ""
                socialNetworks = repository.parseSocialNetworks(ec.socialNetworks)
                
                // Загружаем теги
                tags = viewModel.getTagsForContact(contactId)
                
                // Загружаем напоминания
                reminders = viewModel.getRemindersForContact(contactId)
                
                // Проверяем, нужна ли биометрия
                if (ec.isLocked) {
                    isContactUnlocked = false
                    shouldAutoPrompt = true
                } else {
                    isContactUnlocked = true
                    shouldAutoPrompt = false
                }
            }
            
            // Загружаем доступные теги
            withContext(Dispatchers.IO) {
                availableTags = repository.getAllTags().first().distinct()
            }
        }
    }
    
    suspend fun launchBiometricPrompt(): Boolean {
        if (activity == null) {
            biometricError = "Биометрия недоступна в этом режиме"
            return false
        }
        if (!biometricHelper.isBiometricAvailable(activity)) {
            biometricError = "Настройте отпечаток или PIN-код в настройках устройства"
            return false
        }
        isBiometricInProgress = true
        return try {
            val success = biometricHelper.authenticate(
                activity = activity,
                title = "Разблокируйте контакт",
                subtitle = currentContact?.name?.let { "Доступ к контакту $it" } ?: "Подтвердите личность"
            )
            if (success) {
                biometricError = null
                isContactUnlocked = true
            } else {
                biometricError = "Аутентификация отменена или не выполнена"
            }
            success
        } finally {
            isBiometricInProgress = false
        }
    }

    LaunchedEffect(state.selectedContactId) {
        isContactUnlocked = false
        shouldAutoPrompt = true
        biometricError = null
        isBiometricInProgress = false
    }

    LaunchedEffect(extendedContact?.isLocked, shouldAutoPrompt) {
        if (extendedContact?.isLocked == true && shouldAutoPrompt) {
            shouldAutoPrompt = false
            launchBiometricPrompt()
        } else if (extendedContact?.isLocked == false && extendedContact != null) {
            isContactUnlocked = true
            biometricError = null
        }
    }

    val shouldHideDetails = extendedContact?.isLocked == true && !isContactUnlocked

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar with Back
            TopAppBar(
                title = { Text(text = currentContact?.name ?: "Детали контакта", overflow = TextOverflow.Clip) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Добавить напоминание
                    IconButton(onClick = { showAddReminder = true }, enabled = !shouldHideDetails) {
                        Icon(Icons.Default.Add, contentDescription = "Add Reminder")
                    }
                    // Редактирование
                    IconButton(onClick = { showEditContact = true }, enabled = !shouldHideDetails) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    // Замок (скрытые контакты)
                    IconButton(
                        onClick = {
                            extendedContact?.let { ec ->
                                val currentlyLocked = ec.isLocked
                                if (!currentlyLocked) {
                                    // Включаем режим скрытого контакта
                                    val newLockState = true
                                    viewModel.toggleContactLock(ec.contactId, newLockState)
                                    extendedContact = ec.copy(isLocked = newLockState)
                                    isContactUnlocked = false
                                    shouldAutoPrompt = false
                                    biometricError = null
                                } else {
                                    // Уже скрытый контакт
                                    if (!isContactUnlocked) {
                                        // Сначала попросим биометрию
                                        coroutineScope.launch {
                                            launchBiometricPrompt()
                                        }
                                    } else {
                                        // Контакт уже разблокирован в этой сессии — можем убрать из скрытых
                                        val newLockState = false
                                        viewModel.toggleContactLock(ec.contactId, newLockState)
                                        extendedContact = ec.copy(isLocked = newLockState)
                                        isContactUnlocked = true
                                        shouldAutoPrompt = false
                                        biometricError = null
                                    }
                                }
                            }
                        }
                    ) {
                        val locked = extendedContact?.isLocked == true
                        if (locked)
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Скрытый контакт"
                        )
                        else Icon(
                            painter = painterResource(R.drawable.open),
                            contentDescription = "Обычный контакт"
                        )
                    }
                    // Прочие действия
                    IconButton(onClick = { showMoreOptions = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )

            when {
                shouldHideDetails -> {
                    LockedContactPlaceholder(
                        contactName = currentContact?.name ?: "контакту",
                        isLoading = isBiometricInProgress,
                        errorMessage = biometricError,
                        onUnlockClick = {
                            coroutineScope.launch {
                                launchBiometricPrompt()
                            }
                        }
                    )
                }
                extendedContact == null && currentContact != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ContactHeaderCard(contact = currentContact)
                        }
                        
                        item {
                            ContactActionsCard(contact = currentContact, extendedContact = extendedContact)
                        }
                        
                        item {
                            ContactInfoSection(extendedContact = extendedContact)
                        }
                        
                        item {
                            SocialMediaSection(socialNetworks = socialNetworks, context = context)
                        }
                        
                        item {
                            TagsSection(
                                tags = tags,
                                onAddClick = { showAddTag = true },
                                onRemoveTag = { tag ->
                                    state.selectedContactId?.let {
                                        viewModel.removeTag(it, tag)
                                        tags = tags - tag
                                    }
                                }
                            )
                        }
                        
                        item {
                            NotesSection(note = notes, onEditClick = { showAddNote = true })
                        }
                        
                        item {
                            RemindersSection(
                                reminders = reminders,
                                onDeleteReminder = { reminderId ->
                                    viewModel.deleteReminder(reminderId)
                                    // Триггерим обновление напоминаний
                                    reminderUpdateTrigger++
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Диалог редактирования контакта
        if (showEditContact && extendedContact != null) {
            EditContactDialog(
                contact = extendedContact,
                onDismiss = { showEditContact = false },
                onSave = { biography, notesText, socials ->
                    viewModel.updateExtendedContact(
                        contactId = state.selectedContactId!!,
                        biography = biography,
                        notes = notesText,
                        socialNetworks = socials
                    )
                    socialNetworks = socials
                    notes = notesText
                    extendedContact = extendedContact?.copy(
                        biography = biography,
                        notes = notesText,
                        socialNetworks = com.google.gson.Gson().toJson(socials)
                    )
                    showEditContact = false
                }
            )
        }
        
        // Диалог добавления заметки
        if (showAddNote && state.selectedContactId != null) {
            AddNoteDialog(
                initialNote = notes,
                onDismiss = { showAddNote = false },
                onSave = { note ->
                    viewModel.updateExtendedContact(
                        contactId = state.selectedContactId!!,
                        notes = note
                    )
                    notes = note
                    extendedContact = extendedContact?.copy(notes = note)
                }
            )
        }
        
        // Диалог добавления тега
        if (showAddTag && state.selectedContactId != null) {
            AddTagDialog(
                availableTags = availableTags.ifEmpty { listOf("Друзья", "Работа", "Семья", "Коллеги", "Важные") },
                onDismiss = { showAddTag = false },
                onAddTag = { tag ->
                    viewModel.addTag(state.selectedContactId!!, tag)
                    tags = tags + tag
                }
            )
        }
        
        // Диалог добавления напоминания
        if (showAddReminder && state.selectedContactId != null) {
            AddReminderDialog(
                onDismiss = { showAddReminder = false },
                onConfirm = { title, description, type, days ->
                    viewModel.createReminder(
                        contactId = state.selectedContactId!!,
                        type = type,
                        title = title,
                        description = description,
                        daysFromNow = days
                    )
                    // Триггерим обновление напоминаний
                    reminderUpdateTrigger++
                }
            )
        }
        
        // Меню дополнительных опций
        if (showMoreOptions && state.selectedContactId != null && extendedContact != null) {
            AlertDialog(
                onDismissRequest = { showMoreOptions = false },
                title = { Text("Дополнительно") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.toggleContactLock(
                                    state.selectedContactId!!,
                                    !extendedContact!!.isLocked
                                )
                                val newLockState = !extendedContact!!.isLocked
                                extendedContact = extendedContact?.copy(isLocked = newLockState)
                                if (newLockState) {
                                    isContactUnlocked = false
                                    shouldAutoPrompt = true
                                    biometricError = null
                                } else {
                                    isContactUnlocked = true
                                    shouldAutoPrompt = false
                                    biometricError = null
                                }
                                showMoreOptions = false
                            }
                        ) {
                            Text(if (extendedContact!!.isLocked) "Разблокировать контакт" else "Заблокировать контакт")
                        }
                        TextButton(
                            onClick = {
                                // Экспорт контакта
                                val contact = currentContact
                                if (contact != null) {
                                    val vCard = "BEGIN:VCARD\nVERSION:3.0\nFN:${contact.name}\n"
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, vCard)
                                        type = "text/x-vcard"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Экспортировать контакт")
                                    context.startActivity(shareIntent)
                                }
                                showMoreOptions = false
                            }
                        ) {
                            Text("Экспортировать контакт")
                        }
                        TextButton(
                            onClick = {
                                viewModel.deleteContact(state.selectedContactId!!)
                                showMoreOptions = false
                            }
                        ) {
                            Text("Удалить контакт", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMoreOptions = false }) {
                        Text("Отмена")
                    }
                },
                dismissButton = null
            )
        }
    }
}

@Composable
fun LockedContactPlaceholder(
    contactName: String,
    isLoading: Boolean,
    errorMessage: String?,
    onUnlockClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Контакт защищен",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Для просмотра данных требуется подтвердить доступ к $contactName.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!errorMessage.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onUnlockClick,
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Проверка...")
            } else {
                Text("Разблокировать")
            }
        }
    }
}

@Composable
fun ContactHeaderCard(contact: Contact?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = contact?.photoUri,
                contentDescription = "Contact Photo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = null
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = contact?.name ?: "Неизвестный",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (contact?.phones?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phones.first(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ContactActionsCard(contact: Contact?, extendedContact: com.example.contactsapp.data.model.ExtendedContact?) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = Icons.Default.Call,
                label = "Позвонить",
                onClick = {
                    contact?.phones?.firstOrNull()?.let { phone ->
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_CALL,
                            android.net.Uri.parse("tel:$phone")
                        )
                        context.startActivity(intent)
                    }
                }
            )
            ActionButton(
                icon = Icons.Default.MailOutline,
                label = "SMS",
                onClick = {
                    contact?.phones?.firstOrNull()?.let { phone ->
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("sms:$phone")
                        )
                        context.startActivity(intent)
                    }
                }
            )
            ActionButton(
                icon = Icons.Default.Email,
                label = "Email",
                onClick = {
                    if (extendedContact != null) {
                        val emailsJson = extendedContact.emails
                        if (!emailsJson.isNullOrBlank()) {
                            try {
                                val emails = com.google.gson.Gson().fromJson(
                                    emailsJson,
                                    Array<String>::class.java
                                )
                                if (emails.isNotEmpty()) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:")
                                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(emails[0]))
                                    }
                                    context.startActivity(intent)
                                    return@ActionButton
                                }
                            } catch (_: Exception) {
                                // Игнорируем и падаем в общий обработчик ниже
                            }
                        }
                    }

                    // Если нет расширенного контакта или email'ов — просто открываем почтовый клиент
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:")
                    }
                    context.startActivity(intent)
                }
            )
            ActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = {
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, contact?.name)
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            )
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun ContactInfoSection(extendedContact: com.example.contactsapp.data.model.ExtendedContact?) {
    val emails = remember(extendedContact?.emails) {
        if (extendedContact?.emails.isNullOrBlank()) {
            emptyList<String>()
        } else {
            try {
                com.google.gson.Gson().fromJson(
                    extendedContact?.emails,
                    Array<String>::class.java
                ).toList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    SectionCard(title = "Информация") {
        if (emails.isNotEmpty()) {
            emails.forEach { email ->
                InfoRow(icon = Icons.Default.Email, label = "Email", value = email)
            }
        } else {
            InfoRow(icon = Icons.Default.Email, label = "Email", value = "Не указан")
        }
        InfoRow(icon = Icons.Default.LocationOn, label = "Адрес", value = "Не указан")
        extendedContact?.biography?.let { biography ->
            if (biography.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = biography,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SocialMediaSection(socialNetworks: Map<String, String>, context: Context) {
    SectionCard(title = "Социальные сети") {
        if (socialNetworks.isEmpty()) {
            Text(
                text = "Добавьте ссылки на соцсети",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            socialNetworks.forEach { (platform, url) ->
                SocialMediaRow(platform = platform, username = url, context = context)
            }
        }
    }
}

@Composable
fun TagsSection(tags: List<String>, onAddClick: () -> Unit, onRemoveTag: (String) -> Unit) {
    SectionCard(title = "Теги") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tags.forEach { tag ->
                TagChip(
                    label = tag,
                    onRemove = { onRemoveTag(tag) }
                )
            }
            TagChip(label = "+", onClick = onAddClick)
        }
    }
}

@Composable
fun NotesSection(note: String, onEditClick: () -> Unit) {
    SectionCard(title = "Заметки") {
        Text(
            text = if (note.isEmpty()) "Добавьте заметку о контакте..." else note,
            style = MaterialTheme.typography.bodyMedium,
            color = if (note.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable(onClick = onEditClick)
        )
    }
}

@Composable
fun RemindersSection(reminders: List<Reminder>, onDeleteReminder: (Long) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru", "RU")) }
    
    SectionCard(title = "Напоминания") {
        if (reminders.isEmpty()) {
            Text(
                text = "Напоминаний нет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            reminders.forEachIndexed { index, reminder ->
                if (index > 0) HorizontalDivider()
                ReminderItem(
                    icon = Icons.Default.Notifications,
                    title = reminder.title,
                    date = dateFormat.format(java.util.Date(reminder.scheduledDate)),
                    onDelete = { onDeleteReminder(reminder.id) }
                )
            }
        }
    }
}

@Composable
fun ReminderItem(icon: ImageVector, title: String, date: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
        modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SocialMediaRow(platform: String, username: String, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = platform,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = username,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = {
            val url = if (username.startsWith("http://") || username.startsWith("https://")) {
                username
            } else if (username.startsWith("@")) {
                // Попытка открыть через приложение соцсети
                when (platform.lowercase()) {
                    "telegram" -> "https://t.me/${username.substring(1)}"
                    "instagram" -> "https://instagram.com/${username.substring(1)}"
                    "facebook" -> "https://facebook.com/${username.substring(1)}"
                    "twitter" -> "https://twitter.com/${username.substring(1)}"
                    else -> username
                }
            } else {
                username
            }
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                // Если не удалось открыть, игнорируем
            }
        }) {
            Icon(Icons.Default.Send, contentDescription = "Open")
        }
    }
}

@Composable
fun TagChip(label: String, onClick: () -> Unit = {}, onRemove: (() -> Unit)? = null) {
    if (label == "+") {
        FilterChip(
            selected = false,
            onClick = onClick,
            label = { Text(label) },
            modifier = Modifier.padding(vertical = 4.dp)
        )
    } else {
        AssistChip(
            onClick = {},
            label = { Text(label) },
            trailingIcon = if (onRemove != null) {
                {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Удалить",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else null,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun SectionHeader(letter: Char) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun EnhancedContactItem(
    contact: Contact,
    onClick: () -> Unit,
    viewModel: MainScreenViewModel
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var extendedContact by remember { mutableStateOf<com.example.contactsapp.data.model.ExtendedContact?>(null) }

    LaunchedEffect(contact.id) {
        extendedContact = viewModel.getExtendedContact(contact.id)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contact Photo
        AsyncImage(
            model = contact.photoUri,
            contentDescription = "Contact Photo",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentScale = ContentScale.Crop,
            placeholder = null
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Contact Info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (extendedContact?.isLocked == true) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Скрытый контакт",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (contact.phones.isNotEmpty()) {
                Text(
                    text = contact.phones.first(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Открыть") },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )

                if (extendedContact != null) {
                    val isLocked = extendedContact?.isLocked == true
                    DropdownMenuItem(
                        text = { Text(if (isLocked) "Разблокировать контакт" else "Заблокировать контакт") },
                        onClick = {
                            extendedContact?.let { ec ->
                                viewModel.toggleContactLock(contact.id, !ec.isLocked)
                                extendedContact = ec.copy(isLocked = !ec.isLocked)
                            }
                            showMenu = false
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Экспортировать контакт") },
                    onClick = {
                        val vCard = "BEGIN:VCARD\nVERSION:3.0\nFN:${contact.name}\n"
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, vCard)
                            type = "text/x-vcard"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Экспортировать контакт")
                        context.startActivity(shareIntent)
                        showMenu = false
                    }
                )

                DropdownMenuItem(
                    text = { Text("Удалить контакт", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        viewModel.deleteContact(contact.id)
                        showMenu = false
                    }
                )
            }
        }
    }
}

