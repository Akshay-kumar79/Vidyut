package com.akshaw.vidyut

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.akshaw.vidyut.navigation.Route
import com.akshaw.vidyut.presentation.mainscreen.MainScreen
import com.akshaw.vidyut.ui.theme.Animations
import com.akshaw.vidyut.ui.theme.VidyutTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VidyutTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val navController = rememberNavController()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                ) {
                    NavHost(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it)
                            .background(MaterialTheme.colorScheme.background),
                        enterTransition = { Animations.Default.enter() },
                        popEnterTransition = { Animations.Default.enter() },
                        exitTransition = { Animations.Default.exit() },
                        popExitTransition = { Animations.Default.exit() },
                        navController = navController,
                        startDestination = Route.MainScreen.route
                    ) {
                        composable(route = Route.MainScreen.route) {
                            MainScreen(
                                snackbarHostState = snackbarHostState,
                                shouldShowRequestPermissionRationale = ::shouldShowRequestPermissionRationale,
                                openAppSettings = ::openAppSettings
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}