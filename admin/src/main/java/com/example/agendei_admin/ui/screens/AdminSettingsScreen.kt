package com.example.agendei_admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(onBack: () -> Unit) {
    var price by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        db.collection("config").document("pricing").get().addOnSuccessListener {
            price = it.getString("value") ?: "49.90"
        }
        db.collection("config").document("announcement").get().addOnSuccessListener {
            alertMessage = it.getString("message") ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações do Império") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text("Assinatura Mensal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Defina o valor que os salões verão na tela de assinatura.", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Valor (Ex: 49.90)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("R$ ", modifier = Modifier.padding(start = 12.dp)) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Aviso Global", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Uma mensagem que aparecerá para todos os usuários (Opcional).", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = alertMessage,
                onValueChange = { alertMessage = it },
                label = { Text("Mensagem de Alerta") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    isLoading = true
                    val batch = db.batch()
                    batch.set(db.collection("config").document("pricing"), mapOf("value" to price))
                    batch.set(db.collection("config").document("announcement"), mapOf("message" to alertMessage))
                    
                    batch.commit().addOnSuccessListener {
                        isLoading = false
                        Toast.makeText(context, "Configurações salvas com sucesso!", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        isLoading = false
                        Toast.makeText(context, "Erro ao salvar: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                else {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Tudo")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Essas alterações refletem instantaneamente no app dos usuários.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
