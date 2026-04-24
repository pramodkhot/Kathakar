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
import androidx.compose.ui.*
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

// ── Coming Soon stub ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(title: String, reason: String, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(title) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { p ->
        Box(Modifier.fillMaxSize().padding(p).padding(32.dp), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🚧", fontSize = 52.sp)
                Spacer(Modifier.height(16.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(reason, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onBack) { Text("Go back") }
            }
        }
    }
}

// ── Login ─────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(viewModel: AuthViewModel, onSuccess: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(ctx.getString(R.string.default_web_client_id))
            .requestEmail().build()
    }
    val googleClient  = remember { GoogleSignIn.getClient(ctx, gso) }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        try { viewModel.signInWithGoogle(GoogleSignIn.getSignedInAccountFromIntent(r.data).getResult(ApiException::class.java)) }
        catch (_: ApiException) { }
    }
    LaunchedEffect(state.isAuthenticated) { if (state.isAuthenticated) onSuccess() }

    var email      by remember { mutableStateOf("") }
    var password   by remember { mutableStateOf("") }
    var name       by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {

        Text("कथाकार", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Kathakar", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Stories without boundaries", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))

        AnimatedVisibility(isRegister) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp), singleLine = true)
        }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp), singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp), singleLine = true)

        AnimatedVisibility(state.error != null) {
            Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(state.error ?: "", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
            }
        }
        AnimatedVisibility(isRegister) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text("🎁 You'll get ${MvpConfig.FREE_COINS_ON_SIGNUP} free coins on signup!",
                    Modifier.padding(12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { viewModel.clearError()
            if (isRegister) viewModel.register(name, email, password) else viewModel.signInWithEmail(email, password) },
            Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isLoading) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (isRegister) "Create Account" else "Sign In", fontWeight = FontWeight.Medium)
        }
        TextButton(onClick = { isRegister = !isRegister; viewModel.clearError() }) {
            Text(if (isRegister) "Already have an account? Sign in" else "New here? Create account")
        }
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        OutlinedButton(onClick = { googleLauncher.launch(googleClient.signInIntent) },
            Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Continue with Google", fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ── Home ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(user: User, onStoryClick: (String) -> Unit, onWriteClick: () -> Unit,
               onLibraryClick: () -> Unit, onProfileClick: () -> Unit,
               vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val triggerLoadMore by remember { derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= state.stories.size - 4 && state.hasMore && !state.isLoadingMore
    } }
    LaunchedEffect(triggerLoadMore) { if (triggerLoadMore) vm.loadMore() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("कथाकार", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                        Text("🪙 ${user.coinBalance}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onProfileClick) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                            Box(Modifier.size(32.dp), Alignment.Center) {
                                Text(user.initials, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Read") })
                NavigationBarItem(selected = false, onClick = onWriteClick, icon = { Icon(Icons.Default.Edit, null) }, label = { Text("Write") })
                NavigationBarItem(selected = false, onClick = onLibraryClick, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Library") })
                NavigationBarItem(selected = false, onClick = onProfileClick, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") })
            }
        }
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                OutlinedTextField(value = state.searchQuery, onValueChange = vm::onSearch,
                    placeholder = { Text("Search stories, authors...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true, shape = RoundedCornerShape(24.dp))
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedCategory == null, onClick = { vm.onCategory(null) }, label = { Text("All") }) }
                    items(vm.categories) { cat -> FilterChip(selected = state.selectedCategory == cat, onClick = { vm.onCategory(cat) }, label = { Text(cat) }) }
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = state.selectedLanguage == null, onClick = { vm.onLanguage(null) }, label = { Text("All Languages") }) }
                    items(vm.languages) { (code, name) -> FilterChip(selected = state.selectedLanguage == code, onClick = { vm.onLanguage(code) }, label = { Text(name) }) }
                }
            }
            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() } }
            } else {
                items(state.stories, key = { it.storyId }) { story ->
                    StoryCard(story, onClick = { onStoryClick(story.storyId) }, Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
                }
                if (state.isLoadingMore) {
                    item { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) } }
                }
            }
        }
    }
}

@Composable
fun StoryCard(story: Story, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(model = story.coverUrl.ifEmpty { null }, contentDescription = story.title,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(story.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("by ${story.authorName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                Text(story.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (story.category.isNotEmpty()) SuggestionChip(onClick = {}, label = { Text(story.category, fontSize = 11.sp) })
                    SuggestionChip(onClick = {}, label = { Text("${story.totalEpisodes} eps", fontSize = 11.sp) })
                }
            }
        }
    }
}

