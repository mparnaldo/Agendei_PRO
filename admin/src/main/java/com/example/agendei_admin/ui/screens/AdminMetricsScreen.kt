package com.example.agendei_admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agendei_pro.core.model.Appointment
import com.example.agendei_pro.core.model.Salon
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormatSymbols
import java.util.*

data class MonthData(val monthLabel: String, val count: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMetricsScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var price by remember { mutableStateOf(49.90) }
    var salons by remember { mutableStateOf<List<Salon>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Obter preço
        db.collection("config").document("pricing").get().addOnSuccessListener {
            price = it.getString("value")?.toDoubleOrNull() ?: 49.90
        }

        // Obter salões
        db.collection("salons").get().addOnSuccessListener { snapshot ->
            salons = snapshot.documents.mapNotNull { it.toObject(Salon::class.java)?.copy(id = it.id) }
        }

        // Obter agendamentos
        db.collection("appointments").get().addOnSuccessListener { snapshot ->
            appointments = snapshot.documents.mapNotNull { it.toObject(Appointment::class.java) }
            isLoading = false
        }
    }

    // Cálculos de Métricas
    val totalSalons = salons.size
    val activeSubscriptions = salons.count { it.isSubscribed }
    
    // Um salão está em trial se não for premium e a data de início for recente (menos de 10 dias)
    val now = Date()
    val tenDaysInMs = 10L * 24L * 60L * 60L * 1000L
    val trialSalons = salons.count { salon ->
        if (salon.isSubscribed) false
        else {
            val start = salon.trialStartDate ?: now
            now.time - start.time < tenDaysInMs
        }
    }
    
    val expiredSalons = (totalSalons - activeSubscriptions - trialSalons).coerceAtLeast(0)

    val mrr = activeSubscriptions * price
    val grossRevenue = activeSubscriptions * price // Faturamento acumulado estimado atual mensal
    
    val churnRate = if (totalSalons > 0) (expiredSalons.toFloat() / totalSalons.toFloat()) * 100f else 0f
    val ltv = if (churnRate > 0f) price / (churnRate / 100f) else mrr

    // Agrupamento de Agendamentos por Mês (últimos 6 meses)
    val monthsData = remember(appointments) {
        val calendar = Calendar.getInstance()
        val ptBrLocale = Locale("pt", "BR")
        val symbols = DateFormatSymbols(ptBrLocale)
        val shortMonths = symbols.shortMonths

        val counts = IntArray(12) { 0 }
        appointments.forEach { appt ->
            appt.date?.let { date ->
                calendar.time = date
                val month = calendar.get(Calendar.MONTH)
                if (month in 0..11) {
                    counts[month]++
                }
            }
        }

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val list = mutableListOf<MonthData>()
        // Mostra os últimos 6 meses cronologicamente
        for (i in 5 downTo 0) {
            val targetMonth = (currentMonth - i + 12) % 12
            val label = shortMonths[targetMonth].uppercase().replace(".", "")
            list.add(MonthData(label, counts[targetMonth]))
        }
        list
    }

    val maxAppointmentCount = monthsData.maxOfOrNull { it.count } ?: 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Métricas e Saúde Financeira") },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Resumo do Ecossistema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Cards de Métricas Financeiras
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        title = "MRR",
                        value = String.format(Locale.getDefault(), "R$ %.2f", mrr),
                        subtitle = "Receita Recorrente",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "LTV",
                        value = String.format(Locale.getDefault(), "R$ %.2f", ltv),
                        subtitle = "Valor de Vida Útil",
                        icon = Icons.Default.AttachMoney,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        title = "Churn Rate",
                        value = String.format(Locale.getDefault(), "%.1f %%", churnRate),
                        subtitle = "$expiredSalons salões expirados",
                        icon = Icons.Default.Analytics,
                        color = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Faturamento",
                        value = String.format(Locale.getDefault(), "R$ %.2f", grossRevenue),
                        subtitle = "Mensal Estimado",
                        icon = Icons.Default.ShowChart,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Detalhamento de Salões
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Distribuição de Salões", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Total Cadastrado:")
                            Text("$totalSalons", fontWeight = FontWeight.Bold)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Assinantes Ativos:")
                            Text("$activeSubscriptions", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Em Período de Testes:")
                            Text("$trialSalons", fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Expirados (Sem Assinatura):")
                            Text("$expiredSalons", fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Gráfico de Engajamento Global (Agendamentos por mês)
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Agendamentos Criados (Últimos 6 Meses)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Gráfico de Barras Responsivo e Elegante
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            monthsData.forEach { data ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${data.count}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .fillMaxHeight(fraction = (data.count.toFloat() / maxAppointmentCount.toFloat()).coerceIn(0.05f, 1f))
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = data.monthLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
