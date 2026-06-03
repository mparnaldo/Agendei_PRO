package com.example.agendei_pro.ui.screens

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalonPromotionScreen(onNavigateBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var displayType by remember { mutableStateOf("BANNER") } // BANNER, POPUP, NONE (Apenas Push)
    var duration by remember { mutableStateOf("PERMANENT") } // ONCE, DAY, WEEK, PERMANENT
    var isLoading by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val salonId = auth.currentUser?.uid ?: ""
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promover Salão / Aviso") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
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
                text = "Envie uma mensagem promocional ou comunicado importante para todos os seus clientes vinculados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo de Título
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título da Promoção/Comunicado") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Campaign, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Mensagem
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Mensagem da Promoção") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selecionar Tipo de Exibição
            Text("Formato de Exibição no Cliente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    label = { Text("Banner no Topo") }
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
                    if (salonId.isBlank()) {
                        Toast.makeText(context, "Erro: Salão não autenticado!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true

                    val batch = db.batch()
                    val promoRef = db.collection("salons").document(salonId).collection("promotions").document()
                    val activeRef = db.collection("salons").document(salonId).collection("promotions").document("active")

                    val expiresAt = when (duration) {
                        "DAY" -> Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
                        "WEEK" -> Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
                        else -> null
                    }

                    val promoData = mapOf(
                        "id" to promoRef.id,
                        "title" to title,
                        "message" to message,
                        "displayType" to displayType,
                        "duration" to duration,
                        "expiresAt" to expiresAt,
                        "timestamp" to Date()
                    )

                    batch.set(promoRef, promoData)
                    if (displayType != "NONE") {
                        batch.set(activeRef, promoData)
                    } else {
                        batch.delete(activeRef)
                    }

                    batch.commit().addOnSuccessListener {
                        isLoading = false
                        Toast.makeText(context, "Campanha enviada com sucesso!", Toast.LENGTH_SHORT).show()
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
                    Text("Enviar Campanha")
                }
            }
        }
    }
}
