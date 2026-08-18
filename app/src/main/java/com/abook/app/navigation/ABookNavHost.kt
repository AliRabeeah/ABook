package com.abook.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.abook.app.ui.library.LibraryScreen
import com.abook.app.ui.reader.ReaderScreen

private object Routes {
    const val LIBRARY = "library"
    const val READER = "reader/{bookId}"
    fun readerRoute(bookId: Long) = "reader/$bookId"
}

@Composable
fun ABookNavHost() {
    val navController: NavHostController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        enterTransition = {
            // "Hero-like" transition: تكبير وتلاشي تدريجي يعطي إحساس انتقال الغلاف لصفحة القراءة
            androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = tween(320)) +
                androidx.compose.animation.fadeIn(animationSpec = tween(280))
        },
        exitTransition = {
            androidx.compose.animation.scaleOut(targetScale = 1.05f, animationSpec = tween(280)) +
                androidx.compose.animation.fadeOut(animationSpec = tween(220))
        },
        popEnterTransition = {
            androidx.compose.animation.scaleIn(initialScale = 1.05f, animationSpec = tween(320)) +
                androidx.compose.animation.fadeIn(animationSpec = tween(280))
        },
        popExitTransition = {
            androidx.compose.animation.scaleOut(targetScale = 0.92f, animationSpec = tween(280)) +
                androidx.compose.animation.fadeOut(animationSpec = tween(220))
        }
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpenBook = { bookId -> navController.navigate(Routes.readerRoute(bookId)) }
            )
        }
        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            ReaderScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
