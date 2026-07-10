package com.example.agendei_admin.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class SupportTicket(
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userType: String = "", // CLIENT, SALON
    val status: String = "BOT", // BOT, PENDING_HUMAN, RESOLVED
    val lastMessageText: String = "",
    val lastMessageTime: Date? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSupportScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTabIndex) {
        isLoading = true
        val statusFilter = when (selectedTabIndex) {
            0 -> "PENDING_HUMAN"
            1 -> "BOT"
            else -> "RESOLVED"
        }

        db.collection("support_chats")
            .whereEqualTo("status", statusFilter)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(SupportTicket::class.java) }
                    tickets = list.sortedByDescending { it.lastMessageTime }
                } else {
                    tickets = emptyList()
                }
                isLoading = false
            }
    }

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
            // Barra de Abas do Suporte
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Pendentes") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Fila do Bot") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Resolvidos") }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (tickets.isEmpty()) {
                val emptyTitle = when (selectedTabIndex) {
                    0 -> "Nenhum chamado pendente!"
                    1 -> "Nenhum chat em triagem com o Bot."
                    else -> "Nenhum histórico de chamado resolvido."
                }
                val emptyDesc = when (selectedTabIndex) {
                    0 -> "Todos os salões e clientes foram atendidos."
                    1 -> "O assistente virtual está ocioso no momento."
                    else -> "Os chamados resolvidos aparecerão arquivados aqui."
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
                                    Text(
                                        text = ticket.userName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                Spacer(modifier = Modifier.height(12.dp))
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
