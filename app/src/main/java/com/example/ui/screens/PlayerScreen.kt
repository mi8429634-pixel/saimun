package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MovieViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    movieId: Long,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.publishedMovies.collectAsState()
    val watchHistory by viewModel.userWatchHistory.collectAsState()

    val movie = remember(movies, movieId) { movies.find { it.id == movieId } }

    if (movie == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Playback state variables
    var isPlaying by remember { mutableStateOf(true) }
    val totalDurationMs = remember { movie.durationMinutes * 60000L }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    // Audio / Subtitles selection
    var selectedAudioTrack by remember { mutableStateOf(movie.audioTrack ?: "English (Atmos 5.1)") }
    var selectedSubtitleTrack by remember { mutableStateOf(movie.subtitlesUrl?.split(", ")?.firstOrNull() ?: "Off") }

    // Overlay controls visibility
    var showControls by remember { mutableStateOf(true) }

    // Dialog state
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }

    // Resume tracking
    val previousProgress = remember(watchHistory, movieId) {
        watchHistory.find { it.history.movieId == movieId }?.history
    }

    val coroutineScope = rememberCoroutineScope()

    // Prompt for resume if there is previous watch history
    LaunchedEffect(previousProgress) {
        if (previousProgress != null && previousProgress.lastPositionMs > 5000L && previousProgress.lastPositionMs < previousProgress.totalDurationMs - 5000L) {
            showResumeDialog = true
        }
    }

    // Auto-advance player time when isPlaying is true
    LaunchedEffect(isPlaying, playbackSpeed) {
        while (isPlaying) {
            delay((1000 / playbackSpeed).toLong())
            if (currentPositionMs < totalDurationMs) {
                currentPositionMs += 1000
                // Auto-save progress back to database in real-time
                viewModel.savePlaybackProgress(movieId, currentPositionMs, totalDurationMs)
            } else {
                isPlaying = false
            }
        }
    }

    // Hide controls after 4 seconds of idle
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
            .testTag("player_screen_${movie.id}")
    ) {
        // Mock Video Backdrop Visualizer (ambient pulsing colors simulating film playback)
        val pulseOffset by animateFloatAsState(
            targetValue = if (isPlaying) 1.0f else 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulsingBackdrop"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE50914).copy(alpha = 0.15f * pulseOffset),
                            Color(0xFF0F0F12)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                )
                Text(
                    text = if (isPlaying) "Streaming in Ultra HD 4K" else "Playback Paused",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )

                // Subtitle Overlay text rendered during playback
                if (isPlaying && selectedSubtitleTrack != "Off") {
                    Spacer(modifier = Modifier.height(40.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "[Speaking ${selectedSubtitleTrack.substringBefore(" ")}] Streaming MoviePremium exclusive content...",
                            color = Color.Yellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // CONTROLS OVERLAY
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 })
        ) {
            // Top Controls Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Exit Player",
                                tint = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = movie.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Playing • ${playbackSpeed}x • $selectedAudioTrack",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Settings triggers
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { showSubtitleDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showAudioDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Audio Tracks",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showSpeedDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // CENTER CONTROLS (PLAY/PAUSE/REWIND)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Rewind 10s
                IconButton(
                    onClick = {
                        currentPositionMs = (currentPositionMs - 10000L).coerceAtLeast(0L)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause Toggle
                FilledIconButton(
                    onClick = { isPlaying = !isPlaying },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Forward 10s
                IconButton(
                    onClick = {
                        currentPositionMs = (currentPositionMs + 10000L).coerceAtMost(totalDurationMs)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // BOTTOM SCRUBBER & CONTROLS
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 40 }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scrubber Bar
                Slider(
                    value = currentPositionMs.toFloat(),
                    onValueChange = {
                        isPlaying = false
                        currentPositionMs = it.toLong()
                    },
                    onValueChangeFinished = {
                        isPlaying = true
                        viewModel.savePlaybackProgress(movieId, currentPositionMs, totalDurationMs)
                    },
                    valueRange = 0f..totalDurationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_timeline_slider")
                )

                // Timestamp Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMillis(currentPositionMs),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Picture in picture simulation button
                        IconButton(
                            onClick = {
                                isPlaying = true
                                showControls = false
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "Picture-in-Picture",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Fullscreen simulation button
                        IconButton(
                            onClick = {
                                showControls = false
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = formatMillis(totalDurationMs),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // DIALOGS: 1. RESUME PLAYBACK
        if (showResumeDialog && previousProgress != null) {
            AlertDialog(
                onDismissRequest = { showResumeDialog = false },
                title = { Text("Resume Movie?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "You stopped watching '${movie.title}' at ${formatMillis(previousProgress.lastPositionMs)}. Would you like to pick up where you left off?",
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            currentPositionMs = previousProgress.lastPositionMs
                            showResumeDialog = false
                            isPlaying = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Resume Playback", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            currentPositionMs = 0L
                            showResumeDialog = false
                            isPlaying = true
                        }
                    ) {
                        Text("Start Over", color = Color.White)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // DIALOGS: 2. SPEED SELECTOR
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text("Playback Speed", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playbackSpeed = speed
                                        showSpeedDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                                    color = Color.White,
                                    fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                                )
                                if (playbackSpeed == speed) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // DIALOGS: 3. AUDIO SELECTION
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                title = { Text("Select Audio Language", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "English (Atmos 5.1)",
                            "English (Stereo 2.0)",
                            "Spanish (Dolby 5.1)",
                            "Japanese (Stereo 2.0)",
                            "Director's Commentary"
                        ).forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAudioTrack = track
                                        showAudioDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track,
                                    color = Color.White,
                                    fontWeight = if (selectedAudioTrack == track) FontWeight.Bold else FontWeight.Normal
                                )
                                if (selectedAudioTrack == track) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // DIALOGS: 4. SUBTITLES SELECTION
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                title = { Text("Select Subtitles", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Off",
                            "English (SRT)",
                            "Japanese (SRT)",
                            "Spanish (SRT)",
                            "French (SRT)"
                        ).forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSubtitleTrack = sub
                                        showSubtitleDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sub,
                                    color = Color.White,
                                    fontWeight = if (selectedSubtitleTrack == sub) FontWeight.Bold else FontWeight.Normal
                                )
                                if (selectedSubtitleTrack == sub) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

fun formatMillis(millis: Long): String {
    val totalSecs = millis / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
