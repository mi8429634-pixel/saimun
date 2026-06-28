package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.firebase.MovieMessagingService
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Movie
import com.example.data.model.User
import com.example.ui.AuthState
import com.example.ui.MovieViewModel
import com.example.ui.WatchHistoryWithMovie

@Composable
fun ProfileScreen(
    viewModel: MovieViewModel,
    onNavigateToDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val authState by viewModel.authState.collectAsState()

    val watchlist by viewModel.userWatchlist.collectAsState()
    val favorites by viewModel.userFavorites.collectAsState()
    val watchHistory by viewModel.userWatchHistory.collectAsState()

    // Avatar list
    val avatarIcons = mapOf(
        "avatar_admin" to Icons.Default.AdminPanelSettings,
        "avatar_user_1" to Icons.Default.LocalMovies,
        "avatar_user_2" to Icons.Default.Face,
        "avatar_user_3" to Icons.Default.TheaterComedy,
        "avatar_user_4" to Icons.Default.VideogameAsset,
        "avatar_user_5" to Icons.Default.Star
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        if (currentUser == null) {
            // Authentication Forms
            AuthFlowSection(viewModel = viewModel)
        } else {
            // Logged In Profile UI
            val user = currentUser!!

            var isEditingProfile by remember { mutableStateOf(false) }
            var editName by remember { mutableStateOf(user.name) }
            var editAvatar by remember { mutableStateOf(user.avatarUrl) }

            val context = LocalContext.current
            var hasNotificationPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasNotificationPermission = isGranted
            }

            var releasesSubscribed by remember { mutableStateOf(true) }
            var watchlistSubscribed by remember { mutableStateOf(true) }
            var announcementsSubscribed by remember { mutableStateOf(true) }

            // Profile Header Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = avatarIcons[user.avatarUrl] ?: Icons.Default.Person
                        Icon(
                            imageVector = icon,
                            contentDescription = "Avatar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Name + Role Badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Role Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (user.role == "Admin") MaterialTheme.colorScheme.primary
                                    else if (user.role == "Moderator") MaterialTheme.colorScheme.secondary
                                    else Color.DarkGray
                                )
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = user.role.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user.role == "Moderator") Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Profile Edit Toggle
                    if (!isEditingProfile) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isEditingProfile = true },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", color = Color.White)
                            }

                            Button(
                                onClick = { viewModel.logout() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("logout_button")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Editing Inputs
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Display Name", color = Color.Gray) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Select Movie Avatar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )

                            // Avatar grid horizontal selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                avatarIcons.forEach { (key, icon) ->
                                    val isSelected = editAvatar == key
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { editAvatar = key },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.updateUserProfile(editName, editAvatar)
                                        isEditingProfile = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.0f)
                                ) {
                                    Text("Save Changes", fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = {
                                        editName = user.name
                                        editAvatar = user.avatarUrl
                                        isEditingProfile = false
                                    },
                                    modifier = Modifier.weight(1.0f)
                                ) {
                                    Text("Cancel", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // PUSH NOTIFICATION PREFERENCES CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Notification Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Android 13+ Permission Warning Banner
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Notifications Blocked",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "To receive release alerts and announcements on this device, please grant the system permission.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Grant Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                    // Channel 1: New Releases
                    NotificationToggleItem(
                        title = "New Movie Releases",
                        description = "Get notified instantly when new blockbuster titles and cinema releases are published.",
                        checked = releasesSubscribed,
                        onCheckedChange = { checked ->
                            releasesSubscribed = checked
                            if (checked) {
                                MovieMessagingService.subscribeToTopic("movie_releases")
                            } else {
                                MovieMessagingService.unsubscribeFromTopic("movie_releases")
                            }
                        }
                    )

                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f))

                    // Channel 2: Watchlist Updates
                    NotificationToggleItem(
                        title = "Watchlist & Bookmarks",
                        description = "Receive updates, status changes, and notifications regarding titles on your Watchlist.",
                        checked = watchlistSubscribed,
                        onCheckedChange = { checked ->
                            watchlistSubscribed = checked
                            if (checked) {
                                MovieMessagingService.subscribeToTopic("watchlist")
                            } else {
                                MovieMessagingService.unsubscribeFromTopic("watchlist")
                            }
                        }
                    )

                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f))

                    // Channel 3: Administrative Broadcasts
                    NotificationToggleItem(
                        title = "Announcements",
                        description = "Receive announcements regarding platform updates, maintenance, and community alerts.",
                        checked = announcementsSubscribed,
                        onCheckedChange = { checked ->
                            announcementsSubscribed = checked
                            if (checked) {
                                MovieMessagingService.subscribeToTopic("announcements")
                            } else {
                                MovieMessagingService.unsubscribeFromTopic("announcements")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Watchlist
            ProfileMovieSection(
                title = "My Watchlist",
                movies = watchlist,
                emptyMessage = "Your watchlist is empty. Add movies while browsing to save them here!",
                onMovieClick = onNavigateToDetails,
                tagPrefix = "watchlist"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Favorites
            ProfileMovieSection(
                title = "My Favorites",
                movies = favorites,
                emptyMessage = "No favorites added. Tap the heart on movie details to save your absolute favorites!",
                onMovieClick = onNavigateToDetails,
                tagPrefix = "favorites"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // History
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Recently Watched",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                if (watchHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No watch history available. Start playing movies to track your history!",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(watchHistory) { entry ->
                            MovieCard(
                                movie = entry.movie,
                                onClick = { onNavigateToDetails(entry.movie.id) },
                                testTag = "history_card_${entry.movie.id}"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileMovieSection(
    title: String,
    movies: List<Movie>,
    emptyMessage: String,
    onMovieClick: (Long) -> Unit,
    tagPrefix: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        if (movies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(movies) { movie ->
                    MovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie.id) },
                        testTag = "${tagPrefix}_card_${movie.id}"
                    )
                }
            }
        }
    }
}

@Composable
fun AuthFlowSection(viewModel: MovieViewModel) {
    var authMode by remember { mutableStateOf("login") } // login, register, forgot

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("User") }

    val authState by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App logo or visual spacer
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Default.LocalMovies,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "MoviePremium",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text(
            text = "Join our premium commercial streaming lounge.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Auth States Banner feedback
        AnimatedVisibility(
            visible = authState is AuthState.Error || authState is AuthState.ForgotPasswordSent,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (authState is AuthState.Error) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (authState is AuthState.Error) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(14.dp)
            ) {
                val messageText = when (val state = authState) {
                    is AuthState.Error -> state.message
                    is AuthState.ForgotPasswordSent -> "Password recovery instructions successfully transmitted to ${state.email}."
                    else -> ""
                }
                Text(
                    text = messageText,
                    color = if (authState is AuthState.Error) Color.White else MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Card Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = when (authMode) {
                        "login" -> "Sign In"
                        "register" -> "Create Account"
                        else -> "Reset Password"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                if (authMode == "register") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name", color = Color.Gray) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.LightGray) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_name_input")
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = Color.Gray) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color.LightGray) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                )

                if (authMode != "forgot") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color.Gray) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.LightGray) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                    )
                }

                // Role selection for registration to test Admin roles
                if (authMode == "register") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Simulate Account Permission Level:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("User", "Admin").forEach { role ->
                                val isSelected = role == selectedRole
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedRole = role }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(text = role, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Submit button
                Button(
                    onClick = {
                        if (authMode == "login") {
                            viewModel.loginUser(email, password)
                        } else if (authMode == "register") {
                            viewModel.signUpUser(name, email, selectedRole)
                        } else {
                            viewModel.forgotPassword(email)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_submit_button")
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            text = when (authMode) {
                                "login" -> "Access Account"
                                "register" -> "Create Account"
                                else -> "Request Password Reset"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Auth modes switcher links
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (authMode == "login") {
                        Text(
                            text = "Forgot Password?",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clickable { authMode = "forgot" }
                                .padding(4.dp)
                        )
                        Text(
                            text = "New to MoviePremium? Create Account",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clickable { authMode = "register" }
                                .padding(4.dp)
                        )
                    } else {
                        Text(
                            text = "Already have an account? Sign In",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clickable { authMode = "login" }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        // Demo login help block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Demo Account Credentials:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Text("• Admin: admin@moviepremium.com (Unlocks Admin Dashboard!)", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                Text("• Premium User: user@moviepremium.com", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun NotificationToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1.0f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

