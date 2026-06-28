package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Movie
import com.example.ui.MovieViewModel
import com.example.ui.WatchHistoryWithMovie
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: MovieViewModel,
    onNavigateToDetails: (Long) -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.publishedMovies.collectAsState()
    val continueWatchingList by viewModel.userWatchHistory.collectAsState()

    val trendingMovies = remember(movies) { movies.filter { it.isTrending } }
    val popularMovies = remember(movies) { movies.filter { it.isPopular } }
    val latestMovies = remember(movies) { movies.filter { it.isLatest } }
    val topRatedMovies = remember(movies) { movies.filter { it.isTopRated } }
    val recommendedMovies = remember(movies) { movies.filter { it.isRecommended } }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // 1. Hero Slideshow
        if (movies.isNotEmpty()) {
            HeroSlideshow(
                movies = movies.take(5),
                onPlayClick = onNavigateToPlayer,
                onInfoClick = onNavigateToDetails
            )
        } else {
            // Loading / Placeholder banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.DarkGray, MaterialTheme.colorScheme.background)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Space before rows
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Continue Watching (Conditional)
        val partiallyWatched = continueWatchingList.filter { it.history.lastPositionMs < it.history.totalDurationMs - 5000 && it.history.lastPositionMs > 5000 }
        if (partiallyWatched.isNotEmpty()) {
            ContinueWatchingRow(
                history = partiallyWatched,
                onMovieClick = onNavigateToPlayer,
                onInfoClick = onNavigateToDetails
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. Trending
        if (trendingMovies.isNotEmpty()) {
            MovieRow(
                title = "Trending Now",
                movies = trendingMovies,
                onMovieClick = onNavigateToDetails,
                tagPrefix = "trending"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. Latest Releases
        if (latestMovies.isNotEmpty()) {
            MovieRow(
                title = "New Releases",
                movies = latestMovies,
                onMovieClick = onNavigateToDetails,
                tagPrefix = "latest"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. Popular
        if (popularMovies.isNotEmpty()) {
            MovieRow(
                title = "Popular Hits",
                movies = popularMovies,
                onMovieClick = onNavigateToDetails,
                tagPrefix = "popular"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 6. Top Rated
        if (topRatedMovies.isNotEmpty()) {
            MovieRow(
                title = "Top Rated Masterpieces",
                movies = topRatedMovies,
                onMovieClick = onNavigateToDetails,
                tagPrefix = "top_rated"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 7. Recommended
        if (recommendedMovies.isNotEmpty()) {
            MovieRow(
                title = "Recommended For You",
                movies = recommendedMovies,
                onMovieClick = onNavigateToDetails,
                tagPrefix = "recommended"
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HeroSlideshow(
    movies: List<Movie>,
    onPlayClick: (Long) -> Unit,
    onInfoClick: (Long) -> Unit
) {
    var currentSlideIndex by remember { mutableStateOf(0) }

    // Auto-advance slides
    LaunchedEffect(movies) {
        while (movies.size > 1) {
            delay(5000)
            currentSlideIndex = (currentSlideIndex + 1) % movies.size
        }
    }

    val currentMovie = movies.getOrNull(currentSlideIndex) ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .testTag("hero_slideshow")
    ) {
        // Backing Image with dynamic crossfade simulation
        AnimatedContent(
            targetState = currentMovie,
            transitionSpec = {
                fadeIn(animationSpec = tween(1000)) togetherWith fadeOut(animationSpec = tween(1000))
            },
            label = "HeroImage"
        ) { targetMovie ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(targetMovie.bannerUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Featured Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
            )
        }

        // Beautiful Cinematic Vignette and Dark Gradient Mask
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Slide Indicators
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            movies.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentSlideIndex) 20.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index == currentSlideIndex) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.4f)
                        )
                )
            }
        }

        // Movie Info Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quality / Category / Rating pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FEATURED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = currentMovie.genre.split(", ").firstOrNull() ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentMovie.rating.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Title
            Text(
                text = currentMovie.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Synopsis
            Text(
                text = currentMovie.synopsis,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onPlayClick(currentMovie.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("hero_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = { onInfoClick(currentMovie.id) },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("More Info", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingRow(
    history: List<WatchHistoryWithMovie>,
    onMovieClick: (Long) -> Unit,
    onInfoClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Continue Watching",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(history) { entry ->
                ContinueWatchingCard(
                    entry = entry,
                    onPlayClick = { onMovieClick(entry.movie.id) },
                    onDetailsClick = { onInfoClick(entry.movie.id) }
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    entry: WatchHistoryWithMovie,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    val progress = entry.history.lastPositionMs.toFloat() / entry.history.totalDurationMs.toFloat()
    val progressPercent = (progress * 100).toInt()

    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onPlayClick() }
            .testTag("continue_watching_card_${entry.movie.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(entry.movie.bannerUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = entry.movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Play icon overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Info button top-right
            IconButton(
                onClick = onDetailsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Details",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Live progress line
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.2f),
        )

        // Metadata
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = entry.movie.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$progressPercent% completed • Resume",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MovieRow(
    title: String,
    movies: List<Movie>,
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

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.id) },
                    testTag = "movie_${tagPrefix}_${movie.id}"
                )
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Star Rating Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = movie.rating.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Text(
            text = "${movie.releaseYear} • ${movie.genre.split(", ").firstOrNull() ?: ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
