package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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

@Composable
fun DetailScreen(
    movieId: Long,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.publishedMovies.collectAsState()
    val watchlist by viewModel.userWatchlist.collectAsState()
    val favorites by viewModel.userFavorites.collectAsState()

    val movie = remember(movies, movieId) { movies.find { it.id == movieId } }

    if (movie == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val isFavorite = remember(favorites, movieId) { favorites.any { it.id == movieId } }
    val isInWatchlist = remember(watchlist, movieId) { watchlist.any { it.id == movieId } }

    val recommendedInGenre = remember(movies, movie) {
        movies.filter { other ->
            other.id != movie.id && other.genre.split(", ").any { g -> movie.genre.contains(g) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .testTag("detail_screen_${movie.id}")
    ) {
        // Banner Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.bannerUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${movie.title} banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Linear Backdrop Mask
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // Back Navigation Icon
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(top = 40.dp, start = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = Color.White
                )
            }
        }

        // Content Details Section
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .offset(y = (-30).dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title, Rating, and Year Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Movie Poster (overlapping overlay)
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(movie.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${movie.title} poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Title + Primary stats
                Column(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = movie.releaseYear.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Divider(
                            modifier = Modifier
                                .height(12.dp)
                                .width(1.dp),
                            color = Color.Gray
                        )
                        Text(
                            text = formatDuration(movie.durationMinutes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = movie.rating.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/10",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Primary Play / Trailer Button and Favorites Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onNavigateToPlayer(movie.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.0f)
                        .height(50.dp)
                        .testTag("detail_play_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch Film", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Add to Watchlist
                IconButton(
                    onClick = { viewModel.toggleWatchlist(movie.id) },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("detail_watchlist_toggle")
                ) {
                    Icon(
                        imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (isInWatchlist) MaterialTheme.colorScheme.primary else Color.White
                    )
                }

                // Add to Favorites
                IconButton(
                    onClick = { viewModel.toggleFavorite(movie.id) },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("detail_favorite_toggle")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }

            // Genres Wrap List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                movie.genre.split(", ").forEach { genre ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Synopsis
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Synopsis",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = movie.synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    lineHeight = 22.sp
                )
            }

            Divider(color = Color.DarkGray)

            // Language & Country block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailMetaBlock("Language", movie.language)
                DetailMetaBlock("Country", movie.country)
                DetailMetaBlock("Classification", "PG-13")
            }

            Divider(color = Color.DarkGray)

            // Cast list
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Starring Cast",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    movie.cast.split(", ").forEach { actor ->
                        CastAvatarCard(actorName = actor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recommended Rows
            if (recommendedInGenre.isNotEmpty()) {
                MovieRow(
                    title = "Similar Titles",
                    movies = recommendedInGenre,
                    onMovieClick = { id ->
                        onNavigateToPlayer(id) // Or recursive navigate
                    },
                    tagPrefix = "similar"
                )
            }
        }
    }
}

@Composable
fun DetailMetaBlock(label: String, value: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CastAvatarCard(actorName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = actorName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
