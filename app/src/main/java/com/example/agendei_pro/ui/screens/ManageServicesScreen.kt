package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agendei_pro.core.model.Service
import com.example.agendei_pro.ui.viewmodel.ServicesViewModel
import java.text.NumberFormat
import java.util.Locale

data class SuggestedService(
    val name: String,
    val category: String,
    val defaultPrice: Double,
    val defaultDuration: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageServicesScreen(
    viewModel: ServicesViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val services by viewModel.services.collectAsState()
    val segment by viewModel.salonSegment.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSuggestion by remember { mutableStateOf<SuggestedService?>(null) }

    // Sugestões inteligentes de acordo com o segmento/ramo do salão 💈💇💅✨
    val suggestions = when (segment) {
        "CABELEIREIRO" -> listOf(
            SuggestedService("Corte Feminino", "Corte e Penteado", 80.0, 60),
            SuggestedService("Corte Masculino", "Corte e Penteado", 45.0, 40),
            SuggestedService("Escova / Chapinha", "Corte e Penteado", 50.0, 45),
            SuggestedService("Penteado Completo", "Corte e Penteado", 120.0, 60),
            SuggestedService("Coloração / Tintura", "Coloração", 90.0, 90),
            SuggestedService("Luzes / Mechas", "Coloração", 250.0, 180),
            SuggestedService("Hidratação / Reconstrução", "Tratamentos", 70.0, 45),
            SuggestedService("Progressiva", "Tratamentos", 180.0, 120)
        )
        "MANICURE" -> listOf(
            SuggestedService("Pé e Mão", "Unhas Comuns", 50.0, 60),
            SuggestedService("Apenas Mão", "Unhas Comuns", 25.0, 30),
            SuggestedService("Apenas Pé", "Unhas Comuns", 30.0, 30),
            SuggestedService("Alongamento em Gel", "Alongamentos", 120.0, 120),
            SuggestedService("Manutenção Alongamento", "Alongamentos", 70.0, 90),
            SuggestedService("Esmaltação em Gel", "Unhas Comuns", 45.0, 40),
            SuggestedService("Banho de Gel", "Alongamentos", 80.0, 60),
            SuggestedService("Spa dos Pés", "Tratamentos", 60.0, 45)
        )
        "ESTETICA" -> listOf(
            SuggestedService("Limpeza de Pele", "Facial", 120.0, 90),
            SuggestedService("Peeling Químico", "Facial", 150.0, 45),
            SuggestedService("Drenagem Linfática", "Corporal", 90.0, 60),
            SuggestedService("Massagem Modeladora", "Corporal", 80.0, 50),
            SuggestedService("Massagem Relaxante", "Corporal", 100.0, 60),
            SuggestedService("Design de Sobrancelha", "Sobrancelhas e Cílios", 30.0, 30),
            SuggestedService("Micropigmentação", "Sobrancelhas e Cílios", 350.0, 120),
            SuggestedService("Extensão de Cílios", "Sobrancelhas e Cílios", 140.0, 120)
        )
        else -> listOf( // BARBEARIA
            SuggestedService("Corte Social", "Cabelo", 35.0, 30),
            SuggestedService("Corte Degradê", "Cabelo", 45.0, 45),
            SuggestedService("Barba Simples", "Barba", 25.0, 20),
            SuggestedService("Barba Completa (Terapia)", "Barba", 40.0, 40),
            SuggestedService("Sobrancelha", "Cabelo", 15.0, 15),
            SuggestedService("Corte + Barba", "Combos", 60.0, 60),
            SuggestedService("Pigmentação", "Barba", 20.0, 20),
            SuggestedService("Selagem / Progressiva", "Cabelo", 80.0, 60)
        )
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Meus Serviços") }, 
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }
            ) 
        },
        floatingActionButton = { 
            FloatingActionButton(onClick = { 
                selectedSuggestion = null
                showAddDialog = true 
            }) { 
                Icon(Icons.Default.Add, null) 
            } 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            Text("Sugestões Rápidas (Ramo: ${if (segment == "CABELEIREIRO") "Cabelo" else if (segment == "ESTETICA") "Estética" else segment.lowercase().replaceFirstChar { it.uppercase() }}):", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall)
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp), 
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestions) { s ->
                    AssistChip(
                        onClick = { 
                            selectedSuggestion = s
                            showAddDialog = true
                        }, 
                        label = { Text(text = s.name) }
                    )
                }
            }

            if (services.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum serviço cadastrado.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f), 
                    contentPadding = PaddingValues(16.dp), 
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(services) { service ->
                        ServiceItem(service, onDelete = { viewModel.deleteService(service.id) })
                    }
                }
            }
        }

        if (showAddDialog) {
            AddServiceDialog(
                segment = segment,
                prefilledService = selectedSuggestion,
                onDismiss = { showAddDialog = false },
                onConfirm = { n, p, d, c, o -> 
                    viewModel.addService(n, p, d, c, o)
                    showAddDialog = false 
                }
            )
        }
    }
}

