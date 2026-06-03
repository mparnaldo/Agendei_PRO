package com.example.agendei_pro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agendei_pro.core.model.Service
import com.example.agendei_pro.ui.viewmodel.SchedulingViewModel
import com.example.agendei_pro.ui.viewmodel.TimeSlot
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulingScreen(
    salonId: String,
    preselectedServiceId: String? = null,
    viewModel: SchedulingViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val services by viewModel.services.collectAsState()
    val availableSlots by viewModel.availableSlots.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()

    var step by remember { mutableIntStateOf(1) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedCategory by remember { mutableStateOf("TODOS") }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    LaunchedEffect(Unit) { viewModel.loadServices(salonId) }
    
    LaunchedEffect(Unit) {
        viewModel.statusMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isSuccess) { if (isSuccess) onSuccess() }

    LaunchedEffect(services, preselectedServiceId) {
        if (!preselectedServiceId.isNullOrBlank() && services.isNotEmpty()) {
            val matchingService = services.find { it.id == preselectedServiceId }
            if (matchingService != null) {
                viewModel.selectService(matchingService)
                step = 2
            }
        }
    }

    val categories = remember(services) {
        listOf("TODOS") + services.map { it.category }.distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(when(step) {
                        1 -> "Escolha o Serviço"
                        2 -> "Escolha o Dia"
                        else -> "Escolha o Horário"
                    }) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (step > 1) step-- else onNavigateBack() }) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when(step) {
                1 -> {
                    // Filtro de Categorias 🏷️
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory),
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent
                    ) {
                        categories.forEach { cat ->
                            Tab(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                text = { Text(cat) }
                            )
                        }
                    }

                    val filteredServices = if (selectedCategory == "TODOS") services 
                                          else services.filter { it.category == selectedCategory }

                    ServiceSelectionList(filteredServices) { 
                        viewModel.selectService(it)
                        step = 2 
                    }
                }
                2 -> {
                    DatePicker(state = datePickerState, modifier = Modifier.weight(1f))
                    Button(
                        onClick = { 
                            val selectedUtcMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedDateMillis = localCal.timeInMillis
                            viewModel.loadAvailableSlots(salonId, localCal.time)
                            step = 3 
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) { Text("Próximo: Escolher Horário") }
                }
                3 -> DateTimeSelection(
                    slots = availableSlots,
                    onConfirm = { timeStr ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = selectedDateMillis
                        val parts = timeStr.split(":")
                        cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                        cal.set(Calendar.MINUTE, parts[1].toInt())
                        cal.set(Calendar.SECOND, 0)
                        viewModel.createAppointment(salonId, cal.time)
                    }
                )
            }
        }
    }
}

@Composable
fun ServiceSelectionList(services: List<Service>, onServiceSelected: (Service) -> Unit) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(services) { service ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onServiceSelected(service) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(service.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${service.durationMinutes} min", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(currencyFormatter.format(service.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (service.observation.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(service.observation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelection(slots: List<TimeSlot>, onConfirm: (String) -> Unit) {
    var selectedTime by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (slots.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Buscando horários...", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            Text("Horários disponíveis:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), 
                horizontalArrangement = Arrangement.spacedBy(8.dp), 
                verticalArrangement = Arrangement.spacedBy(8.dp), 
                modifier = Modifier.weight(1f)
            ) {
                items(slots) { slot ->
                    FilterChip(
                        selected = selectedTime == slot.time,
                        onClick = { if (slot.isAvailable) selectedTime = slot.time },
                        label = { Text(slot.time) },
                        enabled = slot.isAvailable,
                        colors = FilterChipDefaults.filterChipColors(
                            disabledContainerColor = Color.LightGray.copy(alpha = 0.2f),
                            disabledLabelColor = Color.Gray
                        )
                    )
                }
            }
        }
        
        Button(
            onClick = { onConfirm(selectedTime) }, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            enabled = selectedTime.isNotEmpty()
        ) {
            Text("Finalizar Agendamento")
        }
    }
}
