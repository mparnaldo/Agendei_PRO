package com.example.agendei_admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agendei_pro.core.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminClientsScreen(onBack: () -> Unit) {
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("profiles").addSnapshotListener { snapshot, _ ->
            users = snapshot?.toObjects(UserProfile::class.java) ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Clientes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(users) { user ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(user.name, style = MaterialTheme.typography.titleMedium)
                        Text(user.email, style = MaterialTheme.typography.bodySmall)
                        Text("ID: ${user.uid}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
