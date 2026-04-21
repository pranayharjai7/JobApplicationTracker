package com.pranay.jobtracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.pranay.jobtracker.security.BiometricAuthenticator
import com.pranay.jobtracker.security.SecurityManager
import com.pranay.jobtracker.ui.ApplicationDetailScreen
import com.pranay.jobtracker.ui.DashboardScreen
import com.pranay.jobtracker.ui.theme.JobApplicationTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        var showSplash by mutableStateOf(true)
        var isLocked by mutableStateOf(false)
        
        // Initial lock check
        if (securityManager.isBiometricEnabled()) {
            isLocked = true
        }
        
        // Keep the system splash on screen slightly longer to hide the Compose bootstrap
        splashScreen.setKeepOnScreenCondition { false } 
        
        setContent {
            JobApplicationTrackerTheme {
                val alpha = remember { Animatable(1f) }
                
                LaunchedEffect(Unit) {
                    delay(2020) // Stay for exactly 2.02s
                    alpha.animateTo(0f, animationSpec = tween(600))
                    showSplash = false
                    
                    if (isLocked) {
                        BiometricAuthenticator(this@MainActivity).authenticate(
                            onSuccess = { isLocked = false },
                            onError = { _, _ -> /* Stay locked or offer retry */ }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Main App Content
                    if (!isLocked) {
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
                    } else {
                        // Blurred or placeholder state when locked
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = {
                                BiometricAuthenticator(this@MainActivity).authenticate(
                                    onSuccess = { isLocked = false },
                                    onError = { _, _ -> }
                                )
                            }) {
                                Text("Unlock JobTrack")
                            }
                        }
                    }

                    // Animated GIF Overlay
                    if (showSplash) {
                        val imageLoader = ImageLoader.Builder(this@MainActivity)
                            .components {
                                if (android.os.Build.VERSION.SDK_INT >= 28) {
                                    add(ImageDecoderDecoder.Factory())
                                } else {
                                    add(GifDecoder.Factory())
                                }
                            }
                            .build()

                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(alpha = alpha.value)
                                .testTag("splash_screen"),
                            color = Color(0xFFFDFDFA)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = rememberAsyncImagePainter(
                                        model = R.raw.jobtrack_splash,
                                        imageLoader = imageLoader
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(300.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
