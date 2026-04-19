package com.pranay.jobtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pranay.jobtracker.ui.ApplicationDetailScreen
import com.pranay.jobtracker.ui.DashboardScreen
import com.pranay.jobtracker.ui.theme.JobApplicationTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Custom exit animation for the splash screen
        splashScreen.setOnExitAnimationListener { splashProvider ->
            val fadeOut = android.view.animation.AlphaAnimation(1f, 0f).apply {
                duration = 600
                fillAfter = true
            }
            splashProvider.view.startAnimation(fadeOut)
            splashProvider.remove()
        }
        
        setContent {
            JobApplicationTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                onApplicationClick = { id ->
                                    navController.navigate("detail/$id")
                                }
                            )
                        }
                        composable(
                            "detail/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType })
                        ) {
                            ApplicationDetailScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
