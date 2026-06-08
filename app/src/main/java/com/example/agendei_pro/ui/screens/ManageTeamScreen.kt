package com.example.agendei_pro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agendei_pro.core.model.Professional
import com.example.agendei_pro.core.model.Service
import com.example.agendei_pro.ui.viewmodel.TeamViewModel
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTeamScreen(
    viewModel: TeamViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val salon by viewModel.salon.collectAsState()
    val professionals by viewModel.professionals.collectAsState()
    val services by viewModel.services.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedProfessional by remember { mutableStateOf<Professional?>(null) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showSettingsDialogFor by remember { mutableStateOf<Professional?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minha Equipe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val maxProfs = salon?.maxProfessionals ?: 2
                    if (professionals.size >= maxProfs) {
                        showUpgradeDialog = true
                    } else {
                        selectedProfessional = null
                        showAddEditDialog = true
                    }
                }
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Plan Info Header
            salon?.let { s ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plano ${s.subscriptionPlan}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Você cadastrou ${professionals.size} de ${s.maxProfessionals} profissionais permitidos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (professionals.size >= s.maxProfessionals) {
                            TextButton(onClick = { showUpgradeDialog = true }) {
                                Text("Upgrade", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (professionals.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Groups,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum profissional cadastrado.",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(professionals, key = { it.id }) { pro ->
                        ProfessionalItem(
                            professional = pro,
                            services = services,
                            onEdit = {
                                selectedProfessional = pro
                                showAddEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteProfessional(pro.id) { success, err ->
                                    if (success) {
                                        Toast.makeText(context, "Profissional excluído!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Erro: $err", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onConfig = if (salon?.isConfigurationIndividualized == true) {
                                { showSettingsDialogFor = pro }
                            } else null
                        )
                    }
                }
            }
        }

        // Add / Edit Dialog
        if (showAddEditDialog) {
            AddEditProfessionalDialog(
                professional = selectedProfessional,
                allServices = services,
                isSaving = isSaving,
                onDismiss = { showAddEditDialog = false },
                onSearchGooglePhoto = { email, callback ->
                    viewModel.fetchGoogleProfilePhoto(email, callback)
                },
                onConfirm = { name, specs, photoUrl, photoUri ->
                    if (selectedProfessional == null) {
                        viewModel.addProfessional(name, specs, photoUrl, photoUri) { success, err ->
                            if (success) {
                                Toast.makeText(context, "Profissional cadastrado!", Toast.LENGTH_SHORT).show()
                                showAddEditDialog = false
                            } else {
                                Toast.makeText(context, "Erro: $err", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        val updatedPro = selectedProfessional!!.copy(
                            name = name,
                            specialties = specs,
                            photoUrl = photoUrl ?: selectedProfessional!!.photoUrl
                        )
                        viewModel.updateProfessional(updatedPro, photoUri) { success, err ->
                            if (success) {
                                Toast.makeText(context, "Profissional atualizado!", Toast.LENGTH_SHORT).show()
                                showAddEditDialog = false
                            } else {
                                Toast.makeText(context, "Erro: $err", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }

        // Settings Dialog
        showSettingsDialogFor?.let { pro ->
            ProfessionalSettingsDialog(
                professional = pro,
                allServices = services,
                salon = salon,
                onDismiss = { showSettingsDialogFor = null },
                onConfirm = { updatedPro ->
                    viewModel.updateProfessional(updatedPro, null) { success, err ->
                        if (success) {
                            Toast.makeText(context, "Configurações atualizadas!", Toast.LENGTH_SHORT).show()
                            showSettingsDialogFor = null
                        } else {
                            Toast.makeText(context, "Erro ao atualizar: $err", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        // Upgrade Plan Dialog
        if (showUpgradeDialog) {
            val db = FirebaseFirestore.getInstance()
            val userUid = salon?.id ?: ""
            AlertDialog(
                onDismissRequest = { showUpgradeDialog = false },
                title = { Text("Upgrade de Plano Necessário") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Você atingiu o limite de profissionais para o plano atual. Escolha um plano superior para continuar cadastrando:")
                        
                        val upgradeOptions = listOf(
                            Triple("BRONZE", 2, "Plano Bronze (2 Profs - R$ 110,00/mês)"),
                            Triple("PRATA", 5, "Plano Prata (5 Profs - R$ 150,00/mês)"),
                            Triple("OURO", 10, "Plano Ouro (10 Profs - R$ 200,00/mês)")
                        )

                        upgradeOptions.forEach { (planName, limit, label) ->
                            Button(
                                onClick = {
                                    db.collection("salons").document(userUid).update(mapOf(
                                        "subscriptionPlan" to planName,
                                        "maxProfessionals" to limit,
                                        "isSubscribed" to true
                                    )).addOnSuccessListener {
                                        Toast.makeText(context, "Plano atualizado para $planName!", Toast.LENGTH_SHORT).show()
                                        showUpgradeDialog = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (salon?.subscriptionPlan == planName) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.primary
                                ),
                                enabled = (salon?.subscriptionPlan != planName)
                            ) {
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showUpgradeDialog = false }) {
                        Text("Fechar")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfessionalItem(
    professional: Professional,
    services: List<Service>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConfig: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo or Initial Circle Avatar
            if (!professional.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = professional.photoUrl,
                    contentDescription = "Foto de ${professional.name}",
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
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = professional.name.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = professional.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val specialtyNames = if (professional.specialties.isEmpty()) {
                    "Todos os Serviços"
                } else {
                    professional.specialties.mapNotNull { id ->
                        services.find { it.id == id }?.name
                    }.joinToString(", ")
                }
                
                Text(
                    text = "Especialidades: $specialtyNames",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (onConfig != null) {
                IconButton(onClick = onConfig) {
                    Icon(Icons.Default.Settings, "Configurações", tint = MaterialTheme.colorScheme.secondary)
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalSettingsDialog(
    professional: Professional,
    allServices: List<Service>,
    salon: com.example.agendei_pro.core.model.Salon?,
    onDismiss: () -> Unit,
    onConfirm: (Professional) -> Unit
) {
    var hasCustomSchedule by remember { mutableStateOf(professional.hasCustomSchedule) }
    var openingTime by remember { mutableStateOf(if (professional.hasCustomSchedule) professional.openingTime else salon?.openingTime ?: "08:00") }
    var closingTime by remember { mutableStateOf(if (professional.hasCustomSchedule) professional.closingTime else salon?.closingTime ?: "18:00") }
    var breakStart by remember { mutableStateOf(if (professional.hasCustomSchedule) professional.breakStart else salon?.breakStart ?: "12:00") }
    var breakEnd by remember { mutableStateOf(if (professional.hasCustomSchedule) professional.breakEnd else salon?.breakEnd ?: "13:00") }
    var selectedDays by remember { mutableStateOf(if (professional.hasCustomSchedule) professional.workingDays else salon?.workingDays ?: listOf(2, 3, 4, 5, 6, 7)) }

    var hasCustomLoyalty by remember { mutableStateOf(professional.hasCustomLoyalty) }
    var hasLoyaltyProgram by remember { mutableStateOf(professional.hasLoyaltyProgram) }
    var loyaltyRequired by remember { mutableStateOf(professional.loyaltyRequiredServices.toString()) }
    var loyaltyReward by remember { mutableStateOf(professional.loyaltyRewardDescription) }
    
    val overriddenPrices = remember {
        mutableStateMapOf<String, String>().apply {
            professional.servicePrices.forEach { (serviceId, price) ->
                put(serviceId, price.toString())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes de ${professional.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Working Hours Customizable Toggle ⏰
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Horário de Trabalho Personalizado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Definir jornada de trabalho específica para este profissional", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = hasCustomSchedule, onCheckedChange = { hasCustomSchedule = it })
                }

                if (hasCustomSchedule) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = openingTime,
                            onValueChange = { openingTime = it },
                            label = { Text("Abre") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = closingTime,
                            onValueChange = { closingTime = it },
                            label = { Text("Fecha") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = breakStart,
                            onValueChange = { breakStart = it },
                            label = { Text("Almoço Início") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = breakEnd,
                            onValueChange = { breakEnd = it },
                            label = { Text("Almoço Fim") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Dias de Trabalho", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1 to "D", 2 to "S", 3 to "T", 4 to "Q", 5 to "Q", 6 to "S", 7 to "S").forEach { (id, label) ->
                            FilterChip(
                                selected = selectedDays.contains(id),
                                onClick = {
                                    selectedDays = if (selectedDays.contains(id)) {
                                        selectedDays.filter { it != id }
                                    } else {
                                        selectedDays + id
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Este profissional segue os horários e dias de funcionamento gerais do salão.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // 2. Loyalty Customizable Toggle 🎁
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fidelidade Personalizada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Cartão de fidelidade específico para a agenda deste profissional", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = hasCustomLoyalty, onCheckedChange = { hasCustomLoyalty = it })
                }

                if (hasCustomLoyalty) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Habilitar Fidelidade", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Switch(checked = hasLoyaltyProgram, onCheckedChange = { hasLoyaltyProgram = it })
                    }

                    if (hasLoyaltyProgram) {
                        OutlinedTextField(
                            value = loyaltyRequired,
                            onValueChange = { loyaltyRequired = it },
                            label = { Text("Serviços para Ganhar Prêmio") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = loyaltyReward,
                            onValueChange = { loyaltyReward = it },
                            label = { Text("Descrição do Prêmio (Ex: Barba Grátis)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Este profissional participa do programa de fidelidade geral do salão.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // 3. Service Overrides 💸
                Text("Preços Diferenciados (Overrides)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Deixe em branco para usar o preço padrão do salão.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val eligibleServices = allServices.filter { service ->
                    professional.specialties.isEmpty() || professional.specialties.contains(service.id)
                }

                eligibleServices.forEach { service ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(text = service.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Padrão: R$ %.2f".format(service.price), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = overriddenPrices[service.id] ?: "",
                            onValueChange = { input ->
                                if (input.isEmpty()) {
                                    overriddenPrices.remove(service.id)
                                } else if (input.toDoubleOrNull() != null || input.endsWith(".")) {
                                    overriddenPrices[service.id] = input
                                }
                            },
                            label = { Text("Preço") },
                            placeholder = { Text("Ex: 75.00") },
                            prefix = { Text("R$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPrices = overriddenPrices.mapNotNull { (id, priceStr) ->
                        val doubleVal = priceStr.toDoubleOrNull()
                        if (doubleVal != null) id to doubleVal else null
                    }.toMap()

                    val updatedPro = professional.copy(
                        openingTime = openingTime,
                        closingTime = closingTime,
                        breakStart = breakStart,
                        breakEnd = breakEnd,
                        workingDays = selectedDays,
                        servicePrices = finalPrices,
                        hasCustomSchedule = hasCustomSchedule,
                        hasCustomLoyalty = hasCustomLoyalty,
                        hasLoyaltyProgram = hasLoyaltyProgram,
                        loyaltyRequiredServices = loyaltyRequired.toIntOrNull() ?: 10,
                        loyaltyRewardDescription = loyaltyReward
                    )
                    onConfirm(updatedPro)
                }
            ) {
                Text("Salvar Ajustes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditProfessionalDialog(
    professional: Professional?,
    allServices: List<Service>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSearchGooglePhoto: (String, (String?, String?, String?) -> Unit) -> Unit,
    onConfirm: (String, List<String>, String?, android.net.Uri?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(professional?.name ?: "") }
    var email by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(professional?.photoUrl) }
    var selectedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSearchingGoogle by remember { mutableStateOf(false) }

    val selectedSpecialties = remember { 
        mutableStateListOf<String>().apply {
            professional?.specialties?.let { addAll(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (professional == null) "Novo Profissional" else "Editar Profissional") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular Photo Picker Preview
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedPhotoUri != null) {
                        AsyncImage(
                            model = selectedPhotoUri,
                            contentDescription = "Foto selecionada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Foto do Google",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Selecionar Foto",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Foto",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Alterar Foto (Galeria)", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Google Account Email Link Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail Google") },
                            placeholder = { Text("profissional@gmail.com") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                if (email.isNotBlank()) {
                                    isSearchingGoogle = true
                                    onSearchGooglePhoto(email) { fetchedPhoto, fetchedName, error ->
                                        isSearchingGoogle = false
                                        if (error != null) {
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        } else {
                                            if (!fetchedPhoto.isNullOrBlank()) photoUrl = fetchedPhoto
                                            if (!fetchedName.isNullOrBlank() && name.isBlank()) name = fetchedName
                                            Toast.makeText(context, "Foto carregada com sucesso!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = email.isNotBlank() && !isSearchingGoogle
                        ) {
                            if (isSearchingGoogle) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Link, contentDescription = "Buscar Foto Google")
                            }
                        }
                    }
                    Text(
                        text = "Vincule a conta Google do profissional para puxar a foto e o nome dele do Agendei.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Profissional") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Serviços Habilitados:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Se nenhum for selecionado, o profissional realizará todos os serviços.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Specialties selection list using flow chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allServices.forEach { service ->
                        val isSelected = selectedSpecialties.contains(service.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedSpecialties.remove(service.id)
                                } else {
                                    selectedSpecialties.add(service.id)
                                }
                            },
                            label = { Text(service.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedSpecialties.toList(), photoUrl, selectedPhotoUri) },
                enabled = name.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Salvar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancelar")
            }
        }
    )
}
