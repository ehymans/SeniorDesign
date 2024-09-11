package com.sur_tec.helmetiq.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unSelectedIcon: ImageVector,
    val label: String
)

val bottonNavItems = listOf(
    BottomNavItem(Screens.MAINSCREEN.name, Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    BottomNavItem(
        Screens.CONTACTSSCREEN.name,
        Icons.Filled.Call,
        Icons.Outlined.Call,
        "Emergency"
    )
)

