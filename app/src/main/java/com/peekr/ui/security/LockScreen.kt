package com.peekr.ui.security

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // تشغيل بصمة تلقائي عند فتح الشاشة
    LaunchedEffect(Unit) {
        if (viewModel.isBiometricEnabled()) {
            showBiometricPrompt(context, onUnlocked) { /* فشل - المستخدم يدخل PIN */ }
        }
    }

    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) onUnlocked()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // أيقونة القفل
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                "Peekr مقفول",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // لوحة الـ PIN
            PinPad(
                enteredPin = uiState.enteredPin,
                error = uiState.error,
                onDigit = { viewModel.addDigit(it) },
                onDelete = { viewModel.deleteDigit() },
                onConfirm = { viewModel.verifyPin() }
            )

            // زرار البصمة
            if (viewModel.isBiometricEnabled()) {
                TextButton(
                    onClick = {
                        showBiometricPrompt(context, onUnlocked) {}
                    }
                ) {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("استخدام البصمة")
                }
            }
        }
    }
}

@Composable
fun PinPad(
    enteredPin: String,
    error: String?,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // مؤشرات الـ PIN
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                Surface(
                    shape = CircleShape,
                    color = if (index < enteredPin.length)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(16.dp)
                ) {}
            }
        }

        // خطأ
        AnimatedVisibility(visible = error != null) {
            Text(
                error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // أرقام
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )

        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { digit ->
                    PinButton(label = digit, onClick = { onDigit(digit) })
                }
            }
        }

        // الصف الأخير: حذف - 0 - تأكيد
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PinButton(
                label = "⌫",
                onClick = onDelete,
                color = MaterialTheme.colorScheme.errorContainer
            )
            PinButton(label = "0", onClick = { onDigit("0") })
            PinButton(
                label = "✓",
                onClick = onConfirm,
                color = MaterialTheme.colorScheme.primaryContainer,
                enabled = enteredPin.length >= 4
            )
        }
    }
}

@Composable
fun PinButton(
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (enabled) color else color.copy(alpha = 0.4f),
        modifier = Modifier.size(72.dp),
        enabled = enabled
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

fun showBiometricPrompt(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onFailed: () -> Unit
) {
    val activity = context as? FragmentActivity ?: return
    val executor = ContextCompat.getMainExecutor(context)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
        override fun onAuthenticationFailed() { onFailed() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onFailed() }
    }

    val prompt = BiometricPrompt(activity, executor, callback)
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Peekr")
        .setSubtitle("استخدم بصمتك لفتح التطبيق")
        .setNegativeButtonText("استخدام الـ PIN")
        .build()

    prompt.authenticate(info)
}
