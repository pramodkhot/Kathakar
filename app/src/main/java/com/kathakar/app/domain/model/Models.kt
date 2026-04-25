package com.kathakar.app.domain.model

import com.google.firebase.Timestamp

object MvpConfig {
    const val FREE_COINS_ON_SIGNUP   = 100
    const val EPISODE_UNLOCK_COST    = 10
    const val AUTHOR_REVENUE_PERCENT = 60
}

enum class UserRole    { READER, WRITER, ADMIN }
enum class StoryStatus { DRAFT, PUBLISHED, SUSPENDED }
enum class EpisodeStatus { DRAFT, PUBLISHED }
enum class CoinTxnType { SIGNUP_BONUS, EPISODE_UNLOCK, AUTHOR_EARNING }

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
    val createdAt: Timestamp? = null,
    val isBanned: Boolean = false
) {
    val initials: String get() = name.split(" ").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "K" }
    val isAdmin  get() = role == UserRole.ADMIN
    val isWriter get() = role == UserRole.WRITER || role == UserRole.ADMIN
}

data class Follow(val followerId: String = "", val followeeId: String = "", val createdAt: Timestamp? = null)

data class Story(
    val storyId: String = "",
    val title: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val category: String = "",
    val language: String = "en",
    val searchTokens: List<String> = emptyList(),
    val status: String = "DRAFT",
    val totalEpisodes: Int = 0,
    val totalReads: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Episode(
    val episodeId: String = "",
    val storyId: String = "",
    val authorId: String = "",
    val chapterNumber: Int = 1,
    val title: String = "",
    val content: String = "",
    val wordCount: Int = 0,
    val unlockCostCoins: Int = MvpConfig.EPISODE_UNLOCK_COST,
    val isFree: Boolean = false,
    val status: String = "DRAFT",
    val createdAt: Timestamp? = null
)

data class LibraryEntry(
    val userId: String = "",
    val storyId: String = "",
    val storyTitle: String = "",
    val storyCoverUrl: String = "",
    val authorName: String = "",
    val lastEpisodeRead: Int = 0,
    val lastReadAt: Timestamp? = null,
    val isBookmarked: Boolean = false
)

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
    val createdAt: Timestamp? = null
)

data class AdminStats(val totalUsers: Int = 0, val totalStories: Int = 0, val totalCoinsCirculated: Int = 0)

object KathakarMeta {
    val CATEGORIES = listOf("Romance","Thriller","Fantasy","Horror","Drama","Comedy","Mystery","Sci-Fi","Historical","Mythology","Devotional","Biography")
    val LANGUAGES  = listOf("en" to "English","hi" to "Hindi","mr" to "Marathi","ta" to "Tamil","te" to "Telugu","bn" to "Bengali","gu" to "Gujarati","kn" to "Kannada")
}

fun generateSearchTokens(title: String, authorName: String): List<String> {
    val tokens = mutableSetOf<String>()
    (title + " " + authorName).lowercase().split(" ").filter { it.length >= 2 }.forEach { word ->
        for (i in 2..word.length) tokens.add(word.substring(0, i))
    }
    return tokens.toList()
}
