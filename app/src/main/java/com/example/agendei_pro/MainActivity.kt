package com.example.agendei_pro

import android.os.Bundle
import android.os.Build
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.agendei_pro.core.auth.AuthManager
import com.example.agendei_pro.core.service.NotificationService
import com.example.agendei_pro.ui.screens.*
import com.example.agendei_pro.ui.theme.Agendei_PROTheme
import com.example.agendei_pro.ui.viewmodel.AuthState
import com.example.agendei_pro.ui.viewmodel.MainViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isProVersion = try { BuildConfig.FLAVOR == "pro" } catch (e: Exception) { true }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        
        setContent {
            val viewModel: MainViewModel = viewModel()
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            
            Agendei_PROTheme(selectedTheme = selectedTheme) {
                val authState by viewModel.authState.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                val clientAppointments by viewModel.clientAppointments.collectAsState()
                val pendingSalonAppointments by viewModel.pendingSalonAppointments.collectAsState()
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                
                var showSplash by remember { mutableStateOf(true) }

                val context = LocalContext.current

                LaunchedEffect(Unit) { viewModel.checkUserStatus(isProVersion) }

                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.AuthenticatedWithSalon, is AuthState.AuthenticatedClient -> {
                            val serviceIntent = Intent(context, NotificationService::class.java)
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                            } catch (e: Exception) {
                                // Evita falhas se o app estiver em background ou com restrições de OEM
                            }
                        }
                        is AuthState.Unauthenticated -> {
                            try {
                                context.stopService(Intent(context, NotificationService::class.java))
                            } catch (e: Exception) {}
                        }
                        else -> {}
                    }
                }

                if (showSplash) {
                    SplashScreen(isProVersion) { showSplash = false }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                if (isProVersion) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.agendeimenu),
                                            contentDescription = "Agendei PRO Logo",
                                            modifier = Modifier
                                                .height(55.dp)
                                                .fillMaxWidth(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Menu Agendei",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                NavigationDrawerItem(label = { Text("Meu Perfil") }, selected = false, onClick = { scope.launch { drawerState.close() }; navController.navigate("profile") })
                                NavigationDrawerItem(label = { Text("Temas") }, selected = false, onClick = { scope.launch { drawerState.close() }; navController.navigate("themes") })
                                if (isProVersion) {
                                    NavigationDrawerItem(label = { Text("Ajustes do Salão") }, selected = false, onClick = { scope.launch { drawerState.close() }; navController.navigate("salon_settings") })
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                NavigationDrawerItem(label = { Text("Sair") }, selected = false, onClick = { scope.launch { drawerState.close() }; viewModel.logout() })
                            }
                        }
                    ) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            NavHost(navController = navController, startDestination = "main_flow") {
                                composable("main_flow") {
                                    when (val state = authState) {
                                        is AuthState.Loading -> PlaceholderScreen("Iniciando...")
                                        is AuthState.Unauthenticated -> WelcomeScreen(
                                            onNavigateToLogin = { scope.launch { AuthManager(navController.context).signInWithGoogle().onSuccess { viewModel.checkUserStatus(isProVersion) } } },
                                            onNavigateToRegister = { scope.launch { AuthManager(navController.context).signInWithGoogle().onSuccess { viewModel.checkUserStatus(isProVersion) } } }
                                        )
                                        is AuthState.AuthenticatedNoSalon -> RegisterSalonScreen { n, a, p, s -> viewModel.registerSalon(n, a, p, s) }
                                        is AuthState.TrialExpired -> SubscriptionScreen(
                                            salonName = state.salon.name,
                                            onSubscribe = { /* Integrar com Billing depois */ },
                                            onLogout = { viewModel.logout() }
                                        )
                                        is AuthState.AuthenticatedWithSalon -> DashboardScreen(
                                            salonName = state.salon.name,
                                            salonCode = state.salon.code,
                                            logoUrl = state.salon.logoUrl,
                                            logoShape = state.salon.logoShape,
                                            daysRemaining = state.daysRemaining,
                                            pendingAppointments = pendingSalonAppointments,
                                            userPhotoUrl = userProfile?.photoUrl,
                                            onManageServices = { navController.navigate("services") },
                                            onViewAgenda = { navController.navigate("agenda") },
                                            onUpdateStatus = { id, s -> viewModel.updateAppointmentStatus(id, s) },
                                            onSettingsClick = { scope.launch { drawerState.open() } },
                                            onThemeClick = { navController.navigate("themes") },
                                            onProfileClick = { navController.navigate("profile") }
                                        )
                                        is AuthState.AuthenticatedClient -> {
                                            val binding = state.bindings.firstOrNull() ?: return@composable
                                            ClientDashboardScreen(
                                                salonName = binding.salonName,
                                                salonLogoUrl = binding.salonLogoUrl,
                                                salonLogoShape = binding.salonLogoShape,
                                                appointments = clientAppointments,
                                                userPhotoUrl = userProfile?.photoUrl,
                                                onNewAppointment = { navController.navigate("scheduling/${binding.salonId}") },
                                                onUnlinkSalon = { viewModel.unlinkCurrentSalon(binding.salonId) },
                                                onDeleteAppointment = { viewModel.deleteAppointment(it) },
                                                onProfileClick = { navController.navigate("profile") },
                                                onMenuClick = { scope.launch { drawerState.open() } }
                                            )
                                        }
                                        else -> PlaceholderScreen("Erro")
                                    }
                                }
                                composable("profile") {
                                    val state = authState as? AuthState.AuthenticatedWithSalon
                                    val subscriptionStatusText = if (isProVersion) {
                                        if (state != null) {
                                            if (state.daysRemaining == 999) "Assinatura Ativa" else "Período de teste: ${state.daysRemaining} dias restantes"
                                        } else {
                                            "Não assinado"
                                        }
                                    } else null

                                    ProfileScreen(
                                        currentName = userProfile?.name ?: "",
                                        userPhotoUrl = userProfile?.photoUrl,
                                        subscriptionStatus = subscriptionStatusText,
                                        onSaveName = { viewModel.updateProfileName(it) },
                                        onLogout = { viewModel.logout() },
                                        onDeleteAccount = { viewModel.deleteUserAccount { navController.popBackStack() } },
                                        onThemeClick = { navController.navigate("themes") },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("themes") { ThemeSelectionScreen(selectedTheme.id, { viewModel.setTheme(it) }, { navController.popBackStack() }) }
                                composable("services") { ManageServicesScreen { navController.popBackStack() } }
                                composable("agenda") { AgendaScreen(onUpdateStatus = { id, s -> viewModel.updateAppointmentStatus(id, s) }, onNavigateBack = { navController.popBackStack() }) }
                                composable("salon_settings") {
                                    val state = authState as? AuthState.AuthenticatedWithSalon
                                    val uploadProgress by viewModel.uploadProgress.collectAsState()
                                    SalonSettingsScreen(
                                        currentName = state?.salon?.name ?: "",
                                        currentOpening = state?.salon?.openingTime ?: "08:00",
                                        currentClosing = state?.salon?.closingTime ?: "18:00",
                                        currentBreakStart = state?.salon?.breakStart ?: "12:00",
                                        currentBreakEnd = state?.salon?.breakEnd ?: "13:00",
                                        currentDays = state?.salon?.workingDays ?: listOf(2,3,4,5,6,7),
                                        currentAutoAccept = state?.salon?.autoAccept ?: false,
                                        currentLogoUrl = state?.salon?.logoUrl,
                                        currentLogoShape = state?.salon?.logoShape ?: "ROUND",
                                        currentSegment = state?.salon?.segment ?: "BARBEARIA",
                                        uploadProgress = uploadProgress,
                                        onSave = { name, o, c, bs, be, d, a, s, seg -> viewModel.updateSalonSettings(name, o, c, bs, be, d, a, s, seg); navController.popBackStack() },
                                        onLogoSelected = { uri -> viewModel.uploadSalonLogo(uri) },
                                        onRemoveLogo = { viewModel.uploadSalonLogo(android.net.Uri.EMPTY) },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("scheduling/{salonId}") { backStackEntry ->
                                    val salonId = backStackEntry.arguments?.getString("salonId") ?: ""
                                    SchedulingScreen(salonId = salonId, onNavigateBack = { navController.popBackStack() }, onSuccess = { navController.popBackStack() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Text(text = text, modifier = Modifier.fillMaxSize())
}

fun showLocalNotification(context: Context, title: String, message: String) {
    val channelId = "agendamentos_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Notificações de Agendamentos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificações de novos agendamentos e confirmações"
            enableLights(true)
            enableVibration(true)
            setSound(defaultSoundUri, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setAutoCancel(true)
        .setSound(defaultSoundUri)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setContentIntent(pendingIntent)

    notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
}
