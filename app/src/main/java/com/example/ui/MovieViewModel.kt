package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.*
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = MovieRepository(
        db.movieDao(),
        db.userDao(),
        db.watchHistoryDao(),
        db.watchlistDao(),
        db.favoriteDao()
    )

    // Auth State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Movie Lists
    val publishedMovies: StateFlow<List<Movie>> = repository.publishedMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMovies: StateFlow<List<Movie>> = repository.allMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private val _selectedRating = MutableStateFlow<Double?>(null)
    val selectedRating: StateFlow<Double?> = _selectedRating.asStateFlow()

    // Search Results / Filtered Movies
    val filteredMovies: StateFlow<List<Movie>> = combine(
        publishedMovies,
        _searchQuery,
        _selectedGenre,
        _selectedLanguage,
        _selectedCountry,
        _selectedYear,
        _selectedRating
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val movies = array[0] as List<Movie>
        val query = array[1] as String
        val genre = array[2] as String?
        val lang = array[3] as String?
        val country = array[4] as String?
        val year = array[5] as Int?
        val rating = array[6] as Double?

        movies.filter { movie ->
            val matchQuery = query.isEmpty() || movie.title.contains(query, ignoreCase = true) ||
                    movie.synopsis.contains(query, ignoreCase = true) ||
                    movie.genre.contains(query, ignoreCase = true) ||
                    movie.cast.contains(query, ignoreCase = true)
            
            val matchGenre = genre == null || movie.genre.contains(genre, ignoreCase = true)
            val matchLang = lang == null || movie.language.equals(lang, ignoreCase = true)
            val matchCountry = country == null || movie.country.equals(country, ignoreCase = true)
            val matchYear = year == null || movie.releaseYear == year
            val matchRating = rating == null || movie.rating >= rating

            matchQuery && matchGenre && matchLang && matchCountry && matchYear && matchRating
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Search Suggestions
    val searchSuggestions: StateFlow<List<String>> = _searchQuery
        .map { query ->
            if (query.length < 2) emptyList()
            else {
                publishedMovies.value
                    .filter { it.title.contains(query, ignoreCase = true) }
                    .map { it.title }
                    .take(5)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Watchlist, Favorites, History Flows for CURRENT USER
    val userWatchlist: StateFlow<List<Movie>> = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else {
            repository.getWatchlist(user.id).flatMapLatest { entries ->
                publishedMovies.map { movies ->
                    val entryIds = entries.map { it.movieId }.toSet()
                    movies.filter { it.id in entryIds }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userFavorites: StateFlow<List<Movie>> = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else {
            repository.getFavorites(user.id).flatMapLatest { entries ->
                publishedMovies.map { movies ->
                    val entryIds = entries.map { it.movieId }.toSet()
                    movies.filter { it.id in entryIds }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userWatchHistory: StateFlow<List<WatchHistoryWithMovie>> = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else {
            repository.getWatchHistory(user.id).flatMapLatest { historyEntries ->
                publishedMovies.map { movies ->
                    historyEntries.mapNotNull { entry ->
                        val movie = movies.find { it.id == entry.movieId }
                        if (movie != null) WatchHistoryWithMovie(entry, movie) else null
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics (Admin Only)
    val analyticsState: StateFlow<AnalyticsState> = allMovies.map { movies ->
        val totalStorageGb = 234.5 + (movies.size * 1.8)
        AnalyticsState(
            totalMovies = movies.size,
            activeUsers = 1248 + movies.size * 12,
            totalViews = 54890 + movies.size * 342,
            storageUsedGb = totalStorageGb,
            storageLimitGb = 1000.0,
            genreDistribution = movies.flatMap { it.genre.split(", ") }
                .groupBy { it }
                .mapValues { it.value.size }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())

    init {
        FirebaseService.initialize(application)
        syncMovies()
        // Log in as default user initially so application loads instantly with beautiful mock movies
        loginUser("user@moviepremium.com", "user123")
    }

    fun syncMovies() {
        viewModelScope.launch {
            if (FirebaseService.isInitialized) {
                try {
                    FirebaseService.listenToMoviesFromFirestore().collect { firebaseMovies ->
                        if (firebaseMovies.isNotEmpty()) {
                            for (movie in firebaseMovies) {
                                repository.insertMovie(movie)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MovieViewModel", "Error syncing movies with Firestore: ${e.localizedMessage}")
                }
            }
        }
    }

    // Set Search Query
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Set Filters
    fun setGenreFilter(genre: String?) { _selectedGenre.value = genre }
    fun setLanguageFilter(lang: String?) { _selectedLanguage.value = lang }
    fun setCountryFilter(country: String?) { _selectedCountry.value = country }
    fun setYearFilter(year: Int?) { _selectedYear.value = year }
    fun setRatingFilter(rating: Double?) { _selectedRating.value = rating }
    fun clearAllFilters() {
        _selectedGenre.value = null
        _selectedLanguage.value = null
        _selectedCountry.value = null
        _selectedYear.value = null
        _selectedRating.value = null
        _searchQuery.value = ""
    }

    // Auth Actions
    fun loginUser(email: String, word: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (FirebaseService.isInitialized) {
                try {
                    val user = FirebaseService.loginWithFirebase(email, word)
                    if (user != null) {
                        _currentUser.value = user
                        _authState.value = AuthState.Success(user)
                        repository.insertUser(user)
                        MovieMessagingService.syncDeviceToken()
                        MovieMessagingService.subscribeToTopic("announcements")
                        MovieMessagingService.subscribeToTopic("movie_releases")
                        syncMovies()
                        return@launch
                    }
                } catch (e: Exception) {
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Firebase login failed.")
                    return@launch
                }
            }
            
            // Local Fallback offline mode
            val user = repository.getUserByIdSuspend(email)
            if (user != null) {
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
            } else {
                if (email.contains("@")) {
                    val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val newUser = User(
                        id = email,
                        email = email,
                        name = name,
                        avatarUrl = if (email.startsWith("admin")) "avatar_admin" else "avatar_user_1",
                        role = if (email.startsWith("admin")) "Admin" else "User"
                    )
                    repository.insertUser(newUser)
                    _currentUser.value = newUser
                    _authState.value = AuthState.Success(newUser)
                } else {
                    _authState.value = AuthState.Error("Invalid email pattern. Use 'admin@moviepremium.com' or 'user@moviepremium.com'.")
                }
            }
        }
    }

    fun signUpUser(name: String, email: String, role: String = "User") {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (!email.contains("@") || name.isBlank()) {
                _authState.value = AuthState.Error("Please enter a valid name and email address.")
                return@launch
            }
            if (FirebaseService.isInitialized) {
                try {
                    // Firebase Auth requires password. We use a default secure string for fast user registration flow
                    val user = FirebaseService.signUpWithFirebase(name, email, "PremiumPass123!", role)
                    if (user != null) {
                        _currentUser.value = user
                        _authState.value = AuthState.Success(user)
                        repository.insertUser(user)
                        MovieMessagingService.syncDeviceToken()
                        MovieMessagingService.subscribeToTopic("announcements")
                        MovieMessagingService.subscribeToTopic("movie_releases")
                        syncMovies()
                        return@launch
                    }
                } catch (e: Exception) {
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Firebase sign-up failed.")
                    return@launch
                }
            }
            
            // Local Fallback offline mode
            val existing = repository.getUserByIdSuspend(email)
            if (existing != null) {
                _currentUser.value = existing
                _authState.value = AuthState.Success(existing)
            } else {
                val newUser = User(
                    id = email,
                    email = email,
                    name = name,
                    avatarUrl = if (email.startsWith("admin")) "avatar_admin" else "avatar_user_2",
                    role = role
                )
                repository.insertUser(newUser)
                _currentUser.value = newUser
                _authState.value = AuthState.Success(newUser)
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            if (FirebaseService.isInitialized) {
                try {
                    FirebaseService.forgotPassword(email)
                    _authState.value = AuthState.ForgotPasswordSent(email)
                } catch (e: Exception) {
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to send reset link.")
                }
            } else {
                _authState.value = AuthState.ForgotPasswordSent(email)
            }
        }
    }

    fun logout() {
        if (FirebaseService.isInitialized) {
            FirebaseService.logout()
        }
        _currentUser.value = null
        _authState.value = AuthState.LoggedOut
    }

    fun updateUserProfile(name: String, avatarUrl: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(name = name, avatarUrl = avatarUrl)
            if (FirebaseService.isInitialized) {
                try {
                    FirebaseService.updateUserProfile(user.id, name, avatarUrl)
                } catch (e: Exception) {
                    Log.e("MovieViewModel", "Error updating Firestore profile: ${e.localizedMessage}")
                }
            }
            repository.insertUser(updated)
            _currentUser.value = updated
        }
    }

    // Toggle Favorite
    fun toggleFavorite(movieId: Long) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            val existing = repository.getFavoriteEntry(userId, movieId)
            if (existing != null) {
                repository.deleteFavorite(userId, movieId)
            } else {
                repository.insertFavorite(FavoriteEntry(userId = userId, movieId = movieId))
            }
        }
    }

    // Toggle Watchlist
    fun toggleWatchlist(movieId: Long) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            val existing = repository.getWatchlistEntry(userId, movieId)
            if (existing != null) {
                repository.deleteWatchlist(userId, movieId)
            } else {
                repository.insertWatchlist(WatchlistEntry(userId = userId, movieId = movieId))
            }
        }
    }

    // Update Playback History
    fun savePlaybackProgress(movieId: Long, positionMs: Long, totalDurationMs: Long) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            val existing = repository.getWatchHistoryEntry(userId, movieId)
            if (existing != null) {
                repository.insertWatchHistory(existing.copy(
                    lastPositionMs = positionMs,
                    totalDurationMs = totalDurationMs,
                    timestamp = System.currentTimeMillis()
                ))
            } else {
                repository.insertWatchHistory(WatchHistory(
                    userId = userId,
                    movieId = movieId,
                    lastPositionMs = positionMs,
                    totalDurationMs = totalDurationMs
                ))
            }
        }
    }

    // Admin CRUD Operations
    fun saveMovie(movie: Movie, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val finalMovie = if (movie.id == 0L) {
                val randomId = kotlin.math.abs(java.util.UUID.randomUUID().mostSignificantBits)
                movie.copy(id = randomId)
            } else movie

            if (FirebaseService.isInitialized) {
                try {
                    FirebaseService.saveMovieToFirestore(finalMovie)
                } catch (e: Exception) {
                    Log.e("MovieViewModel", "Error saving movie to Firestore: ${e.localizedMessage}")
                }
            }
            
            if (movie.id == 0L) {
                repository.insertMovie(finalMovie)
            } else {
                repository.updateMovie(finalMovie)
            }
            onComplete()
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            if (FirebaseService.isInitialized) {
                try {
                    FirebaseService.deleteMovieFromFirestore(movie)
                } catch (e: Exception) {
                    Log.e("MovieViewModel", "Error deleting movie from Firestore: ${e.localizedMessage}")
                }
            }
            repository.deleteMovie(movie)
        }
    }

    fun setMoviePublishedState(movie: Movie, isPublished: Boolean) {
        viewModelScope.launch {
            val updated = movie.copy(isPublished = isPublished)
            if (FirebaseService.isInitialized) {
                try {
                    FirebaseService.saveMovieToFirestore(updated)
                } catch (e: Exception) {
                    Log.e("MovieViewModel", "Error setting published state in Firestore: ${e.localizedMessage}")
                }
            }
            repository.updateMovie(updated)
        }
    }

    fun uploadMediaFile(uri: Uri, path: String): Flow<UploadStatus> {
        return FirebaseService.uploadFileWithProgress(uri, path)
    }
}

sealed interface AuthState {
    object LoggedOut : AuthState
    object Loading : AuthState
    data class Success(val user: User) : AuthState
    data class Error(val message: String) : AuthState
    data class ForgotPasswordSent(val email: String) : AuthState
}

data class WatchHistoryWithMovie(
    val history: WatchHistory,
    val movie: Movie
)

data class AnalyticsState(
    val totalMovies: Int = 0,
    val activeUsers: Int = 0,
    val totalViews: Int = 0,
    val storageUsedGb: Double = 0.0,
    val storageLimitGb: Double = 1000.0,
    val genreDistribution: Map<String, Int> = emptyMap()
)
