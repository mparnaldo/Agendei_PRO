package com.example.agendei_admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.agendei_admin.ui.screens.*
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Um tema básico por enquanto, depois podemos copiar os temas do PRO
            MaterialTheme {
                val navController = rememberNavController()
                val auth = FirebaseAuth.getInstance()
                var user by remember { mutableStateOf(auth.currentUser) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (user == null || user?.email != "mpires.arnaldo@gmail.com") {
                        AdminLoginScreen { user = auth.currentUser }
                    } else {
                        NavHost(navController = navController, startDestination = "dashboard") {
                            composable("dashboard") { 
                                AdminDashboardScreen(
                                    onNavigateToSalons = { navController.navigate("salons") },
                                    onNavigateToClients = { navController.navigate("clients") },
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onNavigateToMetrics = { navController.navigate("metrics") },
                                    onNavigateToBroadcast = { navController.navigate("broadcast") }
                                ) 
                            }
                            composable("salons") { AdminSalonsScreen(onBack = { navController.popBackStack() }) }
                            composable("clients") { AdminClientsScreen(onBack = { navController.popBackStack() }) }
                            composable("settings") { AdminSettingsScreen(onBack = { navController.popBackStack() }) }
                            composable("metrics") { AdminMetricsScreen(onBack = { navController.popBackStack() }) }
                            composable("broadcast") { AdminBroadcastScreen(onBack = { navController.popBackStack() }) }
                        }
                    }
                }
            }
        }
    }
}
