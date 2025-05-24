package com.example.contactsapp.compose

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.contactsapp.CleanedStatusModel
import com.example.contactsapp.Contact
import com.example.contactsapp.MainScreenState
import com.example.contactsapp.MainScreenViewModel
import com.example.contactsapp.R

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel = viewModel<MainScreenViewModel>()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermission(isGranted)
        if (isGranted) {
            viewModel.loadContacts(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndLoadContacts(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            viewModel.state.value.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            viewModel.state.value.permissionGranted -> {
                MainScreenContent(
                    state = viewModel.state.value,
                    onButtonClick = { viewModel.bindService(context) },
                    getGroupedContacts = { contacts -> viewModel.getGroupedContactsList(contacts) },
                    loadContacts = { viewModel.loadContacts(context) }
                )
            }

            !viewModel.state.value.permissionGranted -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        launcher.launch(Manifest.permission.READ_CONTACTS)
                        launcher.launch(Manifest.permission.WRITE_CONTACTS)
                    }) {
                        Text(text = stringResource(R.string.contacts_permission))
                    }
                }
            }

        }
    }
}

@Composable
fun MainScreenContent(
    state: MainScreenState,
    onButtonClick: (context: Context) -> Unit,
    getGroupedContacts: (contacts: List<Contact>) -> Map<Char, List<Contact>>,
    loadContacts: (context: Context) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            getGroupedContacts(state.contactsList).forEach { (letter, contacts) ->
                item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }
                item { Box(modifier = Modifier.padding(start = 16.dp)) { Text(text = letter.toString()) } }
                item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

                items(contacts) { ContactItem(it) }
            }

        }
        val context = LocalContext.current

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                state.cleanupStatus == CleanedStatusModel.SUCCESS -> {
                    Text(stringResource(R.string.deleted))
                    loadContacts(context)
                }

                state.cleanupStatus == CleanedStatusModel.NO_DUPLICATES -> {
                    Text(stringResource(R.string.not_duplicates))
                }

                state.cleanupStatus == CleanedStatusModel.ERROR -> Text(stringResource(R.string.not_deleted))
            }
        }
        Button(
            onClick = { onButtonClick(context) },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.deduplicate_button))
        }
    }
}

@Composable
fun ContactItem(contact: Contact) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.padding(end = 8.dp),
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            alignment = Alignment.Center
        )
        Column {
            Text(text = contact.name, style = MaterialTheme.typography.titleMedium)
            contact.phones.forEach { phone ->
                Text(text = phone, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}