package com.kathakar.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
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
// ── Reusable UserAvatar composable ───────────────────────────────────────────
// Shows profile photo if available, falls back to initials circle
// Use this EVERYWHERE a user/author avatar is needed
@Composable
fun UserAvatar(
    photoUrl: String,
    initials: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    if (photoUrl.isNotEmpty()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Profile",
            modifier = modifier.size(size).clip(RoundedCornerShape(50)),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
            modifier = modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.35).sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}


// ── Reading Challenge Widget ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingChallengeWidget(
    challenge: com.kathakar.app.domain.model.ReadingChallenge?,
    onSetGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (challenge == null) return
    Card(modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📖 Reading Challenge",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                TextButton(onClick = onSetGoal, contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)) {
                    Text(text = "Change Goal", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            // Stats row
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = challenge.totalPagesRead.toString(),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = "Total pages", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = challenge.todayPagesRead.toString(),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = "Today", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = challenge.dailyPageGoal.toString(),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = "Daily goal", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (challenge.isGoalMet) "🎉" else challenge.remainingToday.toString(),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = if (challenge.isGoalMet) "Done!" else "Remaining",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(10.dp))
            // Progress bar
            LinearProgressIndicator(
                progress = { challenge.progressFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (challenge.isGoalMet) Color(0xFF22C55E)
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(4.dp))
            Text(text = "${challenge.progressPercent}% of daily goal",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        }
    }
}

