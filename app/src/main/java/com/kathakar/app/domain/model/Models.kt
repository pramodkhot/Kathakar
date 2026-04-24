package com.kathakar.app.domain.model

import com.google.firebase.Timestamp

object MvpConfig {
    const val FREE_COINS_ON_SIGNUP     = 100
    const val EPISODE_UNLOCK_COST      = 10
    const val AUTHOR_REVENUE_PERCENT   = 60
    const val PAYMENTS_ENABLED         = false
    const val AI_WRITING_ENABLED       = false
}

enum class UserRole { READER, WRITER, ADMIN }
enum class StoryStatus { DRAFT, PUBLISHED, SUSPENDED }
enum class EpisodeStatus { DRAFT, PUBLISHED }
enum class CoinTxnType { SIGNUP_BONUS, EPISODE_UNLOCK, AUTHOR_EARNING }

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val role: UserRole = UserRole.READER,
    val coinBalance: Int = 0,
    val totalCoinsEarned: Int = 0,
    val createdAt: Timestamp? = null,
    val isBanned: Boolean = false
) {
    val initials: String get() = name.split(" ")
        .filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "K" }
}

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
    val status: StoryStatus = StoryStatus.DRAFT,
    val totalEpisodes: Int = 0,
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
    val status: EpisodeStatus = EpisodeStatus.DRAFT,
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

object KathakarMeta {
    val CATEGORIES = listOf(
        "Romance", "Thriller", "Fantasy", "Horror",
        "Drama", "Comedy", "Mystery", "Sci-Fi",
        "Historical", "Mythology", "Devotional", "Biography"
    )
    val LANGUAGES = listOf(
        "en" to "English", "hi" to "हिंदी", "mr" to "मराठी",
        "ta" to "தமிழ்", "te" to "తెలుగు", "bn" to "বাংলা",
        "gu" to "ગુજરાતી", "kn" to "ಕನ್ನಡ"
    )
}

fun generateSearchTokens(title: String, authorName: String): List<String> {
    val tokens = mutableSetOf<String>()
    "$title $authorName".lowercase().split(" ").filter { it.length >= 2 }.forEach { word ->
        for (i in 2..word.length) tokens.add(word.substring(0, i))
    }
    return tokens.toList()
}
