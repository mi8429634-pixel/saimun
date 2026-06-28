package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.Movie
import com.example.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseService {
    private const val TAG = "FirebaseService"
    
    var isInitialized = false
        private set

    val auth: FirebaseAuth?
        get() = if (isInitialized) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    val firestore: FirebaseFirestore?
        get() = if (isInitialized) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    val storage: FirebaseStorage?
        get() = if (isInitialized) {
            try {
                FirebaseStorage.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    fun initialize(context: Context) {
        try {
            var app = FirebaseApp.getApps(context).firstOrNull()
            if (app == null) {
                app = FirebaseApp.initializeApp(context)
            }
            if (app != null) {
                // Ensure we can retrieve actual SDK instances without throwing
                FirebaseAuth.getInstance(app)
                FirebaseFirestore.getInstance(app)
                FirebaseStorage.getInstance(app)
                isInitialized = true
                Log.d(TAG, "Firebase services successfully verified and active.")
            } else {
                isInitialized = false
                Log.w(TAG, "Firebase initialized but app instance is null. Falling back to offline/cache mode.")
            }
        } catch (e: Exception) {
            isInitialized = false
            Log.w(TAG, "Firebase failed to initialize or verify. Running in offline/cache fallback mode. Reason: ${e.localizedMessage}")
        }
    }

    // AUTH SERVICE INTEGRATION
    suspend fun loginWithFirebase(email: String, pword: String): User? {
        val authInstance = auth ?: return null
        return try {
            val result = authInstance.signInWithEmailAndPassword(email, pword).await()
            val firebaseUser = result.user ?: return null
            
            // Fetch profile details from Firestore if available
            val firestoreInstance = firestore
            var userRole = if (email.startsWith("admin")) "Admin" else "User"
            var userName = firebaseUser.displayName ?: email.substringBefore("@")
            var userAvatar = if (email.startsWith("admin")) "avatar_admin" else "avatar_user_1"

            if (firestoreInstance != null) {
                try {
                    val doc = firestoreInstance.collection("users").document(firebaseUser.uid).get().await()
                    if (doc.exists()) {
                        userName = doc.getString("name") ?: userName
                        userRole = doc.getString("role") ?: userRole
                        userAvatar = doc.getString("avatarUrl") ?: userAvatar
                    } else {
                        // Seed user profile doc
                        val profile = mapOf(
                            "id" to firebaseUser.uid,
                            "email" to email,
                            "name" to userName,
                            "avatarUrl" to userAvatar,
                            "role" to userRole
                        )
                        firestoreInstance.collection("users").document(firebaseUser.uid).set(profile).await()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching user record from Firestore: ${e.message}")
                }
            }

            User(
                id = firebaseUser.uid,
                email = email,
                name = userName,
                avatarUrl = userAvatar,
                role = userRole
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Login failed: ${e.message}")
            throw e
        }
    }

    suspend fun signUpWithFirebase(name: String, email: String, pword: String, role: String = "User"): User? {
        val authInstance = auth ?: return null
        return try {
            val result = authInstance.createUserWithEmailAndPassword(email, pword).await()
            val firebaseUser = result.user ?: return null
            
            val userAvatar = if (email.startsWith("admin")) "avatar_admin" else "avatar_user_2"
            val user = User(
                id = firebaseUser.uid,
                email = email,
                name = name,
                avatarUrl = userAvatar,
                role = role
            )

            val firestoreInstance = firestore
            if (firestoreInstance != null) {
                val profile = mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "name" to user.name,
                    "avatarUrl" to user.avatarUrl,
                    "role" to user.role
                )
                firestoreInstance.collection("users").document(user.id).set(profile).await()
            }

            user
        } catch (e: Exception) {
            Log.e(TAG, "Firebase SignUp failed: ${e.message}")
            throw e
        }
    }

    fun logout() {
        auth?.signOut()
    }

    suspend fun forgotPassword(email: String) {
        val authInstance = auth ?: throw IllegalStateException("Firebase Auth not available")
        authInstance.sendPasswordResetEmail(email).await()
    }

    suspend fun updateUserProfile(userId: String, name: String, avatarUrl: String) {
        val firestoreInstance = firestore ?: return
        try {
            val updates = mapOf(
                "name" to name,
                "avatarUrl" to avatarUrl
            )
            firestoreInstance.collection("users").document(userId).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile in Firestore: ${e.message}")
        }
    }

    // FIRESTORE MOVIES CRUD
    suspend fun fetchMoviesFromFirestore(): List<Movie> {
        val firestoreInstance = firestore ?: return emptyList()
        return try {
            val snapshot = firestoreInstance.collection("movies").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    Movie(
                        id = doc.getLong("id") ?: doc.id.hashCode().toLong(),
                        title = doc.getString("title") ?: "",
                        synopsis = doc.getString("synopsis") ?: "",
                        posterUrl = doc.getString("posterUrl") ?: "",
                        bannerUrl = doc.getString("bannerUrl") ?: "",
                        trailerUrl = doc.getString("trailerUrl") ?: "",
                        videoUrl = doc.getString("videoUrl") ?: "",
                        genre = doc.getString("genre") ?: "Uncategorized",
                        durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 120,
                        rating = doc.getDouble("rating") ?: 8.0,
                        releaseYear = doc.getLong("releaseYear")?.toInt() ?: 2026,
                        country = doc.getString("country") ?: "USA",
                        language = doc.getString("language") ?: "English",
                        cast = doc.getString("cast") ?: "Unknown Cast",
                        isPublished = doc.getBoolean("isPublished") ?: true,
                        isTrending = doc.getBoolean("isTrending") ?: false,
                        isLatest = doc.getBoolean("isLatest") ?: false,
                        isPopular = doc.getBoolean("isPopular") ?: false,
                        isTopRated = doc.getBoolean("isTopRated") ?: false,
                        isRecommended = doc.getBoolean("isRecommended") ?: false,
                        subtitlesUrl = doc.getString("subtitlesUrl"),
                        audioTrack = doc.getString("audioTrack") ?: "English (Stereo 2.0)",
                        category = doc.getString("category") ?: "Movie"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing movie document: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore fetch movies failed: ${e.message}")
            emptyList()
        }
    }

    fun listenToMoviesFromFirestore(): Flow<List<Movie>> = callbackFlow {
        val firestoreInstance = firestore
        if (firestoreInstance == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestoreInstance.collection("movies")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Movie(
                                id = doc.getLong("id") ?: doc.id.hashCode().toLong(),
                                title = doc.getString("title") ?: "",
                                synopsis = doc.getString("synopsis") ?: "",
                                posterUrl = doc.getString("posterUrl") ?: "",
                                bannerUrl = doc.getString("bannerUrl") ?: "",
                                trailerUrl = doc.getString("trailerUrl") ?: "",
                                videoUrl = doc.getString("videoUrl") ?: "",
                                genre = doc.getString("genre") ?: "Uncategorized",
                                durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 120,
                                rating = doc.getDouble("rating") ?: 8.0,
                                releaseYear = doc.getLong("releaseYear")?.toInt() ?: 2026,
                                country = doc.getString("country") ?: "USA",
                                language = doc.getString("language") ?: "English",
                                cast = doc.getString("cast") ?: "Unknown Cast",
                                isPublished = doc.getBoolean("isPublished") ?: true,
                                isTrending = doc.getBoolean("isTrending") ?: false,
                                isLatest = doc.getBoolean("isLatest") ?: false,
                                isPopular = doc.getBoolean("isPopular") ?: false,
                                isTopRated = doc.getBoolean("isTopRated") ?: false,
                                isRecommended = doc.getBoolean("isRecommended") ?: false,
                                subtitlesUrl = doc.getString("subtitlesUrl"),
                                audioTrack = doc.getString("audioTrack") ?: "English (Stereo 2.0)",
                                category = doc.getString("category") ?: "Movie"
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing doc in snapshot listener: ${e.message}")
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose {
            listener.remove()
        }
    }

    suspend fun saveMovieToFirestore(movie: Movie) {
        val firestoreInstance = firestore ?: return
        try {
            val docId = if (movie.id == 0L) UUID.randomUUID().toString() else movie.id.toString()
            val data = mapOf(
                "id" to if (movie.id == 0L) docId.hashCode().toLong() else movie.id,
                "title" to movie.title,
                "synopsis" to movie.synopsis,
                "posterUrl" to movie.posterUrl,
                "bannerUrl" to movie.bannerUrl,
                "trailerUrl" to movie.trailerUrl,
                "videoUrl" to movie.videoUrl,
                "genre" to movie.genre,
                "durationMinutes" to movie.durationMinutes,
                "rating" to movie.rating,
                "releaseYear" to movie.releaseYear,
                "country" to movie.country,
                "language" to movie.language,
                "cast" to movie.cast,
                "isPublished" to movie.isPublished,
                "isTrending" to movie.isTrending,
                "isLatest" to movie.isLatest,
                "isPopular" to movie.isPopular,
                "isTopRated" to movie.isTopRated,
                "isRecommended" to movie.isRecommended,
                "subtitlesUrl" to movie.subtitlesUrl,
                "audioTrack" to movie.audioTrack,
                "category" to movie.category
            )
            firestoreInstance.collection("movies").document(docId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore save movie failed: ${e.message}")
            throw e
        }
    }

    suspend fun deleteMovieFromFirestore(movie: Movie) {
        val firestoreInstance = firestore ?: return
        try {
            firestoreInstance.collection("movies").document(movie.id.toString()).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore delete movie failed: ${e.message}")
            throw e
        }
    }

    // FIREBASE STORAGE STREAMED RESUMABLE UPLOAD ENGINE
    fun uploadFileWithProgress(
        localUri: Uri,
        storagePath: String
    ): Flow<UploadStatus> = callbackFlow {
        val storageInstance = storage
        if (storageInstance == null) {
            trySend(UploadStatus.Error("Firebase Storage not configured. File caching locally."))
            close()
            return@callbackFlow
        }

        val ref = storageInstance.reference.child(storagePath)
        val uploadTask = ref.putFile(localUri)

        val progressListener = { snapshot: com.google.firebase.storage.UploadTask.TaskSnapshot ->
            val bytesTransferred = snapshot.bytesTransferred
            val totalByteCount = snapshot.totalByteCount
            val progress = if (totalByteCount > 0) {
                (100.0 * bytesTransferred / totalByteCount).toInt()
            } else 0
            trySend(UploadStatus.Progress(progress, bytesTransferred, totalByteCount))
            Unit
        }

        uploadTask.addOnProgressListener(progressListener)
            .addOnPausedListener {
                trySend(UploadStatus.Paused)
                Unit
            }
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    trySend(UploadStatus.Success(downloadUrl.toString()))
                    close()
                }.addOnFailureListener { e ->
                    trySend(UploadStatus.Error(e.localizedMessage ?: "Failed to resolve download URL"))
                    close()
                }
            }
            .addOnFailureListener { e ->
                trySend(UploadStatus.Error(e.localizedMessage ?: "File upload failed"))
                close()
            }

        awaitClose {
            // Cancel task if scope closes before success
            if (!uploadTask.isComplete && !uploadTask.isSuccessful) {
                uploadTask.cancel()
            }
        }
    }
}

sealed interface UploadStatus {
    data class Progress(val percentage: Int, val bytesTransferred: Long, val totalBytes: Long) : UploadStatus
    object Paused : UploadStatus
    data class Success(val downloadUrl: String) : UploadStatus
    data class Error(val errorMsg: String) : UploadStatus
}
