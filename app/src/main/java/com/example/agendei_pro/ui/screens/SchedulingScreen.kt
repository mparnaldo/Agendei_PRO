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
import com.example.agendei_pro.core.model.UserProfile
import com.example.agendei_pro.ui.viewmodel.SchedulingViewModel
import com.example.agendei_pro.ui.viewmodel.TimeSlot
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarMonth
import android.content.Intent
import android.provider.CalendarContract

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulingScreen(
    salonId: String,
    preselectedServiceId: String? = null,
    userProfile: UserProfile? = null,
    onSavePhone: ((String) -> Unit)? = null,
    viewModel: SchedulingViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var showPhoneCaptureDialog by remember { mutableStateOf(false) }
    var pendingTimeStr by remember { mutableStateOf("") }
    var pendingIsRedeem by remember { mutableStateOf(false) }
    val services by viewModel.services.collectAsState()
    val professionals by viewModel.professionals.collectAsState()
    val isLoadingProfs by viewModel.isLoadingProfs.collectAsState()
    val selectedProfessional by viewModel.selectedProfessional.collectAsState()
    val availableSlots by viewModel.availableSlots.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val isLoyaltyEligible by viewModel.isLoyaltyEligible.collectAsState()

    var step by remember { mutableIntStateOf(1) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedCategory by remember { mutableStateOf("TODOS") }
    
    var showWaitingPhoneCaptureDialog by remember { mutableStateOf(false) }
    var showWaitingConfirmDialog by remember { mutableStateOf(false) }
    var joinedWaitingList by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    LaunchedEffect(Unit) { 
        viewModel.loadServices(salonId)
        viewModel.loadProfessionals(salonId)
        viewModel.checkLoyaltyEligibility(salonId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.statusMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            step = 5
        }
    }

    LaunchedEffect(services, preselectedServiceId) {
        if (!preselectedServiceId.isNullOrBlank() && services.isNotEmpty()) {
            val matchingService = services.find { it.id == preselectedServiceId }
            if (matchingService != null) {
                viewModel.selectService(matchingService)
                step = 2
            }
        }
    }

    LaunchedEffect(isLoadingProfs, professionals, step) {
        if (step == 2 && !isLoadingProfs && professionals.isEmpty()) {
            step = 3
        }
    }

    val categories = remember(services) {
        listOf("TODOS") + services.map { it.category }.distinct()
    }

    val goBack = {
        when (step) {
            4 -> step = 3
            3 -> {
                if (professionals.isEmpty()) {
                    step = 1
                } else {
                    step = 2
                }
            }
            2 -> step = 1
            else -> onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            if (step != 5) {
                TopAppBar(
                    title = { 
                        Text(when(step) {
                            1 -> "Escolha o Serviço"
                            2 -> "Escolha o Profissional"
                            3 -> "Escolha o Dia"
                            else -> "Escolha o Horário"
                        }) 
                    },
                    navigationIcon = {
                        IconButton(onClick = goBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            }
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
                    if (isLoadingProfs) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val selectedServiceVal = viewModel.selectedService.collectAsState().value
                        val qualifiedProfs = remember(professionals, selectedServiceVal) {
                            val serviceId = selectedServiceVal?.id ?: ""
                            professionals.filter { pro ->
                                pro.specialties.isEmpty() || pro.specialties.contains(serviceId)
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text(
                                text = "Quem você gostaria que te atendesse?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // "Tanto faz" Option Card
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectProfessional(null)
                                                step = 3
                                            },
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedProfessional == null) 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Groups,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = "Tanto faz (Qualquer Profissional)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "Buscaremos a melhor disponibilidade",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }

                                // List of qualified professionals
                                items(qualifiedProfs, key = { it.id }) { pro ->
                                    val isSelected = selectedProfessional?.id == pro.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectProfessional(pro)
                                                step = 3
                                            },
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!pro.photoUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = pro.photoUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = pro.name.take(1).uppercase(),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = pro.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                val subtitleText = if (pro.specialties.isEmpty()) {
                                                    "Realiza todos os serviços"
                                                } else {
                                                    "Especialista neste serviço"
                                                }
                                                Text(
                                                    text = subtitleText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            val salonVal = viewModel.salon.collectAsState().value
                                            val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
                                            if (salonVal?.isConfigurationIndividualized == true && selectedServiceVal != null) {
                                                val customPrice = pro.servicePrices[selectedServiceVal.id]
                                                if (customPrice != null) {
                                                    Text(
                                                        text = currencyFormatter.format(customPrice),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 15.sp,
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
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
                            step = 4 
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) { Text("Próximo: Escolher Horário") }
                }
                4 -> {
                    val selectedServiceVal = viewModel.selectedService.collectAsState().value
                    val selectedProVal = viewModel.selectedProfessional.collectAsState().value
                    val salonVal = viewModel.salon.collectAsState().value
                    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
                    
                    val displayPrice = remember(selectedServiceVal, selectedProVal, salonVal) {
                        if (selectedServiceVal == null) 0.0
                        else if (salonVal?.isConfigurationIndividualized == true && selectedProVal != null) {
                            selectedProVal.servicePrices[selectedServiceVal.id] ?: selectedServiceVal.price
                        } else {
                            selectedServiceVal.price
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Resumo do Agendamento",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Serviço: ${selectedServiceVal?.name ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Duração: ${selectedServiceVal?.durationMinutes ?: 30} minutos",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Profissional: ${selectedProVal?.name ?: "Qualquer um (Tanto faz)"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Valor: ${currencyFormatter.format(displayPrice)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Text(
                            text = "⏱️ Este serviço tem duração de ${selectedServiceVal?.durationMinutes ?: 30} minutos. Exibindo apenas horários com espaço suficiente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        DateTimeSelection(
                            slots = availableSlots,
                            isLoyaltyEligible = isLoyaltyEligible,
                            onConfirm = { timeStr, isRedeem ->
                                if (userProfile != null && userProfile.phoneNumber.isBlank()) {
                                    pendingTimeStr = timeStr
                                    pendingIsRedeem = isRedeem
                                    showPhoneCaptureDialog = true
                                } else {
                                    val cal = Calendar.getInstance()
                                    cal.timeInMillis = selectedDateMillis
                                    val parts = timeStr.split(":")
                                    cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                                    cal.set(Calendar.MINUTE, parts[1].toInt())
                                    cal.set(Calendar.SECOND, 0)
                                    viewModel.createAppointment(salonId, cal.time, isRedeem)
                                }
                            },
                            onJoinWaitingList = {
                                if (userProfile != null && userProfile.phoneNumber.isBlank()) {
                                    showWaitingPhoneCaptureDialog = true
                                } else {
                                    showWaitingConfirmDialog = true
                                }
                            }
                        )
                    }
                }
                5 -> {
                    val selectedServiceVal = viewModel.selectedService.collectAsState().value
                    val selectedProVal = viewModel.selectedProfessional.collectAsState().value
                    SuccessScreen(
                        isWaitingList = joinedWaitingList,
                        serviceName = selectedServiceVal?.name ?: "",
                        professionalName = selectedProVal?.name ?: "Tanto faz",
                        date = Date(selectedDateMillis),
                        time = pendingTimeStr,
                        onAddCalendar = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putExtra(CalendarContract.Events.TITLE, "${selectedServiceVal?.name ?: ""} - Agendei")
                                putExtra(CalendarContract.Events.DESCRIPTION, "Profissional: ${selectedProVal?.name ?: "Tanto faz"}")
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = selectedDateMillis
                                if (pendingTimeStr.isNotBlank()) {
                                    val parts = pendingTimeStr.split(":")
                                    cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                                    cal.set(Calendar.MINUTE, parts[1].toInt())
                                    cal.set(Calendar.SECOND, 0)
                                }
                                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
                                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cal.timeInMillis + (selectedServiceVal?.durationMinutes ?: 30) * 60 * 1000)
                            }
                            context.startActivity(intent)
                        },
                        onFinish = onSuccess
                    )
                }
            }
        }
    }

    if (showPhoneCaptureDialog) {
        var tempPhone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPhoneCaptureDialog = false },
            title = { Text("Número de WhatsApp") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Para prosseguir com o agendamento, precisamos do seu número de WhatsApp para que o salão possa entrar em contato com você se necessário.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("WhatsApp (com DDD)") },
                        placeholder = { Text("(11) 99999-9999") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        text = "Seu número será compartilhado apenas com os salões onde você realizar agendamentos, em conformidade com a LGPD.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPhone.isNotBlank()) {
                            onSavePhone?.invoke(tempPhone)
                            showPhoneCaptureDialog = false
                            
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = selectedDateMillis
                            val parts = pendingTimeStr.split(":")
                            cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                            cal.set(Calendar.MINUTE, parts[1].toInt())
                            cal.set(Calendar.SECOND, 0)
                            viewModel.createAppointment(salonId, cal.time, pendingIsRedeem)
                        }
                    },
                    enabled = tempPhone.isNotBlank()
                ) {
                    Text("Confirmar e Agendar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneCaptureDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showWaitingConfirmDialog) {
        val selectedServiceVal = viewModel.selectedService.collectAsState().value
        val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
        AlertDialog(
            onDismissRequest = { showWaitingConfirmDialog = false },
            title = { Text("Entrar na Fila de Espera?") },
            text = {
                Text(
                    text = "Deseja entrar na fila de espera para o dia ${dateFormatter.format(Date(selectedDateMillis))} para o serviço ${selectedServiceVal?.name ?: ""}? O salão entrará em contato via WhatsApp se um horário for liberado."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWaitingConfirmDialog = false
                        viewModel.joinWaitingList(
                            salonId = salonId,
                            date = Date(selectedDateMillis),
                            proId = selectedProfessional?.id,
                            proName = selectedProfessional?.name,
                            serviceId = selectedServiceVal?.id ?: "",
                            serviceName = selectedServiceVal?.name ?: "",
                            clientPhone = userProfile?.phoneNumber ?: ""
                        )
                        joinedWaitingList = true
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaitingConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showWaitingPhoneCaptureDialog) {
        val selectedServiceVal = viewModel.selectedService.collectAsState().value
        var tempPhone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showWaitingPhoneCaptureDialog = false },
            title = { Text("WhatsApp para Fila de Espera") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Para entrar na fila de espera, informe seu número de WhatsApp para que o salão possa te avisar se surgir uma vaga.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("WhatsApp (com DDD)") },
                        placeholder = { Text("(11) 99999-9999") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPhone.isNotBlank()) {
                            onSavePhone?.invoke(tempPhone)
                            showWaitingPhoneCaptureDialog = false
                            viewModel.joinWaitingList(
                                salonId = salonId,
                                date = Date(selectedDateMillis),
                                proId = selectedProfessional?.id,
                                proName = selectedProfessional?.name,
                                serviceId = selectedServiceVal?.id ?: "",
                                serviceName = selectedServiceVal?.name ?: "",
                                clientPhone = tempPhone
                            )
                            joinedWaitingList = true
                        }
                    },
                    enabled = tempPhone.isNotBlank()
                ) {
                    Text("Confirmar e Entrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaitingPhoneCaptureDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ServiceSelectionList(services: List<Service>, onServiceSelected: (Service) -> Unit) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(services) { service ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onServiceSelected(service) }, 
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (service.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = service.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(service.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${service.durationMinutes} min", style = MaterialTheme.typography.bodySmall)
                        if (service.observation.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(service.observation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(currencyFormatter.format(service.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelection(
    slots: List<TimeSlot>,
    isLoyaltyEligible: Boolean,
    onConfirm: (String, Boolean) -> Unit,
    onJoinWaitingList: () -> Unit
) {
    var selectedTime by remember { mutableStateOf("") }
    var useLoyaltyReward by remember { mutableStateOf(false) }
    
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

        if (isLoyaltyEligible && selectedTime.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.5.dp, Color(0xFF2E7D32))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                           Text(
                               text = "🎁 Resgatar Prêmio Fidelidade",
                               style = MaterialTheme.typography.titleSmall,
                               fontWeight = FontWeight.Bold,
                               color = Color(0xFF2E7D32)
                           )
                           Text(
                               text = "Usar prêmio disponível para obter este serviço gratuitamente!",
                               style = MaterialTheme.typography.bodySmall,
                               color = Color(0xFF1B5E20)
                           )
                    }
                    Switch(
                        checked = useLoyaltyReward,
                        onCheckedChange = { useLoyaltyReward = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2E7D32),
                            checkedTrackColor = Color(0xFFC8E6C9)
                        )
                    )
                }
            }
        }
        
        Button(
            onClick = { onConfirm(selectedTime, useLoyaltyReward) }, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            enabled = selectedTime.isNotEmpty()
        ) {
            Text(if (useLoyaltyReward) "Agendar com Prêmio Fidelidade" else "Finalizar Agendamento")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onJoinWaitingList,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Entrar na Fila de Espera", fontSize = 14.sp)
        }
    }
}

@Composable
fun SuccessScreen(
    isWaitingList: Boolean,
    serviceName: String,
    professionalName: String,
    date: Date,
    time: String,
    onAddCalendar: () -> Unit,
    onFinish: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isWaitingList) "Fila de Espera Confirmada!" else "Agendamento Realizado!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isWaitingList) 
                "Você foi adicionado à lista e será notificado caso surja uma vaga."
            else 
                "Seu agendamento foi solicitado e está sendo processado.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Detalhes:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Serviço: $serviceName")
                Text("Profissional: $professionalName")
                Text("Data: ${dateFormatter.format(date)}")
                if (!isWaitingList && time.isNotBlank()) {
                    Text("Horário: $time")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!isWaitingList) {
            Button(
                onClick = onAddCalendar,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar à minha Agenda")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Voltar para o Início")
        }
    }
}
