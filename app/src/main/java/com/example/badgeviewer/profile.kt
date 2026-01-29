package com.example.badgeviewer

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/* ===================== API MODELS ===================== */

data class UserResponse(
    val firstName: String,
    val lastName: String,
    val email: String,
    val image: String?,
    val badges: List<UserBadge>,
    val emailPreferences: EmailPreferences?
)
data class EmailPreferences(
    val badgeReceived: Boolean,
    val profileUpdate: Boolean,
    val adminDaily: Boolean
)
data class UserBadge(
    val badgeId: Int,
    val name: String? = null,
    var isPublic: Boolean = true,
    val earnedDate: String? = null,
    val certificateId: String? = null
)


/* ===================== API ===================== */

interface ProfileApi {
    @GET("user")
    suspend fun getUser(@Header("Authorization") token: String): UserResponse

    @GET("badges")
    suspend fun getAllBadges(): BadgesResponse

    @GET("badge/earners/{id}")
    suspend fun getEarners(@Path("id") id: Int): EarnersResponse
}

object ProfileApiClient {
    private const val BASE_URL = "https://profile.deepcytes.io/api/"
    val api: ProfileApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ProfileApi::class.java)
}

/* ===================== SCREEN ===================== */

@Composable
fun ProfileScreen(navController: NavController) {

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val token = prefs.getString("accessToken", null)

    var user by remember { mutableStateOf<UserResponse?>(null) }
    var allBadges by remember { mutableStateOf<List<BadgeDto>>(emptyList()) }
    var selectedBadgeId by remember { mutableStateOf<Int?>(null) }
    var earners by remember { mutableStateOf("—") }
    var loading by remember { mutableStateOf(true) }

    /* ---------- FETCH USER ---------- */
    /* ---------- FETCH USER ---------- */
    LaunchedEffect(navController.currentBackStackEntry) {

        if (token.isNullOrEmpty()) {
            navController.navigate("home") {
                popUpTo(0)
            }
            return@LaunchedEffect
        }

        try {
            user = ProfileApiClient.api.getUser("Bearer $token")
            allBadges = ProfileApiClient.api.getAllBadges().badges
            selectedBadgeId = user?.badges?.firstOrNull()?.badgeId
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loading = false
        }
    }


    /* ---------- FETCH EARNERS ---------- */
    LaunchedEffect(selectedBadgeId) {
        selectedBadgeId?.let {
            earners = try {
                ProfileApiClient.api.getEarners(it).earners.toString()
            } catch (e: Exception) {
                "—"
            }
        }
    }

    if (loading || user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF38C8F8))
        }
        return
    }

    val selectedBadge = allBadges.find { it.id == selectedBadgeId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F1117), Color(0xFF000000))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        /* ================= PROFILE CARD ================= */
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF38C8F8), Color(0xFF8B5CF6))
                            ),
                            CircleShape
                        )
                        .padding(3.dp)
                        .background(Color(0xFF1A1D24), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (user!!.image != null) {
                        AsyncImage(
                            model = "https://profile.deepcytes.io/api${user!!.image}?t=${System.currentTimeMillis()}",

                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            "${user!!.firstName.first()}${user!!.lastName.first()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "${user!!.firstName} ${user!!.lastName}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    user!!.email,
                    color = Color.White.copy(0.5f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(16.dp))

                /* ---------- EDIT PROFILE ---------- */
                Button(
                    onClick = { navController.navigate("edit-profile") },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38C8F8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Profile", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                /* ---------- LOGOUT BUTTON ---------- */
                OutlinedButton(
                    onClick = {
                        handleLogout(context, navController)
                    },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.Red),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))

                Divider(color = Color.White.copy(0.1f))
                Spacer(Modifier.height(16.dp))

                /* ---------- MY BADGES ---------- */
                Text(
                    "MY BADGES",
                    color = Color.White.copy(0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(user!!.badges) { b ->
                        AsyncImage(
                            model = "https://profile.deepcytes.io/api/badge/images/${b.badgeId}",
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    if (b.badgeId == selectedBadgeId) 3.dp else 1.dp,
                                    if (b.badgeId == selectedBadgeId)
                                        Color(0xFF38C8F8)
                                    else Color.White.copy(0.1f),
                                    CircleShape
                                )
                                .clickable { selectedBadgeId = b.badgeId }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        /* ================= BADGE DETAILS ================= */
        selectedBadge?.let { badge ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF12141C), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                AsyncImage(
                    model = "https://profile.deepcytes.io/api/badge/images/${badge.id}",
                    contentDescription = null,
                    modifier = Modifier.size(180.dp)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    badge.name.uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF38C8F8),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    badge.description,
                    color = Color.White.copy(0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BadgeMetric("Status", "Professional")
                    BadgeMetric("Earners", earners)
                    BadgeMetric("Type", "General")
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        BadgesFooterSection()
    }
}

/* ===================== LOGOUT ===================== */

fun handleLogout(context: Context, navController: NavController) {
    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    navController.navigate("home") {
        popUpTo(0)
        launchSingleTop = true
    }
}

/* ===================== HELPERS ===================== */

@Composable
fun BadgeMetric(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 11.sp, color = Color.White.copy(0.5f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
