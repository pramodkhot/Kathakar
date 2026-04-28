package com.kathakar.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.kathakar.app.domain.model.*
import com.kathakar.app.util.FirestoreCollections
import com.kathakar.app.util.Resource
import com.kathakar.app.util.libraryDocId
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ── Firestore collection names ─────────────────────────────────────────────────
// Add POEMS and POEM_LIKES to FirestoreCollections util manually:
// const val POEMS      = "poems"
// const val POEM_LIKES = "poem_likes"

@Singleton
class StoryRepository @Inject constructor(private val db: FirebaseFirestore) {

    private val storiesCol  get() = db.collection(FirestoreCollections.STORIES)
    private val episodesCol get() = db.collection(FirestoreCollections.EPISODES)

    suspend fun getStories(
        category: String? = null,
        language: String? = null,
        lastVisible: DocumentSnapshot? = null
    ): Resource<Pair<List<Story>, DocumentSnapshot?>> {
        return try {
            var q: Query = storiesCol
                .whereEqualTo("status", "PUBLISHED")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
            category?.let { q = q.whereEqualTo("category", it) }
            language?.let  { q = q.whereEqualTo("language", it) }
            lastVisible?.let { q = q.startAfter(it) }
            val snap = q.get().await()
            val list = snap.toObjects(Story::class.java)
            val next = if (snap.size() >= 20) snap.documents.lastOrNull() else null
            Resource.Success(list to next)
        } catch (e: Exception) { Resource.Error("Failed to load stories: " + e.localizedMessage) }
    }

    suspend fun searchStories(query: String): Resource<List<Story>> {
        return try {
            val token = query.lowercase().trim().split(" ")
                .firstOrNull { it.length >= 2 } ?: return Resource.Success(emptyList())
            val snap = storiesCol.whereEqualTo("status", "PUBLISHED")
                .whereArrayContains("searchTokens", token).limit(20).get().await()
            Resource.Success(snap.toObjects(Story::class.java))
        } catch (e: Exception) { Resource.Error("Search failed: " + e.localizedMessage) }
    }