@Composable
fun ServiceItem(service: Service, onDelete: () -> Unit) {
    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(service.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(service.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
                Text(currency.format(service.price), fontWeight = FontWeight.ExtraBold)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
            if (service.observation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(service.observation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceDialog(
    segment: String,
    prefilledService: SuggestedService?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String, String) -> Unit
) {
    var name by remember(prefilledService) { mutableStateOf(prefilledService?.name ?: "") }
    var price by remember(prefilledService) { mutableStateOf(prefilledService?.defaultPrice?.toString() ?: "") }
    var duration by remember(prefilledService) { mutableStateOf(prefilledService?.defaultDuration?.toString() ?: "30") }
    var category by remember(prefilledService) { mutableStateOf(prefilledService?.category ?: "") }
    var observation by remember { mutableStateOf("") }

    var serviceExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Combos de acordo com o segmento
    val suggestedServices = when (segment) {
        "CABELEIREIRO" -> listOf(
            SuggestedService("Corte Feminino", "Corte e Penteado", 80.0, 60),
            SuggestedService("Corte Masculino", "Corte e Penteado", 45.0, 40),
            SuggestedService("Escova / Chapinha", "Corte e Penteado", 50.0, 45),
            SuggestedService("Penteado Completo", "Corte e Penteado", 120.0, 60),
            SuggestedService("Coloração / Tintura", "Coloração", 90.0, 90),
            SuggestedService("Luzes / Mechas", "Coloração", 250.0, 180),
            SuggestedService("Hidratação / Reconstrução", "Tratamentos", 70.0, 45),
            SuggestedService("Progressiva", "Tratamentos", 180.0, 120)
        )
        "MANICURE" -> listOf(
            SuggestedService("Pé e Mão", "Unhas Comuns", 50.0, 60),
            SuggestedService("Apenas Mão", "Unhas Comuns", 25.0, 30),
            SuggestedService("Apenas Pé", "Unhas Comuns", 30.0, 30),
            SuggestedService("Alongamento em Gel", "Alongamentos", 120.0, 120),
            SuggestedService("Manutenção Alongamento", "Alongamentos", 70.0, 90),
            SuggestedService("Esmaltação em Gel", "Unhas Comuns", 45.0, 40),
            SuggestedService("Banho de Gel", "Alongamentos", 80.0, 60),
            SuggestedService("Spa dos Pés", "Tratamentos", 60.0, 45)
        )
        "ESTETICA" -> listOf(
            SuggestedService("Limpeza de Pele", "Facial", 120.0, 90),
            SuggestedService("Peeling Químico", "Facial", 150.0, 45),
            SuggestedService("Drenagem Linfática", "Corporal", 90.0, 60),
            SuggestedService("Massagem Modeladora", "Corporal", 80.0, 50),
            SuggestedService("Massagem Relaxante", "Corporal", 100.0, 60),
            SuggestedService("Design de Sobrancelha", "Sobrancelhas e Cílios", 30.0, 30),
            SuggestedService("Micropigmentação", "Sobrancelhas e Cílios", 350.0, 120),
            SuggestedService("Extensão de Cílios", "Sobrancelhas e Cílios", 140.0, 120)
        )
        else -> listOf( // BARBEARIA
            SuggestedService("Corte Social", "Cabelo", 35.0, 30),
            SuggestedService("Corte Degradê", "Cabelo", 45.0, 45),
            SuggestedService("Barba Simples", "Barba", 25.0, 20),
            SuggestedService("Barba Completa (Terapia)", "Barba", 40.0, 40),
            SuggestedService("Sobrancelha", "Cabelo", 15.0, 15),
            SuggestedService("Corte + Barba", "Combos", 60.0, 60),
            SuggestedService("Pigmentação", "Barba", 20.0, 20),
            SuggestedService("Selagem / Progressiva", "Cabelo", 80.0, 60)
        )
    }

    val suggestedCategories = when (segment) {
        "CABELEIREIRO" -> listOf("Corte e Penteado", "Coloração", "Tratamentos", "Manicure", "Outros")
        "MANICURE" -> listOf("Unhas Comuns", "Alongamentos", "Tratamentos", "Outros")
        "ESTETICA" -> listOf("Facial", "Corporal", "Sobrancelhas e Cílios", "Outros")
        else -> listOf("Cabelo", "Barba", "Combos", "Outros")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Serviço") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Combo de Nome do Serviço com Auto-complete
                ExposedDropdownMenuBox(
                    expanded = serviceExpanded,
                    onExpandedChange = { serviceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do Serviço") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = serviceExpanded,
                        onDismissRequest = { serviceExpanded = false }
                    ) {
                        suggestedServices.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion.name) },
                                onClick = {
                                    name = suggestion.name
                                    category = suggestion.category
                                    price = suggestion.defaultPrice.toString()
                                    duration = suggestion.defaultDuration.toString()
                                    serviceExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = price, 
                    onValueChange = { price = it }, 
                    label = { Text("Preço (R$)") }, 
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = duration, 
                    onValueChange = { duration = it }, 
                    label = { Text("Duração (min)") }, 
                    modifier = Modifier.fillMaxWidth()
                )

                // Combo de Categoria com Auto-complete
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoria") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        suggestedCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = observation, 
                    onValueChange = { observation = it }, 
                    label = { Text("Observação/Descrição") }, 
                    modifier = Modifier.height(100.dp).fillMaxWidth()
                )
            }
        },
        confirmButton = { 
            Button(
                onClick = { 
                    onConfirm(name, price.replace(",", ".").toDoubleOrNull() ?: 0.0, duration.toIntOrNull() ?: 30, category, observation) 
                },
                enabled = name.isNotBlank() && price.isNotBlank()
            ) { Text("Salvar") } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
