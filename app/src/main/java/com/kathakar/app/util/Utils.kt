package com.kathakar.app.util

object FirestoreCollections {
    const val USERS             = "users"
    const val STORIES           = "stories"
    const val EPISODES          = "episodes"
    const val UNLOCKED_EPISODES = "unlocked_episodes"
    const val LIBRARY           = "library"
    const val COIN_TRANSACTIONS = "coin_transactions"
}

fun unlockedDocId(userId: String, episodeId: String) = "${userId}_${episodeId}"
fun libraryDocId(userId: String, storyId: String)    = "${userId}_${storyId}"

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