// Goal Picker Dialog
@Composable
fun GoalPickerDialog(
    currentGoal: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val goals = listOf(10, 20, 40, 50, 100)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Reading Goal", fontWeight = FontWeight.Medium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How many pages do you want to read each day?",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { goal ->
                        val selected = goal == currentGoal
                        Surface(
                            modifier = Modifier.weight(1f).clickable { onSelect(goal) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) androidx.compose.foundation.BorderStroke(
                                2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = goal.toString(), fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "pages", fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(user: User, onStoryClick: (String) -> Unit, onWriteClick: () -> Unit,
               onLibraryClick: () -> Unit, onProfileClick: () -> Unit, onPoemsClick: () -> Unit,
               onSettingsClick: () -> Unit = {},
               vm: HomeViewModel = hiltViewModel(),
               challengeVm: ReadingChallengeViewModel = hiltViewModel()) {
    val state          by vm.state.collectAsState()
    val challengeState by challengeVm.state.collectAsState()
    val listState       = rememberLazyListState()
    LaunchedEffect(user.userId) { challengeVm.load(user.userId) }
    // Reload challenge every time this screen becomes active (user returns from reading)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                challengeVm.load(user.userId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
                    UserAvatar(photoUrl = user.photoUrl, initials = user.initials, size = 32.dp)
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
            // ── Reading Challenge Widget ─────────────────────────────────
            item {
                ReadingChallengeWidget(
                    challenge = challengeState.challenge,
                    onSetGoal = { challengeVm.openGoalPicker() },
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            // Goal picker dialog
            if (challengeState.showGoalPicker) {
                item {
                    GoalPickerDialog(
                        currentGoal = challengeState.challenge?.dailyPageGoal ?: 20,
                        onSelect    = { goal -> challengeVm.setDailyGoal(user.userId, goal) },
                        onDismiss   = { challengeVm.closeGoalPicker() }
                    )
                }
            }
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

private fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000f)
    n >= 1_000     -> String.format("%.1fK", n / 1_000f)
    else           -> n.toString()
}

@Composable
fun StoryCard(story: Story, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            // Cover image — shows photo if available, colored placeholder if not
            if (story.coverUrl.isNotEmpty()) {
                AsyncImage(model = story.coverUrl, contentDescription = story.title,
                    modifier = Modifier.size(72.dp, 96.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop)
            } else {
                Surface(color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(72.dp, 96.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = story.title.take(1).uppercase(),
                            fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = story.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "by " + story.authorName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                Text(text = story.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (story.category.isNotEmpty()) SuggestionChip(onClick = {}, label = { Text(text = story.category, fontSize = 11.sp) })
                    SuggestionChip(onClick = {}, label = { Text(text = story.totalEpisodes.toString() + " eps", fontSize = 11.sp) })
                }
                // Stats row — always shows episode count, shows reads+rating when available
                Row(modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // Reads — show 0 for new stories, formatted for popular ones
                    Text(text = "👁 ${formatCount(story.totalReads)}",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Rating — only show when at least 1 rating exists
                    if (story.totalRatings > 0) {
                        Text(text = "⭐ ${story.displayRating}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Language badge
                    val langName = KathakarMeta.LANGUAGES.find { it.first == story.language }?.second ?: ""
                    if (langName.isNotEmpty() && story.language != "en") {
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp)) {
                            Text(text = langName, fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
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

    // Rating sheet
    if (state.showRatingSheet) {
        StoryRatingSheet(
            storyTitle = state.story?.title ?: "",
            currentRating = state.userRating,
            onSubmit = { stars, review -> vm.submitRating(user.userId, storyId, stars, review) },
            onDismiss = { vm.closeRatingSheet() }
        )
    }
    Scaffold(topBar = { TopAppBar(title = { Text(text = state.story?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { IconButton(onClick = { state.story?.let { vm.toggleBookmark(user.userId, it) } }) {
            Icon(if (state.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } }) }
    ) { p ->
        if (state.isLoading) { Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                // ── Cover image banner ────────────────────────────────────
                val coverUrl = state.story?.coverUrl ?: ""
                if (coverUrl.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        AsyncImage(model = coverUrl, contentDescription = state.story?.title,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        // Dark gradient overlay at bottom for readability
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp)
                            .align(Alignment.BottomCenter)
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
                    }
                } else {
                    // Placeholder banner when no cover
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center) {
                        Text(text = state.story?.title?.take(1)?.uppercase() ?: "K",
                            fontSize = 48.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
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
                // Tags row
                val tags = state.story?.tags ?: emptyList()
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    StoryTagsRow(tags = tags)
                }
                // Stats + Rating row
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Total reads
                    if ((state.story?.totalReads ?: 0L) > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(3.dp))
                            Text(text = "${state.story?.totalReads} reads", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // Average rating
                    if ((state.story?.totalRatings ?: 0) > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(13.dp),
                                tint = Color(0xFFFFB800))
                            Spacer(Modifier.width(3.dp))
                            Text(text = "${state.story?.displayRating} (${state.story?.totalRatings})",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // Rate this story button (readers only)
                    if (user.userId != state.story?.authorId) {
                        OutlinedButton(onClick = { vm.openRatingSheet() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(13.dp),
                                tint = Color(0xFFFFB800))
                            Spacer(Modifier.width(4.dp))
                            Text(text = if (state.userRating != null) "Rated ${state.userRating?.stars}★"
                                else "Rate", fontSize = 12.sp)
                        }
                    }
                }
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
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EpisodeReaderScreen(
    episodeId: String, storyId: String, authorId: String, currentUserId: String,
    currentUser: User? = null,
    onBack: () -> Unit, onEdit: () -> Unit, onDeleted: () -> Unit,
    vm: ReaderViewModel = hiltViewModel(),
    writerVm: WriterViewModel = hiltViewModel(),
    challengeVm: ReadingChallengeViewModel = hiltViewModel()  // ← wired here
) {
    val state    by vm.state.collectAsState()
    val ep       = state.episode
    val wState   by writerVm.state.collectAsState()
    val snackbar  = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isAuthor = currentUserId == authorId

    // Load episode + restore saved page
    LaunchedEffect(episodeId) {
        vm.load(episodeId, currentUserId)
        vm.loadSavedPage(currentUserId, storyId, episodeId)  // chapter-specific page restore
    }

    // Split into pages once episode content is loaded
    LaunchedEffect(ep?.content, state.fontSize) {
        val content = ep?.content ?: return@LaunchedEffect
        if (content.isNotBlank()) vm.splitIntoPages(content, state.fontSize)
    }

    // Auto-save progress when chapter loads
    LaunchedEffect(ep) {
        val episode = ep ?: return@LaunchedEffect
        if (episode.episodeId.isNotEmpty()) vm.saveProgress(currentUserId, storyId, episode)
    }

    LaunchedEffect(wState.message) { wState.message?.let { snackbar.showSnackbar(it); writerVm.clearMessage() } }
    LaunchedEffect(wState.error)   { wState.error?.let   { snackbar.showSnackbar(it); writerVm.clearError() } }

    // Reader colors — Day = white like book page, Night = dark for comfortable reading
    val readerBg        = if (state.isNightMode) Color(0xFF1A1A1A) else Color.White
    val readerTextColor = if (state.isNightMode) Color(0xFFE0D5C5) else Color(0xFF1A1A1A)
    val readerSubColor  = if (state.isNightMode) Color(0xFFB0A898) else Color(0xFF666666)
    val topBarColor     = if (state.isNightMode) Color(0xFF2A2A2A) else Color.White
    val readerFontFamily = when (state.fontFamily) {
        "Serif" -> FontFamily.Serif
        "Mono"  -> FontFamily.Monospace
        else    -> FontFamily.Default
    }

    // Pager state — initialized to saved page
    val scope = rememberCoroutineScope()
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = state.savedPage.coerceIn(0, maxOf(0, state.pages.size - 1)),
        pageCount   = { maxOf(1, state.pages.size) }
    )

    // Sync pager → viewModel when page changes
    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        if (page != state.currentPage) {
            vm.onPageChange(page, currentUserId, storyId, ep)
            // ── Record page read for reading challenge ───────────────────
            // Only count if user is actively reading (not just loading)
            if (state.pages.isNotEmpty() && !state.isLoading) {
                challengeVm.recordPageRead(currentUserId)
            }
        }
    }

    // When pages list is built and saved page is known, jump to saved page
    LaunchedEffect(state.pages.size, state.savedPage) {
        if (state.pages.isNotEmpty() && state.savedPage > 0) {
            val targetPage = state.savedPage.coerceIn(0, state.pages.size - 1)
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    val currentPage  = if (state.pages.isEmpty()) 0 else pagerState.currentPage
    val totalPages   = maxOf(1, state.pages.size)
    val progressFrac = if (totalPages <= 1) 1f else currentPage.toFloat() / (totalPages - 1)

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbar) },
        containerColor = readerBg,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = ep?.let { "Chapter " + it.chapterNumber } ?: "Reading...",
                                maxLines = 1, fontSize = 15.sp,
                                color = if (state.isNightMode) Color(0xFFE0D5C5)
                                        else MaterialTheme.colorScheme.onSurface)
                            if (state.pages.isNotEmpty()) {
                                Text(text = "Page ${currentPage + 1} of $totalPages",
                                    fontSize = 11.sp, color = readerSubColor)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null,
                                tint = if (state.isNightMode) Color(0xFFE0D5C5)
                                       else MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor),
                    actions = {
                        if (!isAuthor) {
                            IconButton(onClick = { vm.toggleLike(currentUserId, episodeId) }) {
                                Icon(if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null,
                                    tint = if (state.isLiked) MaterialTheme.colorScheme.error
                                           else if (state.isNightMode) Color(0xFFE0D5C5)
                                           else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(onClick = { vm.toggleComments(); vm.loadComments(episodeId) }) {
                            Text("💬", fontSize = 16.sp)
                        }
                        IconButton(onClick = { vm.toggleSettingsBar() }) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp),
                                tint = if (state.showSettingsBar) MaterialTheme.colorScheme.primary
                                       else if (state.isNightMode) Color(0xFFE0D5C5)
                                       else MaterialTheme.colorScheme.onSurface)
                        }
                        if (isAuthor) {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
                // ── Reading progress bar under top bar ─────────────────────
                if (state.pages.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress            = { progressFrac },
                        modifier            = Modifier.fillMaxWidth().height(3.dp),
                        color               = MaterialTheme.colorScheme.primary,
                        trackColor          = if (state.isNightMode) Color(0xFF3A3A3A)
                                              else MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    ) { p ->
        if (ep == null) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(p).background(readerBg)) {

            // ── Settings bar ─────────────────────────────────────────────
            if (state.showSettingsBar) {
                ReadingSettingsBar(
                    fontSize     = state.fontSize,
                    isNightMode  = state.isNightMode,
                    fontFamily   = state.fontFamily,
                    onFontSize   = { vm.setFontSize(it) },
                    onNightMode  = { vm.setNightMode(it) },
                    onFontFamily = { vm.setFontFamily(it) }
                )
            }

            // ── Horizontal pager — one page per swipe ─────────────────────
            if (state.pages.isNotEmpty()) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state    = pagerState,
                    modifier = Modifier.weight(1f),
                    pageSpacing = 0.dp
                ) { pageIndex ->
                    val pageText = state.pages.getOrElse(pageIndex) { "" }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(readerBg)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Show chapter header only on first page
                        if (pageIndex == 0) {
                            Text(text = ep!!.title, fontSize = (state.fontSize + 4).sp,
                                fontWeight = FontWeight.Bold, color = readerTextColor)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${ep!!.wordCount} words", fontSize = 12.sp, color = readerSubColor)
                                if (ep!!.readTimeDisplay.isNotEmpty())
                                    Text(ep!!.readTimeDisplay, fontSize = 12.sp, color = readerSubColor)
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                        // Page content
                        Text(
                            text       = pageText,
                            fontSize   = state.fontSize.sp,
                            lineHeight = (state.fontSize * 1.75).sp,
                            fontFamily = readerFontFamily,
                            color      = readerTextColor
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }

                // ── Page navigation footer ─────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topBarColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous page
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(Icons.Default.ArrowBack, "Previous page",
                            tint = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.primary
                                   else readerSubColor)
                    }
                    // Page dots — show up to 7 dots
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        val showDots = minOf(totalPages, 7)
                        val offset  = if (totalPages <= 7) 0
                            else maxOf(0, minOf(currentPage - 3, totalPages - 7))
                        repeat(showDots) { i ->
                            val actualPage = i + offset
                            Box(modifier = Modifier
                                .size(if (actualPage == currentPage) 8.dp else 5.dp)
                                .background(
                                    if (actualPage == currentPage) MaterialTheme.colorScheme.primary
                                    else readerSubColor.copy(alpha = 0.4f),
                                    CircleShape)
                            )
                        }
                    }
                    // Next page
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < totalPages - 1) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        enabled = pagerState.currentPage < totalPages - 1
                    ) {
                        Icon(Icons.Default.ArrowForward, "Next page",
                            tint = if (pagerState.currentPage < totalPages - 1)
                                       MaterialTheme.colorScheme.primary
                                   else readerSubColor)
                    }
                }
            } else {
                // Loading pages
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Comments sheet
    if (state.showComments) {
        ChapterCommentsSheet(
            comments        = state.comments,
            commentText     = state.commentText,
            isPosting       = state.isPostingComment,
            currentUserId   = currentUserId,
            onCommentChange = { vm.onCommentChange(it) },
            onPost = { vm.postComment(currentUserId,
                currentUser?.name ?: "Reader",
                currentUser?.photoUrl ?: "",
                episodeId, storyId) },
            onDelete  = { cid -> vm.deleteComment(cid, episodeId) },
            onDismiss = { vm.toggleComments() }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text(stringResource(R.string.delete_chapter_title)) },
            text    = { Text("\"${ep?.title ?: ""}\": ${stringResource(R.string.delete)}?") },
            confirmButton = {
                Button(onClick = { showDeleteDialog = false; writerVm.deleteEpisode(episodeId, storyId) { onDeleted() } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}


// ── Write Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(user: User, onCreateStory: () -> Unit, onCreateEpisode: (String, Int) -> Unit,
                onAiClick: () -> Unit, onBack: () -> Unit, onReadStory: (String) -> Unit = {},
                onLibraryClick: () -> Unit = {}, onProfileClick: () -> Unit = {},
                onPoemsClick: () -> Unit = {}, onWriterDashboard: () -> Unit = {},
                vm: WriterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(user.userId) { vm.loadMyStories(user.userId) }
    Scaffold(
        topBar = { TopAppBar(
            title = { Text(text = stringResource(R.string.write_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) },
        bottomBar = { KathakarBottomNav(1, onRead = onBack, onWrite = {}, onPoems = onPoemsClick,
            onLibrary = onLibraryClick, onProfile = onProfileClick) }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 },
                    text = { Text(text = stringResource(R.string.my_stories)) })
                Tab(selected = tab == 1, onClick = { tab = 1; onWriterDashboard() },
                    text = { Text(text = stringResource(R.string.writer_dashboard)) })
                Tab(selected = tab == 2, onClick = { tab = 2; onAiClick() },
                    text = { Text(text = stringResource(R.string.ai_assist)) })
            }
            LazyColumn(modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Button(onClick = onCreateStory, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)) {
                        Text(text = stringResource(R.string.new_story_btn), fontWeight = FontWeight.Medium)
                    }
                }
                if (state.isLoading) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }
                items(state.myStories, key = { it.storyId }) { story ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(text = story.title, fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Surface(
                                    color = if (story.status == "PUBLISHED")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)) {
                                    Text(text = story.status.lowercase(), fontSize = 11.sp,
                                        modifier = Modifier.padding(7.dp, 3.dp))
                                }
                            }
                            Text(text = "${story.totalEpisodes} episodes - ${story.category}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp))
                            // Show reads if any
                            if (story.totalReads > 0) {
                                Text(text = "👁 ${story.totalReads} reads", fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onReadStory(story.storyId) },
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(text = stringResource(R.string.read_label), fontSize = 12.sp)
                                }
                                Button(onClick = { onCreateEpisode(story.storyId, story.totalEpisodes + 1) },
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(text = "Ch. ${story.totalEpisodes + 1}", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Edit Episode ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEpisodeScreen(episodeId: String, storyId: String,
                      onDone: () -> Unit, onBack: () -> Unit,
                      vm: WriterViewModel = hiltViewModel()) {
    val state   by vm.state.collectAsState()
    val context  = LocalContext.current
    var fileError    by remember { mutableStateOf<String?>(null) }
    var importedFrom by remember { mutableStateOf<String?>(null) }

    // Load episode content for editing
    LaunchedEffect(episodeId) {
        // Pre-fill with current content — loaded via storyRepo in a real impl
        // For now the writer types new content
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = com.kathakar.app.util.FileUtils.readFile(context, uri)
            if (result.isSuccess) {
                vm.onEpContentChange(result.text)
                importedFrom = result.fileName; fileError = null
            } else { fileError = result.error }
        }
    }

    LaunchedEffect(state.message) {
        if (state.message != null) { onDone(); vm.clearMessage() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(
        title = { Text(text = stringResource(R.string.edit_chapter)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { Text(text = "${state.wordCount} words", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)) }) }
    ) { p ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(p)
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = state.epTitle, onValueChange = vm::onEpTitleChange,
                label = { Text(text = stringResource(R.string.chapter_title_hint)) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            // Import button
            OutlinedButton(onClick = { fileLauncher.launch(arrayOf("text/plain",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = if (importedFrom != null) "Imported: $importedFrom"
                    else stringResource(R.string.import_file_btn))
            }
            fileError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            OutlinedTextField(value = state.epContent, onValueChange = vm::onEpContentChange,
                label = { Text(text = stringResource(R.string.chapter_content_hint)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                shape = RoundedCornerShape(12.dp), minLines = 12)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Button(onClick = { vm.updateEpisode(episodeId, storyId, state.epTitle, state.epContent, onDone) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), enabled = !state.isSaving) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(text = stringResource(R.string.save_chapter_btn), fontWeight = FontWeight.Medium)
            }
        }
    }
}


// ── Copyright Consent Dialog ───────────────────────────────────────────────────
// Shown once when user publishes their first story or poem
@Composable
fun CopyrightConsentDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    var originalWork  by remember { mutableStateOf(false) }
    var agreeTerms    by remember { mutableStateOf(false) }
    val canProceed = originalWork && agreeTerms

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publishing Agreement", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Before publishing, please confirm:",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Checkbox 1
                Row(verticalAlignment = Alignment.Top,
                    modifier = Modifier.clickable { originalWork = !originalWork }) {
                    Checkbox(checked = originalWork, onCheckedChange = { originalWork = it },
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(text = "This is my original work. I hold all rights to this content and it does not violate anyone's copyright.",
                        fontSize = 13.sp, lineHeight = 18.sp)
                }

                // Checkbox 2
                Row(verticalAlignment = Alignment.Top,
                    modifier = Modifier.clickable { agreeTerms = !agreeTerms }) {
                    Checkbox(checked = agreeTerms, onCheckedChange = { agreeTerms = it },
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(text = "I agree to KathaKar's Terms of Service and Content Policy. I understand that violating these terms may result in content removal.",
                        fontSize = 13.sp, lineHeight = 18.sp)
                }

                if (!canProceed) {
                    Text(text = "Please check both boxes to continue.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = onAccept, enabled = canProceed) {
                Text("I Agree & Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


// ── Create Story ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(user: User, onSaved: (String) -> Unit, onBack: () -> Unit, vm: WriterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Copyright consent — shown before first publish
    var showConsentDialog by remember { mutableStateOf(false) }
    // Check if user already gave consent (stored in shared prefs for speed)
    val prefs = remember { context.getSharedPreferences("kathakar_prefs", android.content.Context.MODE_PRIVATE) }
    val hasConsented = remember { prefs.getBoolean("copyright_consent_${user.userId}", false) }

    // Track selected image info for user feedback
    var imageFileSizeKb  by remember { mutableStateOf(0L) }
    var imageWidthPx     by remember { mutableIntStateOf(0) }
    var imageHeightPx    by remember { mutableIntStateOf(0) }
    var imageSizeWarning by remember { mutableStateOf<String?>(null) }

    // Show consent dialog when triggered
    if (showConsentDialog) {
        CopyrightConsentDialog(
            onAccept = {
                prefs.edit().putBoolean("copyright_consent_${user.userId}", true).apply()
                showConsentDialog = false
                vm.saveStory(user.userId, user.name)
            },
            onDismiss = { showConsentDialog = false }
        )
    }

    // Image picker for story cover
    val coverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { pickedUri ->
            vm.onCoverUriChange(pickedUri)
            // Read file size
            try {
                val cursor = context.contentResolver.query(pickedUri, null, null, null, null)
                cursor?.use { c ->
                    val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    c.moveToFirst()
                    if (sizeIndex >= 0) imageFileSizeKb = c.getLong(sizeIndex) / 1024
                }
                // Read image dimensions
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(pickedUri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                    imageWidthPx  = opts.outWidth
                    imageHeightPx = opts.outHeight
                }
                // Warn if image is too large
                imageSizeWarning = when {
                    imageFileSizeKb > 2048 -> "⚠️ Large file (${imageFileSizeKb}KB). Will upload but may be slow."
                    imageFileSizeKb > 5120 -> "⚠️ Very large file. Please choose a smaller image."
                    else -> null
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(state.savedStoryId) { state.savedStoryId?.let { sid ->
        // If cover selected → upload first, THEN navigate
        // If no cover → navigate immediately
        if (state.coverUri != null) {
            vm.uploadCover(sid, state.coverUri!!)
            // uploadCover updates coverUrl in state when done — navigate after
        } else {
            onSaved(sid); vm.resetSaved()
        }
    } }

    // Navigate after cover upload completes (coverUrl gets set by uploadCover)
    LaunchedEffect(state.coverUrl, state.savedStoryId) {
        if (state.savedStoryId != null && state.coverUri != null && state.coverUrl.isNotEmpty()) {
            onSaved(state.savedStoryId!!); vm.resetSaved()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(text = stringResource(R.string.new_story_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(p).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Cover photo picker ───────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth().height(160.dp).clickable { coverLauncher.launch("image/*") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.coverUri != null) {
                        AsyncImage(model = state.coverUri, contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop)
                        Box(modifier = Modifier.fillMaxSize().background(
                            Color.Black.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, null, tint = Color.White,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Change Cover", color = Color.White,
                                    fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("Add Cover Photo", fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium)
                            Text("Tap to pick from gallery", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Image info bar — shown after image is picked ─────────────
            if (state.coverUri != null) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp, 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Size and dimensions row
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            // File size
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(4.dp))
                                Text(text = if (imageFileSizeKb > 0) {
                                    if (imageFileSizeKb >= 1024) String.format("%.1f MB", imageFileSizeKb / 1024f)
                                    else "$imageFileSizeKb KB"
                                } else "Calculating...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium)
                            }
                            // Dimensions
                            if (imageWidthPx > 0 && imageHeightPx > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = "${imageWidthPx} × ${imageHeightPx} px",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            // Quality indicator
                            val quality = when {
                                imageWidthPx >= 800 && imageHeightPx >= 1000 -> "✅ Good quality"
                                imageWidthPx >= 400 -> "⚡ Acceptable"
                                imageWidthPx > 0    -> "⚠️ Low resolution"
                                else -> ""
                            }
                            if (quality.isNotEmpty()) {
                                Text(text = quality, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        // Recommended spec hint
                        Text(text = "Recommended: 800×1000px, under 500KB",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        // Size warning if file too large
                        if (imageSizeWarning != null) {
                            Text(text = imageSizeWarning!!,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

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
            // ── Tags selector ────────────────────────────────────────────
            TagsSelector(
                selectedTags = state.storyTags,
                onTagsChange = { vm.onTagsChange(it) }
            )

            state.error?.let { Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) { Text(text = it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            Button(onClick = {
                // Show consent dialog if never agreed before
                if (!hasConsented) showConsentDialog = true
                else vm.saveStory(user.userId, user.name)
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isSaving && !state.isUploadingCover) {
                if (state.isSaving || state.isUploadingCover) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (state.isUploadingCover) "Uploading cover..." else stringResource(R.string.create_story_button))
                } else Text(text = stringResource(R.string.create_story_button), fontWeight = FontWeight.Medium) }
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

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(
        title = { Text(text = stringResource(R.string.reading_chapter, chapterNumber)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { Text(text = state.wordCount.toString() + " words", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp)) }) }
    ) { p ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(p)
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(8.dp))

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

            // Mood filter chips
            item { LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.selectedMood == null, onClick = { vm.onMoodFilter(null) }, label = { Text(text = stringResource(R.string.filter_all)) }) }
                items(vm.moods) { mood -> FilterChip(selected = state.selectedMood == mood, onClick = { vm.onMoodFilter(mood) }, label = { Text(text = mood) }) }
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
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PoemDetailScreen(poemId: String, authorId: String, user: User,
                     onBack: () -> Unit, onBuyCoins: () -> Unit,
                     onAuthorClick: (String) -> Unit = {},
                     vm: PoemDetailViewModel = hiltViewModel(),
                     followVm: FollowViewModel = hiltViewModel()) {
    val state       by vm.state.collectAsState()
    val followState by followVm.state.collectAsState()
    val snackbar     = remember { SnackbarHostState() }
    val isAuthor     = user.userId == authorId

    // Reading settings — local state, same as story reader
    var isPoemNightMode  by remember { mutableStateOf(false) }
    var poemFontSize     by remember { mutableIntStateOf(20) }
    var showPoemSettings by remember { mutableStateOf(false) }

    // Colors — explicit White for day, dark for night
    val poemBg        = if (isPoemNightMode) Color(0xFF1A1A1A) else Color.White
    val poemTextColor = if (isPoemNightMode) Color(0xFFE0D5C5) else Color(0xFF1A1A1A)
    val poemSubColor  = if (isPoemNightMode) Color(0xFFB0A898) else Color(0xFF666666)
    val poemTopBar    = if (isPoemNightMode) Color(0xFF2A2A2A) else Color.White

    LaunchedEffect(poemId)   { vm.load(poemId, user.userId) }
    LaunchedEffect(authorId) { if (!isAuthor) followVm.check(user.userId, authorId) }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    LaunchedEffect(state.error)   { state.error?.let   { snackbar.showSnackbar(it); vm.clearError() } }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbar) },
        containerColor = poemBg,
        topBar = {
            TopAppBar(
                title = { Text(text = state.poem?.title ?: "", maxLines = 1,
                    overflow = TextOverflow.Ellipsis, color = poemTextColor) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = poemTextColor) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = poemTopBar),
                actions = {
                    IconButton(onClick = { showPoemSettings = !showPoemSettings }) {
                        Icon(Icons.Default.Settings, null,
                            tint = if (showPoemSettings) MaterialTheme.colorScheme.primary
                                   else poemTextColor,
                            modifier = Modifier.size(20.dp))
                    }
                }
            )
        }
    ) { p ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val poem = state.poem ?: return@Scaffold

        // Split poem into pages of 12 lines each
        val poemLines      = poem.content.split("\n")
        val linesPerPage   = 12
        val poemPages      = poemLines.chunked(linesPerPage).map { it.joinToString("\n") }
        val totalPoemPages = maxOf(1, poemPages.size)
        val poemPager      = androidx.compose.foundation.pager.rememberPagerState(
            pageCount = { totalPoemPages })
        val poemScope      = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize().padding(p).background(poemBg)) {

            // ── Settings bar ────────────────────────────────────────────
            if (showPoemSettings) {
                Surface(color = if (isPoemNightMode) Color(0xFF2A2A2A)
                               else MaterialTheme.colorScheme.surfaceVariant) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("A", fontSize = 14.sp, color = poemTextColor,
                                modifier = Modifier.width(24.dp))
                            Slider(value = poemFontSize.toFloat(),
                                onValueChange = { poemFontSize = it.toInt() },
                                valueRange = 14f..28f, steps = 6,
                                modifier = Modifier.weight(1f))
                            Text("A", fontSize = 22.sp, color = poemTextColor)
                        }
                        Text("Font size: ${poemFontSize}sp", fontSize = 11.sp,
                            color = poemSubColor)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isPoemNightMode) "🌙" else "☀️", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(if (isPoemNightMode) "Night mode" else "Day mode",
                                modifier = Modifier.weight(1f), fontSize = 14.sp,
                                color = poemTextColor)
                            Switch(checked = isPoemNightMode,
                                onCheckedChange = { isPoemNightMode = it })
                        }
                    }
                }
            }

            // ── Progress bar ─────────────────────────────────────────────
            if (totalPoemPages > 1) {
                LinearProgressIndicator(
                    progress = { if (totalPoemPages <= 1) 1f
                                 else poemPager.currentPage.toFloat() / (totalPoemPages - 1) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color    = MaterialTheme.colorScheme.primary,
                    trackColor = if (isPoemNightMode) Color(0xFF3A3A3A)
                                 else MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // ── Horizontal pager ─────────────────────────────────────────
            androidx.compose.foundation.pager.HorizontalPager(
                state    = poemPager,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { pageIdx ->
                LazyColumn(modifier = Modifier.fillMaxSize().background(poemBg),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)) {
                    item {
                        // Header chips on first page only
                        if (pageIdx == 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)) {
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)) {
                                    Text(poem.format, fontSize = 11.sp,
                                        modifier = Modifier.padding(7.dp, 3.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer) }
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp)) {
                                    Text(poem.mood, fontSize = 11.sp,
                                        modifier = Modifier.padding(7.dp, 3.dp),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer) }
                            }
                        }
                        // Page content
                        Text(text = poemPages.getOrElse(pageIdx) { "" },
                            fontSize   = poemFontSize.sp,
                            fontStyle  = FontStyle.Italic,
                            lineHeight = (poemFontSize * 1.9).sp,
                            color      = poemTextColor,
                            modifier   = Modifier.padding(vertical = 16.dp))

                        // Author + actions on last page only
                        if (pageIdx == totalPoemPages - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            // Author row — tappable to open author profile
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                .clickable { if (!isAuthor) onAuthorClick(authorId) },
                                verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(
                                    photoUrl = state.authorPhotoUrl,
                                    initials = poem.authorName.take(1).uppercase(),
                                    size = 40.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(poem.authorName, fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary) // orange = clickable hint
                                    Text(stringResource(R.string.poet_label), fontSize = 12.sp,
                                        color = poemSubColor)
                                }
                                if (!isAuthor) {
                                    OutlinedButton(onClick = {
                                        if (followState.isFollowing)
                                            followVm.toggle(user.userId, authorId)
                                        else followVm.toggle(user.userId, authorId)
                                    }, shape = RoundedCornerShape(20.dp)) {
                                        Text(if (followState.isFollowing)
                                            stringResource(R.string.following)
                                            else stringResource(R.string.follow),
                                            fontSize = 12.sp)
                                    }
                                }
                            }
                            // Like + Tip actions row
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { vm.toggleLike(user.userId, poemId) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)) {
                                    Icon(if (state.isLiked) Icons.Default.Favorite
                                         else Icons.Default.FavoriteBorder,
                                        null, modifier = Modifier.size(16.dp),
                                        tint = if (state.isLiked) MaterialTheme.colorScheme.error
                                               else MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(6.dp))
                                    Text("${poem.likesCount} ❤️",
                                        fontSize = 12.sp)
                                }
                                if (!isAuthor) {
                                    Button(onClick = { vm.openTipDialog() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)) {
                                        Text("🪙 Tip Poet",
                                            fontSize = 12.sp)
                                    }
                                }
                            }
                            // Tips earned
                            if (poem.totalTipsCoins > 0) {
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Text(text = stringResource(R.string.tips_earned, poem.totalTipsCoins),
                                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(12.dp, 8.dp))
                                }
                            }
                            // Coin balance card
                            if (!isAuthor && user.coinBalance < 5) {
                                Card(colors = CardDefaults.cardColors(
                                    MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Row(modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("Low coin balance. Buy more coins to tip poets.",
                                            fontSize = 12.sp, modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        TextButton(onClick = onBuyCoins) {
                                            Text(stringResource(R.string.buy_coins),
                                                fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Page navigation footer ───────────────────────────────────
            if (totalPoemPages > 1) {
                Row(modifier = Modifier.fillMaxWidth().background(poemTopBar)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { poemScope.launch {
                            poemPager.animateScrollToPage(poemPager.currentPage - 1) } },
                        enabled = poemPager.currentPage > 0) {
                        Icon(Icons.Default.ArrowBack, "Previous",
                            tint = if (poemPager.currentPage > 0)
                                       MaterialTheme.colorScheme.primary
                                   else poemSubColor.copy(alpha = 0.3f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Page ${poemPager.currentPage + 1} of $totalPoemPages",
                            fontSize = 13.sp, color = poemSubColor,
                            fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)) {
                            val show = minOf(totalPoemPages, 7)
                            val off  = if (totalPoemPages <= 7) 0
                                else maxOf(0, minOf(poemPager.currentPage - 3, totalPoemPages - 7))
                            repeat(show) { i ->
                                val pg = i + off
                                Box(modifier = Modifier
                                    .size(if (pg == poemPager.currentPage) 7.dp else 4.dp)
                                    .background(
                                        if (pg == poemPager.currentPage)
                                            MaterialTheme.colorScheme.primary
                                        else poemSubColor.copy(alpha = 0.4f),
                                        CircleShape))
                            }
                        }
                    }
                    IconButton(
                        onClick = { poemScope.launch {
                            poemPager.animateScrollToPage(poemPager.currentPage + 1) } },
                        enabled = poemPager.currentPage < totalPoemPages - 1) {
                        Icon(Icons.Default.ArrowForward, "Next",
                            tint = if (poemPager.currentPage < totalPoemPages - 1)
                                       MaterialTheme.colorScheme.primary
                                   else poemSubColor.copy(alpha = 0.3f))
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
                title = { Text(stringResource(R.string.tip_poet) + " " + poem.authorName) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Send coins to appreciate this poem:", fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 5).forEach { amt ->
                                OutlinedButton(onClick = { vm.sendTip(user.userId, poem.authorId) },
                                    enabled = user.coinBalance >= amt,
                                    shape = RoundedCornerShape(8.dp)) {
                                    Text("$amt 🪙", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { vm.closeTipDialog() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
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
                  onEditProfile: () -> Unit = {},
                  onCoinDetails: () -> Unit = {},
                  onWriteClick: () -> Unit = {}, onLibraryClick: () -> Unit = {},
                  onPoemsClick: () -> Unit = {}, onNotifications: () -> Unit = {},
                  onSettings: () -> Unit = {}, vm: ProfileViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(user.userId) { vm.load(user.userId) }
    Scaffold(topBar = { TopAppBar(title = { Text(text = stringResource(R.string.profile_title)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = {
            IconButton(onClick = onNotifications) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            TextButton(onClick = onSignOut) {
                Text(text = stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error)
            }
        }) },
        bottomBar = { KathakarBottomNav(4, onRead = onBack, onWrite = onWriteClick, onPoems = onPoemsClick, onLibrary = onLibraryClick, onProfile = {}) }
    ) { p ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                // ── Profile header ─────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar — photo or initials
                    UserAvatar(photoUrl = user.photoUrl, initials = user.initials, size = 72.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = user.name, fontWeight = FontWeight.Medium, fontSize = 17.sp)
                        Text(text = user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val (roleBg, roleColor) = when (user.role) {
                            UserRole.ADMIN  -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                            UserRole.WRITER -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                            UserRole.READER -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer }
                        Surface(color = roleBg, shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Text(text = user.role.name.lowercase(), fontSize = 11.sp, modifier = Modifier.padding(6.dp, 2.dp), color = roleColor) }
                    }
                    // Edit profile button
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, "Edit profile",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                // Bio section
                if (user.bio.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(user.bio, fontSize = 14.sp, lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onEditProfile, contentPadding = PaddingValues(0.dp)) {
                        Text("+ Add about yourself", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(stringResource(R.string.stories_label), user.storiesCount.toString(), Modifier.weight(1f))
                    StatBox(stringResource(R.string.poems_label),   user.poemsCount.toString(),  Modifier.weight(1f))
                    StatBox(stringResource(R.string.followers_label), user.followersCount.toString(), Modifier.weight(1f))
                    StatBox(stringResource(R.string.coins_balance_label), user.coinBalance.toString(), Modifier.weight(1f))
                }
            }
            if (user.isAdmin) { item { Button(onClick = onAdminDashboard, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                Text(text = stringResource(R.string.admin_dashboard), fontWeight = FontWeight.Medium) } } }
            // ── NEW Coin card — full tappable card linking to CoinDetailsScreen ──
            item {
                Card(modifier = Modifier.fillMaxWidth().clickable { onCoinDetails() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("🪙 Coin Balance", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Text("${user.coinBalance}", fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                                if (user.totalCoinsEarned > 0) {
                                    Text("Total earned: ${user.totalCoinsEarned}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("View all details →", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onBuyCoins, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)) {
                                Text(stringResource(R.string.buy_coins))
                            }
                            OutlinedButton(onClick = onSubscribe, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)) {
                                Text(stringResource(R.string.subscribe))
                            }
                        }
                    }
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

// ── Edit Profile Screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    user: User,
    onBack: () -> Unit,
    onSaved: (name: String, bio: String) -> Unit,
    vm: ProfileViewModel = hiltViewModel(),
    authVm: AuthViewModel
) {
    val state   = vm.state.collectAsState().value
    val context = LocalContext.current

    // Photo picker
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { vm.uploadProfilePhoto(user.userId, it) { url ->
            authVm.refreshUser(user.userId)
            onSaved(user.name, user.bio)
        } }
    }

    LaunchedEffect(state.profileSaveSuccess) {
        if (state.profileSaveSuccess) {
            authVm.refreshUser(user.userId)  // refresh UI immediately
            onSaved(state.editName, state.editBio)
        }
    }
    LaunchedEffect(Unit) { vm.openEditProfile(user) }

    Scaffold(topBar = { TopAppBar(
        title = { Text("Edit Profile") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { p ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(p).windowInsetsPadding(WindowInsets.ime).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Profile photo ──────────────────────────────────────────
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                if (state.isUploadingPhoto) {
                    Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Use UserAvatar which handles photo/initials automatically
                    UserAvatar(
                        photoUrl = user.photoUrl,
                        initials = user.initials,
                        size     = 90.dp
                    )
                }
                // Edit badge
                Surface(color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(28.dp).align(Alignment.BottomEnd)
                        .clickable { photoLauncher.launch("image/*") }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, null, tint = Color.White,
                            modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Photo action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                OutlinedButton(onClick = { photoLauncher.launch("image/*") },
                    shape = RoundedCornerShape(20.dp)) {
                    Text("Change Photo", fontSize = 13.sp)
                }
                if (user.photoUrl.isNotEmpty()) {
                    OutlinedButton(onClick = { vm.removeProfilePhoto(user.userId) {} },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Remove", fontSize = 13.sp)
                    }
                }
            }

            HorizontalDivider()

            // ── Name ───────────────────────────────────────────────────
            OutlinedTextField(
                value = state.editName,
                onValueChange = vm::onEditName,
                label = { Text("Display Name") },
                placeholder = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            // ── Bio / About me ─────────────────────────────────────────
            OutlinedTextField(
                value = state.editBio,
                onValueChange = vm::onEditBio,
                label = { Text("About Me") },
                placeholder = { Text("Tell readers about yourself...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = RoundedCornerShape(12.dp),
                minLines = 4,
                maxLines = 8,
                supportingText = { Text("${state.editBio.length}/300 characters",
                    fontSize = 11.sp) }
            )

            // Error
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            // Save button
            Button(
                onClick = { vm.saveProfile(user.userId, state.editName, state.editBio) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !state.isSavingProfile
            ) {
                if (state.isSavingProfile) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Profile", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Coin Details Screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailsScreen(
    user: User,
    onBack: () -> Unit,
    onBuyCoins: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val state = vm.state.collectAsState().value
    LaunchedEffect(user.userId) { vm.load(user.userId) }

    // Calculate stats from transaction history
    val totalSpent   = state.coinHistory.filter { it.coinsAmount < 0 }.sumOf { -it.coinsAmount }
    val totalEarned  = state.coinHistory.filter { it.coinsAmount > 0 }.sumOf { it.coinsAmount }
    val tipsSent     = state.coinHistory.filter { it.type == CoinTxnType.POEM_TIP }.sumOf { -it.coinsAmount }
    val tipsReceived = state.coinHistory.filter { it.type == CoinTxnType.POEM_TIP_RECEIVED }.sumOf { it.coinsAmount }
    val coinsFromUnlocks = state.coinHistory.filter { it.type == CoinTxnType.EPISODE_UNLOCK }.sumOf { -it.coinsAmount }

    Scaffold(topBar = { TopAppBar(
        title = { Text("Coin Details") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { p ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(p),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Balance card ────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Balance", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text("${user.coinBalance}", fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("coins", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onBuyCoins,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)) {
                            Text("Buy More Coins")
                        }
                    }
                }
            }

            // ── Stats grid ─────────────────────────────────────────────
            item {
                Text("Coin Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoinStatCard("Total Earned", "+$totalEarned",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                        Modifier.weight(1f))
                    CoinStatCard("Total Spent", "-$totalSpent",
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer,
                        Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoinStatCard("Tips Given", "-$tipsSent",
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        Modifier.weight(1f))
                    CoinStatCard("Tips Received", "+$tipsReceived",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer,
                        Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                CoinStatCard("Story Unlocks", "-$coinsFromUnlocks",
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Modifier.fillMaxWidth())
            }

            // ── Transaction history ─────────────────────────────────────
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Transaction History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }

            if (state.coinHistory.isEmpty() && !state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        Text("No transactions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(state.coinHistory, key = { it.txnId }) { txn ->
                CoinTransactionRow(txn)
            }
        }
    }
}

@Composable
private fun CoinStatCard(label: String, value: String, bg: androidx.compose.ui.graphics.Color,
                          fg: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp, 12.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = fg)
            Text(label, fontSize = 11.sp, color = fg.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun CoinTransactionRow(txn: CoinTransaction) {
    val isPositive = txn.coinsAmount > 0
    val amountColor = if (isPositive) Color(0xFF22C55E) else MaterialTheme.colorScheme.error
    val amountText  = if (isPositive) "+${txn.coinsAmount}" else "${txn.coinsAmount}"
    val icon = when (txn.type) {
        CoinTxnType.POEM_TIP_RECEIVED -> "🎁"
        CoinTxnType.POEM_TIP          -> "💝"
        CoinTxnType.EPISODE_UNLOCK    -> "🔓"
        CoinTxnType.COIN_PURCHASE     -> "🛒"
        CoinTxnType.SIGNUP_BONUS      -> "🎉"
        CoinTxnType.AUTHOR_EARNING    -> "✍️"
        CoinTxnType.WITHDRAWAL        -> "💰"
        CoinTxnType.REWARDED_AD       -> "📺"
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 22.sp, modifier = Modifier.width(36.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(txn.note.ifEmpty { txn.type.name.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() } },
                fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val date = txn.createdAt?.toDate()?.let {
                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(it)
            } ?: ""
            if (date.isNotEmpty()) {
                Text(date, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(amountText, color = amountColor,
            fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
