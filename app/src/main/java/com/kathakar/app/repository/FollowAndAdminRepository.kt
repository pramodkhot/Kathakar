package com.kathakar.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kathakar.app.domain.model.*
import com.kathakar.app.util.FirestoreCollections
import com.kathakar.app.util.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowRepository @Inject constructor(private val db: FirebaseFirestore) {

    suspend fun isFollowing(followerId: String, followeeId: String): Boolean {
        return try { db.collection(FirestoreCollections.FOLLOWS)
            .document(followerId + "_" + followeeId).get().await().exists()
        } catch (e: Exception) { false }
    }

    suspend fun toggleFollow(followerId: String, followeeId: String): Resource<Boolean> {
        if (followerId == followeeId) return Resource.Error("Cannot follow yourself")
        return try {
            val docRef      = db.collection(FirestoreCollections.FOLLOWS).document(followerId + "_" + followeeId)
            val followerRef = db.collection(FirestoreCollections.USERS).document(followerId)
            val followeeRef = db.collection(FirestoreCollections.USERS).document(followeeId)
            val exists      = docRef.get().await().exists()
            db.runTransaction { t ->
                if (exists) {
                    t.delete(docRef)
                    t.update(followerRef, "followingCount", FieldValue.increment(-1))
                    t.update(followeeRef, "followersCount", FieldValue.increment(-1))
                } else {
                    t.set(docRef, Follow(followerId, followeeId, Timestamp.now()))
                    t.update(followerRef, "followingCount", FieldValue.increment(1))
                    t.update(followeeRef, "followersCount", FieldValue.increment(1))
                }
            }.await()
            Resource.Success(!exists)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Follow failed") }
    }

    suspend fun getFollowingIds(userId: String): Set<String> {
        return try {
            db.collection(FirestoreCollections.FOLLOWS).whereEqualTo("followerId", userId)
                .get().await().documents.mapNotNull { it.getString("followeeId") }.toSet()
        } catch (e: Exception) { emptySet() }
    }
}

@Singleton
class AdminRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val authRepo: AuthRepository
) {
    suspend fun getAllUsers(): Resource<List<User>> {
        return try {
            val snap = db.collection(FirestoreCollections.USERS)
                .orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            Resource.Success(snap.documents.mapNotNull { authRepo.mapToUser(it.id, it.data) })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getAllStories(): Resource<List<Story>> {
        return try {
            val snap = db.collection(FirestoreCollections.STORIES)
                .orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            Resource.Success(snap.toObjects(Story::class.java))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun updateUserRole(userId: String, role: UserRole): Resource<Unit> {
        return try {
            db.collection(FirestoreCollections.USERS).document(userId)
                .update("role", role.name).await(); Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun toggleBan(userId: String, ban: Boolean): Resource<Unit> {
        return try {
            db.collection(FirestoreCollections.USERS).document(userId)
                .update("isBanned", ban).await(); Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun updateStoryStatus(storyId: String, status: String): Resource<Unit> {
        return try {
            db.collection(FirestoreCollections.STORIES).document(storyId)
                .update("status", status).await(); Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun deleteStory(storyId: String): Resource<Unit> {
        return try {
            db.collection(FirestoreCollections.STORIES).document(storyId)
                .delete().await(); Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getStats(): AdminStats {
        return try {
            val users   = db.collection(FirestoreCollections.USERS).get().await().size()
            val stories = db.collection(FirestoreCollections.STORIES).get().await().size()
            val coins   = db.collection(FirestoreCollections.COIN_TRANSACTIONS).get().await()
                .documents.sumOf { ((it.getLong("coinsAmount") ?: 0L).toInt()).coerceAtLeast(0) }
            AdminStats(users, stories, coins)
        } catch (e: Exception) { AdminStats() }
    }
}
