package com.kathakar.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.DocumentSnapshot
import com.kathakar.app.domain.model.*
import com.kathakar.app.repository.*
import com.kathakar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Auth ───────────────────────────────────────────────────────────────────────
data class AuthUiState(
    val user: User? = null,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _s = MutableStateFlow(AuthUiState()); val state = _s.asStateFlow()
    val user get() = _s.value.user
    val isAuthenticated get() = _s.value.isAuthenticated

    fun signInWithGoogle(acc: GoogleSignInAccount) = doAuth { repo.signInWithGoogle(acc) }
    fun signInWithEmail(e: String, p: String) {
        if (e.isBlank() || p.isBlank()) { _s.update { it.copy(error = "Email and password required") }; return }
        doAuth { repo.signInWithEmail(e, p) }
    }
    fun register(n: String, e: String, p: String) {
        if (n.isBlank() || e.isBlank() || p.length < 6) { _s.update { it.copy(error = "All fields required, min 6-char password") }; return }
        doAuth { repo.register(n, e, p) }
    }
    fun signOut() { repo.signOut(); _s.value = AuthUiState() }

    fun savePreferredLanguages(userId: String, langs: List<String>, onDone: () -> Unit) {
        viewModelScope.launch { repo.savePreferredLanguages(userId, langs); onDone() }
    }
    fun clearError() = _s.update { it.copy(error = null) }
    private fun doAuth(block: suspend () -> Resource<User>) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true, error = null) }
        when (val r = block()) {
            is Resource.Success -> _s.update { it.copy(user = r.data, isAuthenticated = true, isLoading = false) }
            is Resource.Error   -> _s.update { it.copy(isLoading = false, error = r.message) }
            else -> Unit
        }
    }
}

// ── Reading Challenge ──────────────────────────────────────────────────────────
data class ReadingChallengeUiState(
    val challenge: com.kathakar.app.domain.model.ReadingChallenge? = null,
    val isLoading: Boolean = false,
    val showGoalPicker: Boolean = false
)

@HiltViewModel
class ReadingChallengeViewModel @Inject constructor(
    private val db: com.google.firebase.firestore.FirebaseFirestore
) : ViewModel() {
    private val _s = MutableStateFlow(ReadingChallengeUiState()); val state = _s.asStateFlow()

    fun load(userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        try {
            val snap = db.collection("reading_challenges").document(userId).get().await()
            val challenge = if (snap.exists()) {
                val data = snap.data ?: emptyMap<String, Any>()
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val lastDate = data["lastReadDate"] as? String ?: ""
                val todayPages = if (lastDate == todayStr)
                    ((data["todayPagesRead"] as? Long) ?: 0L).toInt() else 0
                com.kathakar.app.domain.model.ReadingChallenge(
                    userId         = userId,
                    dailyPageGoal  = ((data["dailyPageGoal"] as? Long) ?: 20L).toInt(),
                    todayPagesRead = todayPages,
                    totalPagesRead = (data["totalPagesRead"] as? Long) ?: 0L,
                    lastReadDate   = lastDate)
            } else {
                com.kathakar.app.domain.model.ReadingChallenge(userId = userId)
            }
            _s.update { it.copy(challenge = challenge, isLoading = false) }
        } catch (e: Exception) {
            _s.update { it.copy(isLoading = false) }
        }
    }

    // Called every time a page is turned in the reader
    fun recordPageRead(userId: String) = viewModelScope.launch {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        try {
            val ref = db.collection("reading_challenges").document(userId)
            val snap = ref.get().await()
            val lastDate = snap.getString("lastReadDate") ?: ""
            val updates = hashMapOf<String, Any>(
                "userId"        to userId,
                "lastReadDate"  to today,
                "totalPagesRead" to com.google.firebase.firestore.FieldValue.increment(1),
                "todayPagesRead" to if (lastDate == today)
                    com.google.firebase.firestore.FieldValue.increment(1) else 1L
            )
            ref.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            // Update local state
            _s.update { st ->
                val current = st.challenge ?: return@update st
                val isToday = current.lastReadDate == today
                st.copy(challenge = current.copy(
                    todayPagesRead = if (isToday) current.todayPagesRead + 1 else 1,
                    totalPagesRead = current.totalPagesRead + 1,
                    lastReadDate   = today))
            }
        } catch (_: Exception) { }
    }

    fun setDailyGoal(userId: String, goal: Int) = viewModelScope.launch {
        try {
            db.collection("reading_challenges").document(userId)
                .set(mapOf("dailyPageGoal" to goal, "userId" to userId),
                    com.google.firebase.firestore.SetOptions.merge()).await()
            _s.update { it.copy(challenge = it.challenge?.copy(dailyPageGoal = goal),
                showGoalPicker = false) }
        } catch (_: Exception) { }
    }

    fun openGoalPicker()  = _s.update { it.copy(showGoalPicker = true) }
    fun closeGoalPicker() = _s.update { it.copy(showGoalPicker = false) }
}


