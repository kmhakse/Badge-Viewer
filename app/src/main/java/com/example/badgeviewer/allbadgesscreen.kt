package com.example.badgeviewer
import androidx.compose.material.icons.filled.Lock
import android.content.Context
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.layout.ContentScale
import retrofit2.http.Header

/* ===================== TOP BAR ===================== */
data class BadgeDto(
    val id: Int,
    val name: String,
    val description: String,
    val image: String? = null,
    val category: String? = null,
    val level: String? = null,
    val vertical: String? = null,
    val holders: Int,
    val yearLaunched: Int
    )
@Composable
fun BadgesTopBar(
    navController: NavController,
    profileImage: String?, // ✅ Add this
    onMenuClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    val isLoggedIn = prefs.getString("accessToken", null) != null
    val name = prefs.getString("name", "U") ?: "U"
    val initials = name.take(2).uppercase()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF02060C), Color(0xFF00040A))))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, null, tint = Color.White)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isLoggedIn) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape) // Ensure clipping for the image
                        .background(Color(0xFF2F80FF), CircleShape)
                        .clickable { navController.navigate("profile") },
                    contentAlignment = Alignment.Center
                ) {
                    // ✅ Profile Image Logic
                    if (profileImage != null) {
                        AsyncImage(
                            model = "https://profile.deepcytes.io/api$profileImage?t=${System.currentTimeMillis()}",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(text = initials, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onLoginClick,
                    border = BorderStroke(1.dp, Color(0xFF04D9FF)),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Login", fontSize = 14.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF04D9FF))
                .shadow(6.dp, ambientColor = Color(0xFF04D9FF))
        )
    }
}


/* ===================== HEADER ===================== */

@Composable
fun AchievementsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Available", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text("Achievements", color = Color(0xFF04D9FF), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)

        Spacer(Modifier.height(12.dp))

        Text(
            "Complete challenges and earn badges to\nshowcase your cybersecurity skills",
            color = Color.White.copy(0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/* ===================== DATA ===================== */


data class BadgesResponse(val badges: List<BadgeDto>)
data class MyBadgesResponse(val badges: List<BadgeDto>)

/* ===================== API ===================== */

interface BadgeApi {
    @GET("badges") suspend fun getAllBadges(): BadgesResponse
    @GET("badges-earned") suspend fun getMyBadges(@Header("Authorization") token: String): MyBadgesResponse
}

object ApiClient {
    private const val BASE_URL = "https://profile.deepcytes.io/api/"
    val api: BadgeApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BadgeApi::class.java)
}

/* ===================== SCREEN ===================== */

@Composable
fun AllBadgesScreen(
    navController: NavController,
    context: Context = LocalContext.current
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var allBadges by remember { mutableStateOf<List<BadgeDto>>(emptyList()) }
    var myBadges by remember { mutableStateOf<List<BadgeDto>>(emptyList()) }
    var profileImage by remember { mutableStateOf<String?>(null) } // ✅ State for profile
    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val token = prefs.getString("accessToken", null)

    LaunchedEffect(Unit) {
        try {
            loading = true

            // 1. Fetch Public Data (Always)
            allBadges = ApiClient.api.getAllBadges().badges

            // 2. Fetch Private Data (Only if logged in)
            if (!token.isNullOrEmpty()) {
                val bearerToken = "Bearer $token"

                // Fetch User profile (for the TopBar image)
                try {
                    val user = ProfileApiClient.api.getUser(bearerToken)
                    profileImage = user.image
                } catch (e: Exception) {
                    Log.e("FETCH_PROFILE", "Failed to load profile image", e)
                }

                // Fetch Earned Badges
                myBadges = ApiClient.api.getMyBadges(bearerToken).badges
            }

        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                prefs.edit().clear().apply()
                navController.navigate("home") { popUpTo(0) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loading = false
        }
    }


    val ownedIds = myBadges.map { it.id }.toSet()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            BadgesDrawerContent(
                onHomeClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("home")
                },
                onBadgesClick = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF000005), Color(0xFF020B12), Color(0xFF000005))
                    )
                )
        ) {

            if (loading) {
                CircularProgressIndicator(
                    color = Color(0xFF04D9FF),
                    modifier = Modifier.align(Alignment.Center)
                )
                return@ModalNavigationDrawer
            }

            Column {

                BadgesTopBar(
                    navController = navController,
                    profileImage = profileImage, // ✅ Pass the URL
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onLoginClick = { navController.navigate("login") { launchSingleTop = true } }
                )



                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    contentPadding = PaddingValues(bottom = 60.dp)
                ) {

                    item { AchievementsHeader() }

                    val sortedBadges = allBadges.sortedByDescending { ownedIds.contains(it.id) }

                    items(sortedBadges) { badge ->
                        BadgeCardWebStyle(
                            badge = badge,
                            owned = ownedIds.contains(badge.id),
                            onClick = {
                                navController.navigate("badge/${badge.id}")
                            }
                        )
                    }



                    item {
                        Spacer(modifier = Modifier.height(32.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = { navController.navigate("home") },
                                shape = RoundedCornerShape(6.dp), // 🔹 rectangle (slight curve)
                                border = BorderStroke(1.dp, Color(0xFF04D9FF)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF04D9FF)
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(180.dp) // 🔹 fixed width like web
                            ) {
                                Text(
                                    text = "Back to Home",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(28.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF04D9FF))
                                .shadow(
                                    elevation = 8.dp,
                                    ambientColor = Color(0xFF04D9FF),
                                    spotColor = Color(0xFF04D9FF)
                                )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }


                    item {
                        Spacer(Modifier.height(40.dp))
                        BadgesFooterSection()
                    }
                }
            }
        }
    }
}

