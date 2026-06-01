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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agendei_pro.core.model.Appointment
import com.example.agendei_pro.ui.viewmodel.AgendaViewModel
import com.example.agendei_pro.ui.viewmodel.AgendaViewMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    viewModel: AgendaViewModel = viewModel(),
    onUpdateStatus: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val appointments by viewModel.appointments.collectAsState()
    val allAppointments by viewModel.allAppointments.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

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
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewMode == AgendaViewMode.CALENDAR_MONTH) {
                // Exibe o calendário no topo
                MonthlyCalendarView(
                    allAppointments = allAppointments,
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.onDateSelected(it) }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Exibe a lista do dia selecionado embaixo
                AgendaListView(
                    appointments = appointments,
                    selectedDate = selectedDate,
                    title = "Agendamentos do Dia",
                    onUpdateStatus = { id, s -> viewModel.updateStatus(id, s) }
                )
            } else {
                // Exibe a linha do tempo / lista contínua rolando
                TimelineView(
                    allAppointments = allAppointments,
                    onUpdateStatus = { id, s -> viewModel.updateStatus(id, s) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineView(allAppointments: List<Appointment>, onUpdateStatus: (String, String) -> Unit) {
    val sdfGroup = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("pt", "BR"))
    val grouped = remember(allAppointments) {
        allAppointments.groupBy { appt ->
            val cal = Calendar.getInstance().apply { time = appt.date ?: Date() }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.time
        }
    }

    if (allAppointments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum agendamento encontrado.", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            grouped.forEach { (date, appts) ->
                stickyHeader {
                    Text(
                        text = sdfGroup.format(date).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 8.dp)
                    )
                }
                items(appts) { appt ->
                    AppointmentItem(appt, onUpdateStatus)
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
    onUpdateStatus: (String, String) -> Unit
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
                items(appointments) { AppointmentItem(it, onUpdateStatus) }
            }
        }
    }
}

@Composable
fun MonthlyCalendarView(allAppointments: List<Appointment>, selectedDate: Date, onDateSelected: (Date) -> Unit) {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = (1..maxDays).map { d ->
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, d)
        c.time
    }

    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.padding(8.dp)) {
        items(days) { date ->
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

@Composable
fun AppointmentItem(appt: Appointment, onUpdateStatus: (String, String) -> Unit) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(appt.date ?: Date())
    val isPending = appt.status == "PENDING"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
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
                color = if (isPending) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
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
                Text(appt.serviceName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            
            if (isPending) {
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

fun isSameDay(d1: Date, d2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = d1 }
    val cal2 = Calendar.getInstance().apply { time = d2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
