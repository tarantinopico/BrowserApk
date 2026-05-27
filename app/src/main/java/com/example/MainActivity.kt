package com.example

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.browser.BrowserViewModel
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.TabsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var isBrowserViewModelInitialized = false
    private lateinit var browserViewModel: BrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as BrowserApplication).container

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: BrowserViewModel = viewModel(
                        factory = BrowserViewModel.provideFactory(
                            applicationContext,
                            appContainer.browserRepository,
                            appContainer.settingsRepository
                        )
                    )

                    BrowserApp(
                        viewModel = viewModel,
                        onViewModelCreated = { vm ->
                            browserViewModel = vm
                            if (!isBrowserViewModelInitialized) {
                                savedInstanceState?.getBundle("TAB_MANAGER_STATE")?.let { bundle ->
                                    vm.tabManager.restoreState(bundle)
                                }
                                isBrowserViewModelInitialized = true
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::browserViewModel.isInitialized) {
            browserViewModel.tabManager.trimMemory(level)
            if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                // Background aggressive cleanup
                System.gc()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::browserViewModel.isInitialized) {
            val bundle = Bundle()
            browserViewModel.tabManager.saveState(bundle)
            outState.putBundle("TAB_MANAGER_STATE", bundle)
        }
    }
}

@Composable
fun BrowserApp(viewModel: BrowserViewModel, onViewModelCreated: (BrowserViewModel) -> Unit) {
    val navController = rememberNavController()
    
    LaunchedEffect(viewModel) {
        onViewModelCreated(viewModel)
    }

    NavHost(navController = navController, startDestination = "browser") {
        composable("browser") {
            BrowserScreen(
                viewModel = viewModel,
                onNavigateToTabs = { navController.navigate("tabs") },
                onNavigateToBookmarks = { navController.navigate("bookmarks") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("tabs") {
            TabsScreen(
                viewModel = viewModel,
                onTabSelected = { navController.popBackStack("browser", inclusive = false) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("bookmarks") {
            BookmarksScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onBookmarkSelected = { url ->
                    viewModel.tabManager.getActiveTab()?.webView?.loadUrl(url)
                    navController.popBackStack("browser", inclusive = false)
                }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onHistoryItemSelected = { url ->
                    viewModel.tabManager.getActiveTab()?.webView?.loadUrl(url)
                    navController.popBackStack("browser", inclusive = false)
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
