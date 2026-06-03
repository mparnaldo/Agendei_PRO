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
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            items(salons, key = { it.id }) { salon ->
                val ownerProfile = userProfiles[salon.ownerUid]
                val remainingDays = salonRepo.getRemainingTrialDays(salon)
                
                SalonDetailCard(
                    salon = salon,
                    ownerEmail = ownerProfile?.email ?: "Carregando...",
                    remainingDays = remainingDays,
                    onTogglePremium = {
                        val newState = !salon.isSubscribed
                        // USAMOS O ID GARANTIDO AQUI!
                        db.collection("salons").document(salon.id)
                            .update("isSubscribed", newState)
                            .addOnSuccessListener {
                                val msg = if (newState) "Premium ATIVADO!" else "Premium REMOVIDO!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "ERRO: ${e.message}", Toast.LENGTH_LONG).show()
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
                
                val statusText = if (salon.isSubscribed) "PREMIUM" else if (remainingDays > 0) "TRIAL" else "EXPIRADO"
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
                        containerColor = if (salon.isSubscribed) Color(0xFFE91E63) else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.CardGiftcard, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (salon.isSubscribed) "Remover Premium" else "Dar Premium Vitalício")
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
