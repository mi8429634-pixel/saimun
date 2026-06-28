package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val synopsis: String,
    val posterUrl: String,
    val bannerUrl: String,
    val trailerUrl: String,
    val videoUrl: String,
    val genre: String, // e.g., "Action, Sci-Fi"
    val durationMinutes: Int,
    val rating: Double,
    val releaseYear: Int,
    val country: String,
    val language: String,
    val cast: String,
    val isPublished: Boolean = true,
    val isTrending: Boolean = false,
    val isLatest: Boolean = false,
    val isPopular: Boolean = false,
    val isTopRated: Boolean = false,
    val isRecommended: Boolean = false,
    val subtitlesUrl: String? = null,
    val audioTrack: String? = "English (Stereo 2.0)",
    val category: String = "Movie"
) : java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String, // e.g. email
    val email: String,
    val name: String,
    val avatarUrl: String,
    val role: String = "User" // "Admin", "Moderator", "User"
) : java.io.Serializable

@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val movieId: Long,
    val lastPositionMs: Long = 0,
    val totalDurationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val movieId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val movieId: Long,
    val timestamp: Long = System.currentTimeMillis()
)
