package com.example.agendei_pro.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.agendei_pro.core.model.Appointment
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    salonName: String,
    salonCode: String,
    logoUrl: String?,
    logoShape: String,
    daysRemaining: Int,
    pendingAppointments: List<Appointment>,
    userPhotoUrl: String?,
    globalAnnouncement: String? = null,
    onManageServices: () -> Unit,
    onViewAgenda: () -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onSettingsClick: () -> Unit,
    onThemeClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { if (logoShape != "RECT" || logoUrl == null) Text(salonName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onSettingsClick) {
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
                            Icon(Icons.Default.Person, "Perfil")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!globalAnnouncement.isNullOrBlank()) {
                item {
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
            }
            // Banner ou Logo Circular 🎨
            item {
                if (logoShape == "RECT" && logoUrl != null) {
                    Column {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentScale = ContentScale.Crop
                        )
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(salonName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                                val statusText = if (daysRemaining == 999) "Assinatura: Ativa" else "Período de teste: $daysRemaining dias restantes"
                                val statusColor = if (daysRemaining == 999) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                Text(statusText, color = statusColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (logoUrl != null) {
                            AsyncImage(model = logoUrl, contentDescription = null, modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Box(contentAlignment = Alignment.Center) { Text(salonName.take(1)) }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(salonName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                            val statusText = if (daysRemaining == 999) "Assinatura: Ativa" else "Período de teste: $daysRemaining dias restantes"
                            val statusColor = if (daysRemaining == 999) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            Text(statusText, color = statusColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Código de Vinculação com Copiar e WhatsApp 📲
            item {
                Card(modifier = Modifier.padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Código do Salão", style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = salonCode, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp), modifier = Modifier.weight(1f))
                            
                            IconButton(onClick = { 
                                clipboardManager.setText(AnnotatedString(salonCode))
                                Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
                            }) { Icon(Icons.Default.ContentCopy, "Copiar") }
                            
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Agende agora no $salonName! Código: $salonCode")
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartilhar via"))
                            }) { Icon(Icons.Default.Share, "WhatsApp") }
                        }
                    }
                }
            }

            // Menu Principal
            item {
                Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(title = "Agenda", icon = Icons.Default.Event, modifier = Modifier.weight(1f), onClick = onViewAgenda)
                    ActionButton(title = "Serviços", icon = Icons.Default.ContentCut, modifier = Modifier.weight(1f), onClick = onManageServices)
                }
            }

            // Solicitações Pendentes ⏳
            item {
                Text("Solicitações Pendentes (${pendingAppointments.size})", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            if (pendingAppointments.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Tudo em dia! Sem solicitações.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(pendingAppointments) { appt ->
                    PendingItem(appt) { onUpdateStatus(appt.id, "CONFIRMED") }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun PendingItem(appt: Appointment, onAccept: () -> Unit) {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(appt.clientName, fontWeight = FontWeight.Bold)
                Text("${appt.serviceName} - ${sdf.format(appt.date ?: Date())}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Aceitar", color = Color.White)
            }
        }
    }
}

@Composable
fun ActionButton(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    ElevatedButton(onClick = onClick, modifier = modifier.height(80.dp), shape = MaterialTheme.shapes.medium) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null)
            Text(title, fontSize = 12.sp)
        }
    }
}
