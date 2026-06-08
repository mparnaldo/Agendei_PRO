package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agendei_pro.core.model.Appointment
import com.example.agendei_pro.ui.viewmodel.AgendaViewModel
import com.example.agendei_pro.ui.viewmodel.AgendaViewMode
import java.text.SimpleDateFormat
import java.util.*
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    viewModel: AgendaViewModel = viewModel(),
    onUpdateStatus: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appointments by viewModel.appointments.collectAsState()
    val allAppointments by viewModel.allAppointments.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val salonHasLoyalty by viewModel.salonHasLoyalty.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    var showBlockDialog by remember { mutableStateOf(false) }

    var professionals by remember { mutableStateOf<List<com.example.agendei_pro.core.model.Professional>>(emptyList()) }
    var selectedProfessionalId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("salons")
                .document(uid)
                .collection("professionals")
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snapshot, _ ->
                    val list = snapshot?.toObjects(com.example.agendei_pro.core.model.Professional::class.java) ?: emptyList()
                    professionals = list
                }
        }
    }

    val filteredDayAppts = remember(appointments, selectedProfessionalId) {
        if (selectedProfessionalId == null) appointments
        else appointments.filter { it.professionalId == selectedProfessionalId || it.professionalId == "ALL" }
    }

    val filteredAllAppts = remember(allAppointments, selectedProfessionalId) {
        if (selectedProfessionalId == null) allAppointments
        else allAppointments.filter { it.professionalId == selectedProfessionalId || it.professionalId == "ALL" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { viewModel.setViewMode(AgendaViewMode.LIST) }) {
                        Icon(
                            Icons.Default.FormatListBulleted,
                            contentDescription = "Lista Completa",
                            tint = if (viewMode == AgendaViewMode.LIST) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { viewModel.setViewMode(AgendaViewMode.CALENDAR_MONTH) }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Calendário",
                            tint = if (viewMode == AgendaViewMode.CALENDAR_MONTH) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showBlockDialog = true },
                icon = { Icon(Icons.Default.Block, null) },
                text = { Text("Bloquear Horário") },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Professional Filter Chips/Tabs Row
            if (professionals.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = if (selectedProfessionalId == null) 0 else professionals.indexOfFirst { it.id == selectedProfessionalId } + 1,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = {}
                ) {
                    Tab(
                        selected = selectedProfessionalId == null,
                        onClick = { selectedProfessionalId = null }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedProfessionalId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = if (selectedProfessionalId == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Todos",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedProfessionalId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    professionals.forEach { pro ->
                        val isSelected = selectedProfessionalId == pro.id
                        Tab(
                            selected = isSelected,
                            onClick = { selectedProfessionalId = pro.id }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                            ) {
                                if (!pro.photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = pro.photoUrl,
                                        contentDescription = pro.name,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pro.name.take(1).uppercase(),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pro.name,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (viewMode == AgendaViewMode.CALENDAR_MONTH) {
                // Exibe o calendário no topo
                MonthlyCalendarView(
                    allAppointments = filteredAllAppts,
                    selectedDate = selectedDate,
                    currentMonth = currentMonth,
                    onNavigateMonth = { viewModel.navigateMonth(it) },
                    onDateSelected = { viewModel.onDateSelected(it) }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Exibe a lista do dia selecionado embaixo
                AgendaListView(
                    appointments = filteredDayAppts,
                    selectedDate = selectedDate,
                    title = "Agendamentos do Dia",
                    hasLoyalty = salonHasLoyalty,
                    onValidateLoyalty = { viewModel.updateLoyaltyValidation(it, true) },
                    onUpdateStatus = { id, s -> viewModel.updateStatus(id, s) },
                    onDeleteBlockage = { viewModel.deleteAppointment(it) }
                )
            } else {
                // Exibe a linha do tempo / lista contínua rolando
                TimelineView(
                    allAppointments = filteredAllAppts,
                    hasLoyalty = salonHasLoyalty,
                    onValidateLoyalty = { viewModel.updateLoyaltyValidation(it, true) },
                    onUpdateStatus = { id, s -> viewModel.updateStatus(id, s) },
                    onDeleteBlockage = { viewModel.deleteAppointment(it) }
                )
            }
        }
    }

    if (showBlockDialog) {
        var selectedBlockDate by remember { mutableStateOf(selectedDate) }
        var timeStr by remember { mutableStateOf("08:00") }
        var reason by remember { mutableStateOf("Almoço") }
        var selectedProId by remember { mutableStateOf("ALL") }
        var showProDropdown by remember { mutableStateOf(false) }
        val selectedProName = remember(selectedProId, professionals) {
            if (selectedProId == "ALL") "Todo o Salão"
            else professionals.find { it.id == selectedProId }?.name ?: "Profissional"
        }
        
        val dateStr = remember(selectedBlockDate) {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedBlockDate)
        }

        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Bloquear Horário") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Selecione os detalhes para o bloqueio da agenda:", style = MaterialTheme.typography.bodyMedium)
                    
                    // Seletor de Data
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val c = Calendar.getInstance().apply { time = selectedBlockDate }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newCal = Calendar.getInstance().apply {
                                            time = selectedBlockDate
                                            set(Calendar.YEAR, y)
                                            set(Calendar.MONTH, m)
                                            set(Calendar.DAY_OF_MONTH, d)
                                        }
                                        selectedBlockDate = newCal.time
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    ) {
                        OutlinedTextField(
                            value = dateStr,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Data do Bloqueio") },
                            trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Selecionar Data") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Seletor de Profissional
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = showProDropdown,
                            onExpandedChange = { showProDropdown = !showProDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedProName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Bloquear para:") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = showProDropdown,
                                onDismissRequest = { showProDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todo o Salão") },
                                    onClick = {
                                        selectedProId = "ALL"
                                        showProDropdown = false
                                    }
                                )
                                professionals.forEach { pro ->
                                    DropdownMenuItem(
                                        text = { Text(pro.name) },
                                        onClick = {
                                            selectedProId = pro.id
                                            showProDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = { timeStr = it },
                        label = { Text("Horário (Ex: 09:30)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motivo / Descrição") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.blockTimeSlot(selectedBlockDate, timeStr, reason, selectedProId, selectedProName)
                        showBlockDialog = false
                    }
                ) {
                    Text("Bloquear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineView(
    allAppointments: List<Appointment>,
    hasLoyalty: Boolean,
    onValidateLoyalty: (String) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onDeleteBlockage: (String) -> Unit
) {
    var sortAscending by remember { mutableStateOf(true) }
    var filterPendingOnly by remember { mutableStateOf(false) }

    // 1. Filter appointments
    val filteredAppts = remember(allAppointments, filterPendingOnly) {
        if (filterPendingOnly) {
            allAppointments.filter { it.status == "PENDING" }
        } else {
            allAppointments
        }
    }

    // 2. Sort appointments
    val sortedAppts = remember(filteredAppts, sortAscending) {
        if (sortAscending) {
            filteredAppts.sortedBy { it.date ?: Date(0) }
        } else {
            filteredAppts.sortedByDescending { it.date ?: Date(0) }
        }
    }

    // 3. Group by Month (using translated String) and then by Day
    val sdfMonthHeader = SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
    val sdfDayHeader = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("pt", "BR"))

    val groupedByMonth = remember(sortedAppts) {
        val map = LinkedHashMap<String, LinkedHashMap<Date, MutableList<Appointment>>>()
        for (appt in sortedAppts) {
            val apptDate = appt.date ?: Date()
            
            // Month key
            val monthCal = Calendar.getInstance().apply {
                time = apptDate
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val monthStr = sdfMonthHeader.format(monthCal.time).replaceFirstChar { it.uppercase() }

            // Day key
            val dayCal = Calendar.getInstance().apply {
                time = apptDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayDate = dayCal.time

            val daysMap = map.getOrPut(monthStr) { LinkedHashMap() }
            val list = daysMap.getOrPut(dayDate) { mutableListOf() }
            list.add(appt)
        }
        map
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter and Sort controls 🎛️
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !filterPendingOnly,
                onClick = { filterPendingOnly = false },
                label = { Text("Todos") }
            )
            FilterChip(
                selected = filterPendingOnly,
                onClick = { filterPendingOnly = true },
                label = { Text("⚠️ Pendentes") }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(
                onClick = { sortAscending = !sortAscending },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (sortAscending) "Mais antigos primeiro" else "Mais recentes primeiro",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (sortedAppts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum agendamento encontrado.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedByMonth.forEach { (monthStr, daysMap) ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = monthStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    daysMap.forEach { (dayDate, appts) ->
                        stickyHeader {
                            Text(
                                text = sdfDayHeader.format(dayDate).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 4.dp)
                            )
                        }
                        items(appts) { appt ->
                            AppointmentItem(
                                appt = appt,
                                hasLoyalty = hasLoyalty,
                                onValidateLoyalty = onValidateLoyalty,
                                onUpdateStatus = onUpdateStatus,
                                onDeleteBlockage = onDeleteBlockage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaListView(
    appointments: List<Appointment>,
    selectedDate: Date,
    title: String,
    hasLoyalty: Boolean,
    onValidateLoyalty: (String) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onDeleteBlockage: (String) -> Unit
) {
    val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("pt", "BR"))
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "${title} - ${sdf.format(selectedDate).replaceFirstChar { it.uppercase() }}",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        
        if (appointments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sem compromissos nesta data.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appointments) { 
                    AppointmentItem(
                        appt = it,
                        hasLoyalty = hasLoyalty,
                        onValidateLoyalty = onValidateLoyalty,
                        onUpdateStatus = onUpdateStatus,
                        onDeleteBlockage = onDeleteBlockage
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyCalendarView(
    allAppointments: List<Appointment>,
    selectedDate: Date,
    currentMonth: Calendar,
    onNavigateMonth: (Int) -> Unit,
    onDateSelected: (Date) -> Unit
) {
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))

    Column {
        // Month Navigation Header 📅
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onNavigateMonth(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior")
            }
            
            Text(
                text = sdfMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { onNavigateMonth(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês")
            }
        }

        // Days of week header (Dom, Seg, Ter...)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Days grid aligned to weekday
        val cal = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday...
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val gridItems = mutableListOf<Date?>()
        for (i in 1 until firstDayOfWeek) {
            gridItems.add(null)
        }
        for (d in 1..maxDays) {
            val c = (currentMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, d)
            }
            gridItems.add(c.time)
        }

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.padding(horizontal = 8.dp)) {
            items(gridItems) { date ->
                if (date == null) {
                    Box(modifier = Modifier.aspectRatio(1f))
                } else {
                    val dayAppts = allAppointments.filter { isSameDay(it.date ?: Date(), date) }
                    val confirmed = dayAppts.count { it.status == "CONFIRMED" }
                    val pending = dayAppts.count { it.status == "PENDING" }
                    val isSelected = isSameDay(date, selectedDate)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = SimpleDateFormat("dd", Locale.getDefault()).format(date),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (dayAppts.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (confirmed > 0) Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2E7D32))) // Verde escuro
                                    if (pending > 0) Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFBC02D))) // Amarelo escuro
                                }
                            } else {
                                Box(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentItem(
    appt: Appointment,
    hasLoyalty: Boolean,
    onValidateLoyalty: (String) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onDeleteBlockage: (String) -> Unit
) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(appt.date ?: Date())
    val isPending = appt.status == "PENDING"
    val isBlocked = appt.status == "BLOCKED"
    val isConfirmed = appt.status == "CONFIRMED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else if (isPending) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isPending) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = time,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge,
                color = if (isBlocked) {
                    MaterialTheme.colorScheme.outline
                } else if (isPending) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                if (isBlocked) {
                    Text(
                        text = "🔒 Horário Bloqueado",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(appt.clientName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    if (isPending) {
                        Text(
                            text = "⚠️ Aguardando Confirmação",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Text(appt.clientName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val detailsText = if (appt.professionalName.isNotBlank()) {
                        "${appt.serviceName} • com ${appt.professionalName}"
                    } else {
                        appt.serviceName
                    }
                    Text(detailsText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                    if (appt.loyaltyRedeemed) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFFFF9C4),
                            contentColor = Color(0xFFF57F17),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = Color(0xFFF57F17)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Cortesia Fidelidade (Grátis)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else if (isConfirmed && hasLoyalty) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (appt.loyaltyValidated) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                contentColor = Color(0xFF2E7D32),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "✓ Fidelidade Concedida",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Fidelidade Pendente",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            if (isBlocked) {
                IconButton(onClick = { onDeleteBlockage(appt.id) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Desbloquear Horário",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else if (isPending) {
                IconButton(
                    onClick = { onUpdateStatus(appt.id, "CONFIRMED") },
                    modifier = Modifier
                        .background(Color(0xFF2E7D32), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmar Agendamento",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isConfirmed && hasLoyalty && !appt.loyaltyValidated) {
                        Button(
                            onClick = { onValidateLoyalty(appt.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Validar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Confirmado",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

fun isSameDay(d1: Date, d2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = d1 }
    val cal2 = Calendar.getInstance().apply { time = d2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
