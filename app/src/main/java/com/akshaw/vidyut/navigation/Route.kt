package com.akshaw.vidyut.navigation

sealed class Route(val route: String) {
    object MainScreen : Route("main_screen")
}