package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.agendei_pro.core.model.UserBinding
import com.example.agendei_pro.ui.theme.Agendei_PROTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySalonsScreen(
    salons: List<UserBinding>,
    onSetDefault: (String) -> Unit,
    onAddNew: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Salões") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = "Vincular novo")
            }
        }
    ) { padding ->
        if (salons.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Você ainda não vinculou nenhum salão.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(salons) { binding ->
                    SalonBindingItem(
                        binding = binding,
                        onSetDefault = { onSetDefault(binding.salonId) }
                    )
                }
            }
        }
    }
}

@Composable
fun SalonBindingItem(binding: UserBinding, onSetDefault: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSetDefault() },
        colors = CardDefaults.cardColors(
            containerColor = if (binding.isDefault) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (binding.isDefault) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = null,
                tint = if (binding.isDefault) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(binding.salonName, fontWeight = FontWeight.Bold)
                Text("Código: ${binding.salonCode}", style = MaterialTheme.typography.labelSmall)
            }
            if (binding.isDefault) {
                Text("PADRÃO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MySalonsPreview() {
    Agendei_PROTheme {
        MySalonsScreen(
            salons = listOf(
                UserBinding("1", "S1", "Salão da Jô", "PRO-1234AB", true),
                UserBinding("1", "S2", "Barbearia Arnaldo", "PRO-5678CD", false)
            ),
            onSetDefault = {},
            onAddNew = {},
            onNavigateBack = {}
        )
    }
}