    suspend fun getStory(storyId: String): Resource<Story> {
        return try {
            val s = storiesCol.document(storyId).get().await().toObject(Story::class.java)
                ?: return Resource.Error("Story not found")
            Resource.Success(s)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getEpisodes(storyId: String): Resource<List<Episode>> {
        return try {
            val snap = episodesCol.whereEqualTo("storyId", storyId).get().await()
            val list = snap.toObjects(Episode::class.java)
                .filter { it.status == "PUBLISHED" }.sortedBy { it.chapterNumber }
            Resource.Success(list)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getAuthorEpisodes(storyId: String): Resource<List<Episode>> {
        return try {
            val snap = episodesCol.whereEqualTo("storyId", storyId).get().await()
            Resource.Success(snap.toObjects(Episode::class.java).sortedBy { it.chapterNumber })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getEpisode(episodeId: String): Resource<Episode> {
        return try {
            val ep = episodesCol.document(episodeId).get().await().toObject(Episode::class.java)
                ?: return Resource.Error("Episode not found")
            Resource.Success(ep)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getAuthorStories(authorId: String): Resource<List<Story>> {
        return try {
            val snap = storiesCol.whereEqualTo("authorId", authorId).get().await()
            Resource.Success(snap.toObjects(Story::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun saveStory(story: Story): Resource<String> {
        return try {
            val ref = if (story.storyId.isEmpty()) storiesCol.document()
                      else storiesCol.document(story.storyId)
            val tokens = generateSearchTokens(story.title, story.authorName)
            val data = HashMap<String, Any>()
            data["storyId"]       = ref.id
            data["title"]         = story.title
            data["description"]   = story.description
            data["coverUrl"]      = story.coverUrl
            data["authorId"]      = story.authorId
            data["authorName"]    = story.authorName
            data["category"]      = story.category
            data["language"]      = story.language
            data["searchTokens"]  = tokens
            data["status"]        = story.status
            data["totalEpisodes"] = story.totalEpisodes
            data["totalReads"]    = story.totalReads
            data["updatedAt"]     = Timestamp.now()
            if (story.createdAt == null) data["createdAt"] = Timestamp.now()
            else data["createdAt"] = story.createdAt
            ref.set(data).await()
            if (story.storyId.isEmpty()) {
                db.collection(FirestoreCollections.USERS).document(story.authorId)
                    .update("storiesCount", FieldValue.increment(1)).await()
            }
            Resource.Success(ref.id)
        } catch (e: Exception) { Resource.Error("Failed to save: " + e.localizedMessage) }
    }

    suspend fun saveEpisode(episode: Episode): Resource<String> {
        return try {
            val ref = if (episode.episodeId.isEmpty()) episodesCol.document()
                      else episodesCol.document(episode.episodeId)
            val wc = episode.content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            val isFreeChapter = (episode.chapterNumber == 1)
            val data = HashMap<String, Any>()
            data["episodeId"]       = ref.id
            data["storyId"]         = episode.storyId
            data["authorId"]        = episode.authorId
            data["chapterNumber"]   = episode.chapterNumber
            data["title"]           = episode.title
            data["content"]         = episode.content
            data["wordCount"]       = wc
            data["unlockCostCoins"] = if (isFreeChapter) 0 else episode.unlockCostCoins
            data["isFree"]          = isFreeChapter
            data["status"]          = episode.status
            if (episode.createdAt == null) data["createdAt"] = Timestamp.now()
            else data["createdAt"] = episode.createdAt
            ref.set(data).await()
            if (episode.episodeId.isEmpty() && episode.status == "PUBLISHED") {
                storiesCol.document(episode.storyId)
                    .update("totalEpisodes", FieldValue.increment(1)).await()
            }
            Resource.Success(ref.id)
        } catch (e: Exception) { Resource.Error("Failed to save: " + e.localizedMessage) }
    }

    suspend fun deleteEpisode(episodeId: String, storyId: String): Resource<Unit> {
        return try {
            episodesCol.document(episodeId).delete().await()
            storiesCol.document(storyId)
                .update("totalEpisodes", FieldValue.increment(-1)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error("Failed to delete: " + e.localizedMessage) }
    }

    suspend fun updateEpisode(episodeId: String, title: String, content: String): Resource<Unit> {
        return try {
            val wc = content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            val updates = HashMap<String, Any>()
            updates["title"]     = title
            updates["content"]   = content
            updates["wordCount"] = wc
            episodesCol.document(episodeId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error("Failed to update: " + e.localizedMessage) }
    }
}

// ── Poem Repository ───────────────────────────────────────────────────────────
@Singleton
class PoemRepository @Inject constructor(private val db: FirebaseFirestore) {

    private val poemsCol    get() = db.collection("poems")
    private val likesCol    get() = db.collection("poem_likes")

    // All poems free to read — no locking ever
    suspend fun getPoems(
        format: String? = null,
        language: String? = null,
        mood: String? = null,
        lastVisible: DocumentSnapshot? = null
    ): Resource<Pair<List<Poem>, DocumentSnapshot?>> {
        return try {
            var q: Query = poemsCol
                .whereEqualTo("status", "PUBLISHED")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
            format?.let   { q = q.whereEqualTo("format", it) }
            language?.let { q = q.whereEqualTo("language", it) }
            mood?.let     { q = q.whereEqualTo("mood", it) }
            lastVisible?.let { q = q.startAfter(it) }
            val snap = q.get().await()
            val list = snap.toObjects(Poem::class.java)
            val next = if (snap.size() >= 30) snap.documents.lastOrNull() else null
            Resource.Success(list to next)
        } catch (e: Exception) { Resource.Error("Failed to load poems: " + e.localizedMessage) }
    }

    suspend fun searchPoems(query: String): Resource<List<Poem>> {
        return try {
            val token = query.lowercase().trim().split(" ")
                .firstOrNull { it.length >= 2 } ?: return Resource.Success(emptyList())
            val snap = poemsCol.whereEqualTo("status", "PUBLISHED")
                .whereArrayContains("searchTokens", token).limit(20).get().await()
            Resource.Success(snap.toObjects(Poem::class.java))
        } catch (e: Exception) { Resource.Error("Search failed: " + e.localizedMessage) }
    }

    suspend fun getPoem(poemId: String): Resource<Poem> {
        return try {
            val p = poemsCol.document(poemId).get().await().toObject(Poem::class.java)
                ?: return Resource.Error("Poem not found")
            Resource.Success(p)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getAuthorPoems(authorId: String): Resource<List<Poem>> {
        return try {
            val snap = poemsCol.whereEqualTo("authorId", authorId).get().await()
            Resource.Success(snap.toObjects(Poem::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun savePoem(poem: Poem): Resource<String> {
        return try {
            val ref = if (poem.poemId.isEmpty()) poemsCol.document()
                      else poemsCol.document(poem.poemId)
            val tokens = generateSearchTokens(poem.title, poem.authorName)
            val data = HashMap<String, Any>()
            data["poemId"]      = ref.id
            data["title"]       = poem.title
            data["content"]     = poem.content
            data["authorId"]    = poem.authorId
            data["authorName"]  = poem.authorName
            data["format"]      = poem.format
            data["language"]    = poem.language
            data["mood"]        = poem.mood
            data["searchTokens"]= tokens
            data["likesCount"]  = poem.likesCount
            data["tipsCount"]   = poem.tipsCount
            data["totalTipsCoins"] = poem.totalTipsCoins
            data["status"]      = "PUBLISHED"  // poems always published immediately
            if (poem.createdAt == null) data["createdAt"] = Timestamp.now()
            else data["createdAt"] = poem.createdAt
            ref.set(data).await()
            if (poem.poemId.isEmpty()) {
                db.collection(FirestoreCollections.USERS).document(poem.authorId)
                    .update("poemsCount", FieldValue.increment(1)).await()
            }
            Resource.Success(ref.id)
        } catch (e: Exception) { Resource.Error("Failed to save poem: " + e.localizedMessage) }
    }

    suspend fun updatePoem(poemId: String, title: String, content: String,
                           format: String, language: String, mood: String): Resource<Unit> {
        return try {
            val updates = HashMap<String, Any>()
            updates["title"]    = title
            updates["content"]  = content
            updates["format"]   = format
            updates["language"] = language
            updates["mood"]     = mood
            poemsCol.document(poemId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun deletePoem(poemId: String, authorId: String): Resource<Unit> {
        return try {
            poemsCol.document(poemId).delete().await()
            db.collection(FirestoreCollections.USERS).document(authorId)
                .update("poemsCount", FieldValue.increment(-1)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    // Like / unlike toggle
    suspend fun toggleLike(userId: String, poemId: String): Resource<Boolean> {
        val likeDocId = userId + "_" + poemId
        val likeRef   = likesCol.document(likeDocId)
        val poemRef   = poemsCol.document(poemId)
        return try {
            var isNowLiked = false
            db.runTransaction { t ->
                val likeSnap = t.get(likeRef)
                if (likeSnap.exists()) {
                    t.delete(likeRef)
                    t.update(poemRef, "likesCount", FieldValue.increment(-1))
                    isNowLiked = false
                } else {
                    t.set(likeRef, PoemLike(userId, poemId, Timestamp.now()))
                    t.update(poemRef, "likesCount", FieldValue.increment(1))
                    isNowLiked = true
                }
            }.await()
            Resource.Success(isNowLiked)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Like failed") }
    }

    suspend fun isLiked(userId: String, poemId: String): Boolean {
        return try { likesCol.document(userId + "_" + poemId).get().await().exists() }
        catch (e: Exception) { false }
    }

    // Tip the poet — reader sends 1-5 coins to poet
    suspend fun tipPoet(
        fromUserId: String,
        toUserId: String,
        poem: Poem,
        coins: Int
    ): Resource<Int> {
        val safeCoins = coins.coerceIn(MvpConfig.POEM_TIP_MIN, MvpConfig.POEM_TIP_MAX)
        val fromRef = db.collection(FirestoreCollections.USERS).document(fromUserId)
        val toRef   = db.collection(FirestoreCollections.USERS).document(toUserId)
        val poemRef = poemsCol.document(poem.poemId)
        val txnFrom = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        val txnTo   = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        return try {
            var newBalance = 0
            db.runTransaction { t ->
                val fromSnap = t.get(fromRef)
                val balance  = (fromSnap.getLong("coinBalance") ?: 0).toInt()
                if (balance < safeCoins) error("Need " + safeCoins + " coins to tip, have " + balance)
                newBalance = balance - safeCoins
                t.update(fromRef, "coinBalance",      FieldValue.increment(-safeCoins.toLong()))
                t.update(toRef,   "coinBalance",      FieldValue.increment(safeCoins.toLong()))
                t.update(toRef,   "totalCoinsEarned", FieldValue.increment(safeCoins.toLong()))
                t.update(poemRef, "tipsCount",        FieldValue.increment(1))
                t.update(poemRef, "totalTipsCoins",   FieldValue.increment(safeCoins.toLong()))
                val tipNote    = "Tipped " + safeCoins + " coins on: " + poem.title
                val receiveNote= "Received tip for: " + poem.title
                t.set(txnFrom, CoinTransaction(txnFrom.id, fromUserId, CoinTxnType.POEM_TIP,
                    -safeCoins, tipNote, relatedPoemId = poem.poemId, createdAt = Timestamp.now()))
                t.set(txnTo, CoinTransaction(txnTo.id, toUserId, CoinTxnType.POEM_TIP_RECEIVED,
                    safeCoins, receiveNote, relatedPoemId = poem.poemId, createdAt = Timestamp.now()))
            }.await()
            Resource.Success(newBalance)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Tip failed") }
    }
}

@Singleton
class LibraryRepository @Inject constructor(private val db: FirebaseFirestore) {
    private val libCol get() = db.collection(FirestoreCollections.LIBRARY)

    suspend fun getUserLibrary(userId: String): Resource<List<LibraryEntry>> {
        return try {
            val snap = libCol.whereEqualTo("userId", userId).get().await()
            Resource.Success(snap.toObjects(LibraryEntry::class.java)
                .sortedByDescending { it.lastReadAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getEntry(userId: String, storyId: String): LibraryEntry? {
        return try { libCol.document(libraryDocId(userId, storyId)).get().await()
            .toObject(LibraryEntry::class.java) } catch (e: Exception) { null }
    }

    suspend fun upsert(entry: LibraryEntry) {
        try { libCol.document(libraryDocId(entry.userId, entry.storyId)).set(entry).await() }
        catch (_: Exception) { }
    }
}
