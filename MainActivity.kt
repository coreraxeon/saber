// File: app/src/main/java/com/example/stylusdraw/MainActivity.kt
package com.example.stylusdraw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stylusdraw.data.NoteRepository
import com.example.stylusdraw.data.SettingsRepository
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.stylusdraw.data.FolderRepository
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import android.graphics.Color
import com.example.stylusdraw.data.FilterRepository


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable full screen by drawing behind the system bars and hiding them
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)?.let { controller: WindowInsetsControllerCompat ->
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Hide only the navigation bars.
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }

        window.statusBarColor = Color.TRANSPARENT

        window.navigationBarColor = Color.TRANSPARENT

        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = true

        NoteRepository.init(this)
        FolderRepository.init(this)
        FilterRepository.init(this)
        SettingsRepository.init(this)

        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val backStackEntry by nav.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                ModalNavigationDrawer(
                    drawerState   = drawerState,
                    gesturesEnabled = currentRoute == "home" || drawerState.isOpen,
                    drawerContent = {
                        SideDrawer(nav) { scope.launch { drawerState.close() } }
                    }
                ) {
                    NavHost(nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(nav) { scope.launch { drawerState.open() } }
                        }
                        composable("folder/{id}") { backStack ->
                            FolderScreen(
                                nav = nav,
                                folderId = backStack.arguments!!.getString("id")!!,
                                onMenu = { scope.launch { drawerState.open() } }
                            )
                        }
                        composable("editor/{id}") { backStack ->
                            TabbedEditor(
                                nav     = nav,
                                startId = backStack.arguments!!.getString("id")!!,
                                openDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        SettingsRepository.save()
    }
    override fun onPause() {
        super.onPause()
        SettingsRepository.save()
    }
}
