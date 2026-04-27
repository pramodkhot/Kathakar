package com.kathakar.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.DocumentSnapshot
import com.kathakar.app.domain.model.*
import com.kathakar.app.repository.*
import com.kathakar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Auth ──────────────────────────────────────────────────────────────────────
data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
) {
    val isAuthenticated get() = user != null
}

@HiltViewModel
class AuthViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _s = MutableStateFlow(AuthUiState())
    val state = _s.asStateFlow()
    init { viewModelScope.launch { repo.currentUserFlow.collect { u -> _s.update { it.copy(user = u) } } } }
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
    fun clearError() = _s.update { it.copy(error = null) }
    private fun doAuth(block: suspend () -> Resource<User>) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true, error = null) }
        when (val r = block()) {
            is Resource.Success -> _s.update { it.copy(isLoading = false, user = r.data) }
            is Resource.Error   -> _s.update { it.copy(isLoading = false, error = r.message) }
            else -> _s.update { it.copy(isLoading = false) }
        }
    }
}

// ── Home ──────────────────────────────────────────────────────────────────────
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
    private val _s = MutableStateFlow(HomeUiState())
    val state = _s.asStateFlow()
    val categories = KathakarMeta.CATEGORIES
    val languages  = KathakarMeta.LANGUAGES
    private var cursor: DocumentSnapshot? = null
    private var searchJob: Job? = null
    init { load() }

    fun load(reset: Boolean = true) {
        if (!reset && (_s.value.isLoadingMore || !_s.value.hasMore)) return
        if (reset) cursor = null
        viewModelScope.launch {
            _s.update { if (reset) it.copy(isLoading = true, stories = emptyList(), error = null) else it.copy(isLoadingMore = true) }
            val s = _s.value
            when (val r = storyRepo.getStories(s.selectedCategory, s.selectedLanguage, cursor)) {
                is Resource.Success -> { val (list, next) = r.data; cursor = next
                    _s.update { it.copy(stories = if (reset) list else it.stories + list, hasMore = next != null, isLoading = false, isLoadingMore = false) } }
                is Resource.Error -> _s.update { it.copy(isLoading = false, isLoadingMore = false, error = r.message) }
                else -> Unit
            }
        }
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
    fun onLanguage(l: String?) { _s.update { it.copy(selectedLanguage = l) }; load() }
    fun loadMore()   = load(reset = false)
    fun clearError() = _s.update { it.copy(error = null) }
    fun refresh()    = load(reset = true)
}

// ── Story ─────────────────────────────────────────────────────────────────────
data class StoryUiState(
    val story: Story? = null,
    val episodes: List<Episode> = emptyList(),
    val unlockedIds: Set<String> = emptySet(),
    val isBookmarked: Boolean = false,
    val isLoading: Boolean = false,
    val unlockingId: String? = null,
    val justUnlockedId: String? = null,
    val newCoinBalance: Int? = null,
    val error: String? = null
)

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val storyRepo: StoryRepository,
    private val coinRepo: CoinRepository,
    private val libRepo: LibraryRepository
) : ViewModel() {
    private val _s = MutableStateFlow(StoryUiState())
    val state = _s.asStateFlow()

    fun load(storyId: String, userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true, error = null) }
        val story    = (storyRepo.getStory(storyId) as? Resource.Success)?.data
        val episodes = (storyRepo.getEpisodes(storyId) as? Resource.Success)?.data ?: emptyList()
        val unlocked = coinRepo.getUnlockedIds(userId, storyId)
        val entry    = libRepo.getEntry(userId, storyId)
        _s.update { it.copy(story = story, episodes = episodes, unlockedIds = unlocked,
            isBookmarked = entry?.isBookmarked ?: false, isLoading = false) }
    }

    fun unlock(episode: Episode, user: User) {
        if (_s.value.unlockingId != null) return
        if (user.userId == episode.authorId) {
            _s.update { it.copy(unlockedIds = it.unlockedIds + episode.episodeId, justUnlockedId = episode.episodeId) }
            return
        }
        viewModelScope.launch {
            _s.update { it.copy(unlockingId = episode.episodeId, error = null) }
            when (val r = coinRepo.unlockEpisode(user.userId, episode.authorId, episode)) {
                is Resource.Success -> _s.update { it.copy(unlockingId = null,
                    unlockedIds = it.unlockedIds + episode.episodeId,
                    justUnlockedId = episode.episodeId, newCoinBalance = r.data) }
                is Resource.Error -> _s.update { it.copy(unlockingId = null, error = r.message) }
                else -> Unit
            }
        }
    }

    fun toggleBookmark(userId: String, story: Story) = viewModelScope.launch {
        val entry = LibraryEntry(userId = userId, storyId = story.storyId, storyTitle = story.title,
            storyCoverUrl = story.coverUrl, authorName = story.authorName, isBookmarked = !_s.value.isBookmarked)
        libRepo.upsert(entry); _s.update { it.copy(isBookmarked = entry.isBookmarked) }
    }

    fun clearJustUnlocked() = _s.update { it.copy(justUnlockedId = null) }
    fun clearError()        = _s.update { it.copy(error = null) }
}

