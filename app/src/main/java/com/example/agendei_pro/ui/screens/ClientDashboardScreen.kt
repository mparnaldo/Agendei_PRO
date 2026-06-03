package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.agendei_pro.core.model.Appointment
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
    globalAnnouncement: String? = null,
    onNewAppointment: () -> Unit,
    onReorderAppointment: (Appointment) -> Unit,
    onUnlinkSalon: () -> Unit,
    onDeleteAppointment: (String) -> Unit,
    onProfileClick: () -> Unit,
    onMenuClick: () -> Unit
) {
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
                onClick = onNewAppointment,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Novo Agendamento") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!globalAnnouncement.isNullOrBlank()) {
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
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Aviso Global",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = globalAnnouncement,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Header do Salão conforme solicitado 🎨
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

            // Seção de Fidelidade (Fidelômetro) 🎁
            if (hasLoyalty) {
                val currentStamps = completedCount % (loyaltyRequired + 1)
                val isRewardReached = currentStamps >= loyaltyRequired

                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRewardReached) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isRewardReached) "Prêmio Disponível! 🎉" else "Fidelômetro 🎁",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRewardReached) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (isRewardReached) {
                            Text(
                                text = "Você completou $loyaltyRequired atendimentos! Seu próximo serviço é GRÁTIS: $loyaltyReward. Informe ao profissional no salão.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        } else {
                            Text(
                                text = "Acumule agendamentos e ganhe prêmios neste salão!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val progress = currentStamps.toFloat() / loyaltyRequired.toFloat()
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$currentStamps de $loyaltyRequired agendamentos para ganhar: $loyaltyReward",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Seção de Reagendamento Rápido 🔁
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
                            Text(
                                text = "Reagendar Último Serviço 🔁",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reagendar")
                        }
                    }
                }
            }

            Text("Meus Próximos Horários", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            if (appointments.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum agendamento ativo.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(16.dp)) {
                    items(appointments) { appt ->
                        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
                        val statusPt = when(appt.status) {
                            "CONFIRMED" -> "Confirmado ✅"
                            "CANCELLED" -> "Cancelado ❌"
                            else -> "Pendente ⏳"
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(appt.serviceName, fontWeight = FontWeight.Bold)
                                    Text(appt.date?.let { sdf.format(it) } ?: "", style = MaterialTheme.typography.bodySmall)
                                    Text(statusPt, style = MaterialTheme.typography.labelSmall)
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
