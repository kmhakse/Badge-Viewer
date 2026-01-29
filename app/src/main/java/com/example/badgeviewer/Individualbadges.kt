package com.example.badgeviewer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path


/* ===================== API ===================== */

data class EarnersResponse(val earners: Int)

interface BadgeDetailApi {
    @GET("badges")
    suspend fun getAllBadges(): BadgesResponse

    @GET("badge/earners/{id}")
    suspend fun getEarners(@Path("id") id: Int): EarnersResponse
}

object BadgeDetailApiClient {
    private const val BASE_URL = "https://profile.deepcytes.io/api/"
    val api: BadgeDetailApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BadgeDetailApi::class.java)
}

/* ===================== SCREEN ===================== */
@Composable
fun IndividualBadgeScreen(navController: NavController) {

    val backStackEntry = navController.currentBackStackEntryAsState()
    val startId = backStackEntry.value?.arguments?.getString("id")?.toIntOrNull() ?: return

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var badges by remember { mutableStateOf<List<BadgeDto>>(emptyList()) }
    var index by remember { mutableStateOf(0) }
    var earners by remember { mutableStateOf("N/A") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(startId) {
        try {
            loading = true

            // 1️⃣ Fetch all badges
            val response = BadgeDetailApiClient.api.getAllBadges()
            badges = response.badges

            index = badges.indexOfFirst { it.id == startId }
                .takeIf { it >= 0 } ?: 0

            // 2️⃣ Fetch earners safely
            earners = try {
                BadgeDetailApiClient.api
                    .getEarners(badges[index].id)
                    .earners
                    .toString()
            } catch (e: Exception) {
                "N/A"
            }

        } catch (e: retrofit2.HttpException) {

            // 🔐 TOKEN EXPIRED / UNAUTHORIZED
            if (e.code() == 401) {
                navController.navigate("home") {
                    popUpTo(0)
                }
            }

        } catch (e: java.net.SocketTimeoutException) {
            // 🌐 NETWORK TOO SLOW
            earners = "—"

        } catch (e: Exception) {
            // 🧯 ANY OTHER FAILURE
            e.printStackTrace()

        } finally {
            loading = false
        }
    }


    if (loading || badges.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF04D9FF))
        }
        return
    }

    val badge = badges[index]

    /* ───────── DRAWER WRAPPER ───────── */
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
                    navController.navigate("badges")
                }
            )
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF000005), Color(0xFF020B12), Color(0xFF000005))
                    )
                )
        ) {

            /* ───── HEADER (SAME AS ALL BADGES) ───── */
            BadgesTopBar(
                navController = navController,
                onMenuClick = { scope.launch { drawerState.open() } },
                onLoginClick = {
                    navController.navigate("login") {
                        launchSingleTop = true
                    }
                }

            )


            Divider(color = Color(0xFF04D9FF), thickness = 1.dp)

            /* ───── CONTENT ───── */
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                /* ───── HERO ───── */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF0A1B2A), Color.Transparent)
                                ),
                                CircleShape
                            )
                    )

                    AsyncImage(
                        model = "https://profile.deepcytes.io/api/badge/images/${badge.id}",
                        contentDescription = badge.name,
                        modifier = Modifier.size(220.dp)
                    )

                    Icon(
                        Icons.Default.ArrowBack,
                        null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(0.4f))
                            .clickable { if (index > 0) index-- }
                            .padding(10.dp)
                    )

                    Icon(
                        Icons.Default.ArrowBack,
                        null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .graphicsLayer { rotationZ = 180f }
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(0.4f))
                            .clickable { if (index < badges.lastIndex) index++ }
                            .padding(10.dp)
                    )

                    Text(
                        "${index + 1} of ${badges.size}",
                        color = Color.White.copy(0.7f),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    badge.name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(32.dp))

                MetricCard("LEVEL", "Professional")
                Spacer(Modifier.height(14.dp))
                MetricCard("EARNERS", earners)
                Spacer(Modifier.height(14.dp))
                MetricCard("VERTICAL", "General")

                Spacer(Modifier.height(28.dp))

                SectionCard("Course") {
                    Text("No course linked to this badge.", color = Color.White.copy(0.7f))
                }

                Spacer(Modifier.height(20.dp))

                SectionCard("Badge Details") {
                    Text(badge.description, color = Color.White.copy(0.75f))
                }

                Spacer(Modifier.height(28.dp))

                /* ───── SKILLS EARNED ───── */
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.img4),
                        contentDescription = null,
                        tint = Color(0xFF04D9FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Skills Earned",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Divider(
                    color = Color(0xFF04D9FF),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                SkillChipWithIcon("Cyber Titan")

                Spacer(Modifier.height(28.dp))

                /* ───── RELATED BADGES ───── */
                Text(
                    "Related Badges",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Divider(
                    color = Color(0xFF04D9FF),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(badges.filter { it.id != badge.id }.take(3)) { _, b ->

                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0A1018))
                                .clickable {
                                    val newIndex = badges.indexOfFirst { it.id == b.id }
                                    if (newIndex >= 0) {
                                        index = newIndex
                                    }
                                }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            AsyncImage(
                                model = "https://profile.deepcytes.io/api/badge/images/${b.id}",
                                contentDescription = b.name,
                                modifier = Modifier.size(68.dp)
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                b.name,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
                BadgesFooterSection()
            }
        }
    }
}

/* ===================== HELPERS ===================== */

@Composable
fun MetricCard(title: String, value: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(92.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1F2D), Color(0xFF050C12))
                )
            )
            .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 12.sp, color = Color.White.copy(0.55f))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun SkillChipWithIcon(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF0E2B3A))
            .border(1.dp, Color(0xFF04D9FF), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.img4),
            contentDescription = null,
            tint = Color(0xFF04D9FF),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color(0xFF04D9FF), fontSize = 14.sp)
    }
}
