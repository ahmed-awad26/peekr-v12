package com.peekr.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    navController: NavController,
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الأمان والخصوصية") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==============================
            // تفعيل القفل
            // ==============================
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("قفل التطبيق",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium)
                                Text("حماية التطبيق عند الدخول",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = uiState.settings.isLockEnabled,
                            onCheckedChange = { viewModel.toggleLock(it) }
                        )
                    }

                    // مدة القفل
                    AnimatedVisibility(visible = uiState.settings.isLockEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Divider()
                            Spacer(Modifier.height(12.dp))
                            Text("قفل بعد:",
                                style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            val options = listOf(
                                0 to "فوراً",
                                15 to "15 ثانية",
                                30 to "30 ثانية",
                                60 to "دقيقة",
                                300 to "5 دقائق"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEach { (seconds, label) ->
                                    FilterChip(
                                        selected = uiState.settings.lockAfterSeconds == seconds,
                                        onClick = { viewModel.setLockAfter(seconds) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==============================
            // PIN
            // ==============================
            AnimatedVisibility(visible = uiState.settings.isLockEnabled) {
                Card {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Pin, null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("رقم PIN",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium)
                                    Text(
                                        if (uiState.settings.isPinEnabled) "مفعّل ✓"
                                        else "غير مفعّل",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (uiState.settings.isPinEnabled)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (uiState.settings.isPinEnabled) {
                                OutlinedButton(
                                    onClick = { viewModel.showChangePinDialog() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("تغيير") }
                            } else {
                                Button(onClick = { viewModel.showSetPinDialog() }) {
                                    Text("تعيين")
                                }
                            }
                        }
                    }
                }
            }

            // ==============================
            // بصمة الإصبع
            // ==============================
            AnimatedVisibility(
                visible = uiState.settings.isLockEnabled && uiState.biometricAvailable
            ) {
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Fingerprint, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("بصمة الإصبع",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium)
                                Text("فتح التطبيق بالبصمة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = uiState.settings.isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // تحقق بالبصمة قبل تفعيلها
                                    showBiometricPrompt(context,
                                        onSuccess = { viewModel.setBiometric(true) },
                                        onFailed = {}
                                    )
                                } else {
                                    viewModel.setBiometric(false)
                                }
                            }
                        )
                    }
                }
            }

            // رسالة لو البصمة مش متاحة
            if (uiState.settings.isLockEnabled && !uiState.biometricAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                        Text(
                            "البصمة مش متاحة على هذا الجهاز أو مش مفعّلة في إعدادات الأندرويد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ديالوج تعيين PIN
    if (uiState.showSetPinDialog) {
        SetPinDialog(
            isChanging = uiState.isChangingPin,
            onConfirm = { pin -> viewModel.setPin(pin) },
            onDismiss = { viewModel.hidePinDialog() },
            errorMessage = uiState.pinError
        )
    }
}

@Composable
fun SetPinDialog(
    isChanging: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (!isChanging) "تعيين PIN جديد"
                else if (step == 1) "أدخل PIN الجديد"
                else "أكد الـ PIN"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (step == 1) "أدخل 4 أرقام على الأقل"
                    else "أعد إدخال نفس الـ PIN للتأكيد",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // مؤشرات
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentPin = if (step == 1) pin else confirmPin
                    repeat(4) { index ->
                        Surface(
                            shape = CircleShape,
                            color = if (index < currentPin.length)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(14.dp)
                        ) {}
                    }
                }

                OutlinedTextField(
                    value = if (step == 1) pin else confirmPin,
                    onValueChange = { value ->
                        if (value.length <= 8 && value.all { it.isDigit() }) {
                            if (step == 1) pin = value else confirmPin = value
                        }
                    },
                    label = { Text("PIN") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    isError = errorMessage != null
                )

                errorMessage?.let {
                    Text(it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1 && pin.length >= 4) {
                        step = 2
                    } else if (step == 2) {
                        if (pin == confirmPin) onConfirm(pin)
                    }
                },
                enabled = if (step == 1) pin.length >= 4 else confirmPin.length >= 4
            ) {
                Text(if (step == 1) "التالي" else "تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
