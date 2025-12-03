package com.example.contactsapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.contactsapp.compose.MainScreen
import com.example.contactsapp.ui.theme.ContactsAppTheme
import com.example.contactsapp.viewmodel.MainScreenViewModel
import com.example.contactsapp.viewmodel.MainScreenViewModelFactory

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactsAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: MainScreenViewModel = viewModel(
                        factory = MainScreenViewModelFactory(this)
                    )
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

fun androidx.compose.ui.Modifier.padding(innerPadding: androidx.compose.foundation.layout.PaddingValues): Modifier {
    return this
}