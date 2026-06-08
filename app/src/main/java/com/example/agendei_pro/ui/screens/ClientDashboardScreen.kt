package com.example.agendei_pro.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.agendei_pro.core.model.Appointment
import com.example.agendei_pro.core.model.Announcement
import com.example.agendei_pro.core.model.LoyaltyState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDashboardScreen(
    salonName: String,
    salonLogoUrl: String?,
    salonLogoShape: String,
    appointments: List<Appointment>,
    userPhotoUrl: String?,
    hasLoyalty: Boolean,
    loyaltyRequired: Int,
    loyaltyReward: String,
    completedCount: Int,
    lastCompletedAppointment: Appointment?,
    loyaltyState: LoyaltyState = LoyaltyState(),
    globalAnnouncement: Announcement? = null,
    salonAnnouncement: Announcement? = null,
    isBlocked: Boolean = false,
    onNewAppointment: () -> Unit,
    onReorderAppointment: (Appointment) -> Unit,
    onMyAppointmentsClick: () -> Unit,
    onUnlinkSalon: () -> Unit,
    onDeleteAppointment: (String) -> Unit,
    onProfileClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("announcements_prefs", Context.MODE_PRIVATE) }
    
    var showRewardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loyaltyState.activeRewardsCount, hasLoyalty) {
        if (hasLoyalty && loyaltyState.activeRewardsCount > 0) {
            showRewardDialog = true
        }
    }
    
    var isGlobalDismissed by remember(globalAnnouncement) {
        mutableStateOf(
            globalAnnouncement?.let { ann ->
                val expired = ann.expiresAt?.let { it.before(Date()) } ?: false
                expired || (ann.duration == "ONCE" && prefs.getBoolean("dismissed_${ann.id}", false))
            } ?: true
        )
    }

    var showGlobalPopup by remember(globalAnnouncement) {
        mutableStateOf(
            globalAnnouncement?.let { ann ->
                val expired = ann.expiresAt?.let { it.before(Date()) } ?: false
                ann.displayType == "POPUP" && !expired && !prefs.getBoolean("dismissed_${ann.id}", false)
            } ?: false
        )
    }

    var isSalonDismissed by remember(salonAnnouncement) {
        mutableStateOf(
            salonAnnouncement?.let { ann ->
                val expired = ann.expiresAt?.let { it.before(Date()) } ?: false
                expired || (ann.duration == "ONCE" && prefs.getBoolean("dismissed_${ann.id}", false))
            } ?: true
        )
    }

    var showSalonPopup by remember(salonAnnouncement) {
        mutableStateOf(
            salonAnnouncement?.let { ann ->
                val expired = ann.expiresAt?.let { it.before(Date()) } ?: false
                ann.displayType == "POPUP" && !expired && !prefs.getBoolean("dismissed_${ann.id}", false)
            } ?: false
        )
    }

    if (showGlobalPopup && globalAnnouncement != null) {
        AlertDialog(
            onDismissRequest = {
                prefs.edit().putBoolean("dismissed_${globalAnnouncement.id}", true).apply()
                showGlobalPopup = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(globalAnnouncement.title.ifBlank { "Comunicado" }, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(globalAnnouncement.message, style = MaterialTheme.typography.bodyLarge)
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("dismissed_${globalAnnouncement.id}", true).apply()
                    showGlobalPopup = false
                }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showSalonPopup && salonAnnouncement != null) {
        AlertDialog(
            onDismissRequest = {
                prefs.edit().putBoolean("dismissed_${salonAnnouncement.id}", true).apply()
                showSalonPopup = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(salonAnnouncement.title.ifBlank { "Mensagem do Salão" }, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(salonAnnouncement.message, style = MaterialTheme.typography.bodyLarge)
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("dismissed_${salonAnnouncement.id}", true).apply()
                    showSalonPopup = false
                }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showRewardDialog) {
        AlertDialog(
            onDismissRequest = { showRewardDialog = false },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(48.dp)) },
            title = { Text("Parabéns! 🏆", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Você atingiu o limite para ser premiado no salão!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (loyaltyState.activeRewardsCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Você tem ${loyaltyState.activeRewardsCount} prêmio(s) de fidelidade disponível(is)!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    if (loyaltyState.nextRewardExpirationDate != null) {
                        val sdfExpiry = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Atenção: Seu prêmio expira em ${sdfExpiry.format(loyaltyState.nextRewardExpirationDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Seu Prêmio:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                            Text(
                                text = loyaltyReward,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Apresente esta tela ou informe seu nome no salão para resgatar seu prêmio de fidelidade.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRewardDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Excelente!")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agendei") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        if (!userPhotoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = "Perfil",
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!isBlocked) onNewAppointment() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Novo Agendamento") },
                containerColor = if (isBlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isBlocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isBlocked) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Bloqueado",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Seu cadastro está temporariamente bloqueado para agendamentos online neste salão. Por favor, entre em contato direto para mais informações.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (globalAnnouncement != null && globalAnnouncement.displayType == "BANNER" && !isGlobalDismissed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Aviso Global",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (globalAnnouncement.title.isNotBlank()) {
                                Text(
                                    text = globalAnnouncement.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = globalAnnouncement.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(onClick = {
                            prefs.edit().putBoolean("dismissed_${globalAnnouncement.id}", true).apply()
                            isGlobalDismissed = true
                        }) {
                            Icon(Icons.Default.Close, "Fechar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (salonAnnouncement != null && salonAnnouncement.displayType == "BANNER" && !isSalonDismissed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Promoção",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (salonAnnouncement.title.isNotBlank()) {
                                Text(
                                    text = salonAnnouncement.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = salonAnnouncement.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(onClick = {
                            prefs.edit().putBoolean("dismissed_${salonAnnouncement.id}", true).apply()
                            isSalonDismissed = true
                        }) {
                            Icon(Icons.Default.Close, "Fechar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            // Header do Salão conforme solicitado 🎨🎨
            if (salonLogoShape == "RECT" && salonLogoUrl != null) {
                // Banner Retangular de ponta a ponta
                AsyncImage(
                    model = salonLogoUrl,
                    contentDescription = "Banner do Salão",
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Logo Redonda + Nome do Salão do lado
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (salonLogoUrl != null) {
                            AsyncImage(
                                model = salonLogoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Box(contentAlignment = Alignment.Center) { Text(salonName.take(1).uppercase(), fontWeight = FontWeight.Bold) }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Salão Vinculado", style = MaterialTheme.typography.labelSmall)
                            Text(salonName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onUnlinkSalon) { Icon(Icons.Default.LinkOff, null, tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            // Seção de Fidelidade (Fidelômetro)
            if (hasLoyalty) {
                val currentStamps = completedCount
                val isRewardReached = loyaltyState.activeRewardsCount > 0

                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRewardReached) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isRewardReached) BorderStroke(1.5.dp, Color(0xFF2E7D32)) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isRewardReached) Icons.Default.EmojiEvents else Icons.Default.CardMembership,
                                contentDescription = null,
                                tint = if (isRewardReached) Color(0xFFFBC02D) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRewardReached) "Prêmio de Fidelidade! 🎉" else "Fidelômetro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isRewardReached) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (isRewardReached) {
                            Text(
                                text = "Você possui ${loyaltyState.activeRewardsCount} serviço(s) fidelidade GRÁTIS disponível(is): $loyaltyReward. Escolha usar seu prêmio no próximo agendamento!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            if (loyaltyState.nextRewardExpirationDate != null) {
                                val sdfExpiry = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Atenção: O resgate de 1 prêmio expira em ${sdfExpiry.format(loyaltyState.nextRewardExpirationDate)}",
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (loyaltyState.expiredRewardsCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Prêmios expirados: ${loyaltyState.expiredRewardsCount}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(
                                text = "Acumule agendamentos e ganhe prêmios neste salão!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val progress = (currentStamps.toFloat() / loyaltyRequired.toFloat()).coerceAtMost(1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$currentStamps de $loyaltyRequired selos para ganhar: $loyaltyReward",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (loyaltyState.expiredRewardsCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Prêmios não resgatados a tempo (expirados): ${loyaltyState.expiredRewardsCount}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Seção de Reagendamento Rápido
            if (lastCompletedAppointment != null) {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Autorenew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reagendar Último Serviço",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastCompletedAppointment.serviceName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Button(
                            onClick = { onReorderAppointment(lastCompletedAppointment) },
                            enabled = !isBlocked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reagendar")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Meus Próximos Horários", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onMyAppointmentsClick) {
                    Text("Ver Todos", fontWeight = FontWeight.Bold)
                }
            }
            
            val activeAppointments = remember(appointments) {
                appointments.filter { it.status != "CANCELLED" }
            }
            if (activeAppointments.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum agendamento ativo.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(16.dp)) {
                    items(activeAppointments) { appt ->
                        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
                        val (statusText, badgeColor, badgeContentColor) = when(appt.status) {
                            "CONFIRMED" -> Triple("Confirmado", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            "CANCELLED" -> Triple("Cancelado", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                            else -> Triple("Pendente", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(appt.serviceName, fontWeight = FontWeight.Bold)
                                    Text(appt.date?.let { sdf.format(it) } ?: "", style = MaterialTheme.typography.bodySmall)
                                    
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = badgeColor,
                                            contentColor = badgeContentColor,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (appt.loyaltyRedeemed) {
                                            Surface(
                                                color = Color(0xFFFFF9C4),
                                                contentColor = Color(0xFFF57F17),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CardGiftcard,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(11.dp),
                                                        tint = Color(0xFFF57F17)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Cortesia Fidelidade",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (appt.status != "CONFIRMED") {
                                    IconButton(onClick = { onDeleteAppointment(appt.id) }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
