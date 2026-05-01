package com.kathakar.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kathakar.app.R
import com.kathakar.app.domain.model.*
import com.kathakar.app.viewmodel.*

// ── Bottom Nav — 5 tabs now ────────────────────────────────────────────────────
@Composable
fun KathakarBottomNav(activeTab: Int, onRead: () -> Unit, onWrite: () -> Unit,
                      onPoems: () -> Unit, onLibrary: () -> Unit, onProfile: () -> Unit) {
    NavigationBar {
        NavigationBarItem(selected = activeTab == 0, onClick = onRead,
            icon = { Icon(Icons.Default.Home, null) }, label = { Text(text = stringResource(R.string.nav_read)) })
        NavigationBarItem(selected = activeTab == 1, onClick = onWrite,
            icon = { Icon(Icons.Default.Edit, null) }, label = { Text(text = stringResource(R.string.nav_write)) })
        NavigationBarItem(selected = activeTab == 2, onClick = onPoems,
            icon = { Icon(Icons.Default.Star, null) }, label = { Text(text = stringResource(R.string.nav_poems)) })
        NavigationBarItem(selected = activeTab == 3, onClick = onLibrary,
            icon = { Icon(Icons.Default.Favorite, null) }, label = { Text(text = stringResource(R.string.nav_library)) })
        NavigationBarItem(selected = activeTab == 4, onClick = onProfile,
            icon = { Icon(Icons.Default.Person, null) }, label = { Text(text = stringResource(R.string.nav_profile)) })
    }
}

// ── Coming Soon ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(title: String, reason: String, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(text = title) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { p ->
        Box(modifier = Modifier.fillMaxSize().padding(p).padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(text = reason, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onBack) { Text(text = stringResource(R.string.go_back)) }
            }
        }
    }
}

// ── Login ─────────────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(viewModel: AuthViewModel, onSuccess: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val gso = remember { GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(ctx.getString(R.string.default_web_client_id)).requestEmail().build() }
    val googleClient   = remember { GoogleSignIn.getClient(ctx, gso) }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        try { viewModel.signInWithGoogle(GoogleSignIn.getSignedInAccountFromIntent(r.data).getResult(ApiException::class.java))
        } catch (_: ApiException) { }
    }
    LaunchedEffect(state.isAuthenticated) { if (state.isAuthenticated) onSuccess() }
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") };  var isRegister by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = stringResource(R.string.app_name), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = stringResource(R.string.app_tagline), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))
        AnimatedVisibility(isRegister) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(text = stringResource(R.string.full_name)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp), singleLine = true)
        }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(text = stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp), singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(text = stringResource(R.string.password)) },
            modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(14.dp), singleLine = true)
        AnimatedVisibility(state.error != null) {
            Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(text = state.error ?: "", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp) }
        }
        AnimatedVisibility(isRegister) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(text = stringResource(R.string.free_coins_signup, MvpConfig.FREE_COINS_ON_SIGNUP),
                    modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer) }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { viewModel.clearError()
            if (isRegister) viewModel.register(name, email, password) else viewModel.signInWithEmail(email, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isLoading) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(text = if (isRegister) stringResource(R.string.create_account) else stringResource(R.string.sign_in), fontWeight = FontWeight.Medium)
        }
        TextButton(onClick = { isRegister = !isRegister; viewModel.clearError() }) {
            Text(text = if (isRegister) stringResource(R.string.already_have_account) else stringResource(R.string.new_here)) }
        Spacer(Modifier.height(40.dp))
    }
}

// ── Home (Stories) ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(user: User, onStoryClick: (String) -> Unit, onWriteClick: () -> Unit,
               onLibraryClick: () -> Unit, onProfileClick: () -> Unit, onPoemsClick: () -> Unit,
               onSettingsClick: () -> Unit = {},
               vm: HomeViewModel = hiltViewModel()) {
    val state     by vm.state.collectAsState()
    val listState  = rememberLazyListState()
    Scaffold(
        topBar = { TopAppBar(
            title = { Text(text = stringResource(R.string.app_name), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            actions = {
                // ⚙️ Settings gear icon
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                // Profile avatar
                IconButton(onClick = onProfileClick) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            Text(text = user.initials, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }) },
        bottomBar = { KathakarBottomNav(0, onRead = {}, onWrite = onWriteClick, onPoems = onPoemsClick, onLibrary = onLibraryClick, onProfile = onProfileClick) }
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item { OutlinedTextField(value = state.searchQuery, onValueChange = vm::onSearch,
                placeholder = { Text(text = stringResource(R.string.search_stories)) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true, shape = RoundedCornerShape(24.dp)) }
            item { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.selectedCategory == null, onClick = { vm.onCategory(null) }, label = { Text(text = stringResource(R.string.filter_all)) }) }
                items(vm.categories) { cat -> FilterChip(selected = state.selectedCategory == cat, onClick = { vm.onCategory(cat) }, label = { Text(text = cat) }) }
            } }
            item { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.selectedLanguage == null, onClick = { vm.onLanguage(null) }, label = { Text(text = stringResource(R.string.filter_all_languages)) }) }
                items(vm.languages) { langPair ->
                        FilterChip(selected = state.selectedLanguage == langPair.first,
                            onClick = { vm.onLanguage(langPair.first) },
                            label = { Text(text = langPair.second) }) }
            } }
            if (state.isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (state.stories.isEmpty() && state.error == null) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.no_stories_yet), style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(R.string.be_first_to_write), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onWriteClick) { Text(text = stringResource(R.string.write_a_story)) }
                    } } }
            } else {
                state.error?.let { err -> item { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                        TextButton(onClick = { vm.refresh() }) { Text(text = stringResource(R.string.retry)) } } } } }
                items(state.stories, key = { it.storyId }) { story ->
                    StoryCard(story = story, onClick = { onStoryClick(story.storyId) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) }
                if (state.isLoadingMore) { item { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) } } }
            }
        }
    }
}

