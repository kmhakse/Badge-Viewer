package com.example.badgeviewer

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*



/* ================= API & MODELS ================= */

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/register/otp")
    suspend fun sendRegisterOtp(@Body body: EmailRequest): BasicResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): BasicResponse

    @POST("auth/reset-password/otp")
    suspend fun sendResetOtp(@Body body: EmailRequest): BasicResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): BasicResponse

    @GET("user")
    suspend fun getUser(
        @Header("Authorization") token: String
    ): UserResponse   // ✅ not EditUserResponse


    /* ✅ UPDATE PROFILE */
    @Multipart
    @PUT("user/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Part("firstName") firstName: RequestBody,
        @Part("lastName") lastName: RequestBody,
        @Part("password") password: RequestBody?,
        @Part("newPassword") newPassword: RequestBody?,
        @Part("badges") badges: RequestBody,              // ← RequestBody
        @Part("emailPreferences") emailPreferences: RequestBody,  // ← RequestBody
        @Part profileImage: MultipartBody.Part?
    ): BasicResponse




    /* ✅ REMOVE IMAGE */
    @DELETE("user/profile/image")
    suspend fun removeProfileImage(
        @Header("Authorization") token: String
    ): BasicResponse
}



object AuthService {
    val api: AuthApi = Retrofit.Builder()
        .baseUrl("https://profile.deepcytes.io/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApi::class.java)
}


data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String
)

data class EmailRequest(
    val email: String
)

data class RegisterRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val otp: Int,
    val password: String
)

data class ResetPasswordRequest(
    val email: String,
    val otp: Int,
    val newPassword: String
)
data class BasicResponse(
    val message: String
)


/* ================= STYLING ================= */

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
    focusedBorderColor = Color(0xFF42B6D9),
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
    focusedContainerColor = Color.White.copy(alpha = 0.08f),
    cursorColor = Color(0xFF42B6D9),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    unfocusedLabelColor = Color.Gray,
    focusedLabelColor = Color(0xFF42B6D9)
)

/* ================= COMMON UI COMPONENTS ================= */

@Composable
fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E293B).copy(alpha = 0.8f),
                        Color(0xFF0F172A).copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun AuthHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Text("←", color = Color.White, fontSize = 20.sp)
                }
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = if (onBack == null) TextAlign.Start else TextAlign.Center
            )
            IconButton(onClick = onClose) {
                Text("✕", color = Color.White, fontSize = 18.sp)
            }
        }
        Text(
            text = subtitle,
            color = Color.LightGray.copy(alpha = 0.7f),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun PrimaryButton(text: String, isLoading: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF42B6D9),
            contentColor = Color.Black,
            disabledContainerColor = Color(0xFF42B6D9).copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            // Fix: Use Modifier.size instead of a 'size' parameter
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}
/* ================= DIALOGS ================= */

@Composable
fun LoginDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit, // ✅ ADD
    onSignupClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
)
 {
    if (!open) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Blurred Background Layer
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .blur(20.dp)
            )

            AuthCard {
                AuthHeader(
                    title = "Welcome Back",
                    subtitle = "Sign in to continue your journey",
                    onClose = onDismiss
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    singleLine = true
                )

                Column {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                        singleLine = true
                    )
                    Text(
                        "Forgot Password?",
                        color = Color(0xFF42B6D9),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp)
                            .clickable { onForgotPasswordClick() }
                    )
                }

                if (error != null) {
                    Text(error!!, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                }

                PrimaryButton("Login", isLoading = loading) {
                    scope.launch {
                        try {
                            loading = true
                            val res = AuthService.api.login(LoginRequest(email.trim(), password))

                            context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                                .edit()
                                .putString("accessToken", res.token)
                                .putString("email", email.trim())                 // ✅ ADD THIS
                                .putString("name", email.substringBefore("@"))    // ✅ name from email
                                .apply()

                            onLoginSuccess()
                            onDismiss()

                        } catch (e: Exception) {
                            error = "Authentication failed. Please check credentials."
                        } finally {
                            loading = false
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New here? ", color = Color.Gray)
                    Text(
                        "Create account",
                        color = Color(0xFF42B6D9),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSignupClick() }
                    )
                }
            }
        }
    }
}

