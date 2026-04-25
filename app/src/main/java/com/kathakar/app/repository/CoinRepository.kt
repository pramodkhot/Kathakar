package com.kathakar.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kathakar.app.domain.model.*
import com.kathakar.app.util.FirestoreCollections
import com.kathakar.app.util.Resource
import com.kathakar.app.util.unlockedDocId
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinRepository @Inject constructor(private val db: FirebaseFirestore) {

    suspend fun unlockEpisode(userId: String, authorId: String, episode: Episode): Resource<Int> {
        if (userId == authorId) return Resource.Error("Authors cannot unlock their own episodes")
        val userRef   = db.collection(FirestoreCollections.USERS).document(userId)
        val authorRef = db.collection(FirestoreCollections.USERS).document(authorId)
        val unlockRef = db.collection(FirestoreCollections.UNLOCKED_EPISODES).document(unlockedDocId(userId, episode.episodeId))
        val txnRef    = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        val aTxnRef   = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        return try {
            var newBalance = 0
            db.runTransaction { t ->
                val userSnap   = t.get(userRef)
                val unlockSnap = t.get(unlockRef)
                if (unlockSnap.exists()) { newBalance = (userSnap.getLong("coinBalance") ?: 0).toInt(); return@runTransaction }
                val balance = (userSnap.getLong("coinBalance") ?: 0).toInt()
                val cost    = episode.unlockCostCoins
                val credit  = cost * MvpConfig.AUTHOR_REVENUE_PERCENT / 100
                if (balance < cost) error("Need " + cost + " coins, have " + balance)
                newBalance = balance - cost
                t.update(userRef,   "coinBalance",        FieldValue.increment(-cost.toLong()))
                t.update(authorRef, "coinBalance",        FieldValue.increment(credit.toLong()))
                t.update(authorRef, "totalCoinsEarned",   FieldValue.increment(credit.toLong()))
                t.set(unlockRef, UnlockedEpisode(userId, episode.episodeId, episode.storyId, cost, Timestamp.now()))
                val unlockNote = "Unlocked: " + episode.title
                val earnNote   = "Earned: "   + episode.title
                t.set(txnRef,  CoinTransaction(txnRef.id,  userId,   CoinTxnType.EPISODE_UNLOCK,  -cost,   unlockNote, episode.episodeId, Timestamp.now()))
                t.set(aTxnRef, CoinTransaction(aTxnRef.id, authorId, CoinTxnType.AUTHOR_EARNING,   credit, earnNote,   episode.episodeId, Timestamp.now()))
            }.await()
            Resource.Success(newBalance)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Unlock failed") }
    }

    suspend fun getUnlockedIds(userId: String, storyId: String): Set<String> {
        return try {
            db.collection(FirestoreCollections.UNLOCKED_EPISODES)
                .whereEqualTo("userId", userId).whereEqualTo("storyId", storyId)
                .get().await().documents.mapNotNull { it.getString("episodeId") }.toSet()
        } catch (e: Exception) { emptySet() }
    }

    suspend fun getCoinHistory(userId: String): Resource<List<CoinTransaction>> {
        return try {
            val snap = db.collection(FirestoreCollections.COIN_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get().await()
            Resource.Success(snap.toObjects(CoinTransaction::class.java))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }
}
