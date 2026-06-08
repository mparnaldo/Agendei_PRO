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
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

        // Criar canal de notificações no início para que o FCM possa exibir as notificações em background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "agendamentos_channel"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        setContent {
            val viewModel: MainViewModel = viewModel()
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            
            Agendei_PROTheme(selectedTheme = selectedTheme) {
                val authState by viewModel.authState.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                val clientAppointments by viewModel.clientAppointments.collectAsState()
                val pendingSalonAppointments by viewModel.pendingSalonAppointments.collectAsState()
                val globalAnnouncement by viewModel.globalAnnouncement.collectAsState()
                val salonAnnouncement by viewModel.salonAnnouncement.collectAsState()
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
                                NavigationDrawerItem(
                                     label = { Text("Meu Perfil") },
                                     icon = { Icon(Icons.Default.AccountCircle, null) },
                                     selected = false,
                                     onClick = { scope.launch { drawerState.close() }; navController.navigate("profile") }
                                 )
                                 NavigationDrawerItem(
                                     label = { Text("Temas") },
                                     icon = { Icon(Icons.Default.Palette, null) },
                                     selected = false,
                                     onClick = { scope.launch { drawerState.close() }; navController.navigate("themes") }
                                 )
                                 NavigationDrawerItem(
                                     label = { Text("Meus Agendamentos") },
                                     icon = { Icon(Icons.Default.EventNote, null) },
                                     selected = false,
                                     onClick = { scope.launch { drawerState.close() }; navController.navigate("client_appointments") }
                                 )
                                 if (isProVersion) {
                                     NavigationDrawerItem(
                                         label = { Text("Carteira de Clientes") },
                                         icon = { Icon(Icons.Default.People, null) },
                                         selected = false,
                                         onClick = { scope.launch { drawerState.close() }; navController.navigate("salon_clients") }
                                     )
                                     NavigationDrawerItem(
                                         label = { Text("Minha Equipe") },
                                         icon = { Icon(Icons.Default.Groups, null) },
                                         selected = false,
                                         onClick = { scope.launch { drawerState.close() }; navController.navigate("team") }
                                     )
                                     NavigationDrawerItem(
                                         label = { Text("Promover Salão") },
                                         icon = { Icon(Icons.Default.Campaign, null) },
                                         selected = false,
                                         onClick = { scope.launch { drawerState.close() }; navController.navigate("salon_promotion") }
                                     )
                                     NavigationDrawerItem(
                                         label = { Text("Desempenho & Finanças") },
                                         icon = { Icon(Icons.Default.TrendingUp, null) },
                                         selected = false,
                                         onClick = { scope.launch { drawerState.close() }; navController.navigate("salon_performance") }
                                     )
                                     NavigationDrawerItem(
                                         label = { Text("Fila de Espera") },
                                         icon = { Icon(Icons.Default.HourglassEmpty, null) },
                                         selected = false,
                                         onClick = { scope.launch { drawerState.close() }; navController.navigate("salon_waiting_list") }
                                     )
                                     NavigationDrawerItem(
                                         label = { Text("Ajustes do Salão") },
                                         icon = { Icon(Icons.Default.Settings, null) },
                                         selected = false,
                                         onClick = { scope.launch { drawerState.close() }; navController.navigate("salon_settings") }
                                     )
                                 }
                                 
                                 HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                 NavigationDrawerItem(
                                     label = { Text("Sair") },
                                     icon = { Icon(Icons.Default.Logout, null) },
                                     selected = false,
                                     onClick = { scope.launch { drawerState.close() }; viewModel.logout() }
                                 )
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
                                        is AuthState.TrialExpired -> {
                                            val bronzePrice by viewModel.subscriptionPriceBronze.collectAsState()
                                            val prataPrice by viewModel.subscriptionPricePrata.collectAsState()
                                            val ouroPrice by viewModel.subscriptionPriceOuro.collectAsState()
                                            SubscriptionScreen(
                                                salonName = state.salon.name,
                                                bronzePrice = bronzePrice,
                                                prataPrice = prataPrice,
                                                ouroPrice = ouroPrice,
                                                onSubscribe = { plan, maxProfs -> viewModel.subscribeToPlan(plan, maxProfs) },
                                                onLogout = { viewModel.logout() }
                                            )
                                        }
                                        is AuthState.AuthenticatedWithSalon -> DashboardScreen(
                                            salonName = state.salon.name,
                                            salonCode = state.salon.code,
                                            logoUrl = state.salon.logoUrl,
                                            logoShape = state.salon.logoShape,
                                            daysRemaining = state.daysRemaining,
                                            pendingAppointments = pendingSalonAppointments,
                                            userPhotoUrl = userProfile?.photoUrl,
                                            globalAnnouncement = globalAnnouncement,
                                            onManageServices = { navController.navigate("services") },
                                            onViewAgenda = { navController.navigate("agenda") },
                                            onFinancialClick = { navController.navigate("salon_performance") },
                                            onUpdateStatus = { id, s -> viewModel.updateAppointmentStatus(id, s) },
                                            onSettingsClick = { scope.launch { drawerState.open() } },
                                            onThemeClick = { navController.navigate("themes") },
                                            onProfileClick = { navController.navigate("profile") }
                                        )
                                        is AuthState.AuthenticatedClient -> {
                                            val binding = state.bindings.firstOrNull() ?: return@composable
                                            val hasLoyalty = state.latestSalon?.hasLoyaltyProgram ?: false
                                            val loyaltyRequired = state.latestSalon?.loyaltyRequiredServices ?: 10
                                            val loyaltyReward = state.latestSalon?.loyaltyRewardDescription ?: "Corte Grátis"
                                            ClientDashboardScreen(
                                                salonName = binding.salonName,
                                                salonLogoUrl = binding.salonLogoUrl,
                                                salonLogoShape = binding.salonLogoShape,
                                                appointments = clientAppointments,
                                                userPhotoUrl = userProfile?.photoUrl,
                                                hasLoyalty = hasLoyalty,
                                                loyaltyRequired = loyaltyRequired,
                                                loyaltyReward = loyaltyReward,
                                                completedCount = state.completedCount,
                                                lastCompletedAppointment = state.lastCompletedAppointment,
                                                loyaltyState = state.loyaltyState,
                                                globalAnnouncement = globalAnnouncement,
                                                salonAnnouncement = salonAnnouncement,
                                                onNewAppointment = { navController.navigate("scheduling/${binding.salonId}") },
                                                onReorderAppointment = { appt ->
                                                    navController.navigate("scheduling/${binding.salonId}?serviceId=${appt.serviceId}")
                                                },
                                                onMyAppointmentsClick = { navController.navigate("client_appointments") },
                                                onUnlinkSalon = { viewModel.unlinkCurrentSalon(binding.salonId) },
                                                onDeleteAppointment = { viewModel.deleteAppointment(it) },
                                                onProfileClick = { navController.navigate("profile") },
                                                onMenuClick = { scope.launch { drawerState.open() } }
                                            )
                                        }
                                        is AuthState.AuthenticatedNoBindings -> {
                                            var linkLoading by remember { mutableStateOf(false) }
                                            var linkError by remember { mutableStateOf<String?>(null) }
                                            LinkSalonScreen(
                                                onLinkSuccess = { viewModel.checkUserStatus(isProVersion) },
                                                onSearchCode = { code ->
                                                    linkLoading = true
                                                    linkError = null
                                                    viewModel.linkNewSalon(code) { success, err ->
                                                        linkLoading = false
                                                        linkError = err
                                                    }
                                                },
                                                isLoading = linkLoading,
                                                errorMessage = linkError
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
                                        currentPhoneNumber = userProfile?.phoneNumber ?: "",
                                        userPhotoUrl = userProfile?.photoUrl,
                                        subscriptionStatus = subscriptionStatusText,
                                        onSaveProfile = { name, phone -> viewModel.updateProfile(name, phone) },
                                        onLogout = { viewModel.logout() },
                                        onDeleteAccount = { viewModel.deleteUserAccount { navController.popBackStack() } },
                                        onThemeClick = { navController.navigate("themes") },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("themes") { ThemeSelectionScreen(selectedTheme.id, { viewModel.setTheme(it) }, { navController.popBackStack() }) }
                                composable("salon_promotion") { SalonPromotionScreen { navController.popBackStack() } }
                                composable("services") { ManageServicesScreen { navController.popBackStack() } }
                                composable("team") { ManageTeamScreen { navController.popBackStack() } }
                                composable("agenda") { AgendaScreen(onUpdateStatus = { id, s -> viewModel.updateAppointmentStatus(id, s) }, onNavigateBack = { navController.popBackStack() }) }
                                composable("salon_performance") { SalonPerformanceScreen { navController.popBackStack() } }
                                composable("salon_waiting_list") { SalonWaitingListScreen { navController.popBackStack() } }
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
                                         currentHasLoyalty = state?.salon?.hasLoyaltyProgram ?: false,
                                         currentLoyaltyRequired = state?.salon?.loyaltyRequiredServices ?: 10,
                                         currentLoyaltyReward = state?.salon?.loyaltyRewardDescription ?: "Corte Grátis",
                                         currentAutoValidateLoyalty = state?.salon?.autoValidateLoyalty ?: false,
                                         currentLoyaltyRedemptionDays = state?.salon?.loyaltyRedemptionDays ?: 30,
                                         currentSlotInterval = state?.salon?.slotIntervalMinutes ?: 30,
                                         currentIsIndividualized = state?.salon?.isConfigurationIndividualized ?: false,
                                         currentHasWaitingList = state?.salon?.hasWaitingList ?: false,
                                         uploadProgress = uploadProgress,
                                         onSave = { name, o, c, bs, be, d, a, s, seg, hl, lr, lrd, avl, lrdays, slotInt, isIndiv, hwl -> 
                                             viewModel.updateSalonSettings(name, o, c, bs, be, d, a, s, seg, hl, lr, lrd, avl, lrdays, slotInt, isIndiv, hwl)
                                             navController.popBackStack()
                                         },
                                         onLogoSelected = { uri -> viewModel.uploadSalonLogo(uri) },
                                         onRemoveLogo = { viewModel.uploadSalonLogo(android.net.Uri.EMPTY) },
                                         onNavigateBack = { navController.popBackStack() }
                                     )
                                 }
                                composable("salon_clients") {
                                    val state = authState as? AuthState.AuthenticatedWithSalon
                                    val loyaltyRequired = state?.salon?.loyaltyRequiredServices ?: 10
                                    val loyaltyReward = state?.salon?.loyaltyRewardDescription ?: "Corte Grátis"
                                    SalonClientsScreen(
                                     viewModel = viewModel,
                                        loyaltyRequired = loyaltyRequired,
                                        loyaltyReward = loyaltyReward,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "scheduling/{salonId}?serviceId={serviceId}",
                                    arguments = listOf(
                                        navArgument("salonId") { type = NavType.StringType },
                                        navArgument("serviceId") { type = NavType.StringType; nullable = true; defaultValue = null }
                                    )
                                ) { backStackEntry ->
                                    val salonId = backStackEntry.arguments?.getString("salonId") ?: ""
                                    val serviceId = backStackEntry.arguments?.getString("serviceId")
                                    SchedulingScreen(
                                        salonId = salonId,
                                        preselectedServiceId = serviceId,
                                        userProfile = userProfile,
                                        onSavePhone = { phone -> viewModel.updateProfile(userProfile?.name ?: "Cliente", phone) },
                                        onNavigateBack = { navController.popBackStack() },
                                        onSuccess = { navController.popBackStack() }
                                    )
                                }
                                composable("client_appointments") {
                                    ClientAppointmentsScreen { navController.popBackStack() }
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
