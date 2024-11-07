import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sur_tec.helmetiq.BluetoothViewModel
import com.sur_tec.helmetiq.Contacts
import com.sur_tec.helmetiq.Mainscreen
import com.sur_tec.helmetiq.navigation.BottomNavItem
import com.sur_tec.helmetiq.navigation.Screens

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HelmetIQNavigation(bluetoothViewModel: BluetoothViewModel) {
    val navController = rememberNavController()
    val bottomBarState = rememberSaveable { (mutableStateOf(true)) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // Update bottom bar visibility based on current route
    LaunchedEffect(navBackStackEntry?.destination?.route) {
        bottomBarState.value = when (navBackStackEntry?.destination?.route) {
            Screens.MAINSCREEN.name,
            Screens.CONTACTSSCREEN.name -> true
            else -> true
        }
    }

    Scaffold(
        bottomBar = {
            if (bottomBarState.value){
                BottomNavigationBar(navController = navController as NavHostController,bottomBarState = bottomBarState.value)
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = Screens.MAINSCREEN.name) {
            composable(Screens.MAINSCREEN.name) {
                Mainscreen(navController = navController, modifier = Modifier.padding(padding),bluetoothViewModel)
            }
            composable(Screens.CONTACTSSCREEN.name) {
                Contacts(navController = navController, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController,bottomBarState : Boolean = true) {
    val items = listOf(
        BottomNavItem(
            route = Screens.MAINSCREEN.name,
            selectedIcon = Icons.Filled.Home,
            unSelectedIcon = Icons.Outlined.Home,
            label = "Home"
        ),
        BottomNavItem(
            route = Screens.CONTACTSSCREEN.name,
            selectedIcon = Icons.Filled.Call,
            unSelectedIcon = Icons.Outlined.Call,
            label = "Emergency Contacts"
        )
    )

    NavigationBar {
        val currentRoute = currentRoute(navController)
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (currentRoute == item.route) item.selectedIcon else item.unSelectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(text = item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        restoreState = true
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}
