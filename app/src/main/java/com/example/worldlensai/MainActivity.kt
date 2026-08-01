package com.example.worldlensai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.worldlensai.data.remote.GeminiRepository
import com.example.worldlensai.data.repository.UserPreferencesRepository
import com.example.worldlensai.ui.navigation.Screen
import com.example.worldlensai.ui.screens.CameraScreen
import com.example.worldlensai.ui.screens.LoginScreen
import com.example.worldlensai.ui.screens.ResultScreen
import com.example.worldlensai.ui.screens.SplashScreen
import com.example.worldlensai.ui.theme.WorldLensAITheme
import com.example.worldlensai.ui.viewmodel.CameraViewModel
import com.example.worldlensai.ui.viewmodel.LoginViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userPreferencesRepository = UserPreferencesRepository(applicationContext)
        val geminiRepository = GeminiRepository(apiKey = BuildConfig.GEMINI_API_KEY)
        
        enableEdgeToEdge()
        setContent {
            WorldLensAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorldLensApp(userPreferencesRepository, geminiRepository)
                }
            }
        }
    }
}

@Composable
fun WorldLensApp(
    userPreferencesRepository: UserPreferencesRepository,
    geminiRepository: GeminiRepository
) {
    val navController = rememberNavController()
    
    // CameraViewModel can be shared between Camera and Result screens
    val cameraViewModel: CameraViewModel = viewModel(
        factory = GenericViewModelFactory { CameraViewModel(geminiRepository, userPreferencesRepository) }
    )
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onNext = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = GenericViewModelFactory { LoginViewModel(userPreferencesRepository) }
            )
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Camera.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Camera.route) {
            CameraScreen(
                viewModel = cameraViewModel,
                onAnalysisComplete = {
                    navController.navigate(Screen.Lesson.route)
                }
            )
        }
        
        composable(Screen.Lesson.route) {
            ResultScreen(
                viewModel = cameraViewModel,
                onBack = {
                    cameraViewModel.resetState()
                    navController.popBackStack()
                }
            )
        }
    }
}

class GenericViewModelFactory<VM : ViewModel>(
    private val creator: () -> VM
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
