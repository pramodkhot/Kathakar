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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
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

// ── Bottom nav shared across tabs ─────────────────────────────────────────────
@Composable
fun KathakarBottomNav(activeTab: Int, onRead: () -> Unit, onWrite: () -> Unit, onLibrary: () -> Unit, onProfile: () -> Unit) {
    NavigationBar {
        NavigationBarItem(selected = activeTab == 0, onClick = onRead,   icon = { Icon(Icons.Default.Home,   null) }, label = { Text(text = "Read") })
        NavigationBarItem(selected = activeTab == 1, onClick = onWrite,  icon = { Icon(Icons.Default.Edit,   null) }, label = { Text(text = "Write") })
        NavigationBarItem(selected = activeTab == 2, onClick = onLibrary,icon = { Icon(Icons.Default.Favorite,null) }, label = { Text(text = "Library") })
        NavigationBarItem(selected = activeTab == 3, onClick = onProfile,icon = { Icon(Icons.Default.Person, null) }, label = { Text(text = "Profile") })
    }
}

// ── Coming soon stub ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(title: String, reason: String, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(text = title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { p ->
        Box(modifier = Modifier.fillMaxSize().padding(p).padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(text = reason, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onBack) { Text(text = "Go back") }
            }
        }
    }
}

// ── Login ─────────────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(viewModel: AuthViewModel, onSuccess: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val gso = remember { GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(ctx.getString(R.string.default_web_client_id)).requestEmail().build() }
    val googleClient = remember { GoogleSignIn.getClient(ctx, gso) }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        try { viewModel.signInWithGoogle(GoogleSignIn.getSignedInAccountFromIntent(r.data).getResult(ApiException::class.java)) } catch (_: ApiException) { }
    }
    LaunchedEffect(state.isAuthenticated) { if (state.isAuthenticated) onSuccess() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "कथाकार", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = "Kathakar", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "Stories without boundaries", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))
        AnimatedVisibility(isRegister) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(text = "Full name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp), singleLine = true)
        }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(text = "Email") },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp), singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(text = "Password") },
            modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(14.dp), singleLine = true)
        AnimatedVisibility(state.error != null) {
            Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(text = state.error ?: "", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
            }
        }
        AnimatedVisibility(isRegister) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(text = "You will get " + MvpConfig.FREE_COINS_ON_SIGNUP + " free coins on signup!",
                    modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { viewModel.clearError()
            if (isRegister) viewModel.register(name, email, password) else viewModel.signInWithEmail(email, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isLoading) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(text = if (isRegister) "Create Account" else "Sign In", fontWeight = FontWeight.Medium)
        }
        TextButton(onClick = { isRegister = !isRegister; viewModel.clearError() }) {
            Text(text = if (isRegister) "Already have an account? Sign in" else "New here? Create account")
        }
        // Google login commented out — enable after SHA-1 setup
        // HorizontalDivider(Modifier.padding(vertical = 16.dp))
        // OutlinedButton(onClick = { googleLauncher.launch(googleClient.signInIntent) },
        //     modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
        //     Text(text = "Continue with Google", fontWeight = FontWeight.Medium)
        // }
        Spacer(Modifier.height(40.dp))
    }
}

