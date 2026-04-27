package com.kathakar.app.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.kathakar.app.ui.screens.*
import com.kathakar.app.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Home           : Screen("home")
    object Library        : Screen("library")
    object Profile        : Screen("profile")
    object Write          : Screen("write")
    object CreateStory    : Screen("write/new")
    object AdminDashboard : Screen("admin/dashboard")
    object BuyCoins       : Screen("stub/coins")
    object Subscribe      : Screen("stub/subscribe")
    object AiWrite        : Screen("stub/ai")
    object CreateEpisode  : Screen("write/episode/{storyId}/{chNum}") {
        fun go(storyId: String, ch: Int) = "write/episode/$storyId/$ch"
    }
    object StoryDetail : Screen("story/{storyId}") {
        fun go(id: String) = "story/$id"
    }
    object EpisodeReader : Screen("episode/{episodeId}") {
        fun go(id: String) = "episode/$id"
    }
}

// Clean tab switching - saves state, no back stack buildup
fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState    = true
    }
}

@Composable
fun KathakarNavGraph(navController: NavHostController) {
    val authVM: AuthViewModel = hiltViewModel()
    val auth by authVM.state.collectAsState()

    NavHost(
        navController    = navController,
        startDestination = if (auth.isAuthenticated) Screen.Home.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(viewModel = authVM, onSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            val user = auth.user ?: return@composable
            HomeScreen(
                user           = user,
                onStoryClick   = { navController.navigate(Screen.StoryDetail.go(it)) },
                onWriteClick   = { navController.switchTab(Screen.Write.route) },
                onLibraryClick = { navController.switchTab(Screen.Library.route) },
                onProfileClick = { navController.switchTab(Screen.Profile.route) }
            )
        }

        composable(Screen.StoryDetail.route,
            listOf(navArgument("storyId") { type = NavType.StringType })) { back ->
            val storyId = back.arguments?.getString("storyId") ?: return@composable
            val user    = auth.user ?: return@composable
            StoryDetailScreen(
                storyId       = storyId,
                user          = user,
                onBack        = { navController.popBackStack() },
                onReadEpisode = { navController.navigate(Screen.EpisodeReader.go(it)) },
                onBuyCoins    = { navController.navigate(Screen.BuyCoins.route) }
            )
        }

        composable(Screen.EpisodeReader.route,
            listOf(navArgument("episodeId") { type = NavType.StringType })) { back ->
            val epId = back.arguments?.getString("episodeId") ?: return@composable
            EpisodeReaderScreen(
                episodeId = epId,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(Screen.Write.route) {
            val user = auth.user ?: return@composable
            WriteScreen(
                user            = user,
                onCreateStory   = { navController.navigate(Screen.CreateStory.route) },
                onCreateEpisode = { sid, ch -> navController.navigate(Screen.CreateEpisode.go(sid, ch)) },
                onAiClick       = { navController.navigate(Screen.AiWrite.route) },
                onBack          = { navController.switchTab(Screen.Home.route) },
                onReadStory     = { navController.navigate(Screen.StoryDetail.go(it)) },
                onLibraryClick  = { navController.switchTab(Screen.Library.route) },
                onProfileClick  = { navController.switchTab(Screen.Profile.route) }
            )
        }

        composable(Screen.CreateStory.route) {
            val user = auth.user ?: return@composable
            CreateStoryScreen(
                user    = user,
                onSaved = { sid ->
                    navController.navigate(Screen.CreateEpisode.go(sid, 1)) {
                        popUpTo(Screen.CreateStory.route) { inclusive = true }
                    }
                },
                onBack  = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateEpisode.route, listOf(
            navArgument("storyId") { type = NavType.StringType },
            navArgument("chNum")   { type = NavType.IntType }
        )) { back ->
            val storyId = back.arguments?.getString("storyId") ?: return@composable
            val ch      = back.arguments?.getInt("chNum") ?: 1
            val user    = auth.user ?: return@composable
            CreateEpisodeScreen(
                storyId       = storyId,
                chapterNumber = ch,
                authorId      = user.userId,
                onDone        = { navController.popBackStack() },
                onBack        = { navController.popBackStack() }
            )
        }

        composable(Screen.Library.route) {
            val user = auth.user ?: return@composable
            LibraryScreen(
                userId         = user.userId,
                onStoryClick   = { navController.navigate(Screen.StoryDetail.go(it)) },
                onBack         = { navController.switchTab(Screen.Home.route) },
                onWriteClick   = { navController.switchTab(Screen.Write.route) },
                onProfileClick = { navController.switchTab(Screen.Profile.route) }
            )
        }

        composable(Screen.Profile.route) {
            val user = auth.user ?: return@composable
            ProfileScreen(
                user             = user,
                onSignOut        = {
                    authVM.signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                onBuyCoins       = { navController.navigate(Screen.BuyCoins.route) },
                onSubscribe      = { navController.navigate(Screen.Subscribe.route) },
                onAdminDashboard = { navController.navigate(Screen.AdminDashboard.route) },
                onBack           = { navController.switchTab(Screen.Home.route) },
                onWriteClick     = { navController.switchTab(Screen.Write.route) },
                onLibraryClick   = { navController.switchTab(Screen.Library.route) }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.BuyCoins.route) {
            ComingSoonScreen("Buy Coins", "Payments coming soon!", onBack = { navController.popBackStack() })
        }
        composable(Screen.Subscribe.route) {
            ComingSoonScreen("Subscriptions", "Subscription plans coming soon.", onBack = { navController.popBackStack() })
        }
        composable(Screen.AiWrite.route) {
            ComingSoonScreen("AI Writing Assistant", "AI features coming soon.", onBack = { navController.popBackStack() })
        }
    }
}
