package com.bekirahmetli.qrcodeapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bekirahmetli.qrcodeapp.ui.scanner.ScannerScreen
import com.bekirahmetli.qrcodeapp.ui.history.HistoryScreen
import com.bekirahmetli.qrcodeapp.ui.generator.GeneratorScreen
import com.bekirahmetli.qrcodeapp.navigation.NavItem


@Composable
fun QrKodApp() {
    val navController = rememberNavController()
    val items = listOf(
        NavItem("scanner", "Tara", Icons.Filled.QrCode2),
        NavItem("history", "Geçmiş", Icons.Filled.History),
        NavItem("generator", "Oluştur", Icons.Filled.Add)
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "scanner",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("scanner") { ScannerScreen() }
            composable("history") { HistoryScreen() }
            composable("generator") { GeneratorScreen() }
        }
    }
}