// ── Reader ────────────────────────────────────────────────────────────────────
@HiltViewModel
class ReaderViewModel @Inject constructor(private val storyRepo: StoryRepository) : ViewModel() {
    private val _ep = MutableStateFlow<Episode?>(null)
    val episode = _ep.asStateFlow()
    fun load(id: String) = viewModelScope.launch {
        _ep.value = (storyRepo.getEpisode(id) as? Resource.Success)?.data
    }
}

// ── Writer ────────────────────────────────────────────────────────────────────
data class WriterUiState(
    val myStories: List<Story> = emptyList(),
    val storyTitle: String = "",
    val storyDesc: String = "",
    val storyCategory: String = "",
    val storyLanguage: String = "en",
    val epTitle: String = "",
    val epContent: String = "",
    val wordCount: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedStoryId: String? = null,
    val savedEpisodeId: String? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class WriterViewModel @Inject constructor(private val storyRepo: StoryRepository) : ViewModel() {
    private val _s = MutableStateFlow(WriterUiState())
    val state = _s.asStateFlow()

    fun loadMyStories(authorId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        when (val r = storyRepo.getAuthorStories(authorId)) {
            is Resource.Success -> _s.update { it.copy(myStories = r.data, isLoading = false) }
            is Resource.Error   -> _s.update { it.copy(isLoading = false, error = r.message) }
            else -> Unit
        }
    }

    // Delete episode — called from EpisodeReaderScreen
    fun deleteEpisode(episodeId: String, storyId: String, onDone: () -> Unit) = viewModelScope.launch {
        when (val r = storyRepo.deleteEpisode(episodeId, storyId)) {
            is Resource.Success -> { _s.update { it.copy(message = "Chapter deleted") }; onDone() }
            is Resource.Error   -> _s.update { it.copy(error = r.message) }
            else -> Unit
        }
    }

    // Update episode — called from EditEpisodeScreen
    fun updateEpisode(episodeId: String, storyId: String, title: String, content: String, onDone: () -> Unit) = viewModelScope.launch {
        if (title.isBlank() || content.isBlank()) { _s.update { it.copy(error = "Title and content required") }; return@launch }
        _s.update { it.copy(isSaving = true, error = null) }
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
    fun onEpTitleChange(v: String)  = _s.update { it.copy(epTitle = v) }
    fun onEpContentChange(v: String) {
        val wc = v.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        _s.update { it.copy(epContent = v, wordCount = wc) }
    }

    fun saveStory(authorId: String, authorName: String) {
        val s = _s.value
        if (s.storyTitle.isBlank() || s.storyDesc.isBlank() || s.storyCategory.isBlank()) {
            _s.update { it.copy(error = "Fill title, description and category") }; return
        }
        viewModelScope.launch {
            _s.update { it.copy(isSaving = true, error = null) }
            val story = Story(title = s.storyTitle, description = s.storyDesc,
                category = s.storyCategory, language = s.storyLanguage,
                authorId = authorId, authorName = authorName, status = "PUBLISHED")
            when (val r = storyRepo.saveStory(story)) {
                is Resource.Success -> _s.update { it.copy(isSaving = false, savedStoryId = r.data) }
                is Resource.Error   -> _s.update { it.copy(isSaving = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun saveEpisode(storyId: String, authorId: String, chapterNumber: Int, publish: Boolean) {
        val s = _s.value
        if (s.epTitle.isBlank() || s.epContent.isBlank()) { _s.update { it.copy(error = "Title and content required") }; return }
        viewModelScope.launch {
            _s.update { it.copy(isSaving = true, error = null) }
            val ep = Episode(storyId = storyId, authorId = authorId, chapterNumber = chapterNumber,
                title = s.epTitle, content = s.epContent,
                status = if (publish) "PUBLISHED" else "DRAFT")
            when (val r = storyRepo.saveEpisode(ep)) {
                is Resource.Success -> _s.update { it.copy(isSaving = false, savedEpisodeId = r.data) }
                is Resource.Error   -> _s.update { it.copy(isSaving = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun resetSaved()   = _s.update { it.copy(savedStoryId = null, savedEpisodeId = null) }
    fun clearMessage() = _s.update { it.copy(message = null) }
    fun clearError()   = _s.update { it.copy(error = null) }
}

// ── Library ───────────────────────────────────────────────────────────────────
data class LibraryUiState(val entries: List<LibraryEntry> = emptyList(), val isLoading: Boolean = false)

@HiltViewModel
class LibraryViewModel @Inject constructor(private val libRepo: LibraryRepository) : ViewModel() {
    private val _s = MutableStateFlow(LibraryUiState())
    val state = _s.asStateFlow()
    fun load(userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        when (val r = libRepo.getUserLibrary(userId)) {
            is Resource.Success -> _s.update { it.copy(entries = r.data, isLoading = false) }
            else -> _s.update { it.copy(isLoading = false) }
        }
    }
}

// ── Profile ───────────────────────────────────────────────────────────────────
data class ProfileUiState(val coinHistory: List<CoinTransaction> = emptyList(), val isLoading: Boolean = false)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val coinRepo: CoinRepository) : ViewModel() {
    private val _s = MutableStateFlow(ProfileUiState())
    val state = _s.asStateFlow()
    fun load(userId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val h = (coinRepo.getCoinHistory(userId) as? Resource.Success)?.data ?: emptyList()
        _s.update { it.copy(coinHistory = h, isLoading = false) }
    }
}

// ── Follow ────────────────────────────────────────────────────────────────────
data class FollowUiState(val isFollowing: Boolean = false, val isLoading: Boolean = false)

@HiltViewModel
class FollowViewModel @Inject constructor(private val followRepo: FollowRepository) : ViewModel() {
    private val _s = MutableStateFlow(FollowUiState())
    val state = _s.asStateFlow()
    fun check(followerId: String, followeeId: String) = viewModelScope.launch {
        _s.update { it.copy(isFollowing = followRepo.isFollowing(followerId, followeeId)) }
    }
    fun toggle(followerId: String, followeeId: String) = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        when (val r = followRepo.toggleFollow(followerId, followeeId)) {
            is Resource.Success -> _s.update { it.copy(isFollowing = r.data, isLoading = false) }
            else -> _s.update { it.copy(isLoading = false) }
        }
    }
}

// ── Admin ─────────────────────────────────────────────────────────────────────
data class AdminUiState(
    val stats: AdminStats = AdminStats(),
    val users: List<User> = emptyList(),
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val selectedTab: Int = 0
)

@HiltViewModel
class AdminViewModel @Inject constructor(private val adminRepo: AdminRepository) : ViewModel() {
    private val _s = MutableStateFlow(AdminUiState())
    val state = _s.asStateFlow()
    fun load() = viewModelScope.launch {
        _s.update { it.copy(isLoading = true) }
        val stats   = adminRepo.getStats()
        val users   = (adminRepo.getAllUsers()   as? Resource.Success)?.data ?: emptyList()
        val stories = (adminRepo.getAllStories() as? Resource.Success)?.data ?: emptyList()
        _s.update { it.copy(stats = stats, users = users, stories = stories, isLoading = false) }
    }
    fun onTabChange(tab: Int) = _s.update { it.copy(selectedTab = tab) }
    fun updateRole(userId: String, role: UserRole) = viewModelScope.launch {
        adminRepo.updateUserRole(userId, role); _s.update { it.copy(message = "Role updated") }; load()
    }
    fun toggleBan(userId: String, banned: Boolean) = viewModelScope.launch {
        adminRepo.toggleBan(userId, !banned)
        _s.update { it.copy(message = if (!banned) "User banned" else "User unbanned") }; load()
    }
    fun suspendStory(storyId: String) = viewModelScope.launch {
        adminRepo.updateStoryStatus(storyId, "SUSPENDED"); _s.update { it.copy(message = "Story suspended") }; load()
    }
    fun restoreStory(storyId: String) = viewModelScope.launch {
        adminRepo.updateStoryStatus(storyId, "PUBLISHED"); _s.update { it.copy(message = "Story restored") }; load()
    }
    fun deleteStory(storyId: String) = viewModelScope.launch {
        adminRepo.deleteStory(storyId); _s.update { it.copy(message = "Story deleted") }; load()
    }
    fun clearMessage() = _s.update { it.copy(message = null) }
}
