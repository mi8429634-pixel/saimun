package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.MovieViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.data.firebase.MovieMessagingService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Notification Channels for Android 8.0+
        MovieMessagingService.createNotificationChannels(this)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MoviePremiumApp()
            }
        }
    }
}

@Composable
fun MoviePremiumApp() {
    val navController = rememberNavController()
    val viewModel: MovieViewModel = viewModel()
    
    // Track active navigation route to show/hide Bottom Bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isPlayerRoute = currentRoute?.startsWith("player") == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!isPlayerRoute) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    // Home Tab
                    NavigationBarItem(
                        selected = currentRoute == "home" || currentRoute == null,
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )

                    // Discover Search Tab
                    NavigationBarItem(
                        selected = currentRoute == "search",
                        onClick = {
                            navController.navigate("search") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = { Text("Search", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_search")
                    )

                    // Admin Console Tab
                    NavigationBarItem(
                        selected = currentRoute == "admin",
                        onClick = {
                            navController.navigate("admin") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "admin") Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                                contentDescription = "Admin"
                            )
                        },
                        label = { Text("Admin", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_admin")
                    )

                    // Profile Space Tab
                    NavigationBarItem(
                        selected = currentRoute == "profile",
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "profile") Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "My Space"
                            )
                        },
                        label = { Text("Profile", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Homepage Route
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { movieId ->
                        navController.navigate("detail/$movieId")
                    },
                    onNavigateToPlayer = { movieId ->
                        navController.navigate("player/$movieId")
                    }
                )
            }

            // Live Search Route
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { movieId ->
                        navController.navigate("detail/$movieId")
                    }
                )
            }

            // Admin Panel Route
            composable("admin") {
                AdminScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { movieId ->
                        navController.navigate("detail/$movieId")
                    }
                )
            }

            // My Space / Profile Route
            composable("profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { movieId ->
                        navController.navigate("detail/$movieId")
                    }
                )
            }

            // Movie Specs Sheet Route
            composable(
                route = "detail/{movieId}",
                arguments = listOf(navArgument("movieId") { type = NavType.LongType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getLong("movieId") ?: 0L
                DetailScreen(
                    movieId = movieId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { id ->
                        navController.navigate("player/$id")
                    }
                )
            }

            // Video Player Route
            composable(
                route = "player/{movieId}",
                arguments = listOf(navArgument("movieId") { type = NavType.LongType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getLong("movieId") ?: 0L
                PlayerScreen(
                    movieId = movieId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
