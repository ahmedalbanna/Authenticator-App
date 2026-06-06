package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.utils.TotpHelper

@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    onSave: (issuer: String, label: String, secret: String, period: Int) -> Unit
) {
    var issuer by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var period by remember { mutableIntStateOf(30) }
    
    var secretError by remember { mutableStateOf("") }

    fun validateAndSave() {
        val cleanSecret = secretKey.replace(" ", "").trim()
        if (cleanSecret.isEmpty()) {
            secretError = "المفتاح السري مطلوب"
            return
        }

        // Validate base32 secret roughly (letters A-Z, digits 2-7)
        val base32Regex = "^[A-Z2-7=]+$".toRegex(RegexOption.IGNORE_CASE)
        if (!base32Regex.matches(cleanSecret)) {
            secretError = "مفتاح سري غير صالح! يجب أن يحتوي على أحرف A-Z وأرقام 2-7 فقط"
            return
        }

        onSave(
            issuer.trim().ifEmpty { "أخرى" },
            accountName.trim().ifEmpty { "حساب غير مسمى" },
            cleanSecret.uppercase(),
            period
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.End // RTL layout by default for Arabic
            ) {
                Text(
                    text = "إدخال يدوي للمفتاح",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Service Name Input (Issuer)
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("اسم الخدمة (Google, GitHub...)") },
                    placeholder = { Text("مثال: Google") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Business, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Account Name Input (Label)
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("اسم الحساب (البريد الإلكتروني...)") },
                    placeholder = { Text("مثال: user@gmail.com") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Secret Key Input
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { 
                        secretKey = it
                        secretError = ""
                    },
                    label = { Text("المفتاح السري (Secret Key)") },
                    placeholder = { Text("أدخل مفتاح Base32") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null)
                    },
                    isError = secretError.isNotEmpty(),
                    supportingText = {
                        if (secretError.isNotEmpty()) {
                            Text(text = secretError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("أدخل رمز Base32 الأبجدي الموفر من الموقع المُراد حمايته")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Period Choice (30s vs 60s)
                Text(
                    text = "صلاحية الرمز:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SegmentedChoiceButton(
                        text = "30 ثانية",
                        isSelected = period == 30,
                        onClick = { period = 30 },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedChoiceButton(
                        text = "60 ثانية",
                        isSelected = period == 60,
                        onClick = { period = 60 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = ::validateAndSave,
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ وتفعيل")
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedChoiceButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val strokeColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(strokeColor)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}