@Composable
fun StoryCard(story: Story, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(model = story.coverUrl.ifEmpty { null }, contentDescription = story.title,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = story.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "by " + story.authorName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                Text(text = story.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (story.category.isNotEmpty()) SuggestionChip(onClick = {}, label = { Text(text = story.category, fontSize = 11.sp) })
                    SuggestionChip(onClick = {}, label = { Text(text = story.totalEpisodes.toString() + " eps", fontSize = 11.sp) })
                }
            }
        }
    }
}

// ── Story Detail ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(storyId: String, user: User, onBack: () -> Unit,
                      onReadEpisode: (String, String) -> Unit, onBuyCoins: () -> Unit,
                      onAuthorClick: (String) -> Unit = {},
                      vm: StoryViewModel = hiltViewModel(), followVm: FollowViewModel = hiltViewModel()) {
    val state       by vm.state.collectAsState()
    val followState by followVm.state.collectAsState()
    LaunchedEffect(storyId) { vm.load(storyId, user.userId) }
    LaunchedEffect(state.story) { state.story?.authorId?.let { aid -> if (aid != user.userId) followVm.check(user.userId, aid) } }
    LaunchedEffect(state.justUnlockedId) { state.justUnlockedId?.let { onReadEpisode(it, state.story?.authorId ?: ""); vm.clearJustUnlocked() } }
    Scaffold(topBar = { TopAppBar(title = { Text(text = state.story?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { IconButton(onClick = { state.story?.let { vm.toggleBookmark(user.userId, it) } }) {
            Icon(if (state.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } }) }
    ) { p ->
        if (state.isLoading) { Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Column(modifier = Modifier.padding(16.dp)) {
                Text(text = state.story?.title ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "by " + (state.story?.authorName ?: ""), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.weight(1f).clickable { state.story?.authorId?.let { onAuthorClick(it) } })
                    if (state.story?.authorId != user.userId) {
                        OutlinedButton(onClick = { state.story?.authorId?.let { followVm.toggle(user.userId, it) } },
                            shape = RoundedCornerShape(20.dp), enabled = !followState.isLoading, modifier = Modifier.height(32.dp)) {
                            Text(text = if (followState.isFollowing) stringResource(R.string.following) else stringResource(R.string.follow), fontSize = 11.sp) } }
                }
                Spacer(Modifier.height(8.dp))
                Text(text = state.story?.description ?: "", fontSize = 14.sp, lineHeight = 22.sp)
            } }
            item {
                val isAuthor = user.userId == (state.story?.authorId ?: "")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(if (isAuthor) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (isAuthor) Text(text = stringResource(R.string.your_story_free), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        else { Text(text = stringResource(R.string.ch1_free_info, MvpConfig.EPISODE_UNLOCK_COST), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = stringResource(R.string.balance_coins, user.coinBalance), fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    }
                }
            }
            state.error?.let { err -> item { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text(text = err, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } } }
            item { Text(text = stringResource(R.string.episodes_heading), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
            items(state.episodes, key = { it.episodeId }) { ep ->
                val isAuthor   = user.userId == (state.story?.authorId ?: "")
                val isUnlocked = isAuthor || ep.isFree || ep.chapterNumber == 1 || state.unlockedIds.contains(ep.episodeId)
                EpisodeRow(episode = ep, isUnlocked = isUnlocked, isUnlocking = false, isAuthor = isAuthor, userCoins = user.coinBalance,
                    onTap = { if (isUnlocked) onReadEpisode(ep.episodeId, state.story?.authorId ?: "") else vm.unlock(ep, user) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeRow(episode: Episode, isUnlocked: Boolean, isUnlocking: Boolean, isAuthor: Boolean, userCoins: Int, onTap: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Card(onClick = { if (isUnlocked) onTap() else showDialog = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(text = episode.chapterNumber.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = episode.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = episode.wordCount.toString() + " words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            when {
                isUnlocking -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                isAuthor    -> Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) { Text(text = stringResource(R.string.yours_label), fontSize = 10.sp, modifier = Modifier.padding(5.dp, 2.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) }
                isUnlocked  -> { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    if (episode.isFree || episode.chapterNumber == 1) Text(text = stringResource(R.string.free_label), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp)) }
                else -> { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(text = episode.unlockCostCoins.toString() + " coins", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp)) }
            }
        }
    }
    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text(text = stringResource(R.string.unlock_episode)) },
            text = { Column { Text(text = episode.title + " costs " + episode.unlockCostCoins + " coins.")
                Text(text = stringResource(R.string.your_balance, userCoins), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                if (userCoins < episode.unlockCostCoins) Text(text = stringResource(R.string.not_enough_coins), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) } },
            confirmButton = { Button(onClick = { showDialog = false; if (userCoins >= episode.unlockCostCoins) onTap() }, enabled = userCoins >= episode.unlockCostCoins) { Text(text = stringResource(R.string.unlock)) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(text = stringResource(R.string.cancel)) } })
    }
}

// ── Episode Reader ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeReaderScreen(episodeId: String, storyId: String, authorId: String, currentUserId: String,
                        currentUser: User? = null,
                        onBack: () -> Unit, onEdit: () -> Unit, onDeleted: () -> Unit,
                        vm: ReaderViewModel = hiltViewModel(), writerVm: WriterViewModel = hiltViewModel()) {
    val state    by vm.state.collectAsState()
    val ep       = state.episode
    val wState   by writerVm.state.collectAsState()
    val snackbar  = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isAuthor = currentUserId == authorId
    LaunchedEffect(episodeId) { vm.load(episodeId, currentUserId) }
    LaunchedEffect(wState.message) { wState.message?.let { snackbar.showSnackbar(it); writerVm.clearMessage() } }
    LaunchedEffect(wState.error)   { wState.error?.let   { snackbar.showSnackbar(it); writerVm.clearError() } }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(text = state.episode?.let { "Chapter " + it.chapterNumber } ?: "Reading...", maxLines = 1) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            actions = { if (isAuthor) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit chapter", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete chapter", tint = MaterialTheme.colorScheme.error) }
            } }) }
    ) { p ->
        if (ep == null) { Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
            item { Text(text = ep!!.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(text = ep!!.wordCount.toString() + " words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Text(text = ep!!.content, fontSize = 16.sp, lineHeight = 28.sp) }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text(text = stringResource(R.string.delete_chapter_title)) },
            text = { Text(text = "\"${ep?.title ?: ""}\" " + stringResource(R.string.delete) + "?") },
            confirmButton = { Button(onClick = { showDeleteDialog = false; writerVm.deleteEpisode(episodeId, storyId) { onDeleted() } },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(text = stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(text = stringResource(R.string.cancel)) } })
    }
}

// ── Edit Episode ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEpisodeScreen(episodeId: String, storyId: String, onDone: () -> Unit, onBack: () -> Unit,
                      readerVm: ReaderViewModel = hiltViewModel(), writerVm: WriterViewModel = hiltViewModel()) {
    val readerState by readerVm.state.collectAsState()
    val ep = readerState.episode
    val wState by writerVm.state.collectAsState()
    var title   by remember { mutableStateOf("") }; var content by remember { mutableStateOf("") }
    var loaded  by remember { mutableStateOf(false) }
    LaunchedEffect(episodeId) { readerVm.load(episodeId, "") }
    LaunchedEffect(ep) { if (ep != null && !loaded) { title = ep!!.title; content = ep!!.content; loaded = true } }
    val wordCount = content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    Scaffold(topBar = { TopAppBar(title = { Text(text = if (ep != null) stringResource(R.string.edit_chapter) + " " + ep!!.chapterNumber else stringResource(R.string.edit_chapter)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { Text(text = wordCount.toString() + " words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp)) }) }
    ) { p ->
        if (ep == null) { Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                Text(text = stringResource(R.string.editing_chapter_of, ep!!.chapterNumber), modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer) }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(text = stringResource(R.string.chapter_title_hint)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text(text = stringResource(R.string.chapter_content_hint)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp), shape = RoundedCornerShape(12.dp), minLines = 15)
            wState.error?.let { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) { Text(text = it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            Button(onClick = { writerVm.updateEpisode(episodeId, storyId, title, content) { onDone() } },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !wState.isSaving && title.isNotBlank() && content.isNotBlank()) {
                if (wState.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(text = stringResource(R.string.save_changes), fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Write (Stories) ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(user: User, onCreateStory: () -> Unit, onCreateEpisode: (String, Int) -> Unit,
                onAiClick: () -> Unit, onBack: () -> Unit, onReadStory: (String) -> Unit = {},
                onLibraryClick: () -> Unit = {}, onProfileClick: () -> Unit = {},
                onPoemsClick: () -> Unit = {}, onWriterDashboard: () -> Unit = {},
                vm: WriterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var tab   by remember { mutableStateOf(0) }
    LaunchedEffect(user.userId) { vm.loadMyStories(user.userId) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.write_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) },
        bottomBar = { KathakarBottomNav(1, onRead = onBack, onWrite = {}, onPoems = onPoemsClick, onLibrary = onLibraryClick, onProfile = onProfileClick) }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(text = stringResource(R.string.my_stories)) })
                Tab(selected = tab == 1, onClick = { tab = 1; onWriterDashboard() }, text = { Text(text = stringResource(R.string.writer_dashboard)) })
                Tab(selected = tab == 2, onClick = { tab = 2; onAiClick() }, text = { Text(text = stringResource(R.string.ai_assist)) })
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Button(onClick = onCreateStory, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(text = stringResource(R.string.new_story)) } }
                if (state.isLoading) { item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } } }
                items(state.myStories, key = { it.storyId }) { story ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = story.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = story.totalEpisodes.toString() + " episodes - " + story.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp)) }
                                Surface(color = if (story.status == "PUBLISHED") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                                    Text(text = story.status.lowercase(), fontSize = 11.sp, modifier = Modifier.padding(7.dp, 3.dp)) } }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onReadStory(story.storyId) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text(text = stringResource(R.string.read_label), fontSize = 12.sp) }
                                Button(onClick = { onCreateEpisode(story.storyId, story.totalEpisodes + 1) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text(text = "Ch. " + (story.totalEpisodes + 1), fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Create Story ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(user: User, onSaved: (String) -> Unit, onBack: () -> Unit, vm: WriterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.savedStoryId) { state.savedStoryId?.let { onSaved(it); vm.resetSaved() } }
    Scaffold(topBar = { TopAppBar(title = { Text(text = stringResource(R.string.new_story_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = state.storyTitle, onValueChange = vm::onTitleChange, label = { Text(text = stringResource(R.string.story_title_hint)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = state.storyDesc, onValueChange = vm::onDescChange, label = { Text(text = stringResource(R.string.story_description_hint)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 3)
            var catExp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExp, onExpandedChange = { catExp = it }) {
                OutlinedTextField(value = state.storyCategory.ifEmpty { stringResource(R.string.select_category) }, onValueChange = {}, readOnly = true, label = { Text(text = stringResource(R.string.category_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExp) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = catExp, onDismissRequest = { catExp = false }) {
                    KathakarMeta.CATEGORIES.forEach { cat -> DropdownMenuItem(text = { Text(text = cat) }, onClick = { vm.onCategoryChange(cat); catExp = false }) } }
            }
            var langExp by remember { mutableStateOf(false) }
            val selectedLang = KathakarMeta.LANGUAGES.find { it.first == state.storyLanguage }?.second ?: "English"
            ExposedDropdownMenuBox(expanded = langExp, onExpandedChange = { langExp = it }) {
                OutlinedTextField(value = selectedLang, onValueChange = {}, readOnly = true, label = { Text(text = stringResource(R.string.language_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExp) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = langExp, onDismissRequest = { langExp = false }) {
                    KathakarMeta.LANGUAGES.forEach { (code, name) -> DropdownMenuItem(text = { Text(text = name) }, onClick = { vm.onLanguageChange(code); langExp = false }) } }
            }
            state.error?.let { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) { Text(text = it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            Button(onClick = { vm.saveStory(user.userId, user.name) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isSaving) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(text = stringResource(R.string.create_story_button), fontWeight = FontWeight.Medium) }
        }
    }
}

// ── Create Episode ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEpisodeScreen(storyId: String, chapterNumber: Int, authorId: String,
                        onDone: () -> Unit, onBack: () -> Unit,
                        vm: WriterViewModel = hiltViewModel()) {
    val state   by vm.state.collectAsState()
    val context  = LocalContext.current
    var fileError   by remember { mutableStateOf<String?>(null) }
    var importedFrom by remember { mutableStateOf<String?>(null) }

    // File picker launcher — accepts .docx and .txt
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = com.kathakar.app.util.FileUtils.readFile(context, uri)
            if (result.isSuccess) {
                vm.onEpContentChange(result.text)
                importedFrom = result.fileName
                fileError    = null
            } else {
                fileError = result.error
            }
        }
    }

    LaunchedEffect(state.savedEpisodeId) { if (state.savedEpisodeId != null) { onDone(); vm.resetSaved() } }

    Scaffold(topBar = { TopAppBar(
        title = { Text(text = stringResource(R.string.reading_chapter, chapterNumber)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { Text(text = state.wordCount.toString() + " words", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp)) }) }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Chapter title
            OutlinedTextField(value = state.epTitle, onValueChange = vm::onEpTitleChange,
                label = { Text(text = stringResource(R.string.chapter_title_hint)) }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true)

            // Chapter 1 free badge
            if (chapterNumber == 1) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                    Text(text = stringResource(R.string.chapter_1_free),
                        modifier = Modifier.padding(10.dp), fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer) }
            }

            // ── FILE UPLOAD SECTION ──────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(R.string.import_from_file), fontWeight = FontWeight.Medium,
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(text = stringResource(R.string.import_file_hint),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp)

                    // Imported file badge
                    importedFrom?.let { fileName ->
                        Surface(color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.padding(8.dp, 5.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(text = fileName, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    vm.onEpContentChange(""); importedFrom = null
                                }, modifier = Modifier.height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)) {
                                    Text(text = stringResource(R.string.clear_label), fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // File error
                    fileError?.let { err ->
                        Surface(color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)) {
                            Text(text = err, modifier = Modifier.padding(10.dp), fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer, lineHeight = 18.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { fileLauncher.launch(
                            arrayOf("text/plain",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = if (importedFrom != null) stringResource(R.string.replace_file) else stringResource(R.string.choose_file))
                    }
                }
            }
            // ── END FILE UPLOAD ───────────────────────────────────────────────

            // Divider between upload and manual typing
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(text = "  " + stringResource(R.string.or_type_manually) + "  ", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // Content editor
            OutlinedTextField(value = state.epContent, onValueChange = vm::onEpContentChange,
                label = { Text(text = stringResource(R.string.write_story_hint)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
                shape = RoundedCornerShape(12.dp), minLines = 10)

            state.error?.let { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                Text(text = it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.saveEpisode(storyId, authorId, chapterNumber, false) },
                    modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSaving) { Text(text = stringResource(R.string.save_draft)) }
                Button(onClick = { vm.saveEpisode(storyId, authorId, chapterNumber, true) },
                    modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSaving && state.epContent.isNotBlank()) {
                    if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(text = stringResource(R.string.publish), fontWeight = FontWeight.Medium) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// POEMS TAB — Main screen, write sheet, detail screen
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemsScreen(user: User, onPoemClick: (String, String) -> Unit,
                onReadClick: () -> Unit, onWriteClick: () -> Unit,
                onLibraryClick: () -> Unit, onProfileClick: () -> Unit,
                vm: PoemsViewModel = hiltViewModel()) {
    val state    by vm.state.collectAsState()
    val snackbar  = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    LaunchedEffect(user.userId) { vm.loadMyPoems(user.userId) }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    LaunchedEffect(state.error)   { state.error?.let   { snackbar.showSnackbar(it); vm.clearError() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(
            title = { Text(text = stringResource(R.string.poems_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            actions = {
                IconButton(onClick = { vm.openWriteSheet() }) {
                    Icon(Icons.Default.Add, "Write poem", tint = MaterialTheme.colorScheme.primary)
                }
            }) },
        bottomBar = { KathakarBottomNav(2, onRead = onReadClick, onWrite = onWriteClick, onPoems = {}, onLibrary = onLibraryClick, onProfile = onProfileClick) }
    ) { p ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(bottom = 16.dp)) {

            // Search bar
            item { OutlinedTextField(value = state.searchQuery, onValueChange = vm::onSearch,
                placeholder = { Text(text = stringResource(R.string.search_poems)) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true, shape = RoundedCornerShape(24.dp)) }

            // Format filter chips
            item { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.selectedFormat == null, onClick = { vm.onFormatFilter(null) }, label = { Text(text = stringResource(R.string.filter_all)) }) }
                items(vm.formats) { fmt -> FilterChip(selected = state.selectedFormat == fmt, onClick = { vm.onFormatFilter(fmt) }, label = { Text(text = fmt) }) }
            } }

            // Language filter chips
            item { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.selectedLanguage == null, onClick = { vm.onLanguageFilter(null) }, label = { Text(text = stringResource(R.string.filter_all_languages)) }) }
                KathakarMeta.LANGUAGES.forEach { (langCode, langName) -> item { FilterChip(selected = state.selectedLanguage == langCode, onClick = { vm.onLanguageFilter(langCode) }, label = { Text(text = langName) }) } }
            } }

            // Write poem banner — encourages writing
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { vm.openWriteSheet() },
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.write_a_poem), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = stringResource(R.string.write_poem_banner_sub), fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Loading
            if (state.isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (state.poems.isEmpty() && state.error == null) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.no_poems_yet), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(text = stringResource(R.string.be_first_poem), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.openWriteSheet() }) { Text(text = stringResource(R.string.write_a_poem)) }
                    } } }
            } else {
                state.error?.let { err -> item { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                        TextButton(onClick = { vm.refresh() }) { Text(text = stringResource(R.string.retry)) } } } } }

                items(state.poems, key = { it.poemId }) { poem ->
                    PoemCard(poem = poem, currentUserId = user.userId,
                        onClick = { onPoemClick(poem.poemId, poem.authorId) },
                        onEdit   = { vm.openWriteSheet(poem) },
                        onDelete = { vm.deletePoem(poem.poemId, user.userId) })
                }
                if (state.isLoadingMore) { item { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) } } }
            }
        }
    }

    // Write poem bottom sheet
    if (state.showWriteSheet) {
        WritePoemSheet(user = user, vm = vm)
    }
}

@Composable
fun PoemCard(poem: Poem, currentUserId: String, onClick: () -> Unit,
             onEdit: () -> Unit, onDelete: () -> Unit) {
    val isAuthor = currentUserId == poem.authorId
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = poem.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "by " + poem.authorName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                }
                if (isAuthor) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
                }
            }
            // Preview — first 3 lines of poem
            val previewLines = poem.content.lines().take(3).joinToString("\n")
            Text(text = previewLines + if (poem.content.lines().size > 3) "\n..." else "",
                fontSize = 14.sp, fontStyle = FontStyle.Italic, lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                    Text(text = poem.format, fontSize = 10.sp, modifier = Modifier.padding(5.dp, 2.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) }
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(6.dp)) {
                    Text(text = poem.mood, fontSize = 10.sp, modifier = Modifier.padding(5.dp, 2.dp), color = MaterialTheme.colorScheme.onTertiaryContainer) }
                Spacer(Modifier.weight(1f))
                Text(text = "♡ " + poem.likesCount, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (poem.tipsCount > 0) Text(text = "✦ " + poem.tipsCount, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePoemSheet(user: User, vm: PoemsViewModel) {
    val state     by vm.state.collectAsState()
    val context    = LocalContext.current
    val isEditing  = state.editingPoem != null
    var fileError    by remember { mutableStateOf<String?>(null) }
    var importedFrom by remember { mutableStateOf<String?>(null) }

    // File picker for poem — .txt only (poems are short, no need for .docx)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = com.kathakar.app.util.FileUtils.readFile(context, uri)
            if (result.isSuccess) {
                vm.onPoemContentChange(result.text)
                importedFrom = result.fileName
                fileError    = null
            } else {
                fileError = result.error
            }
        }
    }

    AlertDialog(
        onDismissRequest = { vm.closeWriteSheet() },
        title = { Text(text = if (isEditing) stringResource(R.string.edit_poem) else stringResource(R.string.write_a_poem),
            fontWeight = FontWeight.Medium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Title
                OutlinedTextField(value = state.poemTitle, onValueChange = vm::onPoemTitleChange,
                    label = { Text(text = stringResource(R.string.title_label)) }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true)

                // Format selector
                var formatExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = formatExp, onExpandedChange = { formatExp = it }) {
                    OutlinedTextField(value = state.poemFormat, onValueChange = {}, readOnly = true,
                        label = { Text(text = stringResource(R.string.format_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(formatExp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = formatExp, onDismissRequest = { formatExp = false }) {
                        vm.formats.forEach { fmt ->
                            DropdownMenuItem(text = { Text(text = fmt) },
                                onClick = { vm.onPoemFormatChange(fmt); formatExp = false }) }
                    }
                }

                // Language + Mood row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var langExp by remember { mutableStateOf(false) }
                    val langName = KathakarMeta.LANGUAGES.find { it.first == state.poemLanguage }?.second ?: "English"
                    ExposedDropdownMenuBox(expanded = langExp, onExpandedChange = { langExp = it },
                        modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = langName, onValueChange = {}, readOnly = true,
                            label = { Text(text = stringResource(R.string.language_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExp) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = langExp, onDismissRequest = { langExp = false }) {
                            KathakarMeta.LANGUAGES.forEach { (code, name) ->
                                DropdownMenuItem(text = { Text(text = name) },
                                    onClick = { vm.onPoemLanguageChange(code); langExp = false }) }
                        }
                    }
                    var moodExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = moodExp, onExpandedChange = { moodExp = it },
                        modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = state.poemMood, onValueChange = {}, readOnly = true,
                            label = { Text(text = stringResource(R.string.mood_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(moodExp) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = moodExp, onDismissRequest = { moodExp = false }) {
                            vm.moods.forEach { mood ->
                                DropdownMenuItem(text = { Text(text = mood) },
                                    onClick = { vm.onPoemMoodChange(mood); moodExp = false }) }
                        }
                    }
                }

                // ── FILE UPLOAD SECTION FOR POEMS ────────────────────────────
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = stringResource(R.string.import_poem_file),
                            fontWeight = FontWeight.Medium, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(text = stringResource(R.string.import_poem_hint),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Imported file badge
                        importedFrom?.let { fileName ->
                            Surface(color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)) {
                                Row(modifier = Modifier.padding(7.dp, 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(5.dp))
                                    Text(text = fileName, fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        vm.onPoemContentChange(""); importedFrom = null
                                    }, modifier = Modifier.height(22.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)) {
                                        Text(text = stringResource(R.string.clear_label), fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        // File error
                        fileError?.let { err ->
                            Text(text = err, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error, lineHeight = 16.sp)
                        }

                        OutlinedButton(
                            onClick = { fileLauncher.launch(
                                arrayOf("text/plain",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(text = if (importedFrom != null) "Replace file"
                                        else "Choose .txt / .docx",
                                fontSize = 12.sp)
                        }
                    }
                }
                // ── END FILE UPLOAD ───────────────────────────────────────────

                // Divider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(text = "  " + stringResource(R.string.or_type_below) + "  ", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Poem content editor
                OutlinedTextField(value = state.poemContent, onValueChange = vm::onPoemContentChange,
                    label = { Text(text = stringResource(R.string.your_poem_hint)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    shape = RoundedCornerShape(12.dp), minLines = 5,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp))

                Text(text = state.wordCount.toString() + " words",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                state.error?.let { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                    Text(text = it, modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp) } }
            }
        },
        confirmButton = {
            Button(onClick = { vm.savePoem(user.userId, user.name) },
                enabled = !state.isSaving && state.poemTitle.isNotBlank() && state.poemContent.isNotBlank()) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(text = if (isEditing) stringResource(R.string.update_label) else stringResource(R.string.publish))
            }
        },
        dismissButton = { TextButton(onClick = { vm.closeWriteSheet() }) { Text(text = stringResource(R.string.cancel)) } }
    )
}

// ── Poem Detail Screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemDetailScreen(poemId: String, authorId: String, user: User,
                     onBack: () -> Unit, onBuyCoins: () -> Unit,
                     vm: PoemDetailViewModel = hiltViewModel(),
                     followVm: FollowViewModel = hiltViewModel()) {
    val state       by vm.state.collectAsState()
    val followState by followVm.state.collectAsState()
    val snackbar     = remember { SnackbarHostState() }
    val isAuthor     = user.userId == authorId

    LaunchedEffect(poemId) { vm.load(poemId, user.userId) }
    LaunchedEffect(authorId) { if (!isAuthor) followVm.check(user.userId, authorId) }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    LaunchedEffect(state.error)   { state.error?.let   { snackbar.showSnackbar(it); vm.clearError() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(
            title = { Text(text = state.poem?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { p ->
        if (state.isLoading) { Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        val poem = state.poem ?: return@Scaffold

        LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)) {
            item {
                // Format + mood chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(text = poem.format, fontSize = 11.sp, modifier = Modifier.padding(7.dp, 3.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) }
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(text = poem.mood, fontSize = 11.sp, modifier = Modifier.padding(7.dp, 3.dp), color = MaterialTheme.colorScheme.onTertiaryContainer) }
                }

                // Full poem content — serif, large, generous line height
                Text(text = poem.content, fontSize = 20.sp, fontStyle = FontStyle.Italic,
                    lineHeight = 38.sp, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 16.dp))

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Author row
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(text = poem.authorName.firstOrNull()?.uppercase() ?: "K", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer) } }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = poem.authorName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(text = stringResource(R.string.poet_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (!isAuthor) {
                        OutlinedButton(onClick = { followVm.toggle(user.userId, authorId) }, shape = RoundedCornerShape(20.dp), enabled = !followState.isLoading, modifier = Modifier.height(32.dp)) {
                            Text(text = if (followState.isFollowing) "Following" else "Follow", fontSize = 11.sp) } }
                }

                Spacer(Modifier.height(20.dp))

                // Actions row — Like + Tip
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Like button
                    OutlinedButton(onClick = { vm.toggleLike(user.userId, poemId) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), enabled = true) {
                        Icon(if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                            tint = if (state.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = poem.likesCount.toString())
                    }

                    // Tip button — only show if not the author
                    if (!isAuthor) {
                        Button(onClick = { vm.openTipDialog() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), enabled = true,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                            Text(text = stringResource(R.string.tip_poet))
                            if (poem.tipsCount > 0) { Spacer(Modifier.width(4.dp)); Text(text = "·" + poem.tipsCount, fontSize = 11.sp) }
                        }
                    } else {
                        // Author sees their tip earnings
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center) {
                                Text(text = stringResource(R.string.tips_earned, poem.totalTipsCoins), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                        }
                    }
                }

                // Coin balance info card
                if (!isAuthor) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.balance_coins, user.coinBalance), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
                            if (user.coinBalance < MvpConfig.POEM_TIP_MIN) {
                                TextButton(onClick = onBuyCoins) { Text(text = stringResource(R.string.buy_coins), fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
    }

    // Tip dialog
    if (state.showTipDialog) {
        val poem = state.poem
        if (poem != null) {
            AlertDialog(
                onDismissRequest = { vm.closeTipDialog() },
                title = { Text(text = stringResource(R.string.tip_poet) + " " + poem.authorName) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.tip_message), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(text = state.selectedTip.toString() + " coin" + (if (state.selectedTip > 1) "s" else ""), fontSize = 28.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.tertiary)
                        Slider(value = state.selectedTip.toFloat(), onValueChange = { vm.onTipChange(it.toInt()) },
                            valueRange = MvpConfig.POEM_TIP_MIN.toFloat()..MvpConfig.POEM_TIP_MAX.toFloat(), steps = MvpConfig.POEM_TIP_MAX - MvpConfig.POEM_TIP_MIN - 1,
                            modifier = Modifier.padding(horizontal = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = MvpConfig.POEM_TIP_MIN.toString() + " min", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = MvpConfig.POEM_TIP_MAX.toString() + " max", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(text = stringResource(R.string.balance_coins, user.coinBalance), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (user.coinBalance < state.selectedTip) {
                            Text(text = stringResource(R.string.not_enough_coins), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { vm.sendTip(user.userId, poem.authorId) },
                        enabled = user.coinBalance >= state.selectedTip,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Text(text = stringResource(R.string.send_tip, state.selectedTip) + if (state.selectedTip > 1) "s" else "")
                    }
                },
                dismissButton = { TextButton(onClick = { vm.closeTipDialog() }) { Text(text = stringResource(R.string.cancel)) } }
            )
        }
    }
}

// ── Library ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(userId: String, onStoryClick: (String) -> Unit, onBack: () -> Unit,
                  onWriteClick: () -> Unit = {}, onProfileClick: () -> Unit = {},
                  onPoemsClick: () -> Unit = {}, vm: LibraryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(userId) { vm.load(userId) }
    Scaffold(topBar = { TopAppBar(title = { Text(text = stringResource(R.string.library_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) },
        bottomBar = { KathakarBottomNav(3, onRead = onBack, onWrite = onWriteClick, onPoems = onPoemsClick, onLibrary = {}, onProfile = onProfileClick) }
    ) { p ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Continue Reading section
            if (state.progress.isNotEmpty()) {
                item {
                    Text("Continue Reading", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                }
                items(state.progress.take(3)) { progress ->
                    ContinueReadingCard(progress = progress,
                        onClick = { onStoryClick(progress.storyId) })
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
            }
            // Bookmarked stories
            if (state.entries.isEmpty() && state.progress.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Favorite, null, modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                            Spacer(Modifier.height(16.dp))
                            Text(text = stringResource(R.string.library_empty), fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = stringResource(R.string.library_empty_sub), fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (state.entries.isNotEmpty()) {
                item {
                    Text("Bookmarked", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                }
                items(state.entries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onStoryClick(entry.storyId) },
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(48.dp, 64.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Edit, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = entry.storyTitle, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = "by ${entry.authorName}", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Ch. ${entry.lastEpisodeRead} of ${entry.totalEpisodes}",
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Profile ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(user: User, onSignOut: () -> Unit, onBuyCoins: () -> Unit, onSubscribe: () -> Unit,
                  onBack: () -> Unit, onAdminDashboard: () -> Unit,
                  onWriteClick: () -> Unit = {}, onLibraryClick: () -> Unit = {},
                  onPoemsClick: () -> Unit = {}, onNotifications: () -> Unit = {},
                  onSettings: () -> Unit = {}, vm: ProfileViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(user.userId) { vm.load(user.userId) }
    Scaffold(topBar = { TopAppBar(title = { Text(text = stringResource(R.string.profile_title)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { TextButton(onClick = onSignOut) { Text(text = stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error) } }) },
        bottomBar = { KathakarBottomNav(4, onRead = onBack, onWrite = onWriteClick, onPoems = onPoemsClick, onLibrary = onLibraryClick, onProfile = {}) }
    ) { p ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50), modifier = Modifier.size(60.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(text = user.initials, fontWeight = FontWeight.Medium, fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer) } }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(text = user.name, fontWeight = FontWeight.Medium, fontSize = 17.sp)
                        Text(text = user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val (roleBg, roleColor) = when (user.role) {
                            UserRole.ADMIN  -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                            UserRole.WRITER -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                            UserRole.READER -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer }
                        Surface(color = roleBg, shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Text(text = user.role.name.lowercase(), fontSize = 11.sp, modifier = Modifier.padding(6.dp, 2.dp), color = roleColor) }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(stringResource(R.string.stories_label), user.storiesCount.toString(), Modifier.weight(1f))
                    StatBox(stringResource(R.string.poems_label),   user.poemsCount.toString(),  Modifier.weight(1f))
                    StatBox(stringResource(R.string.followers_label), user.followersCount.toString(), Modifier.weight(1f))
                    StatBox(stringResource(R.string.coins_balance_label),   user.coinBalance.toString(), Modifier.weight(1f))
                }
            }
            if (user.isAdmin) { item { Button(onClick = onAdminDashboard, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                Text(text = stringResource(R.string.admin_dashboard), fontWeight = FontWeight.Medium) } } }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(R.string.coin_balance_title), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = user.coinBalance.toString() + " coins", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        if (user.totalCoinsEarned > 0) Text(text = stringResource(R.string.total_earned, user.totalCoinsEarned), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onBuyCoins, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text(text = stringResource(R.string.buy_coins)) }
                            OutlinedButton(onClick = onSubscribe, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text(text = stringResource(R.string.subscribe)) } }
                        Text(text = "Preview build - payments not enabled. " + MvpConfig.FREE_COINS_ON_SIGNUP + " coins on signup.",
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (state.coinHistory.isNotEmpty()) {
                item { Text(text = stringResource(R.string.coin_history), fontWeight = FontWeight.Medium, fontSize = 14.sp) }
                items(state.coinHistory) { txn ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = txn.note.ifEmpty { txn.type.name }, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = (if (txn.coinsAmount < 0) "" else "+") + txn.coinsAmount.toString() + " coins",
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = if (txn.coinsAmount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary) }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer) }
    }
}

// ── Admin Dashboard ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onBack: () -> Unit, vm: AdminViewModel = hiltViewModel()) {
    val state    by vm.state.collectAsState()
    val snackbar  = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.admin_dashboard)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.errorContainer)) }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Users",   state.stats.totalUsers.toString(),           Modifier.weight(1f))
                StatBox(stringResource(R.string.stories_label), state.stats.totalStories.toString(),         Modifier.weight(1f))
                StatBox("Poems",   state.stats.totalPoems.toString(),           Modifier.weight(1f))
                StatBox("Coins",   state.stats.totalCoinsCirculated.toString(), Modifier.weight(1f))
            }
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(selected = state.selectedTab == 0, onClick = { vm.onTabChange(0) }, text = { Text(text = "Users (" + state.users.size + ")") })
                Tab(selected = state.selectedTab == 1, onClick = { vm.onTabChange(1) }, text = { Text(text = "Stories (" + state.stories.size + ")") })
            }
            if (state.isLoading) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            else if (state.selectedTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.users, key = { it.userId }) { user -> AdminUserCard(user = user, onRoleChange = { vm.updateRole(user.userId, it) }, onToggleBan = { vm.toggleBan(user.userId, user.isBanned) }) }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.stories, key = { it.storyId }) { story -> AdminStoryCard(story = story, onSuspend = { vm.suspendStory(story.storyId) }, onRestore = { vm.restoreStory(story.storyId) }, onDelete = { vm.deleteStory(story.storyId) }) }
                }
            }
        }
    }
}

@Composable
private fun AdminUserCard(user: User, onRoleChange: (UserRole) -> Unit, onToggleBan: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(text = user.initials, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer) } }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = user.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = user.email, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                val (bg, fg) = when (user.role) {
                    UserRole.ADMIN  -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    UserRole.WRITER -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    UserRole.READER -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer }
                Surface(color = bg, shape = RoundedCornerShape(6.dp)) { Text(text = user.role.name.lowercase(), fontSize = 10.sp, modifier = Modifier.padding(4.dp, 2.dp), color = fg) }
            }
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = user.storiesCount.toString() + " stories", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = user.poemsCount.toString() + " poems", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = user.coinBalance.toString() + " coins", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                if (user.isBanned) Text(text = "BANNED", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) }
            if (user.role != UserRole.ADMIN) {
                TextButton(onClick = { expanded = !expanded }) { Text(text = if (expanded) "Hide actions" else "Manage", fontSize = 11.sp) }
                if (expanded) { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (user.role != UserRole.WRITER) OutlinedButton(onClick = { onRoleChange(UserRole.WRITER) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(text = "Writer", fontSize = 10.sp) }
                    if (user.role != UserRole.READER) OutlinedButton(onClick = { onRoleChange(UserRole.READER) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(text = "Reader", fontSize = 10.sp) }
                    Button(onClick = onToggleBan, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (user.isBanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)) {
                        Text(text = if (user.isBanned) "Unban" else "Ban", fontSize = 10.sp) } } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminStoryCard(story: Story, onSuspend: () -> Unit, onRestore: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = story.title, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "by " + story.authorName + " - " + story.totalEpisodes + " eps", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                val (bg, fg) = when (story.status) {
                    "PUBLISHED" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    "SUSPENDED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    else        -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer }
                Surface(color = bg, shape = RoundedCornerShape(6.dp)) { Text(text = story.status.lowercase(), fontSize = 10.sp, modifier = Modifier.padding(4.dp, 2.dp), color = fg) }
            }
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (story.status == "PUBLISHED") OutlinedButton(onClick = onSuspend, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(text = "Suspend", fontSize = 10.sp) }
                else if (story.status == "SUSPENDED") OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(text = "Restore", fontSize = 10.sp) }
                Button(onClick = { showDelete = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(text = stringResource(R.string.delete), fontSize = 10.sp) }
            }
        }
    }
    if (showDelete) {
        AlertDialog(onDismissRequest = { showDelete = false }, title = { Text(text = stringResource(R.string.delete_chapter_title)) }, text = { Text(text = story.title + " will be permanently deleted.") },
            confirmButton = { Button(onClick = { showDelete = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(text = stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(text = stringResource(R.string.cancel)) } })
    }
}
