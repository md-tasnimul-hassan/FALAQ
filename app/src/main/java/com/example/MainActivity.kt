package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.ShortcutRepository
import com.example.ui.BrowserScreen
import com.example.ui.BrowserViewModel
import com.example.ui.BrowserViewModelFactory
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.lifecycle.lifecycleScope
import com.example.logic.BlocklistManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var browserViewModel: BrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        BlocklistManager.init(applicationContext)

        val db = AppDatabase.getDatabase(applicationContext)
        val factory = BrowserViewModelFactory(applicationContext, db)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                browserViewModel = viewModel(factory = factory)

                androidx.compose.runtime.LaunchedEffect(intent) {
                    handleIntent(intent)
                }

                NavHost(
                    navController = navController,
                    startDestination = "browser",
                    enterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.fadeIn(
                        androidx.compose.animation.core.tween(250)
                    ) },
                    exitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.fadeOut(
                        androidx.compose.animation.core.tween(250)
                    ) },
                    popEnterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.fadeIn(
                        androidx.compose.animation.core.tween(250)
                    ) },
                    popExitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.fadeOut(
                        androidx.compose.animation.core.tween(250)
                    ) }
                ) {
                    composable("browser") {
                        BrowserScreen(
                            viewModel = browserViewModel,
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = browserViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_VIEW) {
            val url = intent.dataString
            intent.action = null
            if (!url.isNullOrEmpty()) {
                if (::browserViewModel.isInitialized) {
                    browserViewModel.loadUrl(url)
                }
            }
        }
    }
}
