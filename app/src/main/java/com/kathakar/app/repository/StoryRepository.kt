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

@Singleton
class StoryRepository @Inject constructor(private val db: FirebaseFirestore) {

    private val storiesCol  get() = db.collection(FirestoreCollections.STORIES)
    private val episodesCol get() = db.collection(FirestoreCollections.EPISODES)

    // KEY FIX: query by string "PUBLISHED" not enum — avoids status mismatch
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
        } catch (e: Exception) {
            Resource.Error("Failed to load stories: " + e.localizedMessage)
        }
    }

    suspend fun searchStories(query: String): Resource<List<Story>> {
        return try {
            val token = query.lowercase().trim().split(" ")
                .firstOrNull { it.length >= 2 } ?: return Resource.Success(emptyList())
            val snap = storiesCol.whereEqualTo("status", "PUBLISHED")
                .whereArrayContains("searchTokens", token).limit(20).get().await()
            Resource.Success(snap.toObjects(Story::class.java))
        } catch (e: Exception) {
            Resource.Error("Search failed: " + e.localizedMessage)
        }
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
            val snap = episodesCol
                .whereEqualTo("storyId", storyId)
                .whereEqualTo("status", "PUBLISHED")
                .orderBy("chapterNumber", Query.Direction.ASCENDING)
                .get().await()
            Resource.Success(snap.toObjects(Episode::class.java))
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
            val snap = storiesCol.whereEqualTo("authorId", authorId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            Resource.Success(snap.toObjects(Story::class.java))
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun saveStory(story: Story): Resource<String> {
        return try {
            val ref = if (story.storyId.isEmpty()) storiesCol.document()
                      else storiesCol.document(story.storyId)
            val tokens = generateSearchTokens(story.title, story.authorName)
            val data = HashMap<String, Any>()
            data["storyId"]      = ref.id
            data["title"]        = story.title
            data["description"]  = story.description
            data["coverUrl"]     = story.coverUrl
            data["authorId"]     = story.authorId
            data["authorName"]   = story.authorName
            data["category"]     = story.category
            data["language"]     = story.language
            data["searchTokens"] = tokens
            data["status"]       = story.status  // stored as plain string "PUBLISHED"/"DRAFT"
            data["totalEpisodes"]= story.totalEpisodes
            data["totalReads"]   = story.totalReads
            data["updatedAt"]    = Timestamp.now()
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
            val data = HashMap<String, Any>()
            data["episodeId"]      = ref.id
            data["storyId"]        = episode.storyId
            data["authorId"]       = episode.authorId
            data["chapterNumber"]  = episode.chapterNumber
            data["title"]          = episode.title
            data["content"]        = episode.content
            data["wordCount"]      = wc
            data["unlockCostCoins"]= episode.unlockCostCoins
            data["isFree"]         = (episode.chapterNumber == 1)
            data["status"]         = episode.status  // stored as plain string
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
}

@Singleton
class LibraryRepository @Inject constructor(private val db: FirebaseFirestore) {
    private val libCol get() = db.collection(FirestoreCollections.LIBRARY)

    suspend fun getUserLibrary(userId: String): Resource<List<LibraryEntry>> {
        return try {
            val snap = libCol.whereEqualTo("userId", userId)
                .orderBy("lastReadAt", Query.Direction.DESCENDING).get().await()
            Resource.Success(snap.toObjects(LibraryEntry::class.java))
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
