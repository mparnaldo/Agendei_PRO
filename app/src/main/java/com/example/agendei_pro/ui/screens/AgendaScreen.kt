package com.example.agendei_pro.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.agendei_pro.core.model.Appointment
import com.example.agendei_pro.core.model.Professional
import com.example.agendei_pro.core.model.Salon
import com.example.agendei_pro.core.model.Service
import com.example.agendei_pro.ui.viewmodel.AgendaViewModel
import java.text.SimpleDateFormat
import java.util.*

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
    val salon by viewModel.salon.collectAsState()
    val services by viewModel.services.collectAsState()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var prefilledBlockTime by remember { mutableStateOf("08:00") }
    var prefilledBlockProId by remember { mutableStateOf("ALL") }

    var selectedStatusTab by remember { mutableIntStateOf(0) } // 0 = Todos, 1 = Pendentes, 2 = Cancelados
    var professionals by remember { mutableStateOf<List<Professional>>(emptyList()) }
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
                    val list = snapshot?.toObjects(Professional::class.java) ?: emptyList()
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

    val pendingCount = remember(filteredDayAppts) {
        filteredDayAppts.count { it.status == "PENDING" }
    }

    val cancelledCount = remember(filteredDayAppts) {
        filteredDayAppts.count { it.status == "CANCELLED" }
    }

    val activeCount = remember(filteredDayAppts) {
        filteredDayAppts.count { it.status != "CANCELLED" }
    }

    val sdfHeaderDate = remember { SimpleDateFormat("EEE, dd.MM", Locale("pt", "BR")) }
    val formattedDateHeader = remember(selectedDate) {
        val formatted = sdfHeaderDate.format(selectedDate)
        formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    val isSelectedToday = remember(selectedDate) {
        isSameDay(selectedDate, Date())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Botão Central de Data Clicável (Estilo Shelton: "Sáb. 22.08 ▼")
                    Surface(
                        onClick = { showDatePickerDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = formattedDateHeader,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    if (!isSelectedToday) {
                        TextButton(
                            onClick = {
                                viewModel.onDateSelected(Date())
                            }
                        ) {
                            Text("Hoje", fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = {
                        prefilledBlockTime = "12:00"
                        prefilledBlockProId = selectedProfessionalId ?: "ALL"
                        showBlockDialog = true
                    }) {
                        Icon(Icons.Default.Block, contentDescription = "Bloquear Horário", tint = MaterialTheme.colorScheme.error)
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
            // Header do Salão / Profissional Selecionado
            if (professionals.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = if (selectedProfessionalId == null) 0 else professionals.indexOfFirst { it.id == selectedProfessionalId } + 1,
                    edgePadding = 12.dp,
                    divider = {},
                    indicator = {}
                ) {
                    Tab(
                        selected = selectedProfessionalId == null,
                        onClick = { selectedProfessionalId = null }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedProfessionalId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = if (selectedProfessionalId == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Todos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
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
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                            ) {
                                if (!pro.photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = pro.photoUrl,
                                        contentDescription = pro.name,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
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
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Abas de Status: Todos | Pendentes | Cancelados (Fim da poluição visual!)
            TabRow(
                selectedTabIndex = selectedStatusTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedStatusTab == 0,
                    onClick = { selectedStatusTab = 0 },
                    text = {
                        Text("Todos ($activeCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                )
                Tab(
                    selected = selectedStatusTab == 1,
                    onClick = { selectedStatusTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pendentes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (pendingCount > 0) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFFF9800),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$pendingCount",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedStatusTab == 2,
                    onClick = { selectedStatusTab = 2 },
                    text = {
                        Text("Cancelados ($cancelledCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                )
            }

            // Visualização da Timeline / Lista
            DailyTimelineView(
                appointments = filteredDayAppts,
                selectedDate = selectedDate,
                selectedStatusTab = selectedStatusTab,
                salon = salon,
                services = services,
                selectedProfessionalId = selectedProfessionalId,
                professionals = professionals,
                onUpdateStatus = { id, s -> viewModel.updateStatus(id, s) },
                onDeleteBlockage = { viewModel.deleteAppointment(it) },
                onBlockSlot = { time, proId ->
                    prefilledBlockTime = time
                    prefilledBlockProId = proId
                    showBlockDialog = true
                }
            )
        }
    }

    // Modal de Calendário Mensal com Marcadores Azuis (Idêntico ao print do Shelton)
    if (showDatePickerDialog) {
        MonthCalendarModalDialog(
            allAppointments = filteredAllAppts,
            selectedDate = selectedDate,
            currentMonth = currentMonth,
            onNavigateMonth = { viewModel.navigateMonth(it) },
            onDateSelected = {
                viewModel.onDateSelected(it)
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    // Diálogo de Bloqueio de Horário com Recorrência
    if (showBlockDialog) {
        BlockSlotWithRecurrenceDialog(
            initialDate = selectedDate,
            initialTime = prefilledBlockTime,
            initialProId = prefilledBlockProId,
            professionals = professionals,
            onDismiss = { showBlockDialog = false },
            onConfirm = { date, time, reason, proId, proName, duration, recurrenceType, count ->
                viewModel.blockTimeSlot(
                    date = date,
                    timeStr = time,
                    reason = reason,
                    professionalId = proId,
                    professionalName = proName,
                    durationMinutes = duration,
                    recurrenceType = recurrenceType,
                    repeatCount = count
                )
                showBlockDialog = false
            }
        )
    }
}

// Diálogo Pop-up do Calendário Mensal com Bolinhas Azuis nos Dias com Agendamentos
@Composable
fun MonthCalendarModalDialog(
    allAppointments: List<Appointment>,
    selectedDate: Date,
    currentMonth: Calendar,
    onNavigateMonth: (Int) -> Unit,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val sdfMonth = SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onNavigateMonth(-1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior")
                }
                Text(
                    text = sdfMonth.format(currentMonth.time).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { onNavigateMonth(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Dias da semana: Dom, Seg, Ter...
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                val cal = (currentMonth.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    items(gridItems) { date ->
                        if (date == null) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        } else {
                            val isSelected = isSameDay(date, selectedDate)
                            val isToday = isSameDay(date, Date())
                            val dayAppts = allAppointments.filter { isSameDay(it.date ?: Date(), date) && it.status != "CANCELLED" }
                            val hasAppointments = dayAppts.isNotEmpty()

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else Color.Transparent
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = SimpleDateFormat("d", Locale.getDefault()).format(date),
                                        fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasAppointments) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else Color(0xFF1976D2) // Marcador azul estilo Google Calendar / Print
                                                )
                                        )
                                    } else {
                                        Spacer(Modifier.height(5.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

// Timeline Diária de Horários (Inspirada no print do Shelton)
@Composable
fun DailyTimelineView(
    appointments: List<Appointment>,
    selectedDate: Date,
    selectedStatusTab: Int,
    salon: Salon?,
    services: List<Service>,
    selectedProfessionalId: String?,
    professionals: List<Professional>,
    onUpdateStatus: (String, String) -> Unit,
    onDeleteBlockage: (String) -> Unit,
    onBlockSlot: (String, String) -> Unit
) {
    val context = LocalContext.current
    val isToday = remember(selectedDate) { isSameDay(selectedDate, Date()) }
    val timeSdf = remember { SimpleDateFormat("HH:mm", Locale.US) }

    // Se selecionou a aba de CANCELADOS
    if (selectedStatusTab == 2) {
        val cancelledAppts = appointments.filter { it.status == "CANCELLED" }
        if (cancelledAppts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Nenhum agendamento cancelado para este dia! 👍", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cancelledAppts) { appt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${appt.serviceName} (${getFormattedTime(appt.date)})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (appt.cancelledBy == "CLIENT") "Cancelado p/ cliente" else "Cancelado p/ salão",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Cliente: ${appt.clientName}", style = MaterialTheme.typography.bodySmall)
                            Text("Profissional: ${appt.professionalName.ifBlank { "Salão" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
        return
    }

    // Se selecionou a aba de PENDENTES
    if (selectedStatusTab == 1) {
        val pendingAppts = appointments.filter { it.status == "PENDING" }
        if (pendingAppts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Nenhuma solicitação pendente no momento! 🎉", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingAppts) { appt ->
                    PendingAppointmentCard(
                        appt = appt,
                        onAccept = { onUpdateStatus(appt.id, "CONFIRMED") },
                        onReject = { onUpdateStatus(appt.id, "CANCELLED") }
                    )
                }
            }
        }
        return
    }

    // ABA "TODOS": TIMELINE COMPLETA DE SLOTS
    val openingTime = salon?.openingTime ?: "08:00"
    val closingTime = salon?.closingTime ?: "20:00"
    val interval = if ((salon?.slotIntervalMinutes ?: 30) > 0) salon!!.slotIntervalMinutes else 30

    val timeSlots = remember(openingTime, closingTime, interval) {
        val list = mutableListOf<String>()
        try {
            val start = timeSdf.parse(openingTime) ?: Date()
            val end = timeSdf.parse(closingTime) ?: Date()
            val cal = Calendar.getInstance().apply { time = start }
            while (!cal.time.after(end)) {
                list.add(timeSdf.format(cal.time))
                cal.add(Calendar.MINUTE, interval)
            }
        } catch (e: Exception) {
            list.addAll(listOf("08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30", "20:00"))
        }
        list
    }

    val currentTimeStr = remember {
        val nowCal = Calendar.getInstance()
        String.format(Locale.US, "%02d:%02d", nowCal.get(Calendar.HOUR_OF_DAY), nowCal.get(Calendar.MINUTE))
    }

    val nonCancelledAppts = remember(appointments) {
        appointments.filter { it.status != "CANCELLED" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        timeSlots.forEach { slotTime ->
            // Checar se há agendamentos começando neste horário
            val apptsInSlot = nonCancelledAppts.filter { appt ->
                getFormattedTime(appt.date) == slotTime
            }

            // Checar se é horário de almoço fixo do salão
            val isLunchBreak = salon?.breakStart != null && salon.breakEnd != null &&
                    slotTime >= salon.breakStart && slotTime < salon.breakEnd

            item(key = slotTime) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Linha vermelha de "Agora" se estiver no dia de hoje
                    if (isToday && slotTime == currentTimeStr.take(3) + "00") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE53935)))
                            HorizontalDivider(color = Color(0xFFE53935), thickness = 2.dp, modifier = Modifier.weight(1f))
                            Text("Agora ($currentTimeStr)", color = Color(0xFFE53935), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Coluna de Horário na Esquerda (08:00, 08:30...)
                        Text(
                            text = slotTime,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(46.dp).padding(top = 8.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        // Conteúdo do Horário
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (apptsInSlot.isNotEmpty()) {
                                apptsInSlot.forEach { appt ->
                                    if (appt.status == "BLOCKED") {
                                        // Bloco de Bloqueio (Almoço, Café, etc.)
                                        BlockageItemCard(
                                            appt = appt,
                                            onDelete = { onDeleteBlockage(appt.id) }
                                        )
                                    } else if (appt.status == "PENDING") {
                                        // Bloco de Pendente
                                        PendingAppointmentCard(
                                            appt = appt,
                                            onAccept = { onUpdateStatus(appt.id, "CONFIRMED") },
                                            onReject = { onUpdateStatus(appt.id, "CANCELLED") }
                                        )
                                    } else {
                                        // Bloco de Agendamento Confirmado / Ativo (Estilo Shelton: Azul escuro com alto contraste)
                                        TimelineAppointmentBlock(
                                            appt = appt,
                                            onConclude = { onUpdateStatus(appt.id, "SERVED") },
                                            onCancel = { onUpdateStatus(appt.id, "CANCELLED") }
                                        )
                                    }
                                }
                            } else if (isLunchBreak) {
                                // Bloco de Intervalo de Almoço
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Restaurant, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Intervalo / Almoço", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            } else {
                                // Horário Livre (Slot fino e discreto)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .clickable { onBlockSlot(slotTime, selectedProfessionalId ?: "ALL") },
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = "🟢 Disponível",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "+ Bloquear",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
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
}

// Bloco de Agendamento Confirmado / Ativo (Alto Contraste Estilo Shelton)
@Composable
fun TimelineAppointmentBlock(
    appt: Appointment,
    onConclude: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val isServed = appt.status == "SERVED"
    val timeRange = remember(appt.date, appt.serviceDurationMinutes) {
        val startStr = getFormattedTime(appt.date)
        val cal = Calendar.getInstance().apply { time = appt.date ?: Date() }
        cal.add(Calendar.MINUTE, if (appt.serviceDurationMinutes > 0) appt.serviceDurationMinutes else 30)
        val endStr = getFormattedTime(cal.time)
        "$startStr - $endStr"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isServed) Color(0xFF2E7D32) else Color(0xFF1565C0) // Azul vibrante de alto contraste
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Horário de início e fim
                Text(
                    text = timeRange,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (appt.clientPhone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val cleanPhone = appt.clientPhone.replace(Regex("[^0-9]"), "")
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Chat, "WhatsApp", tint = Color(0xFF69F0AE), modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    if (!isServed) {
                        IconButton(
                            onClick = onConclude,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, "Concluir", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            // Nome do Cliente em Destaque
            Text(
                text = appt.clientName.ifBlank { "Cliente" },
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )

            // Nome do Serviço e Profissional
            Text(
                text = "${appt.serviceName} • ${appt.professionalName.ifBlank { "Salão" }}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )
        }
    }
}

// Bloco de Bloqueio (Café ☕, Almoço, etc.)
@Composable
fun BlockageItemCard(
    appt: Appointment,
    onDelete: () -> Unit
) {
    val isLunch = appt.clientName.contains("Almoço", ignoreCase = true)
    val isCoffee = appt.clientName.contains("Café", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF37474F) // Grafite escuro elegante
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isLunch) Icons.Default.Restaurant else if (isCoffee) Icons.Default.Coffee else Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFFFCC80),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = appt.clientName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${getFormattedTime(appt.date)} (${appt.serviceDurationMinutes} min)",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir Bloqueio", tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Card de Agendamento Pendente
@Composable
fun PendingAppointmentCard(
    appt: Appointment,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
        border = BorderStroke(1.dp, Color(0xFFFF9800))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ Solicitação Pendente (${getFormattedTime(appt.date)})",
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(appt.clientName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("${appt.serviceName} • Profissional: ${appt.professionalName}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Aceitar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Recusar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Diálogo de Bloqueio com Recorrência
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockSlotWithRecurrenceDialog(
    initialDate: Date,
    initialTime: String,
    initialProId: String,
    professionals: List<Professional>,
    onDismiss: () -> Unit,
    onConfirm: (Date, String, String, String, String, Int, String, Int) -> Unit
) {
    val context = LocalContext.current
    var selectedBlockDate by remember { mutableStateOf(initialDate) }
    var timeStr by remember { mutableStateOf(initialTime) }
    var reason by remember { mutableStateOf("Café ☕") }
    var durationMinutes by remember { mutableIntStateOf(30) }
    var selectedProId by remember { mutableStateOf(initialProId) }
    var recurrenceType by remember { mutableStateOf("ONCE") } // ONCE, DAILY, WEEKLY
    var repeatDaysCount by remember { mutableIntStateOf(30) }

    val selectedProName = remember(selectedProId, professionals) {
        if (selectedProId == "ALL") "Todo o Salão"
        else professionals.find { it.id == selectedProId }?.name ?: "Profissional"
    }

    val dateStr = remember(selectedBlockDate) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedBlockDate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bloquear Horário na Agenda 🔒", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Motivo rápido
                item {
                    Text("Motivo do Bloqueio:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Café ☕", "Almoço", "Médico", "Outro").forEach { r ->
                            FilterChip(
                                selected = reason == r,
                                onClick = { reason = r },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Data e Horário
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
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
                                label = { Text("Data") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = timeStr,
                            onValueChange = { timeStr = it },
                            label = { Text("Hora (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Duração
                item {
                    Text("Duração:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h").forEach { (d, label) ->
                            FilterChip(
                                selected = durationMinutes == d,
                                onClick = { durationMinutes = d },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Recorrência (Pedido expresso do Shelton: "bloquear nos próximos 100 dias")
                item {
                    Text("Repetir Bloqueio:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = recurrenceType == "ONCE",
                            onClick = { recurrenceType = "ONCE" },
                            label = { Text("Apenas hoje", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = recurrenceType == "DAILY",
                            onClick = { recurrenceType = "DAILY" },
                            label = { Text("Diariamente", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = recurrenceType == "WEEKLY",
                            onClick = { recurrenceType = "WEEKLY" },
                            label = { Text("Toda semana", fontSize = 11.sp) }
                        )
                    }
                }

                if (recurrenceType != "ONCE") {
                    item {
                        Text(
                            text = if (recurrenceType == "DAILY") "Repetir por quantos dias?" else "Repetir por quantas semanas?",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (recurrenceType == "DAILY") {
                                listOf(7, 30, 60, 100).forEach { count ->
                                    FilterChip(
                                        selected = repeatDaysCount == count,
                                        onClick = { repeatDaysCount = count },
                                        label = { Text("$count dias", fontSize = 11.sp) }
                                    )
                                }
                            } else {
                                listOf(4, 8, 12, 16).forEach { count ->
                                    FilterChip(
                                        selected = repeatDaysCount == count,
                                        onClick = { repeatDaysCount = count },
                                        label = { Text("$count semanas", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedBlockDate,
                        timeStr,
                        reason,
                        selectedProId,
                        selectedProName,
                        durationMinutes,
                        recurrenceType,
                        repeatDaysCount
                    )
                }
            ) {
                Text("Confirmar Bloqueio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun getFormattedTime(date: Date?): String {
    if (date == null) return ""
    val c = Calendar.getInstance().apply { time = date }
    return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}
