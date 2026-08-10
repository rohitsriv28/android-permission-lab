package com.permissionlab.app.ui.navigation

import com.permissionlab.app.ui.screens.home.HomeScreen
import com.permissionlab.app.ui.screens.camera.CameraScreen
import com.permissionlab.app.ui.screens.gallery.GalleryScreen
import com.permissionlab.app.ui.screens.gallery.PhotoDetailScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

@Composable
fun PermissionLabNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        composable<Home> {
            HomeScreen(
                onNavigateToCamera = { navController.navigate(Camera) },
                onNavigateToGallery = { navController.navigate(Gallery) }
            )
        }
        composable<Camera> {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Gallery> {
            GalleryScreen(
                onNavigateToDetail = { uri ->
                    navController.navigate(PhotoDetail(uri))
                }
            )
        }
        composable<PhotoDetail> {
            PhotoDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
