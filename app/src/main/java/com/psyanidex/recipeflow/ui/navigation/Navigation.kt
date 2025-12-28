package com.psyanidex.recipeflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Recipes : BottomNavItem("recipes", Icons.Default.RestaurantMenu, "Recetas")
    object Calendar : BottomNavItem("calendar", Icons.Default.CalendarMonth, "Plan")
    object Shopping : BottomNavItem("shopping", Icons.Default.ShoppingCart, "Compra")
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(BottomNavItem.Recipes, BottomNavItem.Calendar, BottomNavItem.Shopping)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}