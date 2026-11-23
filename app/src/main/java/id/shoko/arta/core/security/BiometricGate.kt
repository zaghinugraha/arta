package id.shoko.arta.core.security

import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** BiometricPrompt (sidik jari) dengan fallback kredensial perangkat. */
object BiometricGate {
    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String?,
        onSuccess: () -> Unit,
        onError: (code: Int, message: CharSequence) -> Unit,
    ) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            .setAllowedAuthenticators(DeviceSecurity.AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errorCode, errString)
            },
        )
        prompt.authenticate(info)
    }
}

/** Meminta autentikasi dari composable (diisi MainActivity). */
fun interface Authenticator {
    fun authenticate(title: String, onSuccess: () -> Unit, onError: (String) -> Unit)
}

val LocalAuthenticator = staticCompositionLocalOf<Authenticator> { Authenticator { _, _, e -> e("Tidak tersedia") } }
