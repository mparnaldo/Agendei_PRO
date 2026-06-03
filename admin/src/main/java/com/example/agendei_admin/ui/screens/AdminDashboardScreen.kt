package com.example.agendei_admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToSalons: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMetrics: () -> Unit,
    onNavigateToBroadcast: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Painel de Controle Imperial") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            AdminCard("Gerenciar Salões", "Ver trials, assinaturas e presentear", Icons.Default.Store, onNavigateToSalons)
            Spacer(modifier = Modifier.height(16.dp))
            AdminCard("Lista de Clientes", "Ver todos os usuários do app cliente", Icons.Default.People, onNavigateToClients)
            Spacer(modifier = Modifier.height(16.dp))
            AdminCard("Métricas & Finanças", "Acompanhar MRR, Churn, LTV e uso do sistema", Icons.Default.Analytics, onNavigateToMetrics)
            Spacer(modifier = Modifier.height(16.dp))
            AdminCard("Comunicado Global", "Disparar push em massa e alertas no app", Icons.Default.Campaign, onNavigateToBroadcast)
            Spacer(modifier = Modifier.height(16.dp))
            AdminCard("Ajustes Globais", "Preço da assinatura e avisos", Icons.Default.Settings, onNavigateToSettings)
        }
    }
}

@Composable
fun AdminCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(24.dp)) {
            Icon(icon, null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