// ── Story Detail ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(storyId: String, user: User, onBack: () -> Unit,
                      onReadEpisode: (String) -> Unit, onBuyCoins: () -> Unit,
                      vm: StoryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(storyId) { vm.load(storyId, user.userId) }
    LaunchedEffect(state.justUnlockedId) { state.justUnlockedId?.let { onReadEpisode(it); vm.clearJustUnlocked() } }

    Scaffold(topBar = {
        TopAppBar(title = { Text(state.story?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            actions = {
                IconButton(onClick = { state.story?.let { vm.toggleBookmark(user.userId, it) } }) {
                    Icon(if (state.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                        tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            })
    }) { p ->
        if (state.isLoading) { Box(Modifier.fillMaxSize().padding(p), Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(state.story?.title ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("by ${state.story?.authorName}", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(state.story?.description ?: "", fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("🪙 Balance: ${user.coinBalance} coins", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Ch.1 free · others cost ${MvpConfig.EPISODE_UNLOCK_COST} coins", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Preview build — payments not yet enabled.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            state.error?.let { err ->
                item {
                    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(err, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            item { Text("Episodes", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
            items(state.episodes, key = { it.episodeId }) { ep ->
                EpisodeRow(ep, isUnlocked = ep.isFree || state.unlockedIds.contains(ep.episodeId),
                    isUnlocking = state.unlockingId == ep.episodeId, userCoins = user.coinBalance,
                    onTap = { if (ep.isFree || state.unlockedIds.contains(ep.episodeId)) onReadEpisode(ep.episodeId) else vm.unlock(ep, user) })
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, isUnlocked: Boolean, isUnlocking: Boolean, userCoins: Int, onTap: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(10.dp),
        onClick = { if (isUnlocked) onTap() else showDialog = true }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp), modifier = Modifier.size(38.dp)) {
                Box(Alignment.Center) { Text("${episode.chapterNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(episode.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${episode.wordCount} words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isUnlocking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else if (isUnlocked) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                if (episode.isFree) Text("Free", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
            } else {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text("🪙${episode.unlockCostCoins}", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false },
            title = { Text("Unlock episode?") },
            text = {
                Column {
                    Text("\"${episode.title}\" costs ${episode.unlockCostCoins} coins.")
                    Text("Your balance: $userCoins coins", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    if (userCoins < episode.unlockCostCoins) Text("Not enough coins!", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            },
            confirmButton = { Button(onClick = { showDialog = false; if (userCoins >= episode.unlockCostCoins) onTap() }, enabled = userCoins >= episode.unlockCostCoins) { Text("Unlock") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } })
    }
}

// ── Episode Reader ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeReaderScreen(episodeId: String, onBack: () -> Unit, vm: ReaderViewModel = hiltViewModel()) {
    val ep by vm.episode.collectAsState()
    LaunchedEffect(episodeId) { vm.load(episodeId) }
    Scaffold(topBar = {
        TopAppBar(title = { Text(ep?.title ?: "Reading...", maxLines = 1) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { p ->
        if (ep == null) { Box(Modifier.fillMaxSize().padding(p), Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
            item {
                Text("Chapter ${ep!!.chapterNumber}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(ep!!.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Text(ep!!.content, fontSize = 16.sp, lineHeight = 28.sp)
            }
        }
    }
}

// ── Write ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(user: User, onCreateStory: () -> Unit, onCreateEpisode: (String, Int) -> Unit,
                onAiClick: () -> Unit, onBack: () -> Unit, vm: WriterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(0) }
    LaunchedEffect(user.userId) { vm.loadMyStories(user.userId) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Write") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("My Stories") })
                Tab(selected = tab == 1, onClick = { tab = 1; onAiClick() },
                    text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AI Assist")
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(4.dp)) {
                            Text("Soon", fontSize = 9.sp, Modifier.padding(3.dp, 1.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    } })
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Button(onClick = onCreateStory, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("New Story")
                    }
                }
                if (state.isLoading) { item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } } }
                items(state.myStories, key = { it.storyId }) { story ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(story.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, Modifier.weight(1f))
                                Surface(color = if (story.status == StoryStatus.PUBLISHED) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                                    Text(story.status.name.lowercase(), fontSize = 11.sp, Modifier.padding(7.dp, 3.dp))
                                }
                            }
                            Text("${story.totalEpisodes} episodes · ${story.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { onCreateEpisode(story.storyId, story.totalEpisodes + 1) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                                Text("+ Chapter ${story.totalEpisodes + 1}")
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

    Scaffold(topBar = {
        TopAppBar(title = { Text("New Story") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { p ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = state.storyTitle, onValueChange = vm::onTitleChange, label = { Text("Story title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = state.storyDesc, onValueChange = vm::onDescChange, label = { Text("Short description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 3)

            var catExp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExp, onExpandedChange = { catExp = it }) {
                OutlinedTextField(value = state.storyCategory.ifEmpty { "Select category" }, onValueChange = {}, readOnly = true,
                    label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = catExp, onDismissRequest = { catExp = false }) {
                    KathakarMeta.CATEGORIES.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { vm.onCategoryChange(cat); catExp = false }) }
                }
            }

            var langExp by remember { mutableStateOf(false) }
            val selectedLang = KathakarMeta.LANGUAGES.find { it.first == state.storyLanguage }?.second ?: "English"
            ExposedDropdownMenuBox(expanded = langExp, onExpandedChange = { langExp = it }) {
                OutlinedTextField(value = selectedLang, onValueChange = {}, readOnly = true, label = { Text("Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = langExp, onDismissRequest = { langExp = false }) {
                    KathakarMeta.LANGUAGES.forEach { (code, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { vm.onLanguageChange(code); langExp = false }) }
                }
            }

            state.error?.let { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) { Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }

            Button(onClick = { vm.saveStory(user.userId, user.name) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isSaving) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Create Story & Write Chapter 1", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Create Episode ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEpisodeScreen(storyId: String, chapterNumber: Int, authorId: String,
                        onDone: () -> Unit, onBack: () -> Unit, vm: WriterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.savedEpisodeId) { if (state.savedEpisodeId != null) { onDone(); vm.resetSaved() } }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Chapter $chapterNumber") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            actions = { Text("${state.wordCount} words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp)) })
    }) { p ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = state.epTitle, onValueChange = vm::onEpTitleChange, label = { Text("Chapter title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            if (chapterNumber == 1) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                Text("Chapter 1 is always free for readers", Modifier.padding(10.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            OutlinedTextField(value = state.epContent, onValueChange = vm::onEpContentChange, label = { Text("Write your story here...") }, modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp), shape = RoundedCornerShape(12.dp), minLines = 12)
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(10.dp)) {
                Text("✨ AI writing assistant coming soon", Modifier.padding(10.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            state.error?.let { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) { Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.saveEpisode(storyId, authorId, chapterNumber, false) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp), enabled = !state.isSaving) { Text("Save Draft") }
                Button(onClick = { vm.saveEpisode(storyId, authorId, chapterNumber, true) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp), enabled = !state.isSaving && state.epContent.isNotBlank()) {
                    if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Publish", fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Library ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(userId: String, onStoryClick: (String) -> Unit, onBack: () -> Unit, vm: LibraryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(userId) { vm.load(userId) }
    Scaffold(topBar = { TopAppBar(title = { Text("Library") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { p ->
        if (state.isLoading) { Box(Modifier.fillMaxSize().padding(p), Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        if (state.entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(p), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📚", fontSize = 48.sp); Spacer(Modifier.height(12.dp))
                    Text("Your library is empty", style = MaterialTheme.typography.titleMedium)
                    Text("Bookmark stories to save them here", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.entries, key = { it.storyId }) { entry ->
                    Card(Modifier.fillMaxWidth().clickable { onStoryClick(entry.storyId) }, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(entry.storyCoverUrl.ifEmpty { null }, entry.storyTitle, Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.storyTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("by ${entry.authorName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
                  onBack: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(user.userId) { vm.load(user.userId) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Profile") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            actions = { TextButton(onClick = onSignOut) { Text("Sign out", color = MaterialTheme.colorScheme.error) } })
    }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50), modifier = Modifier.size(60.dp)) {
                        Box(Alignment.Center) { Text(user.initials, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text(user.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(user.role.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Coin Balance", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("🪙 ${user.coinBalance}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (user.totalCoinsEarned > 0) Text("Total earned: 🪙 ${user.totalCoinsEarned}", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onBuyCoins, Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Buy Coins") }
                            OutlinedButton(onClick = onSubscribe, Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Subscribe") }
                        }
                        Text("Preview build — payments not enabled. ${MvpConfig.FREE_COINS_ON_SIGNUP} coins given on signup.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (state.coinHistory.isNotEmpty()) {
                item { Text("Coin History", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                items(state.coinHistory) { txn ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(txn.note.ifEmpty { txn.type.name }, fontSize = 13.sp, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${if (txn.coinsAmount < 0) "" else "+"}${txn.coinsAmount} 🪙", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            color = if (txn.coinsAmount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
