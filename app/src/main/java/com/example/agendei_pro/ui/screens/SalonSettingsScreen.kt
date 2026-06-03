package com.example.agendei_pro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SalonSettingsScreen(
    currentName: String,
    currentOpening: String,
    currentClosing: String,
    currentBreakStart: String,
    currentBreakEnd: String,
    currentDays: List<Int>,
    currentAutoAccept: Boolean,
    currentLogoUrl: String?,
    currentLogoShape: String,
    currentSegment: String,
    currentHasLoyalty: Boolean,
    currentLoyaltyRequired: Int,
    currentLoyaltyReward: String,
    uploadProgress: Int?,
    onSave: (String, String, String, String, String, List<Int>, Boolean, String, String, Boolean, Int, String) -> Unit,
    onLogoSelected: (Uri) -> Unit,
    onRemoveLogo: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var opening by remember { mutableStateOf(currentOpening) }
    var closing by remember { mutableStateOf(currentClosing) }
    var breakStart by remember { mutableStateOf(currentBreakStart) }
    var breakEnd by remember { mutableStateOf(currentBreakEnd) }
    var selectedDays by remember { mutableStateOf(currentDays) }
    var autoAccept by remember { mutableStateOf(currentAutoAccept) }
    var logoShape by remember { mutableStateOf(currentLogoShape) }
    var segment by remember { mutableStateOf(currentSegment) }
    var hasLoyalty by remember { mutableStateOf(currentHasLoyalty) }
    var loyaltyRequired by remember { mutableStateOf(currentLoyaltyRequired.toString()) }
    var loyaltyReward by remember { mutableStateOf(currentLoyaltyReward) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onLogoSelected(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajustes do Salão") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
            
            // Logo Config 📸
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Formato da Logo", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.padding(vertical = 8.dp)) {
                        FilterChip(selected = logoShape == "ROUND", onClick = { logoShape = "ROUND" }, label = { Text("Circular") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = logoShape == "RECT", onClick = { logoShape = "RECT" }, label = { Text("Banner (1200x250)") })
                    }

                    Box(contentAlignment = Alignment.Center) {
                        if (uploadProgress != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                CircularProgressIndicator(
                                    progress = { uploadProgress / 100f },
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Carregando: $uploadProgress%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        } else if (currentLogoUrl != null) {
                            AsyncImage(
                                model = currentLogoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(if (logoShape == "ROUND") 100.dp else 240.dp, 100.dp)
                                    .clip(if (logoShape == "ROUND") CircleShape else RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AddPhotoAlternate, null) }
                            }
                        }
                    }

                    Row {
                        Button(
                            onClick = { launcher.launch("image/*") }, 
                            modifier = Modifier.padding(8.dp),
                            enabled = uploadProgress == null
                        ) { Text("Mudar Foto") }
                        if (currentLogoUrl != null && uploadProgress == null) {
                            TextButton(onClick = onRemoveLogo, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Remover") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Informações do Salão", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do Salão") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
            Text("Horários e Intervalos", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = opening, onValueChange = { opening = it }, label = { Text("Abre") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = closing, onValueChange = { closing = it }, label = { Text("Fecha") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(value = breakStart, onValueChange = { breakStart = it }, label = { Text("Almoço Início") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = breakEnd, onValueChange = { breakEnd = it }, label = { Text("Almoço Fim") }, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            Text("Dias de Funcionamento", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1 to "D", 2 to "S", 3 to "T", 4 to "Q", 5 to "Q", 6 to "S", 7 to "S").forEach { (id, label) ->
                    FilterChip(selected = selectedDays.contains(id), onClick = { selectedDays = if (selectedDays.contains(id)) selectedDays.filter { it != id } else selectedDays + id }, label = { Text(label) })
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aceite Automático", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Confirmar agendamentos instantaneamente sem aprovação manual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = autoAccept, onCheckedChange = { autoAccept = it })
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ramo de Atividade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentSettingsButton(label = "💈 Barbearia", selected = segment == "BARBEARIA", onClick = { segment = "BARBEARIA" }, modifier = Modifier.weight(1f))
                            SegmentSettingsButton(label = "💇 Salão/Cabelo", selected = segment == "CABELEIREIRO", onClick = { segment = "CABELEIREIRO" }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentSettingsButton(label = "💅 Manicure", selected = segment == "MANICURE", onClick = { segment = "MANICURE" }, modifier = Modifier.weight(1f))
                            SegmentSettingsButton(label = "✨ Estética", selected = segment == "ESTETICA", onClick = { segment = "ESTETICA" }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Programa de Fidelidade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Gerar cartão de fidelidade automático no aplicativo do cliente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = hasLoyalty, onCheckedChange = { hasLoyalty = it })
                    }

                    if (hasLoyalty) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = loyaltyRequired,
                            onValueChange = { loyaltyRequired = it },
                            label = { Text("Qtd. de Serviços para Ganhar o Prêmio") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = loyaltyReward,
                            onValueChange = { loyaltyReward = it },
                            label = { Text("Descrição do Prêmio (Ex: Corte Grátis)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            Button(
                onClick = { 
                    onSave(
                        name, 
                        opening, 
                        closing, 
                        breakStart, 
                        breakEnd, 
                        selectedDays, 
                        autoAccept, 
                        logoShape, 
                        segment,
                        hasLoyalty,
                        loyaltyRequired.toIntOrNull() ?: 10,
                        loyaltyReward
                    ) 
                }, 
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp).height(56.dp), 
                enabled = name.isNotBlank()
            ) {
                Text("Salvar Todas as Configurações")
            }
        }
    }
}

@Composable
fun SegmentSettingsButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier.height(48.dp)) {
            Text(label, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.height(48.dp)) {
            Text(label)
        }
    }
}
