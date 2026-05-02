package com.kathakar.app.domain.model

import com.google.firebase.Timestamp

// ── Config ────────────────────────────────────────────────────────────────────
object MvpConfig {
    const val FREE_COINS_ON_SIGNUP    = 100
    const val EPISODE_UNLOCK_COST     = 10
    const val AUTHOR_REVENUE_PERCENT  = 60
    const val POEM_TIP_MIN            = 1
    const val POEM_TIP_MAX            = 5
    const val MIN_WITHDRAWAL_COINS    = 500   // minimum coins before payout (hidden)
    const val REWARDED_AD_COINS       = 5     // coins earned per rewarded ad (hidden)
    const val RATING_MIN              = 1
    const val RATING_MAX              = 5
    const val COMMENTS_PAGE_SIZE      = 20
    // Subscription plans (hidden — not live yet)
    const val SUB_ALL_PRICE_INR       = 99
    const val SUB_AUTHOR_PRICE_INR    = 25
    const val AI_SUB_PRICE_INR        = 299
}

// ── Enums ─────────────────────────────────────────────────────────────────────
enum class UserRole    { READER, WRITER, ADMIN }
enum class CoinTxnType {
    SIGNUP_BONUS, EPISODE_UNLOCK, AUTHOR_EARNING,
    POEM_TIP, POEM_TIP_RECEIVED,
    REWARDED_AD,          // hidden — coins from watching ad
    COIN_PURCHASE,        // hidden — real money purchase
    WITHDRAWAL            // hidden — author payout
}
enum class NotificationType {
    NEW_CHAPTER,          // Author published a new chapter
    NEW_FOLLOWER,         // Someone followed you
    STORY_LIKED,          // Someone liked your story/chapter
    POEM_TIPPED,          // Someone tipped your poem
    COMMENT_ON_STORY,     // Someone commented on your story
    SYSTEM                // System announcements
}

// ── User ──────────────────────────────────────────────────────────────────────
data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val role: UserRole = UserRole.READER,
    val coinBalance: Int = 0,
    val totalCoinsEarned: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val storiesCount: Int = 0,
    val poemsCount: Int = 0,
    val totalReadsReceived: Long = 0L,   // total reads across all stories
    val createdAt: Timestamp? = null,
    val isBanned: Boolean = false,
    val preferredLanguages: List<String> = emptyList(),
    // Subscription — hidden until payments live
    val subscriptionType: String = "none",  // none | monthly_all | monthly_author | ai
    val subscriptionExpiry: Timestamp? = null
) {
    val initials: String get() = name.split(" ").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "K" }
    val isAdmin  get() = role == UserRole.ADMIN
    val isWriter get() = true
    val hasActiveSubscription: Boolean get() {
        if (subscriptionType == "none") return false
        val expiry = subscriptionExpiry ?: return false
        return expiry.toDate().after(java.util.Date())
    }
}

// ── Follow ────────────────────────────────────────────────────────────────────
data class Follow(
    val followerId: String = "",
    val followeeId: String = "",
    val createdAt: Timestamp? = null
)

// ── Story ─────────────────────────────────────────────────────────────────────
data class Story(
    val storyId: String = "",
    val title: String = "",
    val description: String = "",
    val coverUrl: String = "",          // Firebase Storage URL
    val authorId: String = "",
    val authorName: String = "",
    val category: String = "",
    val language: String = "en",
    val tags: List<String> = emptyList(),   // user-defined tags: #romance #slowburn
    val searchTokens: List<String> = emptyList(),
    val status: String = "DRAFT",
    val totalEpisodes: Int = 0,
    val totalReads: Long = 0L,
    val avgRating: Float = 0f,
    val totalRatings: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val displayRating: String get() = if (totalRatings == 0) "—"
        else String.format("%.1f", avgRating)
    val estimatedReadMinutes: Int get() = (totalEpisodes * 8)  // ~8 min per chapter
}

// ── Episode ───────────────────────────────────────────────────────────────────
data class Episode(
    val episodeId: String = "",
    val storyId: String = "",
    val authorId: String = "",
    val chapterNumber: Int = 1,
    val title: String = "",
    val content: String = "",
    val wordCount: Int = 0,
    val readTimeMinutes: Int = 0,       // estimated read time
    val unlockCostCoins: Int = MvpConfig.EPISODE_UNLOCK_COST,
    @get:JvmName("getIsFree")
    val isFree: Boolean = false,
    val status: String = "DRAFT",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Timestamp? = null
) {
    val readTimeDisplay: String get() = if (readTimeMinutes <= 0) "" else "$readTimeMinutes min read"
}

// ── Reading Progress ──────────────────────────────────────────────────────────
// Saved per user per story — "Continue Reading" feature
data class ReadingProgress(
    val userId: String = "",
    val storyId: String = "",
    val storyTitle: String = "",
    val storyCoverUrl: String = "",
    val authorName: String = "",
    val lastEpisodeId: String = "",
    val lastChapterNumber: Int = 0,
    val lastChapterTitle: String = "",
    val totalEpisodes: Int = 0,
    val lastPageNumber: Int = 0,      // which page inside the chapter
    val updatedAt: Timestamp? = null
) {
    val progressPercent: Int get() =
        if (totalEpisodes == 0) 0
        else ((lastChapterNumber.toFloat() / totalEpisodes) * 100).toInt().coerceIn(0, 100)
    val progressLabel: String get() = "Ch. $lastChapterNumber of $totalEpisodes"
}

// ── Rating ────────────────────────────────────────────────────────────────────
data class Rating(
    val ratingId: String = "",
    val userId: String = "",
    val storyId: String = "",
    val stars: Int = 0,           // 1-5
    val review: String = "",      // optional text review
    val createdAt: Timestamp? = null
)

