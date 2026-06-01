package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.agendei_pro.ui.theme.Agendei_PROTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentName: String,
    userPhotoUrl: String?,
    subscriptionStatus: String? = null,
    onSaveName: (String) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onThemeClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Sair") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!userPhotoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = userPhotoUrl,
                    contentDescription = "Foto de Perfil",
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            }

            if (!subscriptionStatus.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val isSubscribed = subscriptionStatus.contains("Ativa")
                val badgeBg = if (isSubscribed) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                val badgeText = if (isSubscribed) Color(0xFF2E7D32) else Color(0xFFE65100)
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = subscriptionStatus,
                        color = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Seu Nome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSaveName(name) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank() && name != currentName
            ) { Text("Salvar Alterações de Nome") }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão de Temas em Destaque 🎨
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cores do Aplicativo", fontWeight = FontWeight.Bold)
                        Text("Escolha entre os 10 temas disponíveis", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(onClick = onThemeClick) {
                        Icon(Icons.Default.Palette, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mudar")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Excluir minha conta permanentemente")
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Apagar Conta?") },
                text = { Text("Essa ação é definitiva e apagará todos os seus dados.") },
                confirmButton = {
                    Button(onClick = { onDeleteAccount() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Sim, excluir tudo")
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
            )
        }
    }
}
