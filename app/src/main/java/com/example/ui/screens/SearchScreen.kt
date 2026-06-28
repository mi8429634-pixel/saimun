package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MovieViewModel

@Composable
fun SearchScreen(
    viewModel: MovieViewModel,
    onNavigateToDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val results by viewModel.filteredMovies.collectAsState()

    // Filters state
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedRating by viewModel.selectedRating.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    val genres = listOf("Action", "Sci-Fi", "Adventure", "Thriller", "Drama", "Crime", "Cyberpunk")
    val languages = listOf("English", "Japanese", "Spanish")
    val countries = listOf("USA", "Japan", "UK")
    val years = listOf(2026, 2024, 2022, 2014, 2010, 2008)
    val ratings = listOf(9.0, 8.5, 8.0, 7.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search movies, cast, genres...", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color.White
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1.0f)
                    .testTag("search_input")
            )

            // Filter Button
            FilledIconButton(
                onClick = { showFilterSheet = true },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (selectedGenre != null || selectedLanguage != null || selectedCountry != null || selectedYear != null || selectedRating != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("filter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = Color.White
                )
            }
        }

        // Live Suggestions Overlay / Bubble List
        AnimatedVisibility(
            visible = suggestions.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                suggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSearchQuery(suggestion)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = suggestion,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Active filter chips scrollbar
        val hasActiveFilters = selectedGenre != null || selectedLanguage != null || selectedCountry != null || selectedYear != null || selectedRating != null
        if (hasActiveFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                selectedGenre?.let {
                    FilterBadge(text = it) { viewModel.setGenreFilter(null) }
                }
                selectedLanguage?.let {
                    FilterBadge(text = "Language: $it") { viewModel.setLanguageFilter(null) }
                }
                selectedCountry?.let {
                    FilterBadge(text = "Country: $it") { viewModel.setCountryFilter(null) }
                }
                selectedYear?.let {
                    FilterBadge(text = "Year: $it") { viewModel.setYearFilter(null) }
                }
                selectedRating?.let {
                    FilterBadge(text = "Rating: $it+") { viewModel.setRatingFilter(null) }
                }

                // Clear all link
                Text(
                    text = "Clear All",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.clearAllFilters() }
                        .padding(horizontal = 4.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Search Results
        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SentimentDissatisfied,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Movies Found",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "We couldn't find any movies matching those search terms or active filters. Try refining your parameters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.clearAllFilters() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset Search Filters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1.0f)
            ) {
                items(results) { movie ->
                    MovieCard(
                        movie = movie,
                        onClick = { onNavigateToDetails(movie.id) },
                        testTag = "search_result_${movie.id}"
                    )
                }
            }
        }
    }

    // Modal Filters Bottom Sheet
    if (showFilterSheet) {
        AlertDialog(
            onDismissRequest = { showFilterSheet = false },
            confirmButton = {
                Button(
                    onClick = { showFilterSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearAllFilters(); showFilterSheet = false }) {
                    Text("Clear All", color = Color.White)
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Search Filters", color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showFilterSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Genres
                    FilterCategoryTitle("Genre")
                    FlowFilterRow(
                        items = genres,
                        selectedItem = selectedGenre,
                        onSelect = { viewModel.setGenreFilter(it) }
                    )

                    Divider(color = Color.DarkGray)

                    // Ratings
                    FilterCategoryTitle("Minimum Rating")
                    FlowFilterRow(
                        items = ratings.map { "$it+" },
                        selectedItem = selectedRating?.let { "$it+" },
                        onSelect = { viewModel.setRatingFilter(it?.removeSuffix("+")?.toDoubleOrNull()) }
                    )

                    Divider(color = Color.DarkGray)

                    // Years
                    FilterCategoryTitle("Release Year")
                    FlowFilterRow(
                        items = years.map { it.toString() },
                        selectedItem = selectedYear?.toString(),
                        onSelect = { viewModel.setYearFilter(it?.toIntOrNull()) }
                    )

                    Divider(color = Color.DarkGray)

                    // Languages
                    FilterCategoryTitle("Language")
                    FlowFilterRow(
                        items = languages,
                        selectedItem = selectedLanguage,
                        onSelect = { viewModel.setLanguageFilter(it) }
                    )

                    Divider(color = Color.DarkGray)

                    // Countries
                    FilterCategoryTitle("Country")
                    FlowFilterRow(
                        items = countries,
                        selectedItem = selectedCountry,
                        onSelect = { viewModel.setCountryFilter(it) }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.95f)
        )
    }
}

@Composable
fun FilterBadge(
    text: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = Color.White)
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier
                    .size(12.dp)
                    .clickable(onClick = onDismiss)
            )
        }
    }
}

@Composable
fun FilterCategoryTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun FlowFilterRow(
    items: List<String>,
    selectedItem: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable {
                        if (isSelected) onSelect(null) else onSelect(item)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
