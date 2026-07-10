package com.example.agendei_pro.ui.screens

import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import kotlinx.coroutines.launch
import com.example.agendei_pro.core.repository.AppointmentRepository
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendei_pro.core.model.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientAppointmentsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userUid = auth.currentUser?.uid

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Próximos, 1 = Histórico
    
    var showCancellationBlockDialog by remember { mutableStateOf(false) }
    var cancellationBlockMessage by remember { mutableStateOf("") }
    
    var evaluatedAppointmentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showEvaluationDialog by remember { mutableStateOf(false) }
    var selectedApptForEvaluation by remember { mutableStateOf<Appointment?>(null) }
    var evaluationRating by remember { mutableIntStateOf(5) }
    var evaluationComment by remember { mutableStateOf("") }
    var isFavoriteChecked by remember { mutableStateOf(false) }

    LaunchedEffect(userUid) {
        if (userUid != null) {
            val registration = db.collection("appointments")
                .whereEqualTo("clientUid", userUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }
                    appointments = snapshot?.toObjects(Appointment::class.java) ?: emptyList()
                    isLoading = false
                }
            
            db.collection("reviews")
                .whereEqualTo("userId", userUid)
                .addSnapshotListener { reviewSnapshot, _ ->
                    if (reviewSnapshot != null) {
                        evaluatedAppointmentIds = reviewSnapshot.documents.mapNotNull { it.getString("appointmentId") }.toSet()
                    }
                }
        } else {
            isLoading = false
        }
    }

    val today = Date()
    val upcomingAppointments = remember(appointments) {
        appointments.filter { appt ->
            val apptDate = appt.date
            apptDate != null && !apptDate.before(today) && appt.status != "CANCELLED"
        }.sortedBy { it.date }
    }

    val pastAppointments = remember(appointments) {
        appointments.filter { appt ->
            val apptDate = appt.date
            apptDate == null || apptDate.before(today) || appt.status == "CANCELLED"
        }.sortedByDescending { it.date }
    }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Agendamentos", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Próximos (${upcomingAppointments.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Histórico", fontWeight = FontWeight.Bold) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val listToDisplay = if (selectedTab == 0) upcomingAppointments else pastAppointments

                if (listToDisplay.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedTab == 0) "Nenhum agendamento futuro." else "Nenhum agendamento no histórico.",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(listToDisplay, key = { it.id }) { appt ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = appt.serviceName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = "Profissional: ${appt.professionalName.ifBlank { "Tanto faz" }}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Badge do Status
                                        val (statusText, badgeColor) = when (appt.status) {
                                            "CONFIRMED" -> "Confirmado" to Color(0xFF4CAF50)
                                            "PENDING" -> "Pendente" to Color(0xFFFF9800)
                                            "CANCELLED" -> {
                                                val txt = when (appt.cancelledBy) {
                                                    "CLIENT" -> "Cancelado por você"
                                                    "SALON" -> "Cancelado pelo salão"
                                                    else -> "Cancelado"
                                                }
                                                txt to Color(0xFFE53935)
                                            }
                                            "BLOCKED" -> "Bloqueado" to Color(0xFF757575)
                                            else -> appt.status to MaterialTheme.colorScheme.primary
                                        }

                                        Surface(
                                            color = badgeColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Data e Hora:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = appt.date?.let { dateFormatter.format(it) } ?: "Sem data",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Text(
                                            text = if (appt.loyaltyRedeemed) "Grátis (Fidelidade)" else currencyFormatter.format(appt.servicePrice),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = if (appt.loyaltyRedeemed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Botões de Ação para Agendamentos Futuros
                                    if (selectedTab == 0) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Adicionar ao Calendário
                                            Button(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                                        data = CalendarContract.Events.CONTENT_URI
                                                        putExtra(CalendarContract.Events.TITLE, "${appt.serviceName} - Agendei")
                                                        putExtra(CalendarContract.Events.DESCRIPTION, "Profissional: ${appt.professionalName}")
                                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, appt.date?.time ?: Date().time)
                                                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, (appt.date?.time ?: Date().time) + 30 * 60 * 1000)
                                                    }
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.weight(1.2f),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Calendário", fontSize = 13.sp)
                                            }

                                            // Cancelar Agendamento
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        try {
                                                            val salonSnap = db.collection("salons").document(appt.salonId).get().await()
                                                            val minCancelDelay = salonSnap.getLong("minCancelDelayHours") ?: 0
                                                            val salonPhone = salonSnap.getString("phoneNumber") ?: ""

                                                            val apptTime = appt.date?.time ?: 0L
                                                            val nowTime = System.currentTimeMillis()
                                                            val diffHours = (apptTime - nowTime).toDouble() / (1000.0 * 60.0 * 60.0)

                                                            if (diffHours < minCancelDelay) {
                                                                cancellationBlockMessage = "Não é possível cancelar este agendamento pelo aplicativo devido ao tempo limite de antecedência de $minCancelDelay horas.\n\nPor favor, entre em contato com o salão no telefone/WhatsApp $salonPhone para solicitar a desmarcação."
                                                                showCancellationBlockDialog = true
                                                            } else {
                                                                val result = AppointmentRepository().updateAppointmentStatus(appt.id, "CANCELLED")
                                                                if (result.isSuccess) {
                                                                    Toast.makeText(context, "Agendamento Cancelado!", Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    Toast.makeText(context, "Erro ao cancelar agendamento", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            val result = AppointmentRepository().updateAppointmentStatus(appt.id, "CANCELLED")
                                                            if (result.isSuccess) {
                                                                Toast.makeText(context, "Agendamento Cancelado!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cancelar", fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    // Botões de Ação para Agendamentos Passados (Avaliar)
                                    if (selectedTab == 1 && appt.status == "CONFIRMED" && appt.id !in evaluatedAppointmentIds && appt.professionalId.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                selectedApptForEvaluation = appt
                                                evaluationRating = 5
                                                evaluationComment = ""
                                                showEvaluationDialog = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                        ) {
                                            Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Avaliar Profissional", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

    if (showEvaluationDialog && selectedApptForEvaluation != null) {
        AlertDialog(
            onDismissRequest = { showEvaluationDialog = false },
            title = { Text("Avaliar Atendimento ⭐", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Como foi seu atendimento com ${selectedApptForEvaluation?.professionalName}?",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { evaluationRating = i },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$i Estrelas",
                                    tint = if (i <= evaluationRating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = evaluationComment,
                        onValueChange = { evaluationComment = it },
                        label = { Text("Escreva um comentário (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Checkbox(
                            checked = isFavoriteChecked,
                            onCheckedChange = { isFavoriteChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tornar este colaborador meu favorito",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val appt = selectedApptForEvaluation ?: return@Button
                        val reviewId = db.collection("reviews").document().id
                        val newReview = mapOf(
                            "id" to reviewId,
                            "appointmentId" to appt.id,
                            "salonId" to appt.salonId,
                            "professionalId" to appt.professionalId,
                            "userId" to (userUid ?: ""),
                            "userName" to (auth.currentUser?.displayName ?: "Cliente Agendei"),
                            "rating" to evaluationRating,
                            "comment" to evaluationComment,
                            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        
                        db.collection("reviews").document(reviewId).set(newReview)
                            .addOnSuccessListener {
                                if (isFavoriteChecked) {
                                    db.collection("salons").document(appt.salonId)
                                        .collection("professionals").document(appt.professionalId)
                                        .get()
                                        .addOnSuccessListener { proSnap ->
                                            val proPhotoUrl = proSnap.getString("photoUrl")
                                            val proName = proSnap.getString("name") ?: appt.professionalName
                                            
                                            db.collection("user_bindings")
                                                .whereEqualTo("userId", userUid ?: "")
                                                .whereEqualTo("salonId", appt.salonId)
                                                .get()
                                                .addOnSuccessListener { bindingSnap ->
                                                    val bindingDoc = bindingSnap.documents.firstOrNull()
                                                    if (bindingDoc != null) {
                                                        bindingDoc.reference.update(mapOf(
                                                            "favoriteProfessionalId" to appt.professionalId,
                                                            "favoriteProfessionalName" to proName,
                                                            "favoriteProfessionalPhotoUrl" to proPhotoUrl
                                                        ))
                                                    }
                                                }
                                        }
                                }
                                Toast.makeText(context, "Avaliação enviada com sucesso!", Toast.LENGTH_SHORT).show()
                                showEvaluationDialog = false
                                evaluationComment = ""
                                evaluationRating = 5
                                isFavoriteChecked = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Erro ao enviar avaliação.", Toast.LENGTH_SHORT).show()
                            }
                    }
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEvaluationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showCancellationBlockDialog) {
        AlertDialog(
            onDismissRequest = { showCancellationBlockDialog = false },
            title = { Text("Cancelamento Indisponível ⚠️") },
            text = { Text(cancellationBlockMessage) },
            confirmButton = {
                Button(
                    onClick = { showCancellationBlockDialog = false }
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}
