package com.example.contactsapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class BiometricFragment : Fragment() {
    
    private var onSuccess: (() -> Unit)? = null
    private var onFailure: (() -> Unit)? = null
    
    fun authenticate(
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        this.onSuccess = onSuccess
        this.onFailure = onFailure
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Защищенный контакт")
            .setSubtitle("Для доступа требуется подтверждение")
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
            .setNegativeButtonText("Отмена")
            .build()
            
        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess?.invoke()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure?.invoke()
                }
                
                override fun onAuthenticationFailed() {
                    onFailure?.invoke()
                }
            }
        )
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return null
    }
}

