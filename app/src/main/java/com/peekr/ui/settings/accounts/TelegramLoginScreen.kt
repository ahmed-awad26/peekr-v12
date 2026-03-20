package com.peekr.ui.settings.accounts

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.peekr.data.remote.telegram.TelegramAuthState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramLoginScreen(
    navController: NavController,
    viewModel: TelegramLoginViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val tgBlue = Color(0xFF0088CC)
    var dotCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Dot animation for waiting state
    LaunchedEffect(authState) {
        if (authState is TelegramAuthState.WaitingPairing) {
            while (true) {
                delay(500)
                dotCount = (dotCount + 1) % 4
            }
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is TelegramAuthState.Authorized,
            is TelegramAuthState.PairingSuccess -> {
                delay(1200)
                navController.popBackStack()
            }
            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPolling() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ربط تليجرام") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.stopPolling(); navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(tgBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, null, tint = tgBlue, modifier = Modifier.size(44.dp))
            }

            when (authState) {
                // ==============================
                // Idle / Error — Choose method
                // ==============================
                is TelegramAuthState.Idle,
                is TelegramAuthState.Error -> {
                    val errMsg = (authState as? TelegramAuthState.Error)?.message

                    Text(
                        "ربط حساب تليجرام",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        "تحتاج Bot Token لربط التطبيق بتليجرام",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Error card
                    errMsg?.let {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )) {
                            Text(it, modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Step cards
                    SetupStepCard(
                        step = "1",
                        title = "أنشئ بوت في تليجرام",
                        body = "افتح @BotFather واكتب /newbot — اتبع التعليمات واحصل على Token",
                        color = tgBlue,
                        action = "افتح @BotFather",
                        onAction = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/BotFather"))
                            )
                        }
                    )

                    SetupStepCard(
                        step = "2",
                        title = "أضف الـ Bot Token",
                        body = "احفظ الـ Token في صفحة مفاتيح API",
                        color = tgBlue,
                        action = "مفاتيح API",
                        onAction = { navController.navigate("settings/apikeys") }
                    )

                    SetupStepCard(
                        step = "3",
                        title = "ابدأ ربط حسابك",
                        body = "بعد حفظ الـ Token، اضغط هنا لربط حسابك عبر البوت",
                        color = Color(0xFF4CAF50),
                        action = null, onAction = {}
                    )

                    Button(
                        onClick = { viewModel.startPairing() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = tgBlue)
                    ) {
                        Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("ابدأ الربط", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // ==============================
                // WaitingPairing — Show link + polling
                // ==============================
                is TelegramAuthState.WaitingPairing -> {
                    val state = authState as TelegramAuthState.WaitingPairing
                    val dots = ".".repeat(dotCount)

                    Text("خطوة واحدة فقط",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)

                    // Animated waiting indicator
                    Card(
                        colors = CardDefaults.cardColors(containerColor = tgBlue.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Pairing code display
                            Text("كود الربط", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tgBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    state.pairCode,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = tgBlue,
                                    letterSpacing = 6.sp
                                )
                            }

                            HorizontalDivider()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = tgBlue
                                )
                                Text(
                                    "في انتظار التأكيد$dots",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Open Telegram button
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(state.deepLink))
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = tgBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("افتح في تليجرام", fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall)
                            Text("@${state.botUsername}", style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f))
                        }
                    }

                    Text(
                        "بعد ما تضغط Start في تليجرام، هيتم الربط تلقائياً",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Manual confirm button as backup
                    OutlinedButton(
                        onClick = {
                            scope.launch { viewModel.sendCode("manual_check") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تحقق يدوياً")
                    }

                    // Cancel
                    TextButton(onClick = {
                        viewModel.stopPolling()
                        viewModel.initialize()
                    }) { Text("إلغاء") }
                }

                // ==============================
                // PairingSuccess
                // ==============================
                is TelegramAuthState.PairingSuccess -> {
                    val state = authState as TelegramAuthState.PairingSuccess
                    SuccessView(
                        name = state.firstName,
                        username = state.username,
                        tgBlue = tgBlue
                    )
                }

                // ==============================
                // LoadingTdlib
                // ==============================
                is TelegramAuthState.LoadingTdlib -> {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Text("جاري تحميل TDLib...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // ==============================
                // WaitingPhone (TDLib mode — real phone login)
                // ==============================
                is TelegramAuthState.WaitingPhone -> {
                    Text("أدخل رقم هاتفك",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold)
                    Text("سيتم إرسال كود تحقق عبر تليجرام",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)

                    var phone by remember { mutableStateOf("") }
                    var loading by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        placeholder = { Text("+201234567890") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp)) }
                    )
                    Button(
                        onClick = { loading = true; viewModel.sendPhone(phone) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = phone.isNotEmpty() && !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = tgBlue)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp),
                            color = Color.White, strokeWidth = 2.dp)
                        else Text("إرسال الكود", fontWeight = FontWeight.Bold)
                    }
                }

                // WaitingCode (TDLib mode)
                is TelegramAuthState.WaitingCode -> {
                    Text("أدخل كود التحقق",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold)
                    Text("تم إرسال الكود لتطبيق تليجرام",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)

                    var code by remember { mutableStateOf("") }
                    var loading by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("الكود") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { loading = true; viewModel.sendCode(code) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = code.isNotEmpty() && !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = tgBlue)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp),
                            color = Color.White, strokeWidth = 2.dp)
                        else Text("تأكيد الكود", fontWeight = FontWeight.Bold)
                    }
                }

                // WaitingPassword (TDLib 2FA)
                is TelegramAuthState.WaitingPassword -> {
                    Text("أدخل كلمة المرور",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold)
                    Text("حسابك محمي بخطوة تحقق ثنائية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    var pw by remember { mutableStateOf("") }
                    var loading by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = pw, onValueChange = { pw = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { loading = true; viewModel.sendPassword(pw) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = pw.isNotEmpty() && !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = tgBlue)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp),
                            color = Color.White, strokeWidth = 2.dp)
                        else Text("تأكيد", fontWeight = FontWeight.Bold)
                    }
                }

                // ==============================
                // Authorized (bot token already saved)
                // ==============================
                is TelegramAuthState.Authorized -> {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                        Alignment.Center) {
                        Icon(Icons.Default.Done, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(36.dp))
                    }
                    Text("البوت متصل ✓", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("جاري التوجيه...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    step: String, title: String, body: String,
    color: Color, action: String?, onAction: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(step, color = color, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(body, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                if (action != null) {
                    TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                        Text(action, color = color, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Launch, null, modifier = Modifier.size(14.dp).padding(start = 2.dp),
                            tint = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessView(name: String, username: String, tgBlue: Color) {
    Box(
        Modifier.size(88.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = 0.12f)),
        Alignment.Center
    ) {
        Icon(Icons.Default.Done, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(44.dp))
    }
    Text(
        "مرحباً $name! 🎉",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
    )
    if (username.isNotBlank()) {
        Text("@$username", style = MaterialTheme.typography.bodyLarge, color = tgBlue)
    }
    Text(
        "تم ربط حسابك بنجاح\nيمكنك الآن إضافة القنوات التي تريد متابعتها",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.10f))) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
            Text(
                "الخطوة التالية: أضف قنوات تليجرام من ربط الحسابات",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50)
            )
        }
    }
}
