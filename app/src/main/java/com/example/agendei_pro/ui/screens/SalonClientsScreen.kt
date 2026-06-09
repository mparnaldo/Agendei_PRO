package com.example.agendei_pro.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import com.example.agendei_pro.ui.viewmodel.MainViewModel
import com.example.agendei_pro.ui.viewmodel.AuthState
import com.example.agendei_pro.core.model.Appointment
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalonClientsScreen(
    viewModel: MainViewModel,
    loyaltyRequired: Int,
    loyaltyReward: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clients by viewModel.salonClients.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    var clientToRedeem by remember { mutableStateOf<MainViewModel.SalonClientItem?>(null) }
    var selectedClientForDetails by remember { mutableStateOf<MainViewModel.SalonClientItem?>(null) }
    var clientAppointmentsHistory by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var loadingHistory by remember { mutableStateOf(false) }

    LaunchedEffect(selectedClientForDetails) {
        val client = selectedClientForDetails
        if (client != null) {
            loadingHistory = true
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val currentSalonId = viewModel.authState.value.let { 
                if (it is AuthState.AuthenticatedWithSalon) it.salon.id else "" 
            }
            db.collection("appointments")
                .whereEqualTo("clientUid", client.profile.uid)
                .whereEqualTo("salonId", currentSalonId)
                .get()
                .addOnSuccessListener { snapshot ->
                    clientAppointmentsHistory = snapshot.documents.mapNotNull { it.toObject(Appointment::class.java) }
                        .sortedByDescending { it.date ?: Date(0) }
                    loadingHistory = false
                }
                .addOnFailureListener {
                    loadingHistory = false
                }
        } else {
            clientAppointmentsHistory = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSalonClients()
    }

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) {
            clients
        } else {
            clients.filter {
                it.profile.name.contains(searchQuery, ignoreCase = true) ||
                it.profile.email.contains(searchQuery, ignoreCase = true) ||
                it.profile.phoneNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carteira de Clientes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    val authState = viewModel.authState.collectAsState().value
                    if (authState is AuthState.AuthenticatedWithSalon) {
                        val salon = authState.salon
                        IconButton(onClick = {
                            val text = "Olá! Já estamos usando o Agendei PRO para nossos agendamentos.\n\nBaixe o app do cliente aqui:\nhttps://drive.google.com/file/d/SEU_LINK_AQUI/view?usp=sharing\n\nNosso código de vinculação é: *${salon.code}*\n\nTe esperamos lá!"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                setPackage("com.whatsapp")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback se não tiver WhatsApp
                                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(fallbackIntent, "Compartilhar via"))
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar App", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search Bar 🔍
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome, e-mail ou telefone...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Nenhum cliente vinculado ainda." else "Nenhum cliente correspondente.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredClients) { item ->
                        val hasReward = item.unredeemedStamps >= loyaltyRequired
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedClientForDetails = item },
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isBlocked) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                } else if (hasReward) {
                                    Color(0xFFE8F5E9)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (item.isBlocked) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                            } else if (hasReward) {
                                BorderStroke(1.5.dp, Color(0xFF2E7D32))
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.profile.photoUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = item.profile.photoUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier.size(50.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = item.profile.name.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.profile.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = item.profile.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        if (item.profile.phoneNumber.isNotEmpty()) {
                                            Text(
                                                text = item.profile.phoneNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    // Ações Rápidas (Chamada / WhatsApp)
                                    if (item.profile.phoneNumber.isNotEmpty()) {
                                        IconButton(onClick = {
                                            val cleanPhone = item.profile.phoneNumber.replace(Regex("[^0-9]"), "")
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                                            }
                                            context.startActivity(intent)
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = "WhatsApp",
                                                tint = Color(0xFF25D366)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Informações do Cartão Fidelidade
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Selos: ${item.unredeemedStamps} / $loyaltyRequired",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (hasReward) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasReward) {
                                        Surface(
                                            color = Color(0xFF2E7D32),
                                            contentColor = Color.White,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "🏆 PRÊMIO DISPONÍVEL",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val progress = (item.unredeemedStamps.toFloat() / loyaltyRequired.toFloat()).coerceAtMost(1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (hasReward) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                if (hasReward) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { clientToRedeem = item },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Celebration, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Resgatar Prêmio: $loyaltyReward")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (clientToRedeem != null) {
        val client = clientToRedeem!!
        AlertDialog(
            onDismissRequest = { clientToRedeem = null },
            icon = { Icon(Icons.Default.Celebration, null, tint = Color(0xFFFBC02D)) },
            title = { Text("Resgatar Prêmio Fidelidade") },
            text = {
                Text("Deseja resgatar o prêmio \"$loyaltyReward\" para o cliente ${client.profile.name}? Isso consumirá os $loyaltyRequired selos e resetará seu faturamento de fidelidade.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.redeemLoyaltyRewardForClient(client.profile.uid)
                        Toast.makeText(context, "Prêmio resgatado com sucesso!", Toast.LENGTH_SHORT).show()
                        clientToRedeem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirmar Resgate")
                }
            },
            dismissButton = {
                TextButton(onClick = { clientToRedeem = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (selectedClientForDetails != null) {
        val clientItem = selectedClientForDetails!!
        AlertDialog(
            onDismissRequest = { selectedClientForDetails = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (clientItem.profile.photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = clientItem.profile.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = clientItem.profile.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(clientItem.profile.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(clientItem.profile.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Status de Acesso:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        if (clientItem.isBlocked) {
                            Surface(
                                color = Color(0xFFFFEBEE),
                                contentColor = Color(0xFFC62828),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Acesso Bloqueado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                contentColor = Color(0xFF2E7D32),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Cadastro Ativo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Contact Info
                    if (clientItem.profile.phoneNumber.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(clientItem.profile.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Statistics Calculations
                    val totalServed = clientAppointmentsHistory.count { it.status == "SERVED" }
                    val totalCancelled = clientAppointmentsHistory.count { it.status == "CANCELLED" }
                    val totalAppointments = clientAppointmentsHistory.size
                    
                    Text("Estatísticas do Cliente", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Atendidos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text("$totalServed", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Cancelados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text("$totalCancelled", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text("$totalAppointments", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }

                    // Gráfico de Barra Dividida
                    if (totalServed > 0 || totalCancelled > 0) {
                        val totalServedF = totalServed.toFloat()
                        val totalCancelledF = totalCancelled.toFloat()
                        val ratioServed = totalServedF / (totalServedF + totalCancelledF)
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Relação Atendidos vs Cancelados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                if (totalServed > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(ratioServed.coerceAtLeast(0.05f))
                                            .background(Color(0xFF2E7D32))
                                    )
                                }
                                if (totalCancelled > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight((1f - ratioServed).coerceAtLeast(0.05f))
                                            .background(Color(0xFFC62828))
                                    )
                                }
                            }
                        }
                    }

                    // Histórico Recente
                    Text("Agendamentos Recentes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (loadingHistory) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (clientAppointmentsHistory.isEmpty()) {
                        Text("Sem histórico de agendamentos neste salão.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    } else {
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        clientAppointmentsHistory.take(5).forEach { appt ->
                            val apptDate = appt.date?.let { sdf.format(it) } ?: ""
                            val statusText = when (appt.status) {
                                "SERVED" -> "✓ Prestado"
                                "CANCELLED" -> "✗ Cancelado"
                                "CONFIRMED" -> "Confirmado"
                                "PENDING" -> "Pendente"
                                else -> appt.status
                            }
                            val statusColor = when (appt.status) {
                                "SERVED" -> Color(0xFF2E7D32)
                                "CANCELLED" -> Color(0xFFC62828)
                                "CONFIRMED" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(appt.serviceName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(apptDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Text(statusText, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = statusColor)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }

                    // Botão de Bloqueio/Desbloqueio
                    Spacer(modifier = Modifier.height(8.dp))
                    val isBlockedNew = !clientItem.isBlocked
                    Button(
                        onClick = {
                            viewModel.toggleClientBlockStatus(clientItem.profile.uid, isBlockedNew)
                            selectedClientForDetails = clientItem.copy(isBlocked = isBlockedNew)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (clientItem.isBlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (clientItem.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (clientItem.isBlocked) "Desbloquear Acesso Online" else "Bloquear Acesso Online")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedClientForDetails = null }) {
                    Text("Fechar")
                }
            }
        )
    }
}
