package com.example.contactsapp.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Подтвердите вход",
        subtitle: String = "Используйте биометрию для доступа к контакту"
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                continuation.resume(false)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                continuation.resume(true)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                continuation.resume(false)
            }
        }

        val biometricPrompt = BiometricPrompt(activity , executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)


        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }
}