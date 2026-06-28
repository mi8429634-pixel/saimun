package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class MovieRepository(
    private val movieDao: MovieDao,
    private val userDao: UserDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao,
    private val favoriteDao: FavoriteDao
) {
    // Movies
    val publishedMovies: Flow<List<Movie>> = movieDao.getPublishedMovies()
    val allMovies: Flow<List<Movie>> = movieDao.getAllMovies()

    fun getMovieById(id: Long): Flow<Movie?> = movieDao.getMovieById(id)
    
    suspend fun getMovieByIdSuspend(id: Long): Movie? = movieDao.getMovieByIdSuspend(id)

    suspend fun insertMovie(movie: Movie): Long = movieDao.insertMovie(movie)

    suspend fun updateMovie(movie: Movie) = movieDao.updateMovie(movie)

    suspend fun deleteMovie(movie: Movie) = movieDao.deleteMovie(movie)

    // User Profile
    fun getUserById(id: String): Flow<User?> = userDao.getUserById(id)
    
    suspend fun getUserByIdSuspend(id: String): User? = userDao.getUserByIdSuspend(id)

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    // Watch History
    fun getWatchHistory(userId: String): Flow<List<WatchHistory>> = watchHistoryDao.getWatchHistory(userId)

    suspend fun getWatchHistoryEntry(userId: String, movieId: Long): WatchHistory? =
        watchHistoryDao.getWatchHistoryEntry(userId, movieId)

    suspend fun insertWatchHistory(watchHistory: WatchHistory) =
        watchHistoryDao.insertWatchHistory(watchHistory)

    suspend fun deleteWatchHistory(userId: String, movieId: Long) =
        watchHistoryDao.deleteWatchHistory(userId, movieId)

    suspend fun clearWatchHistory(userId: String) =
        watchHistoryDao.clearWatchHistory(userId)

    // Watchlist
    fun getWatchlist(userId: String): Flow<List<WatchlistEntry>> = watchlistDao.getWatchlist(userId)

    suspend fun getWatchlistEntry(userId: String, movieId: Long): WatchlistEntry? =
        watchlistDao.getWatchlistEntry(userId, movieId)

    suspend fun insertWatchlist(entry: WatchlistEntry) =
        watchlistDao.insertWatchlist(entry)

    suspend fun deleteWatchlist(userId: String, movieId: Long) =
        watchlistDao.deleteWatchlist(userId, movieId)

    // Favorites
    fun getFavorites(userId: String): Flow<List<FavoriteEntry>> = favoriteDao.getFavorites(userId)

    suspend fun getFavoriteEntry(userId: String, movieId: Long): FavoriteEntry? =
        favoriteDao.getFavoriteEntry(userId, movieId)

    suspend fun insertFavorite(entry: FavoriteEntry) =
        favoriteDao.insertFavorite(entry)

    suspend fun deleteFavorite(userId: String, movieId: Long) =
        favoriteDao.deleteFavorite(userId, movieId)
}
