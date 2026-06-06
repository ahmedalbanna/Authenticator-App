package com.example

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AccountUiState
import com.example.ui.MainViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.LockScreen
import com.example.ui.components.ManualAddDialog
import com.example.ui.components.QrScannerDialog
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Secure window content visually to block screenshots and recording
        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val isUnlocked by viewModel.isAppUnlocked.collectAsState()
                val isLockedState by viewModel.isAppLocked.collectAsState()

                if (isLockedState && !isUnlocked) {
                    LockScreen(
                        isSetupMode = false,
                        onPinEntered = { pin ->
                            if (pin == "BIOMETRIC") {
                                // Simulated correct biometric
                                true
                            } else {
                                viewModel.verifyPin(pin)
                            }
                        }
                    )
                } else {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Lock app upon backgrounding for safety
        viewModel.lockApp()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val accounts by viewModel.accountsUiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<AccountUiState?>(null) }
    
    // Quick manual fab menu state
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "المصادق الآمن",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "توليد مفاتيح TOTP مشفرة لحماية حساباتك",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Security configuration lock toggle
                    IconButton(
                        onClick = {
                            if (viewModel.isPinSet()) {
                                // Clear PIN
                                viewModel.setPin(null)
                                Toast.makeText(context, "تم إيقاف تشفير الدخول بأمان.", Toast.LENGTH_SHORT).show()
                            } else {
                                showPinSetupDialog = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isAppLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "قفل التطبيق",
                            tint = if (isAppLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Expanded Action 1: QR Scan
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn() + expandVertically() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { 50 })
                ) {
                    FloatingActionButton(
                        onClick = {
                            isFabMenuExpanded = false
                            showScannerDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "مسح رمز QR")
                    }
                }

                // Expanded Action 2: Manual Add
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn() + expandVertically() + slideInVertically(initialOffsetY = { 20 }),
                    exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { 20 })
                ) {
                    FloatingActionButton(
                        onClick = {
                            isFabMenuExpanded = false
                            showManualDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Keyboard, contentDescription = "إدخال يدوي")
                    }
                }

                // Primary FAB Toggle Drawer
                FloatingActionButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Crossfade(targetState = isFabMenuExpanded) { expanded ->
                        if (expanded) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق قائمة الإضافة")
                        } else {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة حساب جديد")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Search Bar for layout
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("بحث عن خدمة أو حساب...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح البحث")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (accounts.isEmpty()) {
                // Beautiful empty container state with friendly tip & test sample buttons
                EmptyAccountsState(
                    onAddSample = {
                        viewModel.addAccount("ahmedalbannatech@gmail.com", "GitHub", "JBSWY3DPEHPK3PXP")
                        viewModel.addAccount("user@gmail.com", "Google", "MZXW6YTBOI======")
                        viewModel.addAccount("manager@company.com", "Microsoft", "ORXW233SMV2G64TN", period = 60)
                        Toast.makeText(context, "تمت إضافة حسابات تجريبية! 🎉", Toast.LENGTH_SHORT).show()
                    },
                    onTriggerAddManual = { showManualDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts, key = { it.id }) { uiState ->
                        AccountCard(
                            uiState = uiState,
                            onCopy = {
                                val pureCode = uiState.totpCode.replace(" ", "")
                                clipboardManager.setText(AnnotatedString(pureCode))
                                Toast.makeText(context, "تم نسخ الرمز: $pureCode إلى الحافظة! 📋", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteRequested = {
                                accountToDelete = uiState
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Components
    if (showManualDialog) {
        ManualAddDialog(
            onDismiss = { showManualDialog = false },
            onSave = { issuer, label, secret, period ->
                viewModel.addAccount(
                    label = label,
                    issuer = issuer,
                    secret = secret,
                    period = period
                )
                showManualDialog = false
            }
        )
    }

    if (showScannerDialog) {
        QrScannerDialog(
            onDismiss = { showScannerDialog = false },
            onQrScanned = { data ->
                viewModel.addAccount(
                    label = data.label,
                    issuer = data.issuer,
                    secret = data.secret,
                    period = data.period
                )
                showScannerDialog = false
            }
        )
    }

    if (showPinSetupDialog) {
        Dialog(
            onDismissRequest = { showPinSetupDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LockScreen(
                isSetupMode = true,
                onPinEntered = { pin ->
                    viewModel.setPin(pin)
                    showPinSetupDialog = false
                    true
                },
                onCancelSetup = { showPinSetupDialog = false }
            )
        }
    }

    // Account Delete Confirmation Dialogue
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Text(
                    text = "حذف الحساب؟",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف ${accountToDelete?.issuer} (${accountToDelete?.label})؟ سيؤدي ذلك إلى عدم قدرتك على توليد الرموز مرة أخرى.",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        accountToDelete?.let { viewModel.deleteAccount(it.id) }
                        accountToDelete = null
                        Toast.makeText(context, "تم حذف الحساب بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCard(
    uiState: AccountUiState,
    onCopy: () -> Unit,
    onDeleteRequested: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onCopy,
                onLongClick = onDeleteRequested
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = uiState.issuer,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "• ${uiState.label}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = uiState.totpCode,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "انقر للنسخ · اضغط مطولاً للحذف",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp)
            ) {
                val isUrgent = uiState.remainingSeconds <= 5
                val indicatorColor = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                
                CircularProgressIndicator(
                    progress = uiState.progress,
                    color = indicatorColor,
                    strokeWidth = 3.dp,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "${uiState.remainingSeconds}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    ),
                    color = indicatorColor
                )
            }
        }
    }
}

@Composable
fun EmptyAccountsState(
    onAddSample: () -> Unit,
    onTriggerAddManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "لا توجد رموز مصادقة مضافة بعد",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ابدأ بحماية أصولك الرقمية وتأمين حساباتك الآن عبر مسح أكواد الـ QR للمواقع أو إضافتها يدوياً.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onTriggerAddManual,
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("إضافة أول حساب يدوياً")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onAddSample,
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Science, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("توليد حسابات تجريبية سريعة 🧪")
        }
    }
}
