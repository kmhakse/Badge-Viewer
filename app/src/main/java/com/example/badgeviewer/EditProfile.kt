package com.example.badgeviewer

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import okhttp3.RequestBody


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import coil.compose.AsyncImage



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val token = "Bearer " + (prefs.getString("accessToken", "") ?: "")
    val scope = rememberCoroutineScope()

    var badges by remember { mutableStateOf<List<UserBadge>>(emptyList()) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    // Notification States
    var notifyBadge by remember { mutableStateOf(true) }
    var notifyProfile by remember { mutableStateOf(true) }
    var notifyAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val user = AuthService.api.getUser(token)
            firstName = user.firstName
            lastName = user.lastName
            email = user.email
            badges = user.badges
            profileImageUrl = user.image

            // Fix: Use simple if-check instead of complex let to avoid type mismatch
            val prefs = user.emailPreferences
            if (prefs != null) {
                notifyBadge = prefs.badgeReceived
                notifyProfile = prefs.profileUpdate
                notifyAdmin = prefs.adminDaily
            }
        } catch (e: Exception) {
            Log.e("FETCH_USER", "Failed", e)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF05070C), Color.Black)))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Edit Profile", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        // Profile Image Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(90.dp).clip(CircleShape).background(Color(0xFF2F80FF)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else if (profileImageUrl != null) {
                    AsyncImage(model = "https://profile.deepcytes.io/api$profileImageUrl", contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text((firstName.take(1) + lastName.take(1)).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = { imagePicker.launch("image/*") }) { Text("Upload") }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = {
                    scope.launch {
                        try {
                            AuthService.api.removeProfileImage(token)
                            selectedImageUri = null
                            profileImageUrl = null
                            Toast.makeText(context, "Image Removed", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Remove") }
        }

        Spacer(Modifier.height(24.dp))
        Label("Email"); ReadOnlyField(email)

        Spacer(Modifier.height(14.dp))
        Row {
            Column(Modifier.weight(1f)) {
                Label("First Name"); InputField(firstName) { firstName = it }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Label("Last Name"); InputField(lastName) { lastName = it }
            }
        }

        Spacer(Modifier.height(14.dp))
        Label("Your password"); PasswordField(currentPassword) { currentPassword = it }
        Spacer(Modifier.height(14.dp))
        Label("New password"); PasswordField(newPassword) { newPassword = it }

        Spacer(Modifier.height(22.dp))
        Text("*Visible Badges", color = Color.White.copy(.6f), fontSize = 12.sp)

        badges.forEachIndexed { index, badge ->
            ToggleRow(badge.name ?: "Badge ${badge.badgeId}", badge.isPublic) { newValue ->
                badges = badges.toMutableList().apply {
                    this[index] = this[index].copy(isPublic = newValue)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("Notification Preferences", color = Color.White, fontWeight = FontWeight.Bold)
        CheckboxRow("Badge received", notifyBadge) { notifyBadge = it }
        CheckboxRow("Profile updates", notifyProfile) { notifyProfile = it }
        CheckboxRow("Admin daily", notifyAdmin) { notifyAdmin = it }

        Spacer(Modifier.height(26.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        // 1. Prepare JSON Strings (Matches JS JSON.stringify)
                        // Note: Removed spaces to match strict backend parsing
                        val badgesJson = badges.joinToString(prefix = "[", postfix = "]") {
                            """{"badgeId":"${it.badgeId}","isPublic":${it.isPublic}}"""
                        }

                        val emailPrefsJson = """{"badgeReceived":$notifyBadge,"profileUpdate":$notifyProfile,"adminDaily":$notifyAdmin}"""

                        // 2. Helper to create Parts WITHOUT extra content-type headers (Matches JS FormData.append)
                        fun String.toPart(name: String): MultipartBody.Part {
                            return MultipartBody.Part.createFormData(name, this)
                        }

                        // 3. Create all required parts
                        val firstPart = firstName.toPart("firstName")
                        val lastPart = lastName.toPart("lastName")
                        val passwordPart = currentPassword.toPart("password")
                        val newPassPart = newPassword.toPart("newPassword")
                        val badgesPart = badgesJson.toPart("badges")
                        val prefsPart = emailPrefsJson.toPart("emailPreferences")

                        val imagePart = selectedImageUri?.let { uriToMultipart(context, it) }

                        // 4. API CALL
                        val response = AuthService.api.updateProfile(
                            token = token,
                            firstName = firstPart,
                            lastName = lastPart,
                            password = passwordPart,
                            newPassword = newPassPart,
                            badges = badgesPart,
                            emailPreferences = prefsPart,
                            profileImage = imagePart
                        )

                        Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                        navController.popBackStack()

                    } catch (e: Exception) {
                        Log.e("PROFILE_UPDATE", "Detailed Error: ${e.message}", e)
                        Toast.makeText(context, "Update failed. Check credentials/password.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Confirm")
        }
    }
}
@Composable fun Label(text: String) =
    Text(text, color = Color.White.copy(0.7f), fontSize = 14.sp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF38C8F8),
            unfocusedBorderColor = Color.White.copy(0.3f),
            cursorColor = Color(0xFF38C8F8)
        )
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadOnlyField(value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White,
            focusedBorderColor = Color.White.copy(0.3f),
            unfocusedBorderColor = Color.White.copy(0.2f)
        )
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF38C8F8),
            unfocusedBorderColor = Color.White.copy(0.3f),
            cursorColor = Color(0xFF38C8F8)
        )
    )
}


@Composable
fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked, onChange); Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White)
    }
}

@Composable
fun CheckboxRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange); Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White)
    }
}

fun uriToMultipart(context: Context, uri: Uri): MultipartBody.Part {
    val bytes = context.contentResolver.openInputStream(uri)!!.readBytes()
    val body = bytes.toRequestBody("image/*".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("profileImage", "profile.jpg", body)
}
