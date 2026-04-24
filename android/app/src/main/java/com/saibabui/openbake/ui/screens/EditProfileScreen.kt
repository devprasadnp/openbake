package com.saibabui.openbake.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.AuthViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.uiState.collectAsState()
    val user = authState.user

    // Ensure profile is loaded when this screen opens
    LaunchedEffect(Unit) {
        if (user == null) authViewModel.checkAuthState()
    }

    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var phone by remember(user) { mutableStateOf(user?.phone ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var nameTouched by remember { mutableStateOf(false) }
    var phoneTouched by remember { mutableStateOf(false) }
    val nameValid = name.isBlank() || name.trim().length >= 2
    val phoneValid = phone.isBlank() || phone.filter { it.isDigit() }.let { digits -> digits.length == 10 && digits.first() in '6'..'9' }

    // Navigate back on success
    LaunchedEffect(authState.updateSuccess) {
        if (authState.updateSuccess) {
            authViewModel.resetUpdateSuccess()
            onBack()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            // Upload immediately
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@rememberLauncherForActivityResult
                inputStream.close()
                val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(contentType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "avatar.jpg", requestBody)
                authViewModel.uploadAvatar(part)
            } catch (_: Exception) { }
        }
    }

    // Build the avatar image URL (could be local URI or server URL)
    val avatarUrl = selectedImageUri?.toString()
        ?: user?.profileImageUrl?.let { url ->
            if (url.startsWith("http")) url
            else RetrofitClient.getBaseUrl().trimEnd('/') + url
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar with camera overlay
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    com.saibabui.openbake.ui.screens.common.OpenBakeImage(
                        model = avatarUrl,
                        contentDescription = "Profile photo",
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        contentScale = ContentScale.Crop,
                        placeholderEmoji = "",
                        emojiFontSize = 40
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.name?.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                // Camera badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Change photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                "Tap to change photo",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Email (read-only)
            Column {
                Text(
                    "Email",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    Text(
                        text = user?.email ?: "—",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Name field
            com.saibabui.openbake.ui.screens.common.OpenBakeTextField(
                value = name,
                onValueChange = { name = it; nameTouched = true },
                label = { Text("Full Name", fontFamily = Nunito) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                isError = nameTouched && !nameValid,
                supportingText = if (nameTouched && !nameValid) {
                    { Text("Name must be at least 2 characters", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)) }
                } else null
            )

            // Phone field
            com.saibabui.openbake.ui.screens.common.OpenBakeTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10); phoneTouched = true },
                label = { Text("Phone Number", fontFamily = Nunito) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                isError = phoneTouched && !phoneValid,
                supportingText = if (phoneTouched && !phoneValid) {
                    { Text("Enter a valid 10-digit Indian mobile number", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)) }
                } else null
            )

            // Error
            authState.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    authViewModel.updateProfile(
                        name = name.trim().takeIf { it.isNotBlank() },
                        phone = phone.trim().takeIf { it.isNotBlank() }
                    )
                },
                enabled = !authState.isLoading && name.trim().isNotBlank() && nameValid && (phone.isBlank() || phoneValid),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.input,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Save Changes",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = Nunito,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
