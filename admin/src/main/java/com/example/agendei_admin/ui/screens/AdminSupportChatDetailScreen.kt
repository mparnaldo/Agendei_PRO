package com.example.agendei_admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class AdminSupportMessage(
    val id: String = "",
    val sender: String = "", // USER, BOT, ADMIN
    val text: String = "",
    val timestamp: Date? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSupportChatDetailScreen(
    chatId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var messages by remember { mutableStateOf<List<AdminSupportMessage>>(emptyList()) }
    var chatInfo by remember { mutableStateOf<SupportTicket?>(null) }
    var replyText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 1. Escuta as informações do chamado e zera as mensagens não lidas
    LaunchedEffect(chatId) {
        db.collection("support_chats").document(chatId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    chatInfo = snapshot.toObject(SupportTicket::class.java)
                    val unread = snapshot.getLong("unreadCount") ?: 0L
                    if (unread > 0L) {
                        db.collection("support_chats").document(chatId).update("unreadCount", 0)
                    }
                }
            }
    }

    // 2. Escuta as mensagens
    LaunchedEffect(chatId) {
        db.collection("support_chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.documents.mapNotNull { doc ->
                        val msg = doc.toObject(AdminSupportMessage::class.java)
                        msg?.copy(id = doc.id)
                    }
                }
            }
    }

    // 3. Auto-scroll ao receber mensagens
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chatInfo?.userName ?: "Chat de Suporte", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(chatInfo?.userEmail ?: "Carregando...", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (chatInfo != null) {
                        if (chatInfo?.status != "RESOLVED") {
                            TextButton(onClick = {
                                db.collection("support_chats").document(chatId)
                                    .update("status", "RESOLVED")
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Chamado finalizado com sucesso!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                            }) {
                                Text("Finalizar", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = {
                                db.collection("support_chats").document(chatId)
                                    .update("status", "PENDING_HUMAN")
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Chamado reaberto!", Toast.LENGTH_SHORT).show()
                                    }
                            }) {
                                Text("Reabrir", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
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
            // Histórico de Conversa
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.sender == "ADMIN"
                    val bubbleColor = if (isMe) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else if (msg.sender == "BOT") {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    val contentColor = if (isMe) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val align = if (isMe) Alignment.End else Alignment.Start
                    val senderName = when (msg.sender) {
                        "ADMIN" -> "Você (Suporte)"
                        "USER" -> chatInfo?.userName ?: "Usuário"
                        else -> "Assistente Virtual 🤖"
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = align
                    ) {
                        Text(
                            text = senderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    )
                                )
                                .background(bubbleColor)
                                .padding(12.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = contentColor,
                                fontSize = 15.sp
                            )
                        }
                        msg.timestamp?.let {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            }

            // Barra de Digitação ou Mensagem de Resolvido
            if (chatInfo?.status == "RESOLVED") {
                Surface(
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Atendimento Concluído ✓ (Modo Leitura)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Surface(
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { if (it.length <= 250) replyText = it },
                            placeholder = { Text("Digite a resposta para o usuário...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            supportingText = {
                                Text(
                                    text = "${replyText.length}/250",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (replyText.isNotBlank()) {
                                            val newMsg = mapOf(
                                                "sender" to "ADMIN",
                                                "text" to replyText,
                                                "timestamp" to FieldValue.serverTimestamp()
                                            )
                                            db.collection("support_chats").document(chatId)
                                                .collection("messages").add(newMsg)
                                            db.collection("support_chats").document(chatId).update(
                                                mapOf(
                                                    "lastMessageText" to replyText,
                                                    "lastMessageTime" to FieldValue.serverTimestamp()
                                                )
                                            )
                                            replyText = ""
                                        }
                                    },
                                    enabled = replyText.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Enviar",
                                        tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
