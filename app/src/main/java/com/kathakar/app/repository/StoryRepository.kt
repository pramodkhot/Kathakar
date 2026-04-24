package com.kathakar.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    suspend fun getStories(
        category: String? = null,
        language: String? = null,
        lastVisible: DocumentSnapshot? = null
    ): Resource<Pair<List<Story>, DocumentSnapshot?>> = try {
        var q: Query = storiesCol
            .whereEqualTo("status", StoryStatus.PUBLISHED.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(15)
        category?.let { q = q.whereEqualTo("category", it) }
        language?.let  { q = q.whereEqualTo("language", it) }
        lastVisible?.let { q = q.startAfter(it) }
        val snap = q.get().await()
        val list = snap.toObjects(Story::class.java)
        val next = if (snap.size() >= 15) snap.documents.lastOrNull() else null
        Resource.Success(list to next)
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load stories")
    }

    suspend fun searchStories(query: String): Resource<List<Story>> = try {
        val token = query.lowercase().trim().split(" ")
            .firstOrNull { it.length >= 2 } ?: return Resource.Success(emptyList())
        val snap = storiesCol
            .whereEqualTo("status", StoryStatus.PUBLISHED.name)
            .whereArrayContains("searchTokens", token)
            .limit(20).get().await()
        Resource.Success(snap.toObjects(Story::class.java))
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Search failed")
    }

    suspend fun getStory(storyId: String): Resource<Story> = try {
        val doc = storiesCol.document(storyId).get().await()
        Resource.Success(doc.toObject(Story::class.java) ?: return Resource.Error("Not found"))
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed")
    }

    suspend fun getEpisodes(storyId: String): Resource<List<Episode>> = try {
        val snap = episodesCol
            .whereEqualTo("storyId", storyId)
            .whereEqualTo("status", EpisodeStatus.PUBLISHED.name)
            .orderBy("chapterNumber", Query.Direction.ASCENDING)
            .get().await()
        Resource.Success(snap.toObjects(Episode::class.java))
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed")
    }

    suspend fun getEpisode(episodeId: String): Resource<Episode> = try {
        val doc = episodesCol.document(episodeId).get().await()
        Resource.Success(doc.toObject(Episode::class.java) ?: return Resource.Error("Not found"))
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed")
    }

    suspend fun getAuthorStories(authorId: String): Resource<List<Story>> = try {
        val snap = storiesCol.whereEqualTo("authorId", authorId)
            .orderBy("createdAt", Query.Direction.DESCENDING).get().await()
        Resource.Success(snap.toObjects(Story::class.java))
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed")
    }

    suspend fun saveStory(story: Story): Resource<String> = try {
        val ref = if (story.storyId.isEmpty()) storiesCol.document()
                  else storiesCol.document(story.storyId)
        val save = story.copy(
            storyId = ref.id,
            searchTokens = generateSearchTokens(story.title, story.authorName),
            updatedAt = Timestamp.now(),
            createdAt = story.createdAt ?: Timestamp.now()
        )
        ref.set(save).await()
        Resource.Success(ref.id)
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to save")
    }

    suspend fun saveEpisode(episode: Episode): Resource<String> = try {
        val ref = if (episode.episodeId.isEmpty()) episodesCol.document()
                  else episodesCol.document(episode.episodeId)
        val wc = episode.content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        val save = episode.copy(
            episodeId = ref.id, wordCount = wc,
            isFree = episode.chapterNumber == 1,
            createdAt = episode.createdAt ?: Timestamp.now()
        )
        ref.set(save).await()
        if (episode.episodeId.isEmpty() && episode.status == EpisodeStatus.PUBLISHED) {
            storiesCol.document(episode.storyId)
                .update("totalEpisodes", FieldValue.increment(1)).await()
        }
        Resource.Success(ref.id)
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to save")
    }
}

@Singleton
class LibraryRepository @Inject constructor(private val db: FirebaseFirestore) {

    private val libCol get() = db.collection(FirestoreCollections.LIBRARY)

    suspend fun getUserLibrary(userId: String): Resource<List<LibraryEntry>> = try {
        val snap = libCol.whereEqualTo("userId", userId)
            .orderBy("lastReadAt", Query.Direction.DESCENDING).get().await()
        Resource.Success(snap.toObjects(LibraryEntry::class.java))
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }

    suspend fun getEntry(userId: String, storyId: String): LibraryEntry? = try {
        libCol.document(libraryDocId(userId, storyId)).get().await()
            .toObject(LibraryEntry::class.java)
    } catch (e: Exception) { null }

    suspend fun upsert(entry: LibraryEntry) {
        try {
            libCol.document(libraryDocId(entry.userId, entry.storyId)).set(entry).await()
        } catch (_: Exception) { }
    }

    suspend fun toggleBookmark(userId: String, storyId: String): Boolean = try {
        val ref = libCol.document(libraryDocId(userId, storyId))
        val current = ref.get().await().getBoolean("isBookmarked") ?: false
        ref.update("isBookmarked", !current).await()
        !current
    } catch (e: Exception) { false }
}
