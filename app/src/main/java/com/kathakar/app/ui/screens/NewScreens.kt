package com.kathakar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kathakar.app.R
import com.kathakar.app.domain.model.*
import com.kathakar.app.viewmodel.*

// ═══════════════════════════════════════════════════════════════════════════════
// AUTHOR PROFILE SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorProfileScreen(
    authorId: String,
    currentUserId: String,
    onBack: () -> Unit,
    onStoryClick: (String) -> Unit,
    onPoemClick: (String) -> Unit,
    vm: AuthorProfileViewModel = hiltViewModel(),
    followVm: FollowViewModel = hiltViewModel()
) {
    val state  by vm.state.collectAsState()
    val follow by followVm.state.collectAsState()

    LaunchedEffect(authorId) {
        vm.load(authorId)
        if (authorId != currentUserId) {
            followVm.check(currentUserId, authorId)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val author = state.author

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(author?.name ?: "") },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (author == null) return@Scaffold

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Header ─────────────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = author.initials, fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(text = author.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (author.bio.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(text = author.bio, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, lineHeight = 20.sp)
                    }
                    Spacer(Modifier.height(16.dp))

                    // Stats row
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        AuthorStat(label = "Stories",   value = author.storiesCount.toString())
                        AuthorStat(label = "Poems",     value = author.poemsCount.toString())
                        AuthorStat(label = "Followers", value = author.followersCount.toString())
                    }
                    Spacer(Modifier.height(16.dp))

                    // Follow button (only if not own profile)
                    if (authorId != currentUserId) {
                        Button(
                            onClick = { followVm.toggle(currentUserId, authorId) },
                            modifier = Modifier.width(160.dp),
                            colors = if (follow.isFollowing)
                                ButtonDefaults.outlinedButtonColors()
                            else ButtonDefaults.buttonColors(),
                            enabled = !follow.isLoading
                        ) {
                            Icon(if (follow.isFollowing) Icons.Default.Check
                                 else Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (follow.isFollowing) stringResource(R.string.following)
                                 else stringResource(R.string.follow))
                        }
                    }
                }
                HorizontalDivider()
            }

            // ── Tab bar ────────────────────────────────────────────────────────
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.stories_label)) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.poems_label)) })
                }
            }

            // ── Stories ────────────────────────────────────────────────────────
            if (selectedTab == 0) {
                if (state.stories.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center) {
                            Text("No published stories yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.stories) { story ->
                        AuthorStoryCard(story = story, onClick = { onStoryClick(story.storyId) })
                    }
                }
            }

            // ── Poems ──────────────────────────────────────────────────────────
            if (selectedTab == 1) {
                if (state.poems.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center) {
                            Text("No poems yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.poems) { poem ->
                        AuthorPoemCard(poem = poem, onClick = { onPoemClick(poem.poemId) })
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun AuthorStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AuthorStoryCard(story: Story, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        .clickable(onClick = onClick), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Cover placeholder
            Surface(color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp), modifier = Modifier.size(52.dp, 70.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = story.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = story.category, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "${story.totalEpisodes} eps", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (story.totalRatings > 0) {
                        Text(text = "⭐ ${story.displayRating}", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = "${story.totalReads} reads", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AuthorPoemCard(poem: Poem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        .clickable(onClick = onClick), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = poem.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = poem.content.take(80) + if (poem.content.length > 80) "…" else "",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "❤️ ${poem.likesCount}", fontSize = 11.sp)
                Text(text = poem.format, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WRITER DASHBOARD SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriterDashboardScreen(
    authorId: String,
    onBack: () -> Unit,
    onStoryClick: (String) -> Unit,
    vm: WriterDashboardViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(authorId) { vm.load(authorId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Writer Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Overview stats ─────────────────────────────────────────────────
            state.stats?.let { stats ->
                item {
                    Text("Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardStatCard(label = "Total Reads",
                            value = formatLargeNumber(stats.totalReads),
                            icon = Icons.Default.Visibility, modifier = Modifier.weight(1f))
                        DashboardStatCard(label = "Coins Earned",
                            value = stats.totalCoinsEarned.toString(),
                            icon = Icons.Default.Star, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardStatCard(label = "Followers",
                            value = stats.totalFollowers.toString(),
                            icon = Icons.Default.Person, modifier = Modifier.weight(1f))
                        DashboardStatCard(label = "Avg Rating",
                            value = if (stats.totalRatings == 0) "—"
                                else String.format("%.1f ⭐", stats.avgRating),
                            icon = Icons.Default.Star, modifier = Modifier.weight(1f))
                    }
                }

                // ── Top story ──────────────────────────────────────────────────
                if (stats.topStoryTitle.isNotBlank()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Top Story", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stats.topStoryTitle, fontWeight = FontWeight.SemiBold)
                                Text("${formatLargeNumber(stats.topStoryReads)} reads",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }

            // ── Withdrawal info (hidden until payments live) ───────────────────
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Earnings", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text("Coin withdrawal coming soon!", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Minimum payout: ${MvpConfig.MIN_WITHDRAWAL_COINS} coins",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Payment via UPI/Bank transfer", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── My stories breakdown ───────────────────────────────────────────
            if (state.stories.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("My Stories Performance", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
                items(state.stories) { story ->
                    StoryPerformanceRow(story = story,
                        onClick = { onStoryClick(story.storyId) })
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DashboardStatCard(label: String, value: String,
                               icon: androidx.compose.ui.graphics.vector.ImageVector,
                               modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(text = label, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StoryPerformanceRow(story: Story, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = story.title, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${story.totalEpisodes} chapters · ${story.status}",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = formatLargeNumber(story.totalReads), fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = "reads", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatLargeNumber(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000f)
    n >= 1_000     -> String.format("%.1fK", n / 1_000f)
    else           -> n.toString()
}

// ═══════════════════════════════════════════════════════════════════════════════
// READING SETTINGS BAR — shown inside EpisodeReaderScreen
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ReadingSettingsBar(
    fontSize: Int,
    isNightMode: Boolean,
    fontFamily: String,
    onFontSize: (Int) -> Unit,
    onNightMode: (Boolean) -> Unit,
    onFontFamily: (String) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reading Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

            // Font size
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", fontSize = 14.sp, modifier = Modifier.width(24.dp))
                Slider(value = KathakarMeta.FONT_SIZES.indexOf(fontSize).toFloat(),
                    onValueChange = { idx ->
                        onFontSize(KathakarMeta.FONT_SIZES[idx.toInt().coerceIn(0, KathakarMeta.FONT_SIZES.lastIndex)])
                    },
                    valueRange = 0f..(KathakarMeta.FONT_SIZES.size - 1).toFloat(),
                    steps = KathakarMeta.FONT_SIZES.size - 2,
                    modifier = Modifier.weight(1f))
                Text("A", fontSize = 22.sp)
            }
            Text("Font size: ${fontSize}sp", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Night mode
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isNightMode) Icons.Default.NightlightRound
                     else Icons.Default.WbSunny, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (isNightMode) "Night mode" else "Day mode",
                    modifier = Modifier.weight(1f), fontSize = 14.sp)
                Switch(checked = isNightMode, onCheckedChange = onNightMode)
            }

            // Font family
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KathakarMeta.FONT_FAMILIES.forEach { f ->
                    FilterChip(selected = fontFamily == f, onClick = { onFontFamily(f) },
                        label = { Text(f, fontSize = 12.sp) })
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STORY RATING SHEET
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun StoryRatingSheet(
    storyTitle: String,
    currentRating: Rating?,
    onSubmit: (stars: Int, review: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(currentRating?.stars ?: 0) }
    var review by remember { mutableStateOf(currentRating?.review ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this story", fontWeight = FontWeight.Medium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = storyTitle, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium)
                // Star selector
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { selectedStars = star },
                            modifier = Modifier.size(36.dp)) {
                            Icon(if (star <= selectedStars) Icons.Default.Star
                                 else Icons.Default.StarBorder, null,
                                tint = if (star <= selectedStars) Color(0xFFFFB800)
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp))
                        }
                    }
                }
                Text(text = when (selectedStars) {
                    1 -> "Poor"; 2 -> "Fair"; 3 -> "Good"; 4 -> "Very Good"; 5 -> "Excellent"
                    else -> "Tap a star"
                }, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(value = review, onValueChange = { review = it },
                    label = { Text("Write a review (optional)") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    minLines = 2, maxLines = 4)
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedStars, review) },
                enabled = selectedStars > 0) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// CHAPTER COMMENTS SHEET
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterCommentsSheet(
    comments: List<Comment>,
    commentText: String,
    isPosting: Boolean,
    currentUserId: String,
    onCommentChange: (String) -> Unit,
    onPost: () -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Comments (${comments.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            // Comment input
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = commentText, onValueChange = onCommentChange,
                    placeholder = { Text("Write a comment…") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                    maxLines = 3)
                IconButton(onClick = onPost, enabled = !isPosting && commentText.isNotBlank()) {
                    if (isPosting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            if (comments.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No comments yet. Be the first!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(comments) { comment ->
                        CommentRow(comment = comment, isOwn = comment.userId == currentUserId,
                            onDelete = { onDelete(comment.commentId) })
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CommentRow(comment: Comment, isOwn: Boolean, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Avatar
        Surface(color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = comment.userName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = comment.userName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                if (isOwn) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)) {
                        Text("Delete", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(text = comment.text, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// NOTIFICATIONS SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    userId: String,
    onBack: () -> Unit,
    onStoryClick: (String) -> Unit,
    onPoemClick: (String) -> Unit,
    vm: NotificationViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(userId) { vm.load(userId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (state.unreadCount > 0) {
                        TextButton(onClick = { vm.markAllRead(userId) }) {
                            Text("Mark all read", fontSize = 12.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("No notifications yet", fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(state.notifications) { notif ->
                NotificationRow(notif = notif, onClick = {
                    when (notif.type) {
                        NotificationType.NEW_CHAPTER,
                        NotificationType.STORY_LIKED,
                        NotificationType.COMMENT_ON_STORY -> onStoryClick(notif.actionId)
                        NotificationType.POEM_TIPPED -> onPoemClick(notif.actionId)
                        else -> { }
                    }
                })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun NotificationRow(notif: Notification, onClick: () -> Unit) {
    val bgColor = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                  else Color.Transparent
    Row(modifier = Modifier.fillMaxWidth().background(bgColor).clickable(onClick = onClick)
        .padding(16.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Icon by type
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape,
            modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(when (notif.type) {
                    NotificationType.NEW_CHAPTER    -> Icons.Default.Book
                    NotificationType.NEW_FOLLOWER   -> Icons.Default.Person
                    NotificationType.STORY_LIKED    -> Icons.Default.Favorite
                    NotificationType.POEM_TIPPED    -> Icons.Default.Star
                    NotificationType.COMMENT_ON_STORY -> Icons.Default.Comment
                    NotificationType.SYSTEM         -> Icons.Default.Info
                }, null, modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = notif.title, fontWeight = if (!notif.isRead) FontWeight.Bold
                else FontWeight.Normal, fontSize = 14.sp)
            Text(text = notif.body, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (!notif.isRead) {
            Box(modifier = Modifier.size(8.dp).background(
                MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CONTINUE READING CARD — shown in LibraryScreen / HomeScreen
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ContinueReadingCard(progress: ReadingProgress, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Continue Reading", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(text = progress.storyTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "by ${progress.authorName}", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(10.dp))
            // Progress bar
            LinearProgressIndicator(progress = { progress.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = progress.progressLabel, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(text = "${progress.progressPercent}%", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STORY TAGS ROW — shown in StoryDetailScreen and CreateStoryScreen
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun StoryTagsRow(tags: List<String>) {
    if (tags.isEmpty()) return
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(tags) { tag ->
            Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp)) {
                Text(text = "#$tag", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

// Tags selector for Create/Edit story
@Composable
fun TagsSelector(selectedTags: List<String>, onTagsChange: (List<String>) -> Unit) {
    Column {
        Text("Tags (optional)", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(KathakarMeta.POPULAR_TAGS) { tag ->
                val selected = tag in selectedTags
                FilterChip(selected = selected,
                    onClick = {
                        onTagsChange(if (selected) selectedTags - tag else selectedTags + tag)
                    },
                    label = { Text("#$tag", fontSize = 11.sp) })
            }
        }
    }
}
