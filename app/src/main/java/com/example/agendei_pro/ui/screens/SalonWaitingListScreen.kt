package com.example.agendei_pro.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendei_pro.core.model.WaitingEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalonWaitingListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val salonId = auth.currentUser?.uid

    var waitingEntries by remember { mutableStateOf<List<WaitingEntry>>(emptyList()) }
    var salonName by remember { mutableStateOf("Nosso Salão") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(salonId) {
        if (salonId != null) {
            // Buscar Nome do Salão
            db.collection("salons").document(salonId)
                .get()
                .addOnSuccessListener { doc ->
                    salonName = doc.getString("name") ?: "Nosso Salão"
                }

            // Ouvir Fila de Espera em Tempo Real
            val registration = db.collection("waiting_list")
                .whereEqualTo("salonId", salonId)
                .whereEqualTo("status", "WAITING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }
                    waitingEntries = snapshot?.toObjects(WaitingEntry::class.java)
                        ?.sortedBy { it.date ?: Date() } ?: emptyList()
                    isLoading = false
                }
            
            // Remover snapshot listener ao sair
            // (Na realidade do Compose, o snapshot é encerrado na chamada do Lifecycle ou por DisposableEffect, mas para simplificar faremos no disposable)
        } else {
            isLoading = false
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy (EEEE)", Locale("pt", "BR")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fila de Espera", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Clientes aguardando vagas:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                if (waitingEntries.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum cliente na Fila de Espera.",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(waitingEntries, key = { it.id }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.clientName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Serviço: ${entry.serviceName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val targetPro = if (entry.professionalId == "ANY" || entry.professionalId.isBlank()) {
                                            "Qualquer Profissional"
                                        } else {
                                            "Com: ${entry.professionalName}"
                                        }
                                        Text(
                                            text = targetPro,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Data Pretendida: ${entry.date?.let { dateFormatter.format(it) } ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Botões de Ações
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Ação WhatsApp
                                        IconButton(
                                            onClick = {
                                                val cleanPhone = entry.clientPhone.replace("[^0-9]".toRegex(), "")
                                                val formattedPhone = if (cleanPhone.startsWith("55")) cleanPhone else "55$cleanPhone"
                                                val dateStr = entry.date?.let { SimpleDateFormat("dd/MM", Locale.getDefault()).format(it) } ?: ""
                                                
                                                val message = "Olá, ${entry.clientName}! Sou do $salonName. Vi que você está na nossa fila de espera para o dia $dateStr (${entry.serviceName}). Um horário se liberou! Gostaria de agendar?"
                                                val urlEncoded = URLEncoder.encode(message, "UTF-8")
                                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$urlEncoded")
                                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                                context.startActivity(intent)
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFE8F5E9))
                                        ) {
                                            Icon(
                                                Icons.Default.Phone,
                                                "WhatsApp",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Confirmar Agendado
                                        IconButton(
                                            onClick = {
                                                db.collection("waiting_list").document(entry.id)
                                                    .update("status", "COMPLETED")
                                                    .addOnSuccessListener {
                                                        Toast.makeText(context, "Concluído!", Toast.LENGTH_SHORT).show()
                                                    }
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFE3F2FD))
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                "Confirmar",
                                                tint = Color(0xFF1976D2),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Deletar da Fila
                                        IconButton(
                                            onClick = {
                                                db.collection("waiting_list").document(entry.id)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        Toast.makeText(context, "Removido!", Toast.LENGTH_SHORT).show()
                                                    }
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                "Excluir",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
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
    }
}