@Composable
fun SignupDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    onBackToLogin: () -> Unit,
    onOtpSent: (String) -> Unit   // ✅ NEW
) {
    if (!open) return
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .blur(20.dp)
            )

            AuthCard {
                AuthHeader(
                    title = "Join Us",
                    subtitle = "Enter email to receive OTP",
                    onBack = onBackToLogin,
                    onClose = onDismiss
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    singleLine = true
                )

                error?.let {
                    Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                }

                PrimaryButton("Send OTP", loading) {
                    scope.launch {
                        try {
                            loading = true
                            AuthService.api.sendRegisterOtp(
                                EmailRequest(email.trim())
                            )
                            onOtpSent(email.trim())   // ✅ MOVE TO NEXT STEP
                        } catch (e: Exception) {
                            error = "Failed to send OTP"
                        } finally {
                            loading = false
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ForgotPasswordDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    onBackToLogin: () -> Unit,
    onOtpSent: (String) -> Unit   // ✅ NEW
) {
    if (!open) return
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .blur(20.dp)
            )

            AuthCard {
                AuthHeader(
                    title = "Reset Password",
                    subtitle = "We'll send an OTP to your email",
                    onBack = onBackToLogin,
                    onClose = onDismiss
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    singleLine = true
                )

                error?.let {
                    Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                }

                PrimaryButton("Send Recovery OTP", loading) {
                    scope.launch {
                        try {
                            loading = true
                            AuthService.api.sendResetOtp(
                                EmailRequest(email.trim())
                            )
                            onOtpSent(email.trim())   // ✅ MOVE TO NEXT STEP
                        } catch (e: Exception) {
                            error = "Failed to send OTP"
                        } finally {
                            loading = false
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun SignupFinalDialog(
    open: Boolean,
    email: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    if (!open) return
    val scope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .blur(20.dp)
            )

            AuthCard {
                AuthHeader(
                    title = "Complete Signup",
                    subtitle = "Enter OTP and details",
                    onClose = onDismiss
                )

                OutlinedTextField(firstName, { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                OutlinedTextField(lastName, { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                OutlinedTextField(otp, { otp = it },
                    label = { Text("OTP") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                OutlinedTextField(password, { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                error?.let {
                    Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                }

                PrimaryButton("Create Account", loading) {
                    scope.launch {

                        val otpInt = otp.toIntOrNull()
                        if (otpInt == null) {
                            error = "Invalid OTP"
                            return@launch
                        }
                        if (password.length < 8) {
                            error = "Password must be at least 8 characters"
                            return@launch
                        }
                        try {
                            loading = true
                            AuthService.api.register(
                                RegisterRequest(
                                    email = email,
                                    firstName = firstName,
                                    lastName = lastName,
                                    otp = otpInt,
                                    password = password
                                )
                            )
                            onSuccess()
                        } catch (e: Exception) {
                            error = "Signup failed. Check OTP/details."
                        } finally {
                            loading = false
                        }
                    }
                }

            }
        }
    }
}
@Composable
fun LoginScreen(navController: NavController) {

    var showLogin by remember { mutableStateOf(true) }
    var showSignup by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }
    var showSignupFinal by remember { mutableStateOf(false) }
    var showResetFinal by remember { mutableStateOf(false) }

    var emailForOtp by remember { mutableStateOf("") }

    // Close screen → go back
    val closeAll: () -> Unit = {
        showLogin = false
        showSignup = false
        showForgot = false
        showSignupFinal = false
        showResetFinal = false
        navController.popBackStack()
        Unit   // 👈 IMPORTANT
    }


    /* ---------- LOGIN ---------- */
    LoginDialog(
        open = showLogin,
        onDismiss = closeAll,
        onLoginSuccess = {
            closeAll()
        },
        onSignupClick = {
            showLogin = false
            showSignup = true
        },
        onForgotPasswordClick = {
            showLogin = false
            showForgot = true
        }
    )

    /* ---------- SIGNUP EMAIL ---------- */
    SignupDialog(
        open = showSignup,
        onDismiss = closeAll,
        onBackToLogin = {
            showSignup = false
            showLogin = true
        },
        onOtpSent = { email ->
            emailForOtp = email
            showSignup = false
            showSignupFinal = true
        }
    )

    /* ---------- SIGNUP FINAL ---------- */
    SignupFinalDialog(
        open = showSignupFinal,
        email = emailForOtp,
        onDismiss = closeAll,
        onSuccess = {
            closeAll()
        }
    )

    /* ---------- FORGOT PASSWORD ---------- */
    ForgotPasswordDialog(
        open = showForgot,
        onDismiss = closeAll,
        onBackToLogin = {
            showForgot = false
            showLogin = true
        },
        onOtpSent = { email ->
            emailForOtp = email
            showForgot = false
            showResetFinal = true
        }
    )

    /* ---------- RESET FINAL ---------- */
    ResetPasswordFinalDialog(
        open = showResetFinal,
        email = emailForOtp,
        onDismiss = closeAll,
        onSuccess = {
            closeAll()
        }
    )
}

@Composable
fun ResetPasswordFinalDialog(
    open: Boolean,
    email: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    if (!open) return
    val scope = rememberCoroutineScope()

    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .blur(20.dp)
            )

            AuthCard {
                AuthHeader(
                    title = "Reset Password",
                    subtitle = "Enter OTP and new password",
                    onClose = onDismiss
                )

                OutlinedTextField(
                    otp, { otp = it },
                    label = { Text("OTP") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                OutlinedTextField(
                    newPassword, { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                error?.let {
                    Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                }
                PrimaryButton("Reset Password", loading) {
                    scope.launch {

                        val otpInt = otp.toIntOrNull()
                        if (otpInt == null) {
                            error = "Invalid OTP"
                            return@launch
                        }

                        if (newPassword.length < 8) {
                            error = "Password must be at least 8 characters"
                            return@launch
                        }

                        try {
                            loading = true
                            AuthService.api.resetPassword(
                                ResetPasswordRequest(
                                    email = email,
                                    otp = otpInt,
                                    newPassword = newPassword
                                )
                            )
                            onSuccess()
                        } catch (e: Exception) {
                            error = "Reset failed. Check OTP."
                        } finally {
                            loading = false
                        }
                    }
                }
            }
        }
    }
}


