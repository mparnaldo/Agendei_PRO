package com.example.agendei_admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(onBack: () -> Unit) {
    var price by remember { mutableStateOf("") }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("config").document("pricing").get().addOnSuccessListener {
            price = it.getString("value") ?: "49.90"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes Globais") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp)) {
            Text("Valor da Assinatura Mensal", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Preço (R$)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { db.collection("config").document("pricing").set(mapOf("value" to price)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar Novo Preço")
            }
        }
    }
}
