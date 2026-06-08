package com.example.agendei_pro.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.agendei_pro.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalonClientsScreen(
    viewModel: MainViewModel,
    loyaltyRequired: Int,
    loyaltyReward: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clients by viewModel.salonClients.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    var clientToRedeem by remember { mutableStateOf<MainViewModel.SalonClientItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadSalonClients()
    }

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) {
            clients
        } else {
            clients.filter {
                it.profile.name.contains(searchQuery, ignoreCase = true) ||
                it.profile.email.contains(searchQuery, ignoreCase = true) ||
                it.profile.phoneNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carteira de Clientes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search Bar 🔍
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome, e-mail ou telefone...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Nenhum cliente vinculado ainda." else "Nenhum cliente correspondente.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredClients) { item ->
                        val hasReward = item.unredeemedStamps >= loyaltyRequired
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasReward) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (hasReward) BorderStroke(1.5.dp, Color(0xFF2E7D32)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.profile.photoUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = item.profile.photoUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier.size(50.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = item.profile.name.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.profile.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = item.profile.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        if (item.profile.phoneNumber.isNotEmpty()) {
                                            Text(
                                                text = item.profile.phoneNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    // Ações Rápidas (Chamada / WhatsApp)
                                    if (item.profile.phoneNumber.isNotEmpty()) {
                                        IconButton(onClick = {
                                            val cleanPhone = item.profile.phoneNumber.replace(Regex("[^0-9]"), "")
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                                            }
                                            context.startActivity(intent)
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = "WhatsApp",
                                                tint = Color(0xFF25D366)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Informações do Cartão Fidelidade
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Selos: ${item.unredeemedStamps} / $loyaltyRequired",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (hasReward) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasReward) {
                                        Surface(
                                            color = Color(0xFF2E7D32),
                                            contentColor = Color.White,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "🏆 PRÊMIO DISPONÍVEL",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val progress = (item.unredeemedStamps.toFloat() / loyaltyRequired.toFloat()).coerceAtMost(1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (hasReward) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                if (hasReward) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { clientToRedeem = item },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Celebration, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Resgatar Prêmio: $loyaltyReward")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (clientToRedeem != null) {
        val client = clientToRedeem!!
        AlertDialog(
            onDismissRequest = { clientToRedeem = null },
            icon = { Icon(Icons.Default.Celebration, null, tint = Color(0xFFFBC02D)) },
            title = { Text("Resgatar Prêmio Fidelidade") },
            text = {
                Text("Deseja resgatar o prêmio \"$loyaltyReward\" para o cliente ${client.profile.name}? Isso consumirá os $loyaltyRequired selos e resetará seu faturamento de fidelidade.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.redeemLoyaltyRewardForClient(client.profile.uid)
                        Toast.makeText(context, "Prêmio resgatado com sucesso!", Toast.LENGTH_SHORT).show()
                        clientToRedeem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirmar Resgate")
                }
            },
            dismissButton = {
                TextButton(onClick = { clientToRedeem = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
