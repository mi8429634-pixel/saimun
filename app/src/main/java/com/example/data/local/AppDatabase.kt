package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Movie::class,
        User::class,
        WatchHistory::class,
        WatchlistEntry::class,
        FavoriteEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun userDao(): UserDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "movie_premium_db"
                )
                .addCallback(DatabaseCallback(context))
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val dbInstance = getDatabase(context)
                    val movieDao = dbInstance.movieDao()
                    val userDao = dbInstance.userDao()
                    
                    // Create Admin and Default User
                    userDao.insertUser(
                        User(
                            id = "admin@moviepremium.com",
                            email = "admin@moviepremium.com",
                            name = "Administrator",
                            avatarUrl = "avatar_admin",
                            role = "Admin"
                        )
                    )
                    userDao.insertUser(
                        User(
                            id = "user@moviepremium.com",
                            email = "user@moviepremium.com",
                            name = "Premium User",
                            avatarUrl = "avatar_user_1",
                            role = "User"
                        )
                    )

                    // Insert pre-populated high-fidelity movies
                    val defaultMovies = listOf(
                        Movie(
                            id = 1,
                            title = "Interstellar",
                            synopsis = "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival. Faced with dwindling resources on Earth and mysterious anomalies, they embark on a journey that bends both space and time.",
                            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop&q=60",
                            bannerUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1000&auto=format&fit=crop&q=80",
                            trailerUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                            genre = "Sci-Fi, Adventure, Drama",
                            durationMinutes = 169,
                            rating = 8.7,
                            releaseYear = 2014,
                            country = "USA",
                            language = "English",
                            cast = "Matthew McConaughey, Anne Hathaway, Jessica Chastain, Michael Caine",
                            isPublished = true,
                            isTrending = true,
                            isLatest = false,
                            isPopular = true,
                            isTopRated = true,
                            isRecommended = true,
                            subtitlesUrl = "English (SRT)",
                            audioTrack = "English (Dolby Atmos)"
                        ),
                        Movie(
                            id = 2,
                            title = "The Dark Knight",
                            synopsis = "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.",
                            posterUrl = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=500&auto=format&fit=crop&q=60",
                            bannerUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1000&auto=format&fit=crop&q=80",
                            trailerUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                            genre = "Action, Crime, Drama",
                            durationMinutes = 152,
                            rating = 9.0,
                            releaseYear = 2008,
                            country = "USA",
                            language = "English",
                            cast = "Christian Bale, Heath Ledger, Aaron Eckhart, Maggie Gyllenhaal",
                            isPublished = true,
                            isTrending = true,
                            isLatest = false,
                            isPopular = true,
                            isTopRated = true,
                            isRecommended = false,
                            subtitlesUrl = "English (SRT)",
                            audioTrack = "English (DTS-HD)"
                        ),
                        Movie(
                            id = 3,
                            title = "Inception",
                            synopsis = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O., but his tragic past may doom the project.",
                            posterUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&auto=format&fit=crop&q=60",
                            bannerUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1000&auto=format&fit=crop&q=80",
                            trailerUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                            genre = "Sci-Fi, Action, Thriller",
                            durationMinutes = 148,
                            rating = 8.8,
                            releaseYear = 2010,
                            country = "USA",
                            language = "English",
                            cast = "Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page, Tom Hardy",
                            isPublished = true,
                            isTrending = false,
                            isLatest = true,
                            isPopular = true,
                            isTopRated = false,
                            isRecommended = true,
                            subtitlesUrl = "English (SRT), Spanish (SRT)",
                            audioTrack = "English (Stereo 2.0)"
                        ),
                        Movie(
                            id = 4,
                            title = "Dune: Part Two",
                            synopsis = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family. Facing a choice between the love of his life and the fate of the universe, he endeavors to prevent a terrible future.",
                            posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=60",
                            bannerUrl = "https://images.unsplash.com/photo-1547483238-f400e65ccd56?w=1000&auto=format&fit=crop&q=80",
                            trailerUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                            genre = "Sci-Fi, Adventure, Action",
                            durationMinutes = 166,
                            rating = 8.9,
                            releaseYear = 2024,
                            country = "USA",
                            language = "English",
                            cast = "Timothée Chalamet, Zendaya, Rebecca Ferguson, Josh Brolin",
                            isPublished = true,
                            isTrending = true,
                            isLatest = true,
                            isPopular = true,
                            isTopRated = true,
                            isRecommended = true,
                            subtitlesUrl = "English (SRT)",
                            audioTrack = "English (Dolby Digital 5.1)"
                        ),
                        Movie(
                            id = 5,
                            title = "Cyberpunk Neo-Tokyo",
                            synopsis = "In the year 2088, an underground netrunner uncovers a dangerous corporate conspiracy in the neon-drenched metropolis of Neo-Tokyo, risking everything to release the truth to the grid.",
                            posterUrl = "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=500&auto=format&fit=crop&q=60",
                            bannerUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1000&auto=format&fit=crop&q=80",
                            trailerUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                            genre = "Sci-Fi, Action, Cyberpunk",
                            durationMinutes = 118,
                            rating = 8.2,
                            releaseYear = 2026,
                            country = "Japan",
                            language = "Japanese",
                            cast = "Ken Watanabe, Hikari Mitsushima, Tadanobu Asano",
                            isPublished = true,
                            isTrending = false,
                            isLatest = true,
                            isPopular = false,
                            isTopRated = false,
                            isRecommended = true,
                            subtitlesUrl = "English (SRT), Japanese (SRT)",
                            audioTrack = "Japanese (Stereo 2.0)"
                        )
                    )
                    movieDao.insertMovies(defaultMovies)
                }
            }
        }
    }
}
