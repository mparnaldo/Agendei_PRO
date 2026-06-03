package com.example.agendei_admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.agendei_pro.core.auth.AuthManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AdminLoginScreen(onLoginSuccess: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Agendei ADMIN", style = MaterialTheme.typography.headlineLarge)
        Text("Painel de Controle Imperial", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // LOGIN VIA GOOGLE (O jeito fácil)
        Button(
            onClick = {
                isLoading = true
                scope.launch {
                    authManager.signInWithGoogle().onSuccess {
                        if (auth.currentUser?.email == "mpires.arnaldo@gmail.com") {
                            onLoginSuccess()
                        } else {
                            error = "Acesso Negado: Você não é o Arnaldo!"
                            auth.signOut()
                        }
                        isLoading = false
                    }.onFailure {
                        error = "Erro no Google Sign-In: ${it.message}"
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            enabled = !isLoading
        ) {
            Text("Entrar com Google (Recomendado)")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("ou use e-mail e senha", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha Secreta") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(), // BOLINHAS DE PROTEÇÃO! 🛡️
            enabled = !isLoading
        )
        
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                isLoading = true
                scope.launch {
                    try {
                        auth.signInWithEmailAndPassword(email, password)
                        if (auth.currentUser?.email == "mpires.arnaldo@gmail.com") {
                            onLoginSuccess()
                        } else {
                            error = "Você não tem poder aqui!"
                            auth.signOut()
                        }
                    } catch (e: Exception) {
                        error = "Erro: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            else Text("Entrar via E-mail")
        }
    }
}
