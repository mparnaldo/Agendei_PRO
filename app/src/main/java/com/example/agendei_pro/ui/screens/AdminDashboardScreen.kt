package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agendei_pro.core.model.Salon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    salons: List<Salon>,
    onGiftPremium: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel do Ditador (Admin)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = "Total de Salões: ${salons.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(salons) { salon ->
                    SalonAdminCard(salon, onGiftPremium)
                }
            }
        }
    }
}

@Composable
fun SalonAdminCard(salon: Salon, onGiftPremium: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = salon.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Código: ${salon.code}", style = MaterialTheme.typography.labelSmall)
                
                if (salon.isSubscribed) {
                    AssistChip(
                        onClick = {},
                        label = { Text("PREMIUM") },
                        leadingIcon = { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700)) }
                    )
                } else {
                    Text(
                        text = "Trial: ${salon.id} (Status pendente)", // Aqui poderíamos mostrar os dias
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!salon.isSubscribed) {
                Button(
                    onClick = { onGiftPremium(salon.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.CardGiftcard, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Presentear")
                }
            }
        }
    }
}
