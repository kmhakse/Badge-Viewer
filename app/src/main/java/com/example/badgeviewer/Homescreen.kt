package com.example.badgeviewer

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/* ================= API ================= */

object HomeBadgeApi {
    private const val BASE_URL = "https://profile.deepcytes.io/api/"

    val api: BadgeDetailApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BadgeDetailApi::class.java)
}

/* ================= DATA MODELS ================= */

data class Program(
    val title: String,
    val desc: String,
    val image: Int
)

data class WhyJoin(
    val icon: String,
    val title: String,
    val desc: String
)

/* ================= HOME SCREEN ================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    var profileImage by remember { mutableStateOf<String?>(null) }   // ⭐ ADD THIS

    var isLoggedIn by remember {
        mutableStateOf(prefs.getString("accessToken", null) != null)
    }

    var initials by remember {
        mutableStateOf(
            prefs.getString("name", "U")!!.take(2).uppercase()
        )
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            try {
                val token = prefs.getString("accessToken", null)
                val user = ProfileApiClient.api.getUser("Bearer $token")
                profileImage = user.image
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var retryTrigger by remember { mutableStateOf(0) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showSignupDialog by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var badges by remember { mutableStateOf<List<BadgeDto>>(emptyList()) }
    var selectedBadge by remember { mutableStateOf<BadgeDto?>(null) }
    var uiState by remember { mutableStateOf<UiState>(UiState.Loading) }
    var tempEmail by remember { mutableStateOf("") }
    var showSignupFinalDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }







    val whyJoinList = listOf(
        WhyJoin("🏆", "Showcase Skills", "Display your verified cybersecurity skills."),
        WhyJoin("🚀", "Career Growth", "Advance your career with advanced badges."),
        WhyJoin("🔍", "Validate Expertise", "Prove your capabilities through challenges."),
        WhyJoin("🌐", "Join Community", "Connect with cybersecurity professionals.")
    )

    LaunchedEffect(retryTrigger) {
        try {
            val response = HomeBadgeApi.api.getAllBadges()
            badges = response.badges
            selectedBadge = badges.firstOrNull()
            uiState = UiState.Success
        } catch (e: Exception) {
            uiState = UiState.Error("Please connect to the internet")
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onHomeClick = { scope.launch { drawerState.close() } },
                onBadgesClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("badges")
                }
            )
        }
    ) {
        when (uiState) {
            UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF020609)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00D4FF))
                }
            }

            is UiState.Error -> {
                NoInternetScreen { uiState = UiState.Loading; retryTrigger++ }
            }

            UiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFF020609))) {

                    /* ================= HERO ================= */
                    /* ================= HERO ================= */
                    item {
                        TopHeaderBar(
                            navController = navController,
                            isLoggedIn = isLoggedIn,
                            initials = initials,
                            profileImage = profileImage,   // ✅ ADD THIS LINE
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onLoginClick = { showLoginDialog = true }
                        )


                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.img_3),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color(0xFF020609))
                                        )
                                    )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Become a part of\nDeepcytes",
                                fontSize = 39.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 38.sp
                            )

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = "Join our mission to build a cross\nborder cybersafe force together",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            )
                        }
                    }


                    /* ================= PROGRAMS ================= */
                    item { SectionTitle("Programs available") }
                    val programs = listOf(
                        Program(
                            "Fellowship Programs",
                            "A comprehensive program for aspiring cybersecurity professionals.",
                            R.drawable.img
                        ),
                        Program(
                            "Cyber Titan",
                            "Cyber Titan is an initiative dedicated to fortify 65,000+ schools.",
                            R.drawable.img_1
                        ),
                        Program(
                            "Cyber Warrior",
                            "A foundational program designed to build core cyber defense skills.",
                            R.drawable.img_2
                        )
                    )
                    items(programs) { ProgramCard(it); Spacer(Modifier.height(24.dp)) }

                    /* ================= ACHIEVEMENTS ================= */
                    item {
                        SectionTitle("Achievements")
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            selectedBadge?.let {
                                AsyncImage(
                                    model = "https://profile.deepcytes.io/api/badge/images/${it.id}",
                                    contentDescription = it.name,
                                    modifier = Modifier.size(180.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    it.name,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    it.description,
                                    color = Color.White.copy(0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )

                                Spacer(Modifier.height(20.dp))

                                // Centered StatBoxes
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                                ) {
                                    StatBox(value = it.holders.toString(), label = "Holders")
                                    Spacer(Modifier.width(16.dp))
                                    StatBox(
                                        value = it.yearLaunched.toString(),
                                        label = "Year Launched"
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(badges) { badge ->
                                    val isSelected = badge.id == selectedBadge?.id
                                    Card(
                                        modifier = Modifier.size(96.dp)
                                            .clickable { selectedBadge = badge },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(Color(0xFF0D131C)),
                                        border = BorderStroke(
                                            2.dp,
                                            if (isSelected) Color(0xFF00D4FF) else Color.White.copy(
                                                0.05f
                                            )
                                        )
                                    ) {
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = "https://profile.deepcytes.io/api/badge/images/${badge.id}",
                                                contentDescription = badge.name,
                                                modifier = Modifier.size(56.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    /* ================= WHY JOIN US ================= */
                    item { SectionTitle("Why Join Us?") }

                    items(whyJoinList) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(Color(0xFF0D131C))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(24.dp), // Added fillMaxWidth here
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(item.icon, fontSize = 40.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    item.title,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    item.desc,
                                    color = Color.White.copy(0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(40.dp))
                        BadgesFooterSection()
                    }
                }
            }
        }

        LoginDialog(
            open = showLoginDialog,
            onDismiss = { showLoginDialog = false },
            onLoginSuccess = {
                isLoggedIn = true
                initials = prefs.getString("name", "U")!!.take(2).uppercase()
            },
            onSignupClick = {
                showLoginDialog = false
                showSignupDialog = true
            },
            onForgotPasswordClick = {
                showLoginDialog = false
                showForgotDialog = true
            }
        )


        SignupDialog(
            open = showSignupDialog,
            onDismiss = { showSignupDialog = false },
            onBackToLogin = {
                showSignupDialog = false
                showLoginDialog = true
            },
            onOtpSent = { email ->
                tempEmail = email
                showSignupDialog = false
                showSignupFinalDialog = true
            }
        )

        ForgotPasswordDialog(
            open = showForgotDialog,
            onDismiss = { showForgotDialog = false },
            onBackToLogin = {
                showForgotDialog = false
                showLoginDialog = true
            },
            onOtpSent = { email ->
                tempEmail = email
                showForgotDialog = false
                showResetPasswordDialog = true
            }
        )
        SignupFinalDialog(
            open = showSignupFinalDialog,
            email = tempEmail,
            onDismiss = { showSignupFinalDialog = false },
            onSuccess = {
                showSignupFinalDialog = false
                showLoginDialog = true
            }
        )

        ResetPasswordFinalDialog(
            open = showResetPasswordDialog,
            email = tempEmail,
            onDismiss = { showResetPasswordDialog = false },
            onSuccess = {
                showResetPasswordDialog = false
                showLoginDialog = true
            }
        )




    }
}


@Composable
fun ProgramCard(program: Program) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(Color(0xFF0D131C)),
        border = BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Column {
            Image(
                painter = painterResource(program.image),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(program.title, color = Color(0xFF8EEBFF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(program.desc, color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
            }
        }
    }
}
@Composable
fun NoInternetScreen(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020609)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {

            // 📡 Emoji / Icon
            Text(
                text = "📡",
                fontSize = 48.sp
            )

            Spacer(Modifier.height(12.dp))

            // Main message
            Text(
                text = "No Internet Connection",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Sub message
            Text(
                text = "Please check your connection\nand try again",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Retry button
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D4FF)
                )
            ) {
                Text(
                    text = "Retry",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color(0xFF00D4FF),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center, // Centers the text alignment
        modifier = Modifier
            .fillMaxWidth() // Necessary so the text has "room" to be centered
            .padding(
                top = 32.dp,
                bottom = 16.dp,
                start = 16.dp,  // Use start instead of horizontal
                end = 16.dp
    ))
}
/* ================= DRAWER ================= */
@Composable
fun StatBox(value: String, label: String) {
    Card(
        modifier = Modifier.width(160.dp), // Increased slightly for better spacing
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color(0xFF161F2C)),
        border = BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth() // Ensures the column takes full width of the card
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, // Centers children horizontally
            verticalArrangement = Arrangement.Center           // Centers children vertically
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White.copy(0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun TopHeaderBar(
    navController: NavController,
    isLoggedIn: Boolean,
    initials: String,
    profileImage: String?,
    onMenuClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF050A12), Color(0xFF020609))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onMenuClick) {
                Icon(
                    painter = painterResource(R.drawable.img_6),
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(8.dp))

            Image(
                painter = painterResource(R.drawable.dc),
                contentDescription = null,
                modifier = Modifier.height(32.dp)
            )

            Spacer(Modifier.weight(1f))

            // 🔥 CORRECT LOGIC
            if (isLoggedIn) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { navController.navigate("profile") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImage != null) {
                        AsyncImage(
                            model = "https://profile.deepcytes.io/api$profileImage?t=${System.currentTimeMillis()}",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2F80FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onLoginClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF00D4FF))
                ) {
                    Text("Login", color = Color.White)
                }
            }
        }

        Divider(color = Color(0xFF00D4FF), thickness = 1.dp)
    }
}



            /* ================= BUTTONS ================= */


@Composable
fun DrawerContent(
    onHomeClick: () -> Unit,
    onBadgesClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
    ) {

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFF0D131C).copy(alpha = 0.6f))
                .blur(25.dp)
        )

        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(24.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
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

            Spacer(Modifier.height(40.dp))

            DrawerItem("Home", onHomeClick)
            DrawerItem("Badges", onBadgesClick)
        }
    }
}
@Composable
fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0F14), Color(0xFF020609))
                )
            )
            .padding(24.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.dc),
            contentDescription = null,
            modifier = Modifier.height(36.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "© 2026 DeepCytes. All Rights Reserved.",
            color = Color.White.copy(0.5f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}


/* ================= SMALL COMPOSABLES ================= */

@Composable
fun DrawerItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    )
}