// ── Home ──────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(user: User, onStoryClick: (String) -> Unit, onWriteClick: () -> Unit,
               onLibraryClick: () -> Unit, onProfileClick: () -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "कथाकार", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            actions = {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                    Text(text = "Coins: " + user.coinBalance, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onProfileClick) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            Text(text = user.initials, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }) },
        bottomBar = { KathakarBottomNav(0, onRead = {}, onWrite = onWriteClick, onLibrary = onLibraryClick, onProfile = onProfileClick) }
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                OutlinedTextField(value = state.searchQuery, onValueChange = vm::onSearch,
                    placeholder = { Text(text = "Search stories, authors...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true, shape = RoundedCornerShape(24.dp))
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedCategory == null, onClick = { vm.onCategory(null) }, label = { Text(text = "All") }) }
                    items(vm.categories) { cat -> FilterChip(selected = state.selectedCategory == cat, onClick = { vm.onCategory(cat) }, label = { Text(text = cat) }) }
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedLanguage == null, onClick = { vm.onLanguage(null) }, label = { Text(text = "All Languages") }) }
                    items(vm.languages) { (code, name) -> FilterChip(selected = state.selectedLanguage == code, onClick = { vm.onLanguage(code) }, label = { Text(text = name) }) }
                }
            }
            if (state.isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (state.stories.isEmpty() && state.error == null) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No stories yet", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Be the first to write a story!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onWriteClick) { Text(text = "Write a story") }
                    }
                } }
            } else {
                state.error?.let { err ->
                    item { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                            TextButton(onClick = { vm.refresh() }) { Text(text = "Retry") }
                        }
                    } }
                }
                items(state.stories, key = { it.storyId }) { story ->
                    StoryCard(story = story, onClick = { onStoryClick(story.storyId) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
                }
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
fun StoryDetailScreen(storyId: String, user: User, onBack: () -> Unit, onReadEpisode: (String) -> Unit,
                      onBuyCoins: () -> Unit, vm: StoryViewModel = hiltViewModel(), followVm: FollowViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val followState by followVm.state.collectAsState()
    LaunchedEffect(storyId) { vm.load(storyId, user.userId) }
    LaunchedEffect(state.story?.authorId) { state.story?.authorId?.let { if (it != user.userId) followVm.check(user.userId, it) } }
    LaunchedEffect(state.justUnlockedId) { state.justUnlockedId?.let { onReadEpisode(it); vm.clearJustUnlocked() } }
    Scaffold(topBar = { TopAppBar(title = { Text(text = state.story?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { IconButton(onClick = { state.story?.let { vm.toggleBookmark(user.userId, it) } }) {
            Icon(imageVector = if (state.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null,
                tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        } }) }) { p ->
        if (state.isLoading) { Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = state.story?.title ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "by " + (state.story?.authorName ?: ""), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (state.story?.authorId != user.userId) {
                            val isFollowing = followState.isFollowing
                            OutlinedButton(onClick = { state.story?.authorId?.let { followVm.toggle(user.userId, it) } },
                                shape = RoundedCornerShape(20.dp), enabled = !followState.isLoading, modifier = Modifier.height(32.dp)) {
                                Text(text = if (isFollowing) "Following" else "Follow", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(text = state.story?.description ?: "", fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
            item {
                val isAuthor = user.userId == (state.story?.authorId ?: "")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        if (isAuthor) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (isAuthor) {
                            Text(text = "Your story - all chapters free to read",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        } else {
                            Text(text = "Balance: " + user.coinBalance + " coins",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = "Ch.1 free, others cost " + MvpConfig.EPISODE_UNLOCK_COST + " coins",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            state.error?.let { err ->
                item { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = err, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                } }
            }
            item { Text(text = "Episodes", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
            items(state.episodes, key = { it.episodeId }) { ep ->
                // Author reads all their own episodes for free
                val isAuthor = user.userId == (state.story?.authorId ?: "")
                val isUnlocked = isAuthor || ep.isFree || state.unlockedIds.contains(ep.episodeId)
                EpisodeRow(
                    episode    = ep,
                    isUnlocked = isUnlocked,
                    isUnlocking = state.unlockingId == ep.episodeId,
                    isAuthor   = isAuthor,
                    userCoins  = user.coinBalance,
                    onTap      = {
                        if (isUnlocked) onReadEpisode(ep.episodeId)
                        else vm.unlock(ep, user)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeRow(episode: Episode, isUnlocked: Boolean, isUnlocking: Boolean,
                       isAuthor: Boolean, userCoins: Int, onTap: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Card(onClick = { if (isUnlocked) onTap() else showDialog = true },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = episode.chapterNumber.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = episode.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = episode.wordCount.toString() + " words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isUnlocking) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else if (isAuthor) {
                // Author badge
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                    Text(text = "Yours", fontSize = 10.sp, modifier = Modifier.padding(5.dp, 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            } else if (isUnlocked) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                if (episode.isFree) Text(text = "Free", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
            } else {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(text = episode.unlockCostCoins.toString() + " coins", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false },
            title = { Text(text = "Unlock episode?") },
            text = { Column {
                Text(text = episode.title + " costs " + episode.unlockCostCoins + " coins.")
                Text(text = "Your balance: " + userCoins + " coins", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                if (userCoins < episode.unlockCostCoins) Text(text = "Not enough coins!", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            } },
            confirmButton = { Button(onClick = { showDialog = false; if (userCoins >= episode.unlockCostCoins) onTap() }, enabled = userCoins >= episode.unlockCostCoins) { Text(text = "Unlock") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(text = "Cancel") } })
    }
}

// ── Episode Reader ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeReaderScreen(episodeId: String, onBack: () -> Unit, vm: ReaderViewModel = hiltViewModel()) {
    val ep by vm.episode.collectAsState()
    LaunchedEffect(episodeId) { vm.load(episodeId) }
    Scaffold(topBar = { TopAppBar(title = { Text(text = ep?.title ?: "Reading...", maxLines = 1) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { p ->
        if (ep == null) { Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
            item {
                Text(text = "Chapter " + ep!!.chapterNumber, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(text = ep!!.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Text(text = ep!!.content, fontSize = 16.sp, lineHeight = 28.sp)
            }
        }
    }
}

// ── Write ─────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(user: User, onCreateStory: () -> Unit, onCreateEpisode: (String, Int) -> Unit,
                onAiClick: () -> Unit, onBack: () -> Unit, onReadStory: (String) -> Unit = {},
                onLibraryClick: () -> Unit = {}, onProfileClick: () -> Unit = {},
                vm: WriterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(0) }
    LaunchedEffect(user.userId) { vm.loadMyStories(user.userId) }
    Scaffold(topBar = { TopAppBar(title = { Text(text = "Write") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) },
        bottomBar = { KathakarBottomNav(1, onRead = onBack, onWrite = {}, onLibrary = onLibraryClick, onProfile = onProfileClick) }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(text = "My Stories") })
                Tab(selected = tab == 1, onClick = { tab = 1; onAiClick() }, text = { Text(text = "AI Assist (Soon)") })
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Button(onClick = onCreateStory, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(text = "New Story") } }
                if (state.isLoading) { item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } } }
                items(state.myStories, key = { it.storyId }) { story ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = story.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Surface(color = if (story.status == "PUBLISHED") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                                    Text(text = story.status.lowercase(), fontSize = 11.sp, modifier = Modifier.padding(7.dp, 3.dp))
                                }
                            }
                            Text(text = story.totalEpisodes.toString() + " episodes - " + story.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            Spacer(Modifier.height(10.dp))
                            // Two action buttons: Read (free for author) + Add Chapter
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onReadStory(story.storyId) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = "Read")
                                }
                                Button(
                                    onClick = { onCreateEpisode(story.storyId, story.totalEpisodes + 1) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = "Ch. " + (story.totalEpisodes + 1))
                                }
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
    Scaffold(topBar = { TopAppBar(title = { Text(text = "New Story") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { p ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = state.storyTitle, onValueChange = vm::onTitleChange, label = { Text(text = "Story title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = state.storyDesc, onValueChange = vm::onDescChange, label = { Text(text = "Short description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 3)
            var catExp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExp, onExpandedChange = { catExp = it }) {
                OutlinedTextField(value = state.storyCategory.ifEmpty { "Select category" }, onValueChange = {}, readOnly = true, label = { Text(text = "Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExp) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = catExp, onDismissRequest = { catExp = false }) {
                    KathakarMeta.CATEGORIES.forEach { cat -> DropdownMenuItem(text = { Text(text = cat) }, onClick = { vm.onCategoryChange(cat); catExp = false }) }
                }
            }
            var langExp by remember { mutableStateOf(false) }
            val selectedLang = KathakarMeta.LANGUAGES.find { it.first == state.storyLanguage }?.second ?: "English"
            ExposedDropdownMenuBox(expanded = langExp, onExpandedChange = { langExp = it }) {
                OutlinedTextField(value = selectedLang, onValueChange = {}, readOnly = true, label = { Text(text = "Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExp) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = langExp, onDismissRequest = { langExp = false }) {
                    KathakarMeta.LANGUAGES.forEach { (code, name) -> DropdownMenuItem(text = { Text(text = name) }, onClick = { vm.onLanguageChange(code); langExp = false }) }
                }
            }
            state.error?.let { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) { Text(text = it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            Button(onClick = { vm.saveStory(user.userId, user.name) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isSaving) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(text = "Create Story and Write Chapter 1", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Create Episode ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEpisodeScreen(storyId: String, chapterNumber: Int, authorId: String, onDone: () -> Unit, onBack: () -> Unit, vm: WriterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.savedEpisodeId) { if (state.savedEpisodeId != null) { onDone(); vm.resetSaved() } }
    Scaffold(topBar = { TopAppBar(title = { Text(text = "Chapter " + chapterNumber) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { Text(text = state.wordCount.toString() + " words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp)) }) }) { p ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = state.epTitle, onValueChange = vm::onEpTitleChange, label = { Text(text = "Chapter title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            if (chapterNumber == 1) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                Text(text = "Chapter 1 is always free for readers", modifier = Modifier.padding(10.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            OutlinedTextField(value = state.epContent, onValueChange = vm::onEpContentChange, label = { Text(text = "Write your story here...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp), shape = RoundedCornerShape(12.dp), minLines = 12)
            state.error?.let { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) { Text(text = it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.saveEpisode(storyId, authorId, chapterNumber, false) }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp), enabled = !state.isSaving) { Text(text = "Save Draft") }
                Button(onClick = { vm.saveEpisode(storyId, authorId, chapterNumber, true) }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp), enabled = !state.isSaving && state.epContent.isNotBlank()) {
                    if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(text = "Publish", fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Library ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(userId: String, onStoryClick: (String) -> Unit, onBack: () -> Unit,
                  onWriteClick: () -> Unit = {}, onProfileClick: () -> Unit = {},
                  vm: LibraryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(userId) { vm.load(userId) }
    Scaffold(topBar = { TopAppBar(title = { Text(text = "Library") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) },
        bottomBar = { KathakarBottomNav(2, onRead = onBack, onWrite = onWriteClick, onLibrary = {}, onProfile = onProfileClick) }
    ) { p ->
        if (state.isLoading) { Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        if (state.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Your library is empty", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Bookmark stories to save them here", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.entries, key = { it.storyId }) { entry ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onStoryClick(entry.storyId) }, shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = entry.storyCoverUrl.ifEmpty { null }, contentDescription = entry.storyTitle,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = entry.storyTitle, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = "by " + entry.authorName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            if (entry.isBookmarked) Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Profile ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(user: User, onSignOut: () -> Unit, onBuyCoins: () -> Unit, onSubscribe: () -> Unit,
                  onBack: () -> Unit, onAdminDashboard: () -> Unit,
                  onWriteClick: () -> Unit = {}, onLibraryClick: () -> Unit = {},
                  vm: ProfileViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(user.userId) { vm.load(user.userId) }
    Scaffold(topBar = { TopAppBar(title = { Text(text = "Profile") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { TextButton(onClick = onSignOut) { Text(text = "Sign out", color = MaterialTheme.colorScheme.error) } }) },
        bottomBar = { KathakarBottomNav(3, onRead = onBack, onWrite = onWriteClick, onLibrary = onLibraryClick, onProfile = {}) }
    ) { p ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50), modifier = Modifier.size(60.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = user.initials, fontWeight = FontWeight.Medium, fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(text = user.name, fontWeight = FontWeight.Medium, fontSize = 17.sp)
                        Text(text = user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val (roleBg, roleColor) = when (user.role) {
                            UserRole.ADMIN  -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                            UserRole.WRITER -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                            UserRole.READER -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                        }
                        Surface(color = roleBg, shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Text(text = user.role.name.lowercase(), fontSize = 11.sp, modifier = Modifier.padding(6.dp, 2.dp), color = roleColor)
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user.isWriter) StatBox(label = "Stories", value = user.storiesCount.toString(), modifier = Modifier.weight(1f))
                    StatBox(label = "Followers", value = user.followersCount.toString(), modifier = Modifier.weight(1f))
                    StatBox(label = "Following", value = user.followingCount.toString(), modifier = Modifier.weight(1f))
                    StatBox(label = "Coins", value = user.coinBalance.toString(), modifier = Modifier.weight(1f))
                }
            }
            if (user.isAdmin) {
                item { Button(onClick = onAdminDashboard, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                    Text(text = "Admin Dashboard", fontWeight = FontWeight.Medium)
                } }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Coin Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = user.coinBalance.toString() + " coins", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        if (user.totalCoinsEarned > 0) Text(text = "Total earned: " + user.totalCoinsEarned + " coins", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onBuyCoins, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text(text = "Buy Coins") }
                            OutlinedButton(onClick = onSubscribe, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text(text = "Subscribe") }
                        }
                        Text(text = "Preview build - payments not enabled. " + MvpConfig.FREE_COINS_ON_SIGNUP + " coins on signup.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (state.coinHistory.isNotEmpty()) {
                item { Text(text = "Coin history", fontWeight = FontWeight.Medium, fontSize = 14.sp) }
                items(state.coinHistory) { txn ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = txn.note.ifEmpty { txn.type.name }, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = (if (txn.coinsAmount < 0) "" else "+") + txn.coinsAmount + " coins", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = if (txn.coinsAmount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                    }
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
            Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

// ── Admin Dashboard ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onBack: () -> Unit, vm: AdminViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(text = "Admin Dashboard") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.errorContainer)) }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox(label = "Users",   value = state.stats.totalUsers.toString(),           modifier = Modifier.weight(1f))
                StatBox(label = "Stories", value = state.stats.totalStories.toString(),          modifier = Modifier.weight(1f))
                StatBox(label = "Coins",   value = state.stats.totalCoinsCirculated.toString(), modifier = Modifier.weight(1f))
            }
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(selected = state.selectedTab == 0, onClick = { vm.onTabChange(0) }, text = { Text(text = "Users (" + state.users.size + ")") })
                Tab(selected = state.selectedTab == 1, onClick = { vm.onTabChange(1) }, text = { Text(text = "Stories (" + state.stories.size + ")") })
            }
            if (state.isLoading) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            else if (state.selectedTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.users, key = { it.userId }) { user ->
                        AdminUserCard(user = user, onRoleChange = { vm.updateRole(user.userId, it) }, onToggleBan = { vm.toggleBan(user.userId, user.isBanned) })
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.stories, key = { it.storyId }) { story ->
                        AdminStoryCard(story = story, onSuspend = { vm.suspendStory(story.storyId) }, onRestore = { vm.restoreStory(story.storyId) }, onDelete = { vm.deleteStory(story.storyId) })
                    }
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
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = user.initials, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = user.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = user.email, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val (bg, fg) = when (user.role) {
                    UserRole.ADMIN  -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    UserRole.WRITER -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    UserRole.READER -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                }
                Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                    Text(text = user.role.name.lowercase(), fontSize = 10.sp, modifier = Modifier.padding(4.dp, 2.dp), color = fg)
                }
            }
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = user.storiesCount.toString() + " stories", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = user.followersCount.toString() + " followers", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = user.coinBalance.toString() + " coins", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                if (user.isBanned) Text(text = "BANNED", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
            if (user.role != UserRole.ADMIN) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(text = if (expanded) "Hide actions" else "Manage", fontSize = 11.sp)
                }
                if (expanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (user.role != UserRole.WRITER) OutlinedButton(onClick = { onRoleChange(UserRole.WRITER) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(text = "Writer", fontSize = 10.sp) }
                        if (user.role != UserRole.READER) OutlinedButton(onClick = { onRoleChange(UserRole.READER) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(text = "Reader", fontSize = 10.sp) }
                        Button(onClick = onToggleBan, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (user.isBanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)) {
                            Text(text = if (user.isBanned) "Unban" else "Ban", fontSize = 10.sp)
                        }
                    }
                }
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
                    Text(text = "by " + story.authorName + " - " + story.totalEpisodes + " eps", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val (bg, fg) = when (story.status) {
                    "PUBLISHED" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    "SUSPENDED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    else        -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                }
                Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                    Text(text = story.status.lowercase(), fontSize = 10.sp, modifier = Modifier.padding(4.dp, 2.dp), color = fg)
                }
            }
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (story.status == "PUBLISHED") OutlinedButton(onClick = onSuspend, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(text = "Suspend", fontSize = 10.sp) }
                else if (story.status == "SUSPENDED") OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(text = "Restore", fontSize = 10.sp) }
                Button(onClick = { showDelete = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(text = "Delete", fontSize = 10.sp) }
            }
        }
    }
    if (showDelete) AlertDialog(onDismissRequest = { showDelete = false },
        title = { Text(text = "Delete story?") },
        text = { Text(text = story.title + " will be permanently deleted.") },
        confirmButton = { Button(onClick = { showDelete = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(text = "Delete permanently") } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text(text = "Cancel") } })
}
