package com.example.agendei_admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBroadcastScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetTopic by remember { mutableStateOf("salons") } // salons ou clients
    var displayType by remember { mutableStateOf("BANNER") } // BANNER, POPUP, NONE (Apenas Push)
    var duration by remember { mutableStateOf("PERMANENT") } // ONCE, DAY, WEEK, PERMANENT
    var isLoading by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enviar Comunicado (Broadcast)") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Envie uma notificação push em massa para os usuários da plataforma.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo de Título
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título da Notificação") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Campaign, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Conteúdo da Mensagem
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Mensagem / Comunicado") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selecionar Destinatários
            Text("Destinatários", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                FilterChip(
                    selected = targetTopic == "salons",
                    onClick = { targetTopic = "salons" },
                    label = { Text("Salões (PRO)") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = targetTopic == "clients",
                    onClick = { targetTopic = "clients" },
                    label = { Text("Clientes") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selecionar Formato de Exibição
            Text("Formato de Exibição no App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = displayType == "NONE",
                    onClick = { displayType = "NONE" },
                    label = { Text("Apenas Push") }
                )
                FilterChip(
                    selected = displayType == "BANNER",
                    onClick = { displayType = "BANNER" },
                    label = { Text("Banner Topo") }
                )
                FilterChip(
                    selected = displayType == "POPUP",
                    onClick = { displayType = "POPUP" },
                    label = { Text("Pop-up Modal") }
                )
            }

            if (displayType != "NONE") {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Duração da Exibição", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = duration == "ONCE",
                        onClick = { duration = "ONCE" },
                        label = { Text("Única") }
                    )
                    FilterChip(
                        selected = duration == "DAY",
                        onClick = { duration = "DAY" },
                        label = { Text("1 Dia") }
                    )
                    FilterChip(
                        selected = duration == "WEEK",
                        onClick = { duration = "WEEK" },
                        label = { Text("1 Semana") }
                    )
                    FilterChip(
                        selected = duration == "PERMANENT",
                        onClick = { duration = "PERMANENT" },
                        label = { Text("Permanente") }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (title.isBlank() || message.isBlank()) {
                        Toast.makeText(context, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true

                    val batch = db.batch()
                    
                    // 1. Criar o documento de Broadcast (disparará a Cloud Function)
                    val broadcastRef = db.collection("broadcasts").document()
                    val broadcastData = mapOf(
                        "id" to broadcastRef.id,
                        "title" to title,
                        "message" to message,
                        "target" to targetTopic,
                        "timestamp" to Date()
                    )
                    batch.set(broadcastRef, broadcastData)

                    // 2. Gravar no canal específico (announcement_salons ou announcement_clients)
                    val configDocName = if (targetTopic == "salons") "announcement_salons" else "announcement_clients"
                    val configRef = db.collection("config").document(configDocName)

                    if (displayType != "NONE") {
                        val expiresAt = when (duration) {
                            "DAY" -> Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
                            "WEEK" -> Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
                            else -> null
                        }
                        val announcementData = mapOf(
                            "id" to broadcastRef.id,
                            "title" to title,
                            "message" to message,
                            "displayType" to displayType,
                            "expiresAt" to expiresAt,
                            "duration" to duration
                        )
                        batch.set(configRef, announcementData)
                    } else {
                        // Remove o banner/popup anterior para que apenas a notificação seja enviada
                        batch.delete(configRef)
                    }

                    batch.commit().addOnSuccessListener {
                        isLoading = false
                        Toast.makeText(context, "Mensagem enviada com sucesso!", Toast.LENGTH_SHORT).show()
                        title = ""
                        message = ""
                    }.addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(context, "Erro ao enviar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && title.isNotBlank() && message.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Send, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Comunicado")
                }
            }
        }
    }
}
