package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE isPublished = 1")
    fun getPublishedMovies(): Flow<List<Movie>>

    @Query("SELECT * FROM movies")
    fun getAllMovies(): Flow<List<Movie>>

    @Query("SELECT * FROM movies WHERE id = :id")
    fun getMovieById(id: Long): Flow<Movie?>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieByIdSuspend(id: Long): Movie?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: Movie): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<Movie>)

    @Update
    suspend fun updateMovie(movie: Movie)

    @Delete
    suspend fun deleteMovie(movie: Movie)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserByIdSuspend(id: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWatchHistory(userId: String): Flow<List<WatchHistory>>

    @Query("SELECT * FROM watch_history WHERE userId = :userId AND movieId = :movieId LIMIT 1")
    suspend fun getWatchHistoryEntry(userId: String, movieId: Long): WatchHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(watchHistory: WatchHistory)

    @Query("DELETE FROM watch_history WHERE userId = :userId AND movieId = :movieId")
    suspend fun deleteWatchHistory(userId: String, movieId: Long)

    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun clearWatchHistory(userId: String)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWatchlist(userId: String): Flow<List<WatchlistEntry>>

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND movieId = :movieId LIMIT 1")
    suspend fun getWatchlistEntry(userId: String, movieId: Long): WatchlistEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(entry: WatchlistEntry)

    @Query("DELETE FROM watchlist WHERE userId = :userId AND movieId = :movieId")
    suspend fun deleteWatchlist(userId: String, movieId: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY timestamp DESC")
    fun getFavorites(userId: String): Flow<List<FavoriteEntry>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND movieId = :movieId LIMIT 1")
    suspend fun getFavoriteEntry(userId: String, movieId: Long): FavoriteEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entry: FavoriteEntry)

    @Query("DELETE FROM favorites WHERE userId = :userId AND movieId = :movieId")
    suspend fun deleteFavorite(userId: String, movieId: Long)
}