// ── Comment ───────────────────────────────────────────────────────────────────
data class Comment(
    val commentId: String = "",
    val episodeId: String = "",
    val storyId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val text: String = "",
    val likesCount: Int = 0,
    val createdAt: Timestamp? = null
)

// ── Episode Like ──────────────────────────────────────────────────────────────
data class EpisodeLike(
    val userId: String = "",
    val episodeId: String = "",
    val createdAt: Timestamp? = null
)

// ── Writer Stats (for Writer Dashboard) ───────────────────────────────────────
data class WriterStats(
    val authorId: String = "",
    val totalReads: Long = 0L,
    val totalCoinsEarned: Int = 0,
    val totalStories: Int = 0,
    val totalEpisodes: Int = 0,
    val totalFollowers: Int = 0,
    val totalRatings: Int = 0,
    val avgRating: Float = 0f,
    val topStoryTitle: String = "",
    val topStoryReads: Long = 0L,
    val thisWeekReads: Long = 0L,
    val thisWeekCoins: Int = 0
)

// ── Notification ──────────────────────────────────────────────────────────────
data class Notification(
    val notificationId: String = "",
    val userId: String = "",         // recipient
    val type: NotificationType = NotificationType.SYSTEM,
    val title: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val actionId: String = "",       // storyId / poemId / userId to navigate to
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)

// ── Reading Challenge ─────────────────────────────────────────────────────────
// Stored in users/{userId}/readingChallenge subcollection or as part of user doc
data class ReadingChallenge(
    val userId: String = "",
    val dailyPageGoal: Int = 20,          // user-set target, persists until changed
    val todayPagesRead: Int = 0,          // resets at midnight
    val totalPagesRead: Long = 0L,        // all-time counter
    val lastReadDate: String = "",        // "YYYY-MM-DD" — used to detect new day
    val updatedAt: com.google.firebase.Timestamp? = null
) {
    val remainingToday: Int get() = maxOf(0, dailyPageGoal - todayPagesRead)
    val progressFraction: Float get() =
        if (dailyPageGoal == 0) 0f
        else (todayPagesRead.toFloat() / dailyPageGoal).coerceIn(0f, 1f)
    val isGoalMet: Boolean get() = todayPagesRead >= dailyPageGoal
    val progressPercent: Int get() = (progressFraction * 100).toInt()
}
data class Poem(
    val poemId: String = "",
    val title: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val format: String = "Free verse",
    val language: String = "en",
    val mood: String = "Love",
    val searchTokens: List<String> = emptyList(),
    val likesCount: Int = 0,
    val tipsCount: Int = 0,
    val totalTipsCoins: Int = 0,
    val commentsCount: Int = 0,
    val status: String = "PUBLISHED",
    val createdAt: Timestamp? = null
)

data class PoemLike(
    val userId: String = "",
    val poemId: String = "",
    val createdAt: Timestamp? = null
)

// ── Library ───────────────────────────────────────────────────────────────────
data class LibraryEntry(
    val userId: String = "",
    val storyId: String = "",
    val storyTitle: String = "",
    val storyCoverUrl: String = "",
    val authorName: String = "",
    val lastEpisodeRead: Int = 0,
    val totalEpisodes: Int = 0,
    val lastReadAt: Timestamp? = null,
    val isBookmarked: Boolean = false
)

// ── Coin / Unlock ─────────────────────────────────────────────────────────────
data class UnlockedEpisode(
    val userId: String = "",
    val episodeId: String = "",
    val storyId: String = "",
    val coinsSpent: Int = 0,
    val unlockedAt: Timestamp? = null
)

data class CoinTransaction(
    val txnId: String = "",
    val userId: String = "",
    val type: CoinTxnType = CoinTxnType.SIGNUP_BONUS,
    val coinsAmount: Int = 0,
    val note: String = "",
    val relatedEpisodeId: String = "",
    val relatedPoemId: String = "",
    val createdAt: Timestamp? = null
)

// ── Admin ─────────────────────────────────────────────────────────────────────
data class AdminStats(
    val totalUsers: Int = 0,
    val totalStories: Int = 0,
    val totalPoems: Int = 0,
    val totalCoinsCirculated: Int = 0
)

// ── Meta ──────────────────────────────────────────────────────────────────────
object KathakarMeta {
    val CATEGORIES = listOf(
        "Romance","Thriller","Fantasy","Horror","Drama",
        "Comedy","Mystery","Sci-Fi","Historical","Mythology","Devotional","Biography"
    )
    val LANGUAGES = listOf(
        "en" to "English","hi" to "Hindi","mr" to "Marathi",
        "ta" to "Tamil","te" to "Telugu","bn" to "Bengali",
        "gu" to "Gujarati","kn" to "Kannada","pa" to "Punjabi"
    )
    val POEM_FORMATS = listOf("Free verse","Shayari","Haiku","Ghazal","Sonnet","Nazm","Other")
    val POEM_MOODS   = listOf("Love","Nature","Sadness","Joy","Spiritual","Patriotic","Friendship","Other")
    val POPULAR_TAGS = listOf(
        "slowburn","romance","thriller","fantasy","horror",
        "college","enemies-to-lovers","friends-to-lovers","dark","mystery",
        "comedy","family","mythology","historical","devotional"
    )
    val FONT_SIZES   = listOf(14, 16, 18, 20, 22, 24)
    val FONT_FAMILIES = listOf("Default", "Serif", "Mono")
}

fun generateSearchTokens(title: String, authorName: String): List<String> {
    val tokens = mutableSetOf<String>()
    (title + " " + authorName).lowercase().split(" ")
        .filter { it.length >= 2 }
        .forEach { word -> for (i in 2..word.length) tokens.add(word.substring(0, i)) }
    return tokens.toList()
}
