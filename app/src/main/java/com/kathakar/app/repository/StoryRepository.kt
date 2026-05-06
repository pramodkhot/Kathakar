package com.kathakar.app.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.kathakar.app.domain.model.*
import com.kathakar.app.util.FirestoreCollections
import com.kathakar.app.util.Resource
import com.kathakar.app.util.libraryDocId
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ── Firestore collection names ─────────────────────────────────────────────────
// (add new ones alongside existing FirestoreCollections object)
private object Col {
    const val STORIES          = "stories"
    const val EPISODES         = "episodes"
    const val RATINGS          = "ratings"
    const val COMMENTS         = "comments"
    const val EPISODE_LIKES    = "episode_likes"
    const val READING_PROGRESS = "reading_progress"
    const val NOTIFICATIONS    = "notifications"
    const val USERS            = "users"
    const val UNLOCKED         = "unlocked_episodes"
    const val LIBRARY          = "library"
    const val COIN_TXN         = "coin_transactions"
    const val FOLLOWS          = "follows"
    const val POEMS            = "poems"
    const val POEM_LIKES       = "poem_likes"
}

// ── Story Repository ───────────────────────────────────────────────────────────
@Singleton
class StoryRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val storiesCol  get() = db.collection(Col.STORIES)
    private val episodesCol get() = db.collection(Col.EPISODES)

    // ── Stories ────────────────────────────────────────────────────────────────
    suspend fun getStories(
        category: String? = null,
        language: String? = null,
        lastVisible: DocumentSnapshot? = null
    ): Resource<Pair<List<Story>, DocumentSnapshot?>> {
        return try {
            var q: Query = storiesCol.whereEqualTo("status", "PUBLISHED")
            category?.let { q = q.whereEqualTo("category", it) }
            language?.let  { q = q.whereEqualTo("language", it) }
            val snap   = q.get().await()
            val sorted = snap.toObjects(Story::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 }
            val startIdx = if (lastVisible != null) {
                val idx = sorted.indexOfFirst { it.storyId == lastVisible.id }
                if (idx >= 0) idx + 1 else 0
            } else 0
            val page       = sorted.drop(startIdx).take(20)
            val nextCursor = if (page.size >= 20 && startIdx + 20 < sorted.size)
                snap.documents.find { it.id == page.lastOrNull()?.storyId } else null
            Resource.Success(page to nextCursor)
        } catch (e: Exception) { Resource.Error("Failed to load stories: " + e.localizedMessage) }
    }

    suspend fun searchStories(query: String): Resource<List<Story>> {
        return try {
            val token = query.lowercase().trim().split(" ")
                .firstOrNull { it.length >= 2 } ?: return Resource.Success(emptyList())
            val snap = storiesCol.whereEqualTo("status", "PUBLISHED")
                .whereArrayContains("searchTokens", token).get().await()
            Resource.Success(snap.toObjects(Story::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error("Search failed: " + e.localizedMessage) }
    }

    suspend fun getStory(storyId: String): Resource<Story> {
        return try {
            val s = storiesCol.document(storyId).get().await().toObject(Story::class.java)
                ?: return Resource.Error("Story not found")
            Resource.Success(s)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getAuthorStories(authorId: String): Resource<List<Story>> {
        return try {
            val snap = storiesCol.whereEqualTo("authorId", authorId).get().await()
            Resource.Success(snap.toObjects(Story::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    // Increment read count when user opens a chapter
    suspend fun incrementReadCount(storyId: String) {
        try { storiesCol.document(storyId)
            .update("totalReads", FieldValue.increment(1)).await() }
        catch (_: Exception) { }
    }

    // ── Story Cover Upload ─────────────────────────────────────────────────────
    suspend fun uploadStoryCover(storyId: String, imageUri: Uri): Resource<String> {
        return try {
            val ref = storage.reference.child("story_covers/$storyId.jpg")
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            // Update Firestore — retry once if it fails
            try {
                storiesCol.document(storyId).update("coverUrl", url).await()
            } catch (_: Exception) {
                // Story document may not have coverUrl field yet — use set with merge
                storiesCol.document(storyId)
                    .set(mapOf("coverUrl" to url), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            }
            Resource.Success(url)
        } catch (e: Exception) { Resource.Error("Cover upload failed: " + e.localizedMessage) }
    }

    // ── Save Story ─────────────────────────────────────────────────────────────
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
            data["tags"]          = story.tags
            data["searchTokens"]  = tokens
            data["status"]        = story.status
            data["totalEpisodes"] = story.totalEpisodes
            data["totalReads"]    = story.totalReads
            data["avgRating"]     = story.avgRating
            data["totalRatings"]  = story.totalRatings
            data["updatedAt"]     = Timestamp.now()
            if (story.createdAt == null) data["createdAt"] = Timestamp.now()
            else data["createdAt"] = story.createdAt
            ref.set(data).await()
            if (story.storyId.isEmpty()) {
                db.collection(Col.USERS).document(story.authorId)
                    .update("storiesCount", FieldValue.increment(1)).await()
            }
            Resource.Success(ref.id)
        } catch (e: Exception) { Resource.Error("Failed to save: " + e.localizedMessage) }
    }

    // ── Episodes ───────────────────────────────────────────────────────────────
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

    suspend fun saveEpisode(episode: Episode): Resource<String> {
        return try {
            val ref = if (episode.episodeId.isEmpty()) episodesCol.document()
                      else episodesCol.document(episode.episodeId)
            val wc = episode.content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            val readMin = maxOf(1, wc / 200)   // ~200 words per minute
            val isFreeChapter = (episode.chapterNumber == 1)
            val data = HashMap<String, Any>()
            data["episodeId"]       = ref.id
            data["storyId"]         = episode.storyId
            data["authorId"]        = episode.authorId
            data["chapterNumber"]   = episode.chapterNumber
            data["title"]           = episode.title
            data["content"]         = episode.content
            data["wordCount"]       = wc
            data["readTimeMinutes"] = readMin
            data["unlockCostCoins"] = if (isFreeChapter) 0 else episode.unlockCostCoins
            data["isFree"]          = isFreeChapter
            data["status"]          = episode.status
            data["likesCount"]      = episode.likesCount
            data["commentsCount"]   = episode.commentsCount
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
            val readMin = maxOf(1, wc / 200)
            val updates = HashMap<String, Any>()
            updates["title"]           = title
            updates["content"]         = content
            updates["wordCount"]       = wc
            updates["readTimeMinutes"] = readMin
            episodesCol.document(episodeId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error("Failed to update: " + e.localizedMessage) }
    }

    // ── Episode Likes ──────────────────────────────────────────────────────────
    suspend fun toggleEpisodeLike(userId: String, episodeId: String): Resource<Boolean> {
        val likeRef = db.collection(Col.EPISODE_LIKES).document("${userId}_${episodeId}")
        val epRef   = episodesCol.document(episodeId)
        return try {
            var isLiked = false
            db.runTransaction { t ->
                val snap = t.get(likeRef)
                if (snap.exists()) {
                    t.delete(likeRef)
                    t.update(epRef, "likesCount", FieldValue.increment(-1))
                    isLiked = false
                } else {
                    t.set(likeRef, EpisodeLike(userId, episodeId, Timestamp.now()))
                    t.update(epRef, "likesCount", FieldValue.increment(1))
                    isLiked = true
                }
            }.await()
            Resource.Success(isLiked)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun isEpisodeLiked(userId: String, episodeId: String): Boolean {
        return try { db.collection(Col.EPISODE_LIKES)
            .document("${userId}_${episodeId}").get().await().exists() }
        catch (_: Exception) { false }
    }

    // ── Ratings ────────────────────────────────────────────────────────────────
    suspend fun saveRating(rating: Rating): Resource<Unit> {
        return try {
            val ratingId = "${rating.userId}_${rating.storyId}"
            val ref = db.collection(Col.RATINGS).document(ratingId)
            val storyRef = storiesCol.document(rating.storyId)
            val existing = ref.get().await()
            db.runTransaction { t ->
                val storySnap = t.get(storyRef)
                val currentTotal = (storySnap.getLong("totalRatings") ?: 0).toInt()
                val currentAvg   = (storySnap.getDouble("avgRating") ?: 0.0).toFloat()
                if (existing.exists()) {
                    // Update existing rating — recalculate avg
                    val oldStars = (existing.getLong("stars") ?: 0).toInt()
                    val newAvg = if (currentTotal <= 1) rating.stars.toFloat()
                    else ((currentAvg * currentTotal) - oldStars + rating.stars) / currentTotal
                    t.set(ref, mapOf("ratingId" to ratingId, "userId" to rating.userId,
                        "storyId" to rating.storyId, "stars" to rating.stars,
                        "review" to rating.review, "createdAt" to Timestamp.now()))
                    t.update(storyRef, "avgRating", newAvg)
                } else {
                    // New rating
                    val newCount = currentTotal + 1
                    val newAvg = ((currentAvg * currentTotal) + rating.stars) / newCount
                    t.set(ref, mapOf("ratingId" to ratingId, "userId" to rating.userId,
                        "storyId" to rating.storyId, "stars" to rating.stars,
                        "review" to rating.review, "createdAt" to Timestamp.now()))
                    t.update(storyRef, "totalRatings", newCount, "avgRating", newAvg)
                }
            }.await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error("Rating failed: " + e.localizedMessage) }
    }

    suspend fun getUserRating(userId: String, storyId: String): Rating? {
        return try {
            db.collection(Col.RATINGS).document("${userId}_${storyId}")
                .get().await().toObject(Rating::class.java)
        } catch (_: Exception) { null }
    }

    // ── Comments ───────────────────────────────────────────────────────────────
    suspend fun getComments(episodeId: String): Resource<List<Comment>> {
        return try {
            val snap = db.collection(Col.COMMENTS)
                .whereEqualTo("episodeId", episodeId).get().await()
            Resource.Success(snap.toObjects(Comment::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun postComment(comment: Comment): Resource<String> {
        return try {
            val ref = db.collection(Col.COMMENTS).document()
            val data = comment.copy(commentId = ref.id, createdAt = Timestamp.now())
            ref.set(data).await()
            // Increment comment count on episode
            episodesCol.document(comment.episodeId)
                .update("commentsCount", FieldValue.increment(1)).await()
            Resource.Success(ref.id)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun deleteComment(commentId: String, episodeId: String): Resource<Unit> {
        return try {
            db.collection(Col.COMMENTS).document(commentId).delete().await()
            episodesCol.document(episodeId)
                .update("commentsCount", FieldValue.increment(-1)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    // ── Reading Progress ───────────────────────────────────────────────────────
    suspend fun saveReadingProgress(progress: ReadingProgress) {
        try {
            // Key = userId_storyId_episodeId — each chapter has its own record
            val docId = "${progress.userId}_${progress.storyId}_${progress.lastEpisodeId}"
            db.collection(Col.READING_PROGRESS).document(docId)
                .set(progress.copy(updatedAt = Timestamp.now())).await()
        } catch (_: Exception) { }
    }

    suspend fun getReadingProgress(userId: String): Resource<List<ReadingProgress>> {
        return try {
            val snap = db.collection(Col.READING_PROGRESS)
                .whereEqualTo("userId", userId).get().await()
            Resource.Success(snap.toObjects(ReadingProgress::class.java)
                .sortedByDescending { it.updatedAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    // ── Writer Stats Dashboard ─────────────────────────────────────────────────
    suspend fun getWriterStats(authorId: String): Resource<WriterStats> {
        return try {
            val stories = storiesCol.whereEqualTo("authorId", authorId).get().await()
                .toObjects(Story::class.java)
            val totalReads    = stories.sumOf { it.totalReads }
            val totalRatings  = stories.sumOf { it.totalRatings }
            val avgRating     = if (stories.isEmpty()) 0f
                else stories.filter { it.totalRatings > 0 }.map { it.avgRating }
                    .average().toFloat().let { if (it.isNaN()) 0f else it }
            val topStory      = stories.maxByOrNull { it.totalReads }
            val userSnap      = db.collection(Col.USERS).document(authorId).get().await()
            val totalEarned   = (userSnap.getLong("totalCoinsEarned") ?: 0).toInt()
            val followers     = (userSnap.getLong("followersCount") ?: 0).toInt()
            Resource.Success(WriterStats(
                authorId      = authorId,
                totalReads    = totalReads,
                totalCoinsEarned = totalEarned,
                totalStories  = stories.size,
                totalFollowers = followers,
                totalRatings  = totalRatings,
                avgRating     = avgRating,
                topStoryTitle = topStory?.title ?: "",
                topStoryReads = topStory?.totalReads ?: 0L
            ))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    // ── Author Profile ─────────────────────────────────────────────────────────
    suspend fun getAuthorProfile(authorId: String): Resource<User> {
        return try {
            val snap = db.collection(Col.USERS).document(authorId).get().await()
            val data = snap.data ?: return Resource.Error("Author not found")
            val user = User(
                userId          = snap.id,
                name            = data["name"] as? String ?: "",
                email           = data["email"] as? String ?: "",
                photoUrl        = data["photoUrl"] as? String ?: "",
                bio             = data["bio"] as? String ?: "",
                role            = try { UserRole.valueOf(data["role"] as? String ?: "WRITER") }
                                  catch (_: Exception) { UserRole.WRITER },
                coinBalance     = ((data["coinBalance"] as? Long) ?: 0).toInt(),
                totalCoinsEarned= ((data["totalCoinsEarned"] as? Long) ?: 0).toInt(),
                followersCount  = ((data["followersCount"] as? Long) ?: 0).toInt(),
                followingCount  = ((data["followingCount"] as? Long) ?: 0).toInt(),
                storiesCount    = ((data["storiesCount"] as? Long) ?: 0).toInt(),
                poemsCount      = ((data["poemsCount"] as? Long) ?: 0).toInt(),
                createdAt       = data["createdAt"] as? Timestamp,
                isBanned        = (data["isBanned"] as? Boolean) ?: false,
                preferredLanguages = (data["preferredLanguages"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
            )
            Resource.Success(user)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }
}

// ── Poem Repository ────────────────────────────────────────────────────────────
@Singleton
class PoemRepository @Inject constructor(private val db: FirebaseFirestore) {
    private val poemsCol get() = db.collection(Col.POEMS)
    private val likesCol get() = db.collection(Col.POEM_LIKES)

    suspend fun getPoems(
        format: String? = null, language: String? = null,
        mood: String? = null, lastVisible: DocumentSnapshot? = null
    ): Resource<Pair<List<Poem>, DocumentSnapshot?>> {
        return try {
            var q: Query = poemsCol.whereEqualTo("status", "PUBLISHED")
            format?.let   { q = q.whereEqualTo("format", it) }
            language?.let { q = q.whereEqualTo("language", it) }
            mood?.let     { q = q.whereEqualTo("mood", it) }
            val snap   = q.get().await()
            val sorted = snap.toObjects(Poem::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 }
            val startIdx = if (lastVisible != null) {
                val idx = sorted.indexOfFirst { it.poemId == lastVisible.id }
                if (idx >= 0) idx + 1 else 0
            } else 0
            val page       = sorted.drop(startIdx).take(30)
            val nextCursor = if (page.size >= 30 && startIdx + 30 < sorted.size)
                snap.documents.find { it.id == page.lastOrNull()?.poemId } else null
            Resource.Success(page to nextCursor)
        } catch (e: Exception) { Resource.Error("Failed to load poems: " + e.localizedMessage) }
    }

    suspend fun searchPoems(query: String): Resource<List<Poem>> {
        return try {
            val token = query.lowercase().trim().split(" ")
                .firstOrNull { it.length >= 2 } ?: return Resource.Success(emptyList())
            val snap = poemsCol.whereEqualTo("status", "PUBLISHED")
                .whereArrayContains("searchTokens", token).get().await()
            Resource.Success(snap.toObjects(Poem::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
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
            data["poemId"]       = ref.id
            data["title"]        = poem.title
            data["content"]      = poem.content
            data["authorId"]     = poem.authorId
            data["authorName"]   = poem.authorName
            data["format"]       = poem.format
            data["language"]     = poem.language
            data["mood"]         = poem.mood
            data["searchTokens"] = tokens
            data["likesCount"]   = poem.likesCount
            data["tipsCount"]    = poem.tipsCount
            data["totalTipsCoins"] = poem.totalTipsCoins
            data["commentsCount"]  = poem.commentsCount
            data["status"]       = "PUBLISHED"
            if (poem.createdAt == null) data["createdAt"] = Timestamp.now()
            else data["createdAt"] = poem.createdAt
            ref.set(data).await()
            if (poem.poemId.isEmpty()) {
                db.collection(Col.USERS).document(poem.authorId)
                    .update("poemsCount", FieldValue.increment(1)).await()
            }
            Resource.Success(ref.id)
        } catch (e: Exception) { Resource.Error("Failed to save poem: " + e.localizedMessage) }
    }

    suspend fun updatePoem(poemId: String, title: String, content: String,
                           format: String, language: String, mood: String): Resource<Unit> {
        return try {
            poemsCol.document(poemId).update(mapOf(
                "title" to title, "content" to content,
                "format" to format, "language" to language, "mood" to mood)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun deletePoem(poemId: String, authorId: String): Resource<Unit> {
        return try {
            poemsCol.document(poemId).delete().await()
            db.collection(Col.USERS).document(authorId)
                .update("poemsCount", FieldValue.increment(-1)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun toggleLike(userId: String, poemId: String): Resource<Boolean> {
        val likeRef = likesCol.document("${userId}_${poemId}")
        val poemRef = poemsCol.document(poemId)
        return try {
            var isLiked = false
            db.runTransaction { t ->
                val snap = t.get(likeRef)
                if (snap.exists()) {
                    t.delete(likeRef)
                    t.update(poemRef, "likesCount", FieldValue.increment(-1))
                    isLiked = false
                } else {
                    t.set(likeRef, PoemLike(userId, poemId, Timestamp.now()))
                    t.update(poemRef, "likesCount", FieldValue.increment(1))
                    isLiked = true
                }
            }.await()
            Resource.Success(isLiked)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Like failed") }
    }

    suspend fun isLiked(userId: String, poemId: String): Boolean {
        return try { likesCol.document("${userId}_${poemId}").get().await().exists() }
        catch (_: Exception) { false }
    }

    suspend fun tipPoet(fromUserId: String, toUserId: String,
                        poem: Poem, coins: Int): Resource<Int> {
        val safeCoins = coins.coerceIn(MvpConfig.POEM_TIP_MIN, MvpConfig.POEM_TIP_MAX)
        val fromRef = db.collection(Col.USERS).document(fromUserId)
        val toRef   = db.collection(Col.USERS).document(toUserId)
        val poemRef = poemsCol.document(poem.poemId)
        val txnFrom = db.collection(Col.COIN_TXN).document()
        val txnTo   = db.collection(Col.COIN_TXN).document()
        return try {
            var newBalance = 0
            db.runTransaction { t ->
                val fromSnap = t.get(fromRef)
                val balance  = (fromSnap.getLong("coinBalance") ?: 0).toInt()
                if (balance < safeCoins) error("Need $safeCoins coins")
                newBalance = balance - safeCoins
                t.update(fromRef, "coinBalance",      FieldValue.increment(-safeCoins.toLong()))
                t.update(toRef,   "coinBalance",      FieldValue.increment(safeCoins.toLong()))
                t.update(toRef,   "totalCoinsEarned", FieldValue.increment(safeCoins.toLong()))
                t.update(poemRef, "tipsCount",        FieldValue.increment(1))
                t.update(poemRef, "totalTipsCoins",   FieldValue.increment(safeCoins.toLong()))
                t.set(txnFrom, CoinTransaction(txnFrom.id, fromUserId, CoinTxnType.POEM_TIP,
                    -safeCoins, "Tipped on: ${poem.title}", "", poem.poemId, Timestamp.now()))
                t.set(txnTo, CoinTransaction(txnTo.id, toUserId, CoinTxnType.POEM_TIP_RECEIVED,
                    safeCoins, "Tip received: ${poem.title}", "", poem.poemId, Timestamp.now()))
            }.await()
            Resource.Success(newBalance)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Tip failed") }
    }
}

// ── Library Repository ─────────────────────────────────────────────────────────
@Singleton
class LibraryRepository @Inject constructor(private val db: FirebaseFirestore) {
    private val libCol get() = db.collection(Col.LIBRARY)

    suspend fun getUserLibrary(userId: String): Resource<List<LibraryEntry>> {
        return try {
            val snap = libCol.whereEqualTo("userId", userId).get().await()
            Resource.Success(snap.toObjects(LibraryEntry::class.java)
                .sortedByDescending { it.lastReadAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getEntry(userId: String, storyId: String): LibraryEntry? {
        return try { libCol.document(libraryDocId(userId, storyId)).get().await()
            .toObject(LibraryEntry::class.java) } catch (_: Exception) { null }
    }

    suspend fun upsert(entry: LibraryEntry) {
        try { libCol.document(libraryDocId(entry.userId, entry.storyId)).set(entry).await() }
        catch (_: Exception) { }
    }
}

// ── Notification Repository ────────────────────────────────────────────────────
@Singleton
class NotificationRepository @Inject constructor(private val db: FirebaseFirestore) {
    private val notifCol get() = db.collection(Col.NOTIFICATIONS)

    suspend fun getNotifications(userId: String): Resource<List<Notification>> {
        return try {
            val snap = notifCol.whereEqualTo("userId", userId).get().await()
            Resource.Success(snap.toObjects(Notification::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 }.take(50))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun markAllRead(userId: String) {
        try {
            val batch = db.batch()
            val unread = notifCol.whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false).get().await()
            unread.documents.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
        } catch (_: Exception) { }
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            notifCol.whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false).get().await().size()
        } catch (_: Exception) { 0 }
    }

    // Called server-side ideally, but can be called from app for now
    suspend fun createNotification(notif: Notification) {
        try {
            val ref = notifCol.document()
            notifCol.document(ref.id).set(notif.copy(
                notificationId = ref.id, createdAt = Timestamp.now())).await()
        } catch (_: Exception) { }
    }
}
