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
) { val isAuthenticated get() = user != null }

@HiltViewModel
class AuthViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.currentUserFlow.collect { user -> _state.update { it.copy(user = user) } }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) = doAuth { repo.signInWithGoogle(account) }
    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) { _state.update { it.copy(error = "Email and password required") }; return }
        doAuth { repo.signInWithEmail(email, pass) }
    }
    fun register(name: String, email: String, pass: String) {
        if (name.isBlank() || email.isBlank() || pass.length < 6) {
            _state.update { it.copy(error = "All fields required, min 6-char password") }; return
        }
        doAuth { repo.register(name, email, pass) }
    }
    fun signOut() { repo.signOut(); _state.value = AuthUiState() }
    fun clearError() = _state.update { it.copy(error = null) }

    private fun doAuth(block: suspend () -> Resource<User>) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val r = block()) {
            is Resource.Success -> _state.update { it.copy(isLoading = false, user = r.data) }
            is Resource.Error   -> _state.update { it.copy(isLoading = false, error = r.message) }
            else -> _state.update { it.copy(isLoading = false) }
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
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    val categories = KathakarMeta.CATEGORIES
    val languages  = KathakarMeta.LANGUAGES
    private var cursor: DocumentSnapshot? = null
    private var searchJob: Job? = null

    init { load() }

    fun load(reset: Boolean = true) {
        if (!reset && (_state.value.isLoadingMore || !_state.value.hasMore)) return
        if (reset) cursor = null
        viewModelScope.launch {
            _state.update { if (reset) it.copy(isLoading = true, stories = emptyList()) else it.copy(isLoadingMore = true) }
            val s = _state.value
            when (val r = storyRepo.getStories(s.selectedCategory, s.selectedLanguage, cursor)) {
                is Resource.Success -> {
                    val (list, next) = r.data; cursor = next
                    _state.update { it.copy(stories = if (reset) list else it.stories + list,
                        hasMore = next != null, isLoading = false, isLoadingMore = false) }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, isLoadingMore = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun onSearch(q: String) {
        _state.update { it.copy(searchQuery = q) }
        searchJob?.cancel()
        if (q.isBlank()) { load(); return }
        searchJob = viewModelScope.launch {
            delay(400)
            _state.update { it.copy(isLoading = true) }
            when (val r = storyRepo.searchStories(q)) {
                is Resource.Success -> _state.update { it.copy(stories = r.data, isLoading = false, hasMore = false) }
                is Resource.Error   -> _state.update { it.copy(isLoading = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun onCategory(c: String?) { _state.update { it.copy(selectedCategory = c) }; load() }
    fun onLanguage(l: String?) { _state.update { it.copy(selectedLanguage = l) }; load() }
    fun loadMore()   = load(reset = false)
    fun clearError() = _state.update { it.copy(error = null) }
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
    private val _state = MutableStateFlow(StoryUiState())
    val state: StateFlow<StoryUiState> = _state.asStateFlow()

    fun load(storyId: String, userId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        val story    = (storyRepo.getStory(storyId) as? Resource.Success)?.data
        val episodes = (storyRepo.getEpisodes(storyId) as? Resource.Success)?.data ?: emptyList()
        val unlocked = coinRepo.getUnlockedIds(userId, storyId)
        val entry    = libRepo.getEntry(userId, storyId)
        _state.update { it.copy(story = story, episodes = episodes, unlockedIds = unlocked,
            isBookmarked = entry?.isBookmarked ?: false, isLoading = false) }
    }

    fun unlock(episode: Episode, user: User) {
        if (_state.value.unlockingId != null) return
        viewModelScope.launch {
            _state.update { it.copy(unlockingId = episode.episodeId, error = null) }
            when (val r = coinRepo.unlockEpisode(user.userId, episode.authorId, episode)) {
                is Resource.Success -> _state.update { it.copy(unlockingId = null,
                    unlockedIds = it.unlockedIds + episode.episodeId,
                    justUnlockedId = episode.episodeId, newCoinBalance = r.data) }
                is Resource.Error -> _state.update { it.copy(unlockingId = null, error = r.message) }
                else -> Unit
            }
        }
    }

    fun toggleBookmark(userId: String, story: Story) = viewModelScope.launch {
        val entry = LibraryEntry(userId = userId, storyId = story.storyId,
            storyTitle = story.title, storyCoverUrl = story.coverUrl,
            authorName = story.authorName, isBookmarked = !_state.value.isBookmarked)
        libRepo.upsert(entry)
        _state.update { it.copy(isBookmarked = entry.isBookmarked) }
    }

    fun clearJustUnlocked() = _state.update { it.copy(justUnlockedId = null) }
    fun clearError()        = _state.update { it.copy(error = null) }
}

// ── Reader ────────────────────────────────────────────────────────────────────

@HiltViewModel
class ReaderViewModel @Inject constructor(private val storyRepo: StoryRepository) : ViewModel() {
    private val _episode = MutableStateFlow<Episode?>(null)
    val episode: StateFlow<Episode?> = _episode.asStateFlow()

    fun load(episodeId: String) = viewModelScope.launch {
        _episode.value = (storyRepo.getEpisode(episodeId) as? Resource.Success)?.data
    }
}

// ── Writer ────────────────────────────────────────────────────────────────────

data class WriterUiState(
    val myStories: List<Story> = emptyList(),
    val storyTitle: String = "", val storyDesc: String = "",
    val storyCategory: String = "", val storyLanguage: String = "en",
    val epTitle: String = "", val epContent: String = "", val wordCount: Int = 0,
    val isLoading: Boolean = false, val isSaving: Boolean = false,
    val savedStoryId: String? = null, val savedEpisodeId: String? = null,
    val error: String? = null
)

@HiltViewModel
class WriterViewModel @Inject constructor(private val storyRepo: StoryRepository) : ViewModel() {
    private val _state = MutableStateFlow(WriterUiState())
    val state: StateFlow<WriterUiState> = _state.asStateFlow()

    fun loadMyStories(authorId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        when (val r = storyRepo.getAuthorStories(authorId)) {
            is Resource.Success -> _state.update { it.copy(myStories = r.data, isLoading = false) }
            is Resource.Error   -> _state.update { it.copy(isLoading = false, error = r.message) }
            else -> Unit
        }
    }

    fun onTitleChange(v: String)    = _state.update { it.copy(storyTitle = v) }
    fun onDescChange(v: String)     = _state.update { it.copy(storyDesc = v) }
    fun onCategoryChange(v: String) = _state.update { it.copy(storyCategory = v) }
    fun onLanguageChange(v: String) = _state.update { it.copy(storyLanguage = v) }
    fun onEpTitleChange(v: String)  = _state.update { it.copy(epTitle = v) }
    fun onEpContentChange(v: String) {
        val wc = v.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        _state.update { it.copy(epContent = v, wordCount = wc) }
    }

    fun saveStory(authorId: String, authorName: String) {
        val s = _state.value
        if (s.storyTitle.isBlank() || s.storyDesc.isBlank() || s.storyCategory.isBlank()) {
            _state.update { it.copy(error = "Title, description and category required") }; return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val story = Story(title = s.storyTitle, description = s.storyDesc,
                category = s.storyCategory, language = s.storyLanguage,
                authorId = authorId, authorName = authorName, status = StoryStatus.PUBLISHED)
            when (val r = storyRepo.saveStory(story)) {
                is Resource.Success -> _state.update { it.copy(isSaving = false, savedStoryId = r.data) }
                is Resource.Error   -> _state.update { it.copy(isSaving = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun saveEpisode(storyId: String, authorId: String, chapterNumber: Int, publish: Boolean) {
        val s = _state.value
        if (s.epTitle.isBlank() || s.epContent.isBlank()) {
            _state.update { it.copy(error = "Title and content required") }; return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val ep = Episode(storyId = storyId, authorId = authorId,
                chapterNumber = chapterNumber, title = s.epTitle, content = s.epContent,
                status = if (publish) EpisodeStatus.PUBLISHED else EpisodeStatus.DRAFT)
            when (val r = storyRepo.saveEpisode(ep)) {
                is Resource.Success -> _state.update { it.copy(isSaving = false, savedEpisodeId = r.data) }
                is Resource.Error   -> _state.update { it.copy(isSaving = false, error = r.message) }
                else -> Unit
            }
        }
    }

    fun resetSaved() = _state.update { it.copy(savedStoryId = null, savedEpisodeId = null) }
    fun clearError() = _state.update { it.copy(error = null) }
}

// ── Library ───────────────────────────────────────────────────────────────────

data class LibraryUiState(val entries: List<LibraryEntry> = emptyList(), val isLoading: Boolean = false)

@HiltViewModel
class LibraryViewModel @Inject constructor(private val libRepo: LibraryRepository) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    fun load(userId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        when (val r = libRepo.getUserLibrary(userId)) {
            is Resource.Success -> _state.update { it.copy(entries = r.data, isLoading = false) }
            else -> _state.update { it.copy(isLoading = false) }
        }
    }
}

// ── Profile ───────────────────────────────────────────────────────────────────

data class ProfileUiState(val coinHistory: List<CoinTransaction> = emptyList(), val isLoading: Boolean = false)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val coinRepo: CoinRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load(userId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        val history = (coinRepo.getCoinHistory(userId) as? Resource.Success)?.data ?: emptyList()
        _state.update { it.copy(coinHistory = history, isLoading = false) }
    }
}
