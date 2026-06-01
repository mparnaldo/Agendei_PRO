package com.example.agendei_admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agendei_pro.core.model.Salon
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSalonsScreen(onBack: () -> Unit) {
    var salons by remember { mutableStateOf<List<Salon>>(emptyList()) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("salons").addSnapshotListener { snapshot, _ ->
            salons = snapshot?.toObjects(Salon::class.java) ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestão de Salões") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(salons) { salon ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(salon.name, style = MaterialTheme.typography.titleMedium)
                            Text("ID: ${salon.id}", style = MaterialTheme.typography.bodySmall)
                            Text(if (salon.isSubscribed) "Assinatura: ATIVA" else "Modo Trial", 
                                 color = if (salon.isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                        Button(onClick = { 
                            db.collection("salons").document(salon.id).update("isSubscribed", !salon.isSubscribed)
                        }) {
                            Icon(Icons.Default.CardGiftcard, null)
                        }
                    }
                }
            }
        }
    }
}