// ── Home ───────────────────────────────────────────────────────────────────────
data class HomeUiState(
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedLanguage: String? = null,
    val hasMore: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val storyRepo: StoryRepository) : ViewModel() {
    private val _s = MutableStateFlow(HomeUiState()); val state = _s.asStateFlow()
    val categories = KathakarMeta.CATEGORIES; val languages = KathakarMeta.LANGUAGES
    private var cursor: DocumentSnapshot? = null; private var searchJob: Job? = null
    private var preferredLanguages: List<String> = emptyList()

    fun setPreferredLanguages(langs: List<String>) {
        if (langs == preferredLanguages) return
        preferredLanguages = langs; load(reset = true)
    }
    init { load() }

    fun load(reset: Boolean = true) {
        if (!reset && (_s.value.isLoadingMore || !_s.value.hasMore)) return
        if (reset) cursor = null
        viewModelScope.launch {
            _s.update { if (reset) it.copy(isLoading = true, stories = emptyList(), error = null)
                        else it.copy(isLoadingMore = true) }
            val s = _s.value
            if (preferredLanguages.isNotEmpty() && s.selectedLanguage == null) {
                loadPrioritized(s.selectedCategory)
            } else {
                when (val r = storyRepo.getStories(s.selectedCategory, s.selectedLanguage, cursor)) {
                    is Resource.Success -> { val (list, next) = r.data; cursor = next
                        _s.update { it.copy(stories = if (reset) list else it.stories + list,
                            hasMore = next != null, isLoading = false, isLoadingMore = false) } }
                    is Resource.Error -> _s.update { it.copy(isLoading = false, isLoadingMore = false, error = r.message) }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun loadPrioritized(category: String?) {
        try {
            val r = storyRepo.getStories(category, null, null)
            if (r is Resource.Success) {
                val all = r.data.first
                val preferred = all.filter { it.language in preferredLanguages }
                val others    = all.filter { it.language !in preferredLanguages }
                val combined  = preferred + others
                _s.update { it.copy(stories = combined.take(20), hasMore = combined.size > 20,
                    isLoading = false, isLoadingMore = false) }
            } else _s.update { it.copy(isLoading = false, error = (r as? Resource.Error)?.message) }
        } catch (e: Exception) { _s.update { it.copy(isLoading = false, error = e.localizedMessage) } }
    }

    fun onSearch(q: String) {
        _s.update { it.copy(searchQuery = q) }; searchJob?.cancel()
        if (q.isBlank()) { load(); return }
        searchJob = viewModelScope.launch { delay(400); _s.update { it.copy(isLoading = true) }
            when (val r = storyRepo.searchStories(q)) {
                is Resource.Success -> _s.update { it.copy(stories = r.data, isLoading = false, hasMore = false) }
                is Resource.Error   -> _s.update { it.copy(isLoading = false, error = r.message) }
                else -> Unit
            } }
    }
    fun onCategory(c: String?) { _s.update { it.copy(selectedCategory = c) }; load() }
    fun onLanguage(l: String?)  { _s.update { it.copy(selectedLanguage = l) }; load() }
    fun loadMore()   = load(reset = false)
    fun clearError() = _s.update { it.copy(error = null) }
    fun refresh()    = load(reset = true)
}

// ── Story Detail ───────────────────────────────────────────────────────────────
data class StoryUiState(
    val story: Story? = null,
    val episodes: List<Episode> = emptyList(),
    val unlockedIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val justUnlockedId: String? = null,
    val isBookmarked: Boolean = false,
    val userRating: Rating? = null,
    val showRatingSheet: Boolean = false
)

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val storyRepo: StoryRepository,
    private val coinRepo: CoinRepository,
    private val libRepo: LibraryRepository
) : ViewModel() {
    private val _s = MutableStateFlow(StoryUiState()); val state = _s.asStateFlow()
    // Track which stories we already counted a read for this session
    private val countedReads = mutableSetOf<String>()

    fun load(storyId: String, userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val story    = (storyRepo.getStory(storyId) as? Resource.Success)?.data
        val episodes = (storyRepo.getEpisodes(storyId) as? Resource.Success)?.data ?: emptyList()
        val unlocked = coinRepo.getUnlockedIds(userId, storyId)
        val entry    = libRepo.getEntry(userId, storyId)
        val rating   = storyRepo.getUserRating(userId, storyId)
        _s.update { it.copy(story = story, episodes = episodes, unlockedIds = unlocked,
            isLoading = false, isBookmarked = entry?.isBookmarked == true,
            userRating = rating) }
        // Increment read count once per session per story
        if (!countedReads.contains(storyId)) {
            countedReads.add(storyId)
            storyRepo.incrementReadCount(storyId)
        }
    }

    fun unlock(episode: Episode, user: User) {
        viewModelScope.launch {
            when (val r = coinRepo.unlockEpisode(
                userId = user.userId,
                authorId = episode.authorId,
                episode = episode
            )) {
                is Resource.Success -> _s.update { it.copy(
                    unlockedIds = it.unlockedIds + episode.episodeId,
                    justUnlockedId = episode.episodeId) }
                is Resource.Error -> _s.update { it.copy(error = r.message) }
                else -> Unit
            }
        }
    }

    fun toggleBookmark(userId: String, story: Story) = viewModelScope.launch {
        val current = _s.value.isBookmarked
        val entry = LibraryEntry(userId = userId, storyId = story.storyId,
            storyTitle = story.title, storyCoverUrl = story.coverUrl,
            authorName = story.authorName, totalEpisodes = story.totalEpisodes,
            isBookmarked = !current, lastReadAt = com.google.firebase.Timestamp.now())
        libRepo.upsert(entry)
        _s.update { it.copy(isBookmarked = !current) }
    }

    fun submitRating(userId: String, storyId: String, stars: Int, review: String) {
        viewModelScope.launch {
            val rating = Rating(userId = userId, storyId = storyId, stars = stars, review = review)
            storyRepo.saveRating(rating)
            _s.update { it.copy(userRating = rating, showRatingSheet = false) }
            // Reload story to get updated avgRating
            val updated = (storyRepo.getStory(storyId) as? Resource.Success)?.data
            _s.update { it.copy(story = updated ?: it.story) }
        }
    }

    fun openRatingSheet()  = _s.update { it.copy(showRatingSheet = true) }
    fun closeRatingSheet() = _s.update { it.copy(showRatingSheet = false) }
    fun clearJustUnlocked() = _s.update { it.copy(justUnlockedId = null) }
    fun clearError()        = _s.update { it.copy(error = null) }
}

// ── Episode Reader ─────────────────────────────────────────────────────────────
data class ReaderUiState(
    val episode: Episode? = null,
    val isLoading: Boolean = false,
    // Pagination
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val savedPage: Int = 0,          // restored from Firestore on open
    val pages: List<String> = emptyList(),  // text split into pages
    // Reading settings
    val fontSize: Int = 18,
    val isNightMode: Boolean = false,
    val fontFamily: String = "Default",
    // Interaction
    val isLiked: Boolean = false,
    val likesCount: Int = 0,
    val comments: List<Comment> = emptyList(),
    val showComments: Boolean = false,
    val commentText: String = "",
    val isPostingComment: Boolean = false,
    val showSettingsBar: Boolean = false
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val storyRepo: StoryRepository
) : ViewModel() {
    private val _s = MutableStateFlow(ReaderUiState()); val state = _s.asStateFlow()

    fun load(episodeId: String, userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        when (val r = storyRepo.getEpisode(episodeId)) {
            is Resource.Success -> {
                val ep = r.data
                val isLiked = storyRepo.isEpisodeLiked(userId, episodeId)
                _s.update { it.copy(episode = ep, isLoading = false,
                    isLiked = isLiked, likesCount = ep.likesCount) }
            }
            is Resource.Error -> _s.update { it.copy(isLoading = false) }
            else -> Unit
        }
    }

    // Split content into pages — called once when content is known
    // charsPerPage auto-scales with font size: larger font = fewer chars per page
    fun splitIntoPages(content: String, fontSize: Int = 18) {
        if (content.isBlank()) return
        val charsPerPage = when {
            fontSize <= 14 -> 1000
            fontSize <= 16 -> 900
            fontSize <= 18 -> 800
            fontSize <= 20 -> 650
            fontSize <= 22 -> 550
            else           -> 450
        }
        val paragraphs = content.trim().split("\n\n", "\n").filter { it.isNotBlank() }
        val pages = mutableListOf<String>()
        val current = StringBuilder()
        for (para in paragraphs) {
            if (current.length + para.length + 2 > charsPerPage && current.isNotEmpty()) {
                pages.add(current.toString().trim())
                current.clear()
            }
            if (current.isNotEmpty()) current.append("\n\n")
            current.append(para)
        }
        if (current.isNotEmpty()) pages.add(current.toString().trim())
        val finalPages = if (pages.isEmpty()) listOf(content) else pages
        val startPage = _s.value.savedPage.coerceIn(0, maxOf(0, finalPages.size - 1))
        _s.update { it.copy(pages = finalPages, totalPages = finalPages.size, currentPage = startPage) }
    }

    // Restore saved page after loading progress from Firestore
    fun restoreSavedPage(savedPage: Int) {
        val clamped = savedPage.coerceIn(0, maxOf(0, _s.value.totalPages - 1))
        _s.update { it.copy(savedPage = savedPage, currentPage = clamped) }
    }

    // Called on every page swipe — saves to Firestore debounced
    private var saveJob: kotlinx.coroutines.Job? = null
    fun onPageChange(page: Int, userId: String, storyId: String, episode: Episode?) {
        _s.update { it.copy(currentPage = page) }
        val ep = episode ?: return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1500) // wait 1.5s before writing to Firestore
            storyRepo.saveReadingProgress(ReadingProgress(
                userId            = userId,
                storyId           = storyId,
                storyTitle        = "",
                storyCoverUrl     = "",
                authorName        = "",
                lastEpisodeId     = ep.episodeId,
                lastChapterNumber = ep.chapterNumber,
                lastChapterTitle  = ep.title,
                totalEpisodes     = 0,
                lastPageNumber    = page))
        }
    }

    fun saveProgress(userId: String, storyId: String, episode: Episode) = viewModelScope.launch {
        val story = (storyRepo.getStory(storyId) as? Resource.Success)?.data
        storyRepo.saveReadingProgress(ReadingProgress(
            userId            = userId,
            storyId           = storyId,
            storyTitle        = story?.title ?: "",
            storyCoverUrl     = story?.coverUrl ?: "",
            authorName        = story?.authorName ?: "",
            lastEpisodeId     = episode.episodeId,
            lastChapterNumber = episode.chapterNumber,
            lastChapterTitle  = episode.title,
            totalEpisodes     = story?.totalEpisodes ?: episode.chapterNumber,
            lastPageNumber    = _s.value.currentPage))
    }

    fun loadSavedPage(userId: String, storyId: String) = viewModelScope.launch {
        val result = storyRepo.getReadingProgress(userId)
        if (result is Resource.Success) {
            val progress = result.data.find { it.storyId == storyId }
            progress?.let { restoreSavedPage(it.lastPageNumber) }
        }
    }

    fun toggleLike(userId: String, episodeId: String) = viewModelScope.launch {
        val r = storyRepo.toggleEpisodeLike(userId, episodeId)
        if (r is Resource.Success) {
            val isNowLiked = r.data
            _s.update { it.copy(isLiked = isNowLiked,
                likesCount = if (isNowLiked) it.likesCount + 1 else it.likesCount - 1) }
        }
    }

    fun loadComments(episodeId: String) = viewModelScope.launch {
        val r = storyRepo.getComments(episodeId)
        if (r is Resource.Success) _s.update { it.copy(comments = r.data) }
    }

    fun postComment(userId: String, userName: String, userPhoto: String,
                    episodeId: String, storyId: String) = viewModelScope.launch {
        val text = _s.value.commentText.trim()
        if (text.isBlank()) return@launch
        _s.update { it.copy(isPostingComment = true) }
        val comment = Comment(episodeId = episodeId, storyId = storyId,
            userId = userId, userName = userName, userPhotoUrl = userPhoto, text = text)
        val r = storyRepo.postComment(comment)
        if (r is Resource.Success) {
            _s.update { it.copy(commentText = "", isPostingComment = false,
                comments = listOf(comment.copy(commentId = r.data)) + it.comments) }
        } else _s.update { it.copy(isPostingComment = false) }
    }

    fun deleteComment(commentId: String, episodeId: String) = viewModelScope.launch {
        storyRepo.deleteComment(commentId, episodeId)
        _s.update { it.copy(comments = it.comments.filter { c -> c.commentId != commentId }) }
    }

    fun setFontSize(size: Int)     = _s.update { it.copy(fontSize = size) }
    fun setNightMode(on: Boolean)  = _s.update { it.copy(isNightMode = on) }
    fun setFontFamily(f: String)   = _s.update { it.copy(fontFamily = f) }
    fun toggleSettingsBar()        = _s.update { it.copy(showSettingsBar = !it.showSettingsBar) }
    fun toggleComments()           = _s.update { it.copy(showComments = !it.showComments) }
    fun onCommentChange(v: String) = _s.update { it.copy(commentText = v) }
}

// ── Writer ─────────────────────────────────────────────────────────────────────
data class WriterUiState(
    val myStories: List<Story> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedStoryId: String? = null,
    val savedEpisodeId: String? = null,
    val storyTitle: String = "",
    val storyDesc: String = "",
    val storyCategory: String = "",
    val storyLanguage: String = "en",
    val storyTags: List<String> = emptyList(),
    val coverUri: Uri? = null,
    val coverUrl: String = "",
    val isUploadingCover: Boolean = false,
    val epTitle: String = "",
    val epContent: String = "",
    val wordCount: Int = 0,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class WriterViewModel @Inject constructor(
    private val storyRepo: StoryRepository
) : ViewModel() {
    private val _s = MutableStateFlow(WriterUiState()); val state = _s.asStateFlow()

    fun loadMyStories(authorId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        when (val r = storyRepo.getAuthorStories(authorId)) {
            is Resource.Success -> _s.update { it.copy(myStories = r.data, isLoading = false) }
            is Resource.Error   -> _s.update { it.copy(isLoading = false, error = r.message) }
            else -> Unit
        }
    }

    fun uploadCover(storyId: String, uri: Uri) = viewModelScope.launch {
        _s.update { it.copy(isUploadingCover = true) }
        when (val r = storyRepo.uploadStoryCover(storyId, uri)) {
            is Resource.Success -> _s.update { it.copy(coverUrl = r.data, coverUri = uri, isUploadingCover = false) }
            is Resource.Error   -> _s.update { it.copy(isUploadingCover = false, error = r.message) }
            else -> Unit
        }
    }

    fun deleteEpisode(episodeId: String, storyId: String, onDone: () -> Unit) = viewModelScope.launch {
        when (val r = storyRepo.deleteEpisode(episodeId, storyId)) {
            is Resource.Success -> { onDone(); _s.update { it.copy(message = "Chapter deleted") } }
            is Resource.Error   -> _s.update { it.copy(error = r.message) }
            else -> Unit
        }
    }

    fun updateEpisode(episodeId: String, storyId: String, title: String, content: String, onDone: () -> Unit) = viewModelScope.launch {
        _s.update { it.copy(isSaving = true) }
        when (val r = storyRepo.updateEpisode(episodeId, title, content)) {
            is Resource.Success -> { _s.update { it.copy(isSaving = false, message = "Chapter updated") }; onDone() }
            is Resource.Error   -> _s.update { it.copy(isSaving = false, error = r.message) }
            else -> Unit
        }
    }

    fun onTitleChange(v: String)    = _s.update { it.copy(storyTitle = v) }
    fun onDescChange(v: String)     = _s.update { it.copy(storyDesc = v) }
    fun onCategoryChange(v: String) = _s.update { it.copy(storyCategory = v) }
    fun onLanguageChange(v: String) = _s.update { it.copy(storyLanguage = v) }
    fun onTagsChange(tags: List<String>) = _s.update { it.copy(storyTags = tags) }
    fun onEpTitleChange(v: String)  = _s.update { it.copy(epTitle = v) }
    fun onCoverUriChange(uri: Uri)  = _s.update { it.copy(coverUri = uri) }
    fun onEpContentChange(v: String) {
        val wc = v.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
        _s.update { it.copy(epContent = v, wordCount = wc) }
    }

    fun saveStory(authorId: String, authorName: String) {
        val s = _s.value
        if (s.storyTitle.isBlank() || s.storyDesc.isBlank() || s.storyCategory.isBlank()) {
            _s.update { it.copy(error = "Fill title, description and category") }; return
        }
        viewModelScope.launch {
            _s.update { it.copy(isSaving = true) }
            val story = Story(title = s.storyTitle, description = s.storyDesc,
                authorId = authorId, authorName = authorName, coverUrl = s.coverUrl,
                category = s.storyCategory, language = s.storyLanguage, tags = s.storyTags,
                status = "PUBLISHED")
            when (val r = storyRepo.saveStory(story)) {
                is Resource.Success -> _s.update { it.copy(isSaving = false, savedStoryId = r.data) }
                is Resource.Error   -> _s.update { it.copy(isSaving = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun saveEpisode(storyId: String, authorId: String, chapterNumber: Int, publish: Boolean) {
        val s = _s.value
        if (s.epTitle.isBlank() || s.epContent.isBlank()) {
            _s.update { it.copy(error = "Title and content required") }; return
        }
        viewModelScope.launch {
            _s.update { it.copy(isSaving = true) }
            val ep = Episode(storyId = storyId, authorId = authorId,
                chapterNumber = chapterNumber, title = s.epTitle, content = s.epContent,
                status = if (publish) "PUBLISHED" else "DRAFT")
            when (val r = storyRepo.saveEpisode(ep)) {
                is Resource.Success -> _s.update { it.copy(isSaving = false, savedEpisodeId = r.data,
                    epTitle = "", epContent = "", wordCount = 0) }
                is Resource.Error   -> _s.update { it.copy(isSaving = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun resetSaved()   = _s.update { it.copy(savedStoryId = null, savedEpisodeId = null) }
    fun clearMessage() = _s.update { it.copy(message = null) }
    fun clearError()   = _s.update { it.copy(error = null) }
}

// ── Writer Dashboard ───────────────────────────────────────────────────────────
data class WriterDashboardUiState(
    val stats: WriterStats? = null,
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WriterDashboardViewModel @Inject constructor(
    private val storyRepo: StoryRepository
) : ViewModel() {
    private val _s = MutableStateFlow(WriterDashboardUiState()); val state = _s.asStateFlow()

    fun load(authorId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val stats   = (storyRepo.getWriterStats(authorId) as? Resource.Success)?.data
        val stories = (storyRepo.getAuthorStories(authorId) as? Resource.Success)?.data ?: emptyList()
        _s.update { it.copy(stats = stats, stories = stories, isLoading = false) }
    }
}

// ── Author Profile ─────────────────────────────────────────────────────────────
data class AuthorProfileUiState(
    val author: User? = null,
    val stories: List<Story> = emptyList(),
    val poems: List<Poem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AuthorProfileViewModel @Inject constructor(
    private val storyRepo: StoryRepository,
    private val poemRepo: PoemRepository
) : ViewModel() {
    private val _s = MutableStateFlow(AuthorProfileUiState()); val state = _s.asStateFlow()

    fun load(authorId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val author  = (storyRepo.getAuthorProfile(authorId) as? Resource.Success)?.data
        val stories = (storyRepo.getAuthorStories(authorId) as? Resource.Success)?.data
            ?.filter { it.status == "PUBLISHED" } ?: emptyList()
        val poems   = (poemRepo.getAuthorPoems(authorId) as? Resource.Success)?.data ?: emptyList()
        _s.update { it.copy(author = author, stories = stories, poems = poems, isLoading = false) }
    }
}

// ── Poems ──────────────────────────────────────────────────────────────────────
data class PoemsUiState(
    val poems: List<Poem> = emptyList(),
    val myPoems: List<Poem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFormat: String? = null,
    val selectedLanguage: String? = null,
    val selectedMood: String? = null,
    val showWriteSheet: Boolean = false,
    val editingPoem: Poem? = null,
    val poemTitle: String = "",
    val poemContent: String = "",
    val poemFormat: String = "Free verse",
    val poemLanguage: String = "en",
    val poemMood: String = "Love",
    val wordCount: Int = 0,
    val isSaving: Boolean = false,
    val savedPoemId: String? = null,
    val message: String? = null
)

@HiltViewModel
class PoemsViewModel @Inject constructor(private val poemRepo: PoemRepository) : ViewModel() {
    private val _s = MutableStateFlow(PoemsUiState()); val state = _s.asStateFlow()
    val formats = KathakarMeta.POEM_FORMATS; val moods = KathakarMeta.POEM_MOODS
    private var cursor: DocumentSnapshot? = null; private var searchJob: Job? = null
    private var preferredLanguages: List<String> = emptyList()

    fun setPreferredLanguages(langs: List<String>) {
        if (langs == preferredLanguages) return
        preferredLanguages = langs; load(reset = true)
    }
    init { load() }

    fun load(reset: Boolean = true) {
        if (!reset && (_s.value.isLoadingMore || !_s.value.hasMore)) return
        if (reset) cursor = null
        viewModelScope.launch {
            _s.update { if (reset) it.copy(isLoading = true, poems = emptyList(), error = null)
                        else it.copy(isLoadingMore = true) }
            val s = _s.value
            if (preferredLanguages.isNotEmpty() && s.selectedLanguage == null) {
                loadPrioritized(s.selectedFormat, s.selectedMood)
            } else {
                when (val r = poemRepo.getPoems(s.selectedFormat, s.selectedLanguage, s.selectedMood, cursor)) {
                    is Resource.Success -> { val (list, next) = r.data; cursor = next
                        _s.update { it.copy(poems = if (reset) list else it.poems + list,
                            hasMore = next != null, isLoading = false, isLoadingMore = false) } }
                    is Resource.Error -> _s.update { it.copy(isLoading = false, isLoadingMore = false, error = r.message) }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun loadPrioritized(format: String?, mood: String?) {
        try {
            val r = poemRepo.getPoems(format, null, mood, null)
            if (r is Resource.Success) {
                val all = r.data.first
                val preferred = all.filter { it.language in preferredLanguages }
                val others    = all.filter { it.language !in preferredLanguages }
                _s.update { it.copy(poems = (preferred + others).take(30),
                    hasMore = false, isLoading = false, isLoadingMore = false) }
            } else _s.update { it.copy(isLoading = false, error = (r as? Resource.Error)?.message) }
        } catch (e: Exception) { _s.update { it.copy(isLoading = false, error = e.localizedMessage) } }
    }

    fun onSearch(q: String) {
        _s.update { it.copy(searchQuery = q) }; searchJob?.cancel()
        if (q.isBlank()) { load(); return }
        searchJob = viewModelScope.launch { delay(400); _s.update { it.copy(isLoading = true) }
            when (val r = poemRepo.searchPoems(q)) {
                is Resource.Success -> _s.update { it.copy(poems = r.data, isLoading = false, hasMore = false) }
                is Resource.Error   -> _s.update { it.copy(isLoading = false, error = r.message) }
                else -> Unit
            } }
    }
    fun onFormatFilter(f: String?)   { _s.update { it.copy(selectedFormat = f) };   load() }
    fun onLanguageFilter(l: String?) { _s.update { it.copy(selectedLanguage = l) }; load() }
    fun onMoodFilter(m: String?)     { _s.update { it.copy(selectedMood = m) };     load() }
    fun loadMore()                   = load(reset = false)
    fun refresh()                    = load(reset = true)

    fun openWriteSheet(poem: Poem? = null) {
        _s.update { it.copy(showWriteSheet = true, editingPoem = poem,
            poemTitle = poem?.title ?: "", poemContent = poem?.content ?: "",
            poemFormat = poem?.format ?: "Free verse",
            poemLanguage = poem?.language ?: "en", poemMood = poem?.mood ?: "Love",
            wordCount = poem?.content?.split("\\s+".toRegex())?.count { w -> w.isNotEmpty() } ?: 0) }
    }
    fun closeWriteSheet() = _s.update { it.copy(showWriteSheet = false, editingPoem = null) }
    fun onPoemTitleChange(v: String)   = _s.update { it.copy(poemTitle = v) }
    fun onPoemContentChange(v: String) {
        val wc = v.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
        _s.update { it.copy(poemContent = v, wordCount = wc) }
    }
    fun onPoemFormatChange(v: String)  = _s.update { it.copy(poemFormat = v) }
    fun onPoemLanguageChange(v: String)= _s.update { it.copy(poemLanguage = v) }
    fun onPoemMoodChange(v: String)    = _s.update { it.copy(poemMood = v) }

    fun savePoem(authorId: String, authorName: String) {
        val s = _s.value
        if (s.poemTitle.isBlank() || s.poemContent.isBlank()) return
        viewModelScope.launch {
            _s.update { it.copy(isSaving = true) }
            val editing = s.editingPoem
            if (editing != null) {
                poemRepo.updatePoem(editing.poemId, s.poemTitle, s.poemContent,
                    s.poemFormat, s.poemLanguage, s.poemMood)
                _s.update { it.copy(isSaving = false, showWriteSheet = false, editingPoem = null,
                    message = "Poem updated!") }
                load(true)
            } else {
                val poem = Poem(title = s.poemTitle, content = s.poemContent,
                    authorId = authorId, authorName = authorName,
                    format = s.poemFormat, language = s.poemLanguage, mood = s.poemMood)
                when (val r = poemRepo.savePoem(poem)) {
                    is Resource.Success -> { _s.update { it.copy(isSaving = false, showWriteSheet = false,
                        savedPoemId = r.data, message = "Poem published!") }; load(true) }
                    is Resource.Error   -> _s.update { it.copy(isSaving = false, message = r.message) }
                    else -> Unit
                }
            }
        }
    }

    fun deletePoem(poemId: String, authorId: String) = viewModelScope.launch {
        poemRepo.deletePoem(poemId, authorId)
        _s.update { it.copy(poems = it.poems.filter { p -> p.poemId != poemId }, message = "Poem deleted") }
    }

    fun loadMyPoems(authorId: String) = viewModelScope.launch {
        when (val r = poemRepo.getAuthorPoems(authorId)) {
            is Resource.Success -> _s.update { it.copy(myPoems = r.data) }
            else -> Unit
        }
    }
    fun clearMessage() = _s.update { it.copy(message = null) }
    fun clearError()   = _s.update { it.copy(error = null) }
    fun resetSaved()   = _s.update { it.copy(savedPoemId = null) }
}

// ── Poem Detail ────────────────────────────────────────────────────────────────
data class PoemDetailUiState(
    val poem: Poem? = null,
    val isLoading: Boolean = false,
    val isLiked: Boolean = false,
    val showTipDialog: Boolean = false,
    val selectedTip: Int = MvpConfig.POEM_TIP_MIN,
    val error: String? = null,
    val message: String? = null,
    val newBalance: Int? = null
)

@HiltViewModel
class PoemDetailViewModel @Inject constructor(private val poemRepo: PoemRepository) : ViewModel() {
    private val _s = MutableStateFlow(PoemDetailUiState()); val state = _s.asStateFlow()

    fun load(poemId: String, userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val poem    = (poemRepo.getPoem(poemId) as? Resource.Success)?.data
        val isLiked = poemRepo.isLiked(userId, poemId)
        _s.update { it.copy(poem = poem, isLoading = false, isLiked = isLiked) }
    }

    fun toggleLike(userId: String, poemId: String) = viewModelScope.launch {
        val r = poemRepo.toggleLike(userId, poemId)
        if (r is Resource.Success) {
            val liked = r.data
            _s.update { it.copy(isLiked = liked,
                poem = it.poem?.copy(likesCount = if (liked) (it.poem.likesCount + 1) else (it.poem.likesCount - 1))) }
        }
    }

    fun openTipDialog()         = _s.update { it.copy(showTipDialog = true) }
    fun closeTipDialog()        = _s.update { it.copy(showTipDialog = false) }
    fun onTipChange(c: Int)     = _s.update { it.copy(selectedTip = c) }

    fun sendTip(fromUserId: String, toUserId: String) = viewModelScope.launch {
        val poem = _s.value.poem ?: return@launch
        when (val r = poemRepo.tipPoet(fromUserId, toUserId, poem, _s.value.selectedTip)) {
            is Resource.Success -> _s.update { it.copy(showTipDialog = false, newBalance = r.data,
                message = "Tipped ${it.selectedTip} coin${if (it.selectedTip > 1) "s" else ""}! 🎉",
                poem = it.poem?.copy(tipsCount = it.poem.tipsCount + 1,
                    totalTipsCoins = it.poem.totalTipsCoins + it.selectedTip)) }
            is Resource.Error   -> _s.update { it.copy(showTipDialog = false, error = r.message) }
            else -> Unit
        }
    }
    fun clearMessage() = _s.update { it.copy(message = null) }
    fun clearError()   = _s.update { it.copy(error = null) }
}

// ── Library ────────────────────────────────────────────────────────────────────
data class LibraryUiState(
    val entries: List<LibraryEntry> = emptyList(),
    val progress: List<ReadingProgress> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libRepo: LibraryRepository,
    private val storyRepo: StoryRepository
) : ViewModel() {
    private val _s = MutableStateFlow(LibraryUiState()); val state = _s.asStateFlow()

    fun load(userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val entries   = (libRepo.getUserLibrary(userId) as? Resource.Success)?.data ?: emptyList()
        val progress  = (storyRepo.getReadingProgress(userId) as? Resource.Success)?.data ?: emptyList()
        _s.update { it.copy(entries = entries.filter { e -> e.isBookmarked },
            progress = progress, isLoading = false) }
    }
}

// ── Profile ────────────────────────────────────────────────────────────────────
data class ProfileUiState(
    val coinHistory: List<CoinTransaction> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val coinRepo: CoinRepository) : ViewModel() {
    private val _s = MutableStateFlow(ProfileUiState()); val state = _s.asStateFlow()

    fun load(userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        when (val r = coinRepo.getCoinHistory(userId)) {
            is Resource.Success -> _s.update { it.copy(coinHistory = r.data, isLoading = false) }
            is Resource.Error   -> _s.update { it.copy(isLoading = false) }
            else -> Unit
        }
    }
}

// ── Notifications ──────────────────────────────────────────────────────────────
data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notifRepo: NotificationRepository
) : ViewModel() {
    private val _s = MutableStateFlow(NotificationUiState()); val state = _s.asStateFlow()

    fun load(userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val notifs = (notifRepo.getNotifications(userId) as? Resource.Success)?.data ?: emptyList()
        val unread = notifs.count { !it.isRead }
        _s.update { it.copy(notifications = notifs, unreadCount = unread, isLoading = false) }
    }

    fun markAllRead(userId: String) = viewModelScope.launch {
        notifRepo.markAllRead(userId)
        _s.update { it.copy(notifications = it.notifications.map { n -> n.copy(isRead = true) },
            unreadCount = 0) }
    }
}

// ── Follow ─────────────────────────────────────────────────────────────────────
data class FollowUiState(val isFollowing: Boolean = false, val isLoading: Boolean = false)

@HiltViewModel
class FollowViewModel @Inject constructor(private val followRepo: FollowRepository) : ViewModel() {
    private val _s = MutableStateFlow(FollowUiState()); val state = _s.asStateFlow()

    fun check(followerId: String, followeeId: String) = viewModelScope.launch {
        val isFollowing = followRepo.isFollowing(followerId, followeeId)
        _s.update { it.copy(isFollowing = isFollowing) }
    }

    fun toggle(followerId: String, followeeId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val r = followRepo.toggleFollow(followerId, followeeId)
        if (r is Resource.Success) _s.update { it.copy(isFollowing = r.data, isLoading = false) }
        else _s.update { it.copy(isLoading = false) }
    }
}

// ── Admin ──────────────────────────────────────────────────────────────────────
data class AdminUiState(
    val stats: AdminStats = AdminStats(),
    val users: List<User> = emptyList(),
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTab: Int = 0,
    val message: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(private val adminRepo: AdminRepository) : ViewModel() {
    private val _s = MutableStateFlow(AdminUiState()); val state = _s.asStateFlow()

    fun load() = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val stats   = try { adminRepo.getStats() } catch (_: Exception) { AdminStats() }
        val users   = (adminRepo.getAllUsers() as? Resource.Success<List<User>>)?.data ?: emptyList()
        val stories = (adminRepo.getAllStories() as? Resource.Success<List<Story>>)?.data ?: emptyList()
        _s.update { it.copy(stats = stats, users = users, stories = stories, isLoading = false) }
    }
    fun onTabChange(tab: Int)                          = _s.update { it.copy(selectedTab = tab) }
    fun updateRole(userId: String, role: UserRole)     = viewModelScope.launch { adminRepo.updateUserRole(userId, role); load() }
    fun toggleBan(userId: String, banned: Boolean)     = viewModelScope.launch { adminRepo.toggleBan(userId, banned); load() }
    fun suspendStory(storyId: String)                  = viewModelScope.launch { adminRepo.updateStoryStatus(storyId, "SUSPENDED"); load() }
    fun restoreStory(storyId: String)                  = viewModelScope.launch { adminRepo.updateStoryStatus(storyId, "PUBLISHED"); load() }
    fun deleteStory(storyId: String)                   = viewModelScope.launch { adminRepo.deleteStory(storyId); load() }
    fun clearMessage()                                 = _s.update { it.copy(message = null) }
}
