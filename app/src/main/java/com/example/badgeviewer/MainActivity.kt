package com.example.badgeviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(color = Color(0xFF00040A)) {
               MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        // ───── HOME ─────
        composable("home") {
            HomeScreen(navController)
        }

        // ───── ALL BADGES ─────
        composable("badges") {
            AllBadgesScreen(navController = navController)
        }
        // ───── INDIVIDUAL BADGE ─────
        composable("badge/{id}") {
            IndividualBadgeScreen(navController = navController)
        }
        composable("profile") {   // ✅ NEW
            ProfileScreen(navController)
        }
        composable("edit-profile") {
            EditProfileScreen(navController)
        }
        composable("login") {
            LoginScreen(navController)
        }


    }
}
