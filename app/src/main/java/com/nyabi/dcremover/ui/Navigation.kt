package com.nyabi.dcremover.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nyabi.dcremover.ui.dashboard.DashboardScreen
import com.nyabi.dcremover.ui.login.LoginScreen

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
}

@Composable
fun DCRemoverNavHost() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
    }
}
