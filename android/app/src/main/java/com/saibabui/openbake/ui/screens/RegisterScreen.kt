package com.saibabui.openbake.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saibabui.openbake.ui.screens.common.GradientButton
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Email, 1 = OTP

    // Email signup state
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var phoneTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    val emailValid = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val phoneValid = phone.isBlank() || phone.filter { it.isDigit() }.let { digits -> digits.length == 10 && digits.first() in '6'..'9' }
    val passwordValid = password.isEmpty() || password.length >= 6

    // OTP signup state
    var otpName by remember { mutableStateOf("") }
    var otpPhone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpPhoneTouched by remember { mutableStateOf(false) }
    val otpPhoneValid = otpPhone.isBlank() || otpPhone.filter { it.isDigit() }.let { digits -> digits.length == 10 && digits.first() in '6'..'9' }

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) onRegisterSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .offset(x = (-30).dp, y = (-30).dp)
                .alpha(0.07f)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

//            Text(text = "🧁", fontSize = 44.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Join our artisanal bake community",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.xLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Tab row: Email | OTP
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; authViewModel.resetOtpState() }) {
                            Text("Email", modifier = Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text("Phone OTP", modifier = Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // ── Email Signup ──
                        FormField(label = "Full Name", value = name, onValueChange = { name = it },
                            placeholder = "Name", keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)

                        Spacer(modifier = Modifier.height(16.dp))

                        FormField(label = "Email", value = email, onValueChange = { email = it; emailTouched = true },
                            placeholder = "Email", keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
                            isError = emailTouched && !emailValid, errorText = "Enter a valid email address")

                        Spacer(modifier = Modifier.height(16.dp))

                        FormField(label = "Phone", value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10); phoneTouched = true },
                            placeholder = "Phone", keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next,
                            isError = phoneTouched && !phoneValid, errorText = "Enter a valid 10-digit mobile number")

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        com.saibabui.openbake.ui.screens.common.OpenBakeTextField(
                            value = password,
                            onValueChange = { password = it; passwordTouched = true },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Password", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)) },
                            shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.input,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            singleLine = true,
                            isError = passwordTouched && !passwordValid,
                            supportingText = if (passwordTouched && !passwordValid) {
                                { Text("Password must be at least 6 characters", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)) }
                            } else null,
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        authState.error?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        GradientButton(
                            text = if (authState.isLoading) "Creating account…" else "Create Account",
                            onClick = { authViewModel.register(name.trim(), email.trim(), phone.trim(), password) },
                            enabled = name.isNotBlank() && email.isNotBlank() && emailValid && phone.isNotBlank() && phoneValid && password.length >= 6 && !authState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // ── OTP Signup ──
                        FormField(label = "Full Name", value = otpName, onValueChange = { otpName = it },
                            placeholder = "Name", keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)

                        Spacer(modifier = Modifier.height(16.dp))

                        FormField(label = "Phone", value = otpPhone, onValueChange = { otpPhone = it.filter { c -> c.isDigit() }.take(10); otpPhoneTouched = true },
                            placeholder = "Phone", keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done,
                            isError = otpPhoneTouched && !otpPhoneValid, errorText = "Enter a valid 10-digit mobile number")

                        Spacer(modifier = Modifier.height(16.dp))

                        if (authState.otpSent) {
                            FormField(label = "OTP Code", value = otpCode, onValueChange = { otpCode = it.filter { c -> c.isDigit() }.take(6) },
                                placeholder = "OTP Code", keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        authState.error?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        if (!authState.otpSent) {
                            GradientButton(
                                text = if (authState.otpSending) "Sending OTP…" else "Send OTP",
                                onClick = { authViewModel.sendOtp(otpPhone.trim()) },
                                enabled = otpName.isNotBlank() && otpPhone.filter { it.isDigit() }.length == 10 && otpPhoneValid && !authState.otpSending,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            GradientButton(
                                text = if (authState.isLoading) "Verifying…" else "Verify & Sign Up",
                                onClick = { authViewModel.verifyOtp(otpPhone.trim(), otpCode.trim(), otpName.trim()) },
                                enabled = otpCode.length >= 4 && !authState.isLoading,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isError: Boolean = false,
    errorText: String? = null
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))
    com.saibabui.openbake.ui.screens.common.OpenBakeTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)) },
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.input,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { Text(errorText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    )
}
