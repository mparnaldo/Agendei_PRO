package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendei_pro.core.model.Appointment
import com.example.agendei_pro.core.model.Professional
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.drawscope.Stroke
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalonPerformanceScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val salonId = auth.currentUser?.uid

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var professionals by remember { mutableStateOf<List<Professional>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf("MÊS") } // SEMANA, MÊS, ANO, TUDO

    LaunchedEffect(salonId) {
        if (salonId != null) {
            // Buscar Agendamentos
            db.collection("appointments")
                .whereEqualTo("salonId", salonId)
                .get()
                .addOnSuccessListener { snapshot ->
                    appointments = snapshot.documents.mapNotNull { it.toObject(Appointment::class.java) }
                    
                    // Buscar Profissionais
                    db.collection("salons").document(salonId).collection("professionals")
                        .get()
                        .addOnSuccessListener { proSnapshot ->
                            professionals = proSnapshot.documents.mapNotNull { it.toObject(Professional::class.java) }
                            isLoading = false
                        }
                        .addOnFailureListener {
                            isLoading = false
                        }
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    // Filtros de Período
    val now = Calendar.getInstance()
    val currentWeek = now.get(Calendar.WEEK_OF_YEAR)
    val currentMonth = now.get(Calendar.MONTH)
    val currentYear = now.get(Calendar.YEAR)

    val filteredAppointments = remember(appointments, selectedPeriod) {
        val cal = Calendar.getInstance()
        appointments.filter { appt ->
            val date = appt.date ?: return@filter false
            cal.time = date
            when (selectedPeriod) {
                "SEMANA" -> cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek
                "MÊS" -> cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                "ANO" -> cal.get(Calendar.YEAR) == currentYear
                else -> true // TUDO
            }
        }
    }

    // 1. Métricas Financeiras
    val realizedRevenue = filteredAppointments
        .filter { appt -> appt.status == "SERVED" }
        .sumOf { it.servicePrice }

    val expectedRevenue = filteredAppointments
        .filter { appt -> appt.status == "CONFIRMED" || appt.status == "PENDING" }
        .sumOf { it.servicePrice }

    val totalEstimated = realizedRevenue + expectedRevenue

    // 2. Taxa de Cancelamento
    val totalCount = filteredAppointments.size
    val cancelledAppointments = filteredAppointments.filter { it.status == "CANCELLED" || it.status == "BLOCKED" }
    val cancelledCount = cancelledAppointments.size
    val cancellationRate = if (totalCount > 0) (cancelledCount.toFloat() / totalCount.toFloat()) * 100f else 0f

    val ticketMedio = remember(totalEstimated, totalCount, cancelledCount) {
        val completedCount = totalCount - cancelledCount
        if (completedCount > 0) totalEstimated / completedCount else 0.0
    }

    val last6MonthsRevenue = remember(appointments) {
        val cal = Calendar.getInstance()
        val result = mutableListOf<Pair<String, Double>>()
        val sdf = SimpleDateFormat("MMM", Locale("pt", "BR"))
        
        for (i in 5 downTo 0) {
            val monthCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            val monthYearKey = "${monthCal.get(Calendar.YEAR)}_${monthCal.get(Calendar.MONTH)}"
            val label = sdf.format(monthCal.time).replace(".", "").uppercase()
            
            val revenue = appointments.filter { appt ->
                val date = appt.date
                if (date != null && (appt.status == "CONFIRMED" || appt.status == "SERVED")) {
                    cal.time = date
                    val apptMonthYear = "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}"
                    apptMonthYear == monthYearKey
                } else false
            }.sumOf { it.servicePrice }
            
            result.add(Pair(label, revenue))
        }
        result
    }

    // 3. Serviços Mais Agendados
    val servicesRanking = remember(filteredAppointments) {
        filteredAppointments
            .filter { it.status == "CONFIRMED" || it.status == "SERVED" || it.status == "PENDING" }
            .groupBy { it.serviceName }
            .map { (name, list) ->
                val count = list.size
                val revenue = list.sumOf { it.servicePrice }
                Triple(name, count, revenue)
            }
            .sortedByDescending { it.second } // Order by quantity
    }

    // 4. Profissionais Mais Requisitados
    val professionalsRanking = remember(filteredAppointments, professionals) {
        filteredAppointments
            .filter { it.status == "CONFIRMED" || it.status == "SERVED" || it.status == "PENDING" }
            .groupBy { it.professionalId }
            .map { (proId, list) ->
                val proName = list.firstOrNull()?.professionalName?.ifBlank { "Sem nome" } ?: "Tanto faz / Indefinido"
                val photoUrl = professionals.find { it.id == proId }?.photoUrl
                val count = list.size
                val revenue = list.sumOf { it.servicePrice }
                proId to quadruple(proName, photoUrl, count, revenue)
            }
            .sortedByDescending { it.second.third } // Order by quantity
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatórios & Performance", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filtro de Período
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("SEMANA", "MÊS", "ANO", "TUDO").forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period) }
                            )
                        }
                    }
                }

                // Grid de Cards Financeiros (Receitas)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Faturamento Estimado ($selectedPeriod)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currencyFormatter.format(totalEstimated),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Realizado (Passado)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(currencyFormatter.format(realizedRevenue), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Previsto (Futuro)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(currencyFormatter.format(expectedRevenue), fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), fontSize = 16.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ticket Médio por Atendimento", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(currencyFormatter.format(ticketMedio), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Gráfico Mensal (Últimos 6 Meses)
                MonthlyRevenueChart(last6MonthsRevenue, currencyFormatter)

                // Gráfico de Pizza (Serviços Mais Lucrativos)
                ServicesRevenuePieChart(servicesRanking, currencyFormatter)

                // Card de Taxa de Cancelamento e Visão de Agendamentos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Taxa de Cancelamento", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                                CircularProgressIndicator(
                                    progress = { cancellationRate / 100f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (cancellationRate > 15f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 6.dp
                                )
                                Text(String.format(Locale.US, "%.1f%%", cancellationRate), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$cancelledCount de $totalCount agendamentos", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Concluído", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(42.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${totalCount - cancelledCount}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Agendamentos OK", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Ranking de Serviços
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Serviços Mais Agendados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        if (servicesRanking.isEmpty()) {
                            Text("Sem dados no período.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        } else {
                            val maxCount = servicesRanking.maxOf { it.second }.toFloat()
                            servicesRanking.take(5).forEach { (name, count, revenue) ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Text("$count agendamentos (${currencyFormatter.format(revenue)})", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { count.toFloat() / maxCount },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Ranking de Profissionais
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Profissionais Mais Requisitados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        if (professionalsRanking.isEmpty()) {
                            Text("Sem dados no período.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        } else {
                            professionalsRanking.forEach { (proId, data) ->
                                val (proName, photoUrl, count, revenue) = data
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (proId == "") {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Groups, null, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(proName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(proName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("$count atendimentos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text(
                                        text = currencyFormatter.format(revenue),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
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

// Auxiliar para quadriplicar valores de retorno do map
private fun <A, B, C, D> quadruple(a: A, b: B, c: C, d: D): Quadruple<A, B, C, D> {
    return Quadruple(a, b, c, d)
}

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun MonthlyRevenueChart(data: List<Pair<String, Double>>, currencyFormatter: NumberFormat) {
    val maxRevenue = remember(data) { data.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f }
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Evolução Mensal (Últimos 6 Meses)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val paddingBottom = 40f
                val paddingTop = 20f
                val paddingLeft = 20f
                val paddingRight = 20f
                
                val chartHeight = canvasHeight - paddingTop - paddingBottom
                val barWidth = (canvasWidth - paddingLeft - paddingRight) / (data.size * 2f - 1f)
                
                for (i in data.indices) {
                    val (label, valDouble) = data[i]
                    val valFloat = valDouble.toFloat()
                    val barHeight = (valFloat / maxRevenue) * chartHeight
                    
                    val x = paddingLeft + (i * 2 * barWidth)
                    val y = canvasHeight - paddingBottom - barHeight
                    
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(4f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    
                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = android.text.TextPaint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(label, x + barWidth / 2f, canvasHeight - 8f, textPaint)
                    }
                    
                    if (valFloat > 0) {
                        drawContext.canvas.nativeCanvas.apply {
                            val textPaint = android.text.TextPaint().apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = 22f
                                textAlign = android.graphics.Paint.Align.CENTER
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            val formattedVal = if (valFloat >= 1000f) String.format(Locale.getDefault(), "R$ %.1fk", valFloat / 1000f) else String.format(Locale.getDefault(), "R$ %.0f", valFloat)
                            drawText(formattedVal, x + barWidth / 2f, y - 8f, textPaint)
                        }
                    }
                }
                
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, canvasHeight - paddingBottom),
                    end = Offset(canvasWidth, canvasHeight - paddingBottom),
                    strokeWidth = 2f
                )
            }
        }
    }
}

@Composable
fun ServicesRevenuePieChart(servicesRanking: List<Triple<String, Int, Double>>, currencyFormatter: NumberFormat) {
    val totalRevenue = remember(servicesRanking) { servicesRanking.sumOf { it.third }.toFloat() }
    val colors = listOf(
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFF9C27B0),
        Color(0xFFE91E63)
    )
    
    val slices = remember(servicesRanking, totalRevenue) {
        if (totalRevenue <= 0f) return@remember emptyList<Pair<String, Float>>()
        val top = servicesRanking.take(4).map { Pair(it.first, it.third.toFloat()) }.toMutableList()
        if (servicesRanking.size > 4) {
            val othersSum = servicesRanking.drop(4).sumOf { it.third }.toFloat()
            if (othersSum > 0f) {
                top.add(Pair("Outros", othersSum))
            }
        }
        top
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Faturamento por Serviço", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (slices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("Sem faturamento no período.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(110.dp)
                            .weight(1f)
                    ) {
                        var startAngle = -90f
                        for (i in slices.indices) {
                            val (_, value) = slices[i]
                            val sweepAngle = (value / totalRevenue) * 360f
                            drawArc(
                                color = colors[i % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx())
                            )
                            startAngle += sweepAngle
                        }
                    }
                    
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in slices.indices) {
                            val (name, value) = slices[i]
                            val percentage = (value / totalRevenue) * 100f
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colors[i % colors.size])
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(
                                        text = "${currencyFormatter.format(value)} (${String.format(Locale.US, "%.1f%%", percentage)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
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
