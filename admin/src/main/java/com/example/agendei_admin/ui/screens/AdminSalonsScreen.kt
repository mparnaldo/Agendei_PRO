package com.example.agendei_admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agendei_pro.core.model.Salon
import com.example.agendei_pro.core.model.UserProfile
import com.example.agendei_pro.core.repository.SalonRepository
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSalonsScreen(onBack: () -> Unit) {
    var salons by remember { mutableStateOf<List<Salon>>(emptyList()) }
    var userProfiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    val db = FirebaseFirestore.getInstance()
    val salonRepo = remember { SalonRepository() }
    val context = LocalContext.current
    var editingSalonPlan by remember { mutableStateOf<Salon?>(null) }

    LaunchedEffect(Unit) {
        // Buscamos os documentos e mapeamos o ID manualmente para garantir que nunca falhe
        db.collection("salons").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { doc ->
                val s = doc.toObject(Salon::class.java)
                s?.copy(id = doc.id) // GARANTE QUE O ID É O DO DOCUMENTO! 🎯
            } ?: emptyList()
            salons = list
            
            list.forEach { salon ->
                db.collection("profiles").document(salon.ownerUid).get().addOnSuccessListener { doc ->
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        userProfiles = userProfiles + (salon.ownerUid to profile)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestão de Salões") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(salons, key = { it.id }) { salon ->
                    val ownerProfile = userProfiles[salon.ownerUid]
                    val remainingDays = salonRepo.getRemainingTrialDays(salon)
                    
                    SalonDetailCard(
                        salon = salon,
                        ownerEmail = ownerProfile?.email ?: "Carregando...",
                        remainingDays = remainingDays,
                        onTogglePremium = {
                            editingSalonPlan = salon
                        }
                    )
                }
            }

            if (editingSalonPlan != null) {
                val salon = editingSalonPlan!!
                var selectedPlan by remember { mutableStateOf(salon.subscriptionPlan) }
                var maxProfs by remember { mutableStateOf(salon.maxProfessionals) }

                AlertDialog(
                    onDismissRequest = { editingSalonPlan = null },
                    title = { Text("Mudar Plano - ${salon.name}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Selecione o novo plano de assinatura:")
                            
                            val plans = listOf(
                                Triple("TRIAL", 2, "Trial/Gratuito (2 Profs)"),
                                Triple("BRONZE", 2, "Bronze (2 Profs - R$ 110,00)"),
                                Triple("PRATA", 5, "Prata (5 Profs - R$ 150,00)"),
                                Triple("OURO", 10, "Ouro (10 Profs - R$ 200,00)")
                            )

                            plans.forEach { (planName, limit, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedPlan == planName,
                                        onClick = {
                                            selectedPlan = planName
                                            maxProfs = limit
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                db.collection("salons").document(salon.id)
                                    .update(mapOf(
                                        "subscriptionPlan" to selectedPlan,
                                        "maxProfessionals" to maxProfs,
                                        "isSubscribed" to (selectedPlan != "TRIAL")
                                    ))
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Plano atualizado!", Toast.LENGTH_SHORT).show()
                                        editingSalonPlan = null
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        ) {
                            Text("Salvar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingSalonPlan = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SalonDetailCard(
    salon: Salon,
    ownerEmail: String,
    remainingDays: Int,
    onTogglePremium: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = salon.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                val statusText = if (salon.isSubscribed) salon.subscriptionPlan else if (remainingDays > 0) "TRIAL" else "EXPIRADO"
                val statusColor = if (salon.isSubscribed) Color(0xFF4CAF50) else if (remainingDays > 0) Color(0xFF2196F3) else Color(0xFFF44336)
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(Icons.Default.Email, ownerEmail)
            InfoRow(Icons.Default.Work, salon.segment)
            InfoRow(Icons.Default.People, "Plano: ${salon.subscriptionPlan} (Limite: ${salon.maxProfessionals} Prof.)")
            
            if (!salon.isSubscribed) {
                val trialDateStr = salon.trialStartDate?.let { 
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) 
                } ?: "N/A"
                InfoRow(Icons.Default.Event, "Início: $trialDateStr (${remainingDays} dias restantes)")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onTogglePremium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.CardGiftcard, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gerenciar Plano")
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