/* ===================== BADGE CARD ===================== */

@Composable
fun BadgeCardWebStyle(
    badge: BadgeDto,
    owned: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF05070C))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(24.dp)
            )
            .then(
                if (owned) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(vertical = 28.dp, horizontal = 20.dp)
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ===== BADGE IMAGE + LOCK OVERLAY =====
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {

                AsyncImage(
                    model = "https://profile.deepcytes.io/api/badge/images/${badge.id}",
                    contentDescription = badge.name,
                    modifier = Modifier
                        .size(96.dp)
                        .then(if (!owned) Modifier.blur(6.dp) else Modifier),
                    colorFilter = if (!owned)
                        ColorFilter.tint(Color.Gray, BlendMode.SrcIn)
                    else null
                )

                // 🔒 LOCK OVERLAY
                if (!owned) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(42.dp)
                        )

                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = badge.name,
                color = if (owned) Color.White else Color.White.copy(0.35f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = badge.description,
                color = Color.White.copy(if (owned) 0.65f else 0.25f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(14.dp))

            // ===== TAG / LOCK LABEL =====
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (owned) Color(0xFF0E2B3A)
                        else Color.White.copy(0.08f)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (owned) "Cyber Titan" else "Locked",
                    color = if (owned) Color(0xFF04D9FF)
                    else Color.White.copy(0.4f),
                    fontSize = 13.sp
                )
            }
        }
    }
}



/* ===================== LOCK ===================== */

@Composable
fun LockOverlay() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(Color(0xFF04D9FF), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp, 14.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
        )
    }
}

/* ===================== DRAWER ===================== */
@Composable
fun BadgesDrawerContent(
    onHomeClick: () -> Unit,
    onBadgesClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
    ) {

        // 🔹 BLURRED BACKGROUND LAYER
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFF0D131C).copy(alpha = 0.6f))
                .blur(25.dp)
        )

        // 🔹 CONTENT LAYER (NO BLUR)
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(24.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DeepCytes",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onHomeClick() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            DrawerItem("Home", onHomeClick)
            DrawerItem("Badges", onBadgesClick)
        }
    }
}


@Composable
fun BadgesDrawerItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

/* ===================== FOOTER ===================== */

@Composable
fun BadgesFooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B0F14),
                        Color(0xFF020609)
                    )
                )
            )
            .padding(
                start = 10.dp,
                end = 14.dp,
                top = 8.dp,     // 🔹 KEY FIX
                bottom = 12.dp
            )

    ) {

        // ───── LOGO ─────
        Image(
            painter = painterResource(R.drawable.dc),
            contentDescription = "DeepCytes Logo",
            modifier = Modifier.height(48.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ───── RESOURCES / FOLLOW US ─────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {

            Column(
                modifier = Modifier.width(160.dp) // 🔹 controls exact spacing
            ) {
                FooterTitle("RESOURCES")
                Spacer(Modifier.height(10.dp))
                FooterItem("About")
                Spacer(Modifier.height(6.dp))
                FooterItem("Career")
            }

            Spacer(modifier = Modifier.width(56.dp)) // 🔹 precise gap like web

            Column {
                FooterTitle("FOLLOW US")
                Spacer(Modifier.height(10.dp))
                FooterItem("Website")
            }
        }


        Spacer(modifier = Modifier.height(28.dp))

        // ───── LEGAL ─────
        FooterTitle("LEGAL")
        Spacer(Modifier.height(10.dp))
        FooterItem("Privacy Policy")
        Spacer(Modifier.height(6.dp))
        FooterItem("Terms & Conditions")

        Spacer(modifier = Modifier.height(28.dp))

        Divider(
            color = Color.White.copy(alpha = 0.12f),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ───── COPYRIGHT ─────
        Text(
            text = "© 2026 DeepCytes. All Rights Reserved.",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ───── SOCIAL ICONS ─────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SocialIcon(R.drawable.instagram)
            Spacer(Modifier.width(18.dp))
            SocialIcon(R.drawable.twitter)
            Spacer(Modifier.width(18.dp))
            SocialIcon(R.drawable.globe)

        }
    }
}
