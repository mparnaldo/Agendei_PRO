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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke

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
    currentAutoValidateLoyalty: Boolean,
    currentLoyaltyRedemptionDays: Int,
    currentSlotInterval: Int,
    currentIsIndividualized: Boolean,
    currentHasWaitingList: Boolean,
    currentMinBookingDelayHours: Int,
    currentMinCancelDelayHours: Int,
    uploadProgress: Int?,
    onSave: (
        name: String,
        opening: String,
        closing: String,
        breakStart: String,
        breakEnd: String,
        days: List<Int>,
        autoAccept: Boolean,
        logoShape: String,
        segment: String,
        hasLoyalty: Boolean,
        loyaltyRequired: Int,
        loyaltyReward: String,
        autoValidateLoyalty: Boolean,
        loyaltyRedemptionDays: Int,
        slotInterval: Int,
        isIndividualized: Boolean,
        hasWaitingList: Boolean,
        minBookingDelayHours: Int,
        minCancelDelayHours: Int
    ) -> Unit,
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
    var autoValidateLoyalty by remember { mutableStateOf(currentAutoValidateLoyalty) }
    var loyaltyRedemptionDays by remember { mutableStateOf(currentLoyaltyRedemptionDays.toString()) }
    var slotInterval by remember { mutableIntStateOf(currentSlotInterval) }
    var isIndividualized by remember { mutableStateOf(currentIsIndividualized) }
    var hasWaitingList by remember { mutableStateOf(currentHasWaitingList) }
    var minBookingDelayHours by remember { mutableStateOf(currentMinBookingDelayHours.toString()) }
    var minCancelDelayHours by remember { mutableStateOf(currentMinCancelDelayHours.toString()) }

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
            Spacer(Modifier.height(8.dp))
            Text("Intervalo dos Horários", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15 to "15 min", 30 to "30 min", 60 to "1 hora").forEach { (min, label) ->
                    FilterChip(
                        selected = slotInterval == min,
                        onClick = { slotInterval = min },
                        label = { Text(label) }
                    )
                }
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentCard(
                                label = "Barbearia",
                                icon = Icons.Default.ContentCut,
                                selected = segment == "BARBEARIA",
                                onClick = { segment = "BARBEARIA" },
                                modifier = Modifier.weight(1f)
                            )
                            SegmentCard(
                                label = "Salão / Cabelo",
                                icon = Icons.Default.Face,
                                selected = segment == "CABELEIREIRO",
                                onClick = { segment = "CABELEIREIRO" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentCard(
                                label = "Manicure",
                                icon = Icons.Default.Brush,
                                selected = segment == "MANICURE",
                                onClick = { segment = "MANICURE" },
                                modifier = Modifier.weight(1f)
                            )
                            SegmentCard(
                                label = "Estética",
                                icon = Icons.Default.Spa,
                                selected = segment == "ESTETICA",
                                onClick = { segment = "ESTETICA" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentCard(
                                label = "Estúdio de Tattoo",
                                icon = Icons.Default.Palette,
                                selected = segment == "TATTOO",
                                onClick = { segment = "TATTOO" },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
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
                        Text("Configuração por Profissional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Permitir que cada profissional tenha horários de trabalho e preços de serviços próprios", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isIndividualized, onCheckedChange = { isIndividualized = it })
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
                        Text("Fila de Espera Inteligente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Permitir que clientes entrem na fila de espera caso não encontrem horários disponíveis", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = hasWaitingList, onCheckedChange = { hasWaitingList = it })
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Políticas de Antecedência (Horas)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = minBookingDelayHours,
                        onValueChange = { minBookingDelayHours = it },
                        label = { Text("Antecedência Mínima para Agendar (Horas)") },
                        placeholder = { Text("Ex: 3") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minCancelDelayHours,
                        onValueChange = { minCancelDelayHours = it },
                        label = { Text("Antecedência Mínima para Cancelar (Horas)") },
                        placeholder = { Text("Ex: 2") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
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
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = loyaltyRedemptionDays,
                            onValueChange = { loyaltyRedemptionDays = it },
                            label = { Text("Validade para Resgate (Dias após limite)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Validação Automática", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Conceder selos de fidelidade instantaneamente na confirmação do agendamento", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = autoValidateLoyalty, onCheckedChange = { autoValidateLoyalty = it })
                        }
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
                        loyaltyReward,
                        autoValidateLoyalty,
                        loyaltyRedemptionDays.toIntOrNull() ?: 30,
                        slotInterval,
                        isIndividualized,
                        hasWaitingList,
                        minBookingDelayHours.toIntOrNull() ?: 0,
                        minCancelDelayHours.toIntOrNull() ?: 0
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
private fun SegmentCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = border,
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}
