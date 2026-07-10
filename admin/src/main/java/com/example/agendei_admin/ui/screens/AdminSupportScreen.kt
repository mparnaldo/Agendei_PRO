package com.example.agendei_admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class SupportTicket(
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userType: String = "", // CLIENT, SALON
    val status: String = "BOT", // BOT, PENDING_HUMAN, RESOLVED
    val lastMessageText: String = "",
    val lastMessageTime: Date? = null,
    val unreadCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSupportScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var allTickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // 1. Escuta em tempo real TODOS os chamados para cálculo dos badges nas abas
    LaunchedEffect(Unit) {
        isLoading = true
        db.collection("support_chats")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allTickets = snapshot.documents.mapNotNull { it.toObject(SupportTicket::class.java) }
                } else {
                    allTickets = emptyList()
                }
                isLoading = false
            }
    }

    // 2. Filtra e ordena os chamados localmente com base na aba ativa
    val tickets = remember(allTickets, selectedTabIndex) {
        val filtered = when (selectedTabIndex) {
            0 -> allTickets.filter { it.userType == "SALON" && it.status != "RESOLVED" }
            1 -> allTickets.filter { it.userType == "CLIENT" && it.status != "RESOLVED" }
            else -> allTickets.filter { it.status == "RESOLVED" }
        }
        filtered.sortedByDescending { it.lastMessageTime }
    }

    // 3. Calcula os contadores para exibir nos rótulos das abas
    val salonActiveCount = allTickets.count { it.userType == "SALON" && it.status != "RESOLVED" }
    val salonUnreadCount = allTickets.filter { it.userType == "SALON" && it.status != "RESOLVED" }.sumOf { it.unreadCount }

    val clientActiveCount = allTickets.count { it.userType == "CLIENT" && it.status != "RESOLVED" }
    val clientUnreadCount = allTickets.filter { it.userType == "CLIENT" && it.status != "RESOLVED" }.sumOf { it.unreadCount }

    val resolvedCount = allTickets.count { it.status == "RESOLVED" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suporte Técnico 🙋‍♂️") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de Abas do Suporte com Badges
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Salões ($salonActiveCount)")
                            if (salonUnreadCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "$salonUnreadCount ✉",
                                        color = MaterialTheme.colorScheme.onError,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Clientes ($clientActiveCount)")
                            if (clientUnreadCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "$clientUnreadCount ✉",
                                        color = MaterialTheme.colorScheme.onError,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text("Resolvidos ($resolvedCount)")
                    }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (tickets.isEmpty()) {
                val emptyTitle = when (selectedTabIndex) {
                    0 -> "Nenhum chamado de Salão ativo!"
                    1 -> "Nenhum chamado de Cliente ativo!"
                    else -> "Nenhum histórico de chamado resolvido."
                }
                val emptyDesc = when (selectedTabIndex) {
                    0 -> "Todos os proprietários de salões estão atendidos e sem dúvidas."
                    1 -> "Todos os usuários clientes estão com suas dúvidas respondidas."
                    else -> "Os chamados finalizados e concluídos aparecerão aqui."
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = emptyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = emptyDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tickets) { ticket ->
                        val isSalon = ticket.userType == "SALON"
                        val badgeColor = if (isSalon) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        val badgeText = if (isSalon) Color(0xFF2E7D32) else Color(0xFFE65100)
                        val typeLabel = if (isSalon) "SALÃO" else "CLIENTE"

                        val statusLabel = if (ticket.status == "BOT") "🤖 Triagem Bot" else "🙋‍♂️ Suporte Humano"
                        val statusColor = if (ticket.status == "BOT") MaterialTheme.colorScheme.secondaryContainer else Color(0xFFFFF9C4)
                        val statusTextColor = if (ticket.status == "BOT") MaterialTheme.colorScheme.onSecondaryContainer else Color(0xFFF57F17)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToChat(ticket.userId) },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = ticket.userName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        if (ticket.unreadCount > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.error,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "${ticket.unreadCount} novas",
                                                    color = MaterialTheme.colorScheme.onError,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        color = badgeColor,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = typeLabel,
                                            color = badgeText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = ticket.userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Status Badge (Bot vs Humano)
                                if (ticket.status != "RESOLVED") {
                                    Surface(
                                        color = statusColor,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            color = statusTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                Text(
                                    text = ticket.lastMessageText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                ticket.lastMessageTime?.let {
                                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Atualizado em: $dateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
