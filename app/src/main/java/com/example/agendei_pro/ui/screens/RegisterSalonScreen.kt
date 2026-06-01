package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.agendei_pro.ui.theme.Agendei_PROTheme

@Composable
fun RegisterSalonScreen(
    onRegisterClick: (String, String, String, String) -> Unit
) {
    var salonName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedSegment by remember { mutableStateOf("BARBEARIA") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Cadastre seu Salão",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 32.dp, top = 40.dp)
        )

        OutlinedTextField(
            value = salonName,
            onValueChange = { salonName = it },
            label = { Text("Nome do Salão") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Endereço Completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Telefone / WhatsApp") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Selecione o Ramo de Atividade",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp),
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentButton(label = "💈 Barbearia", selected = selectedSegment == "BARBEARIA", onClick = { selectedSegment = "BARBEARIA" }, modifier = Modifier.weight(1f))
                SegmentButton(label = "💇 Salão/Cabelo", selected = selectedSegment == "CABELEIREIRO", onClick = { selectedSegment = "CABELEIREIRO" }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentButton(label = "💅 Manicure", selected = selectedSegment == "MANICURE", onClick = { selectedSegment = "MANICURE" }, modifier = Modifier.weight(1f))
                SegmentButton(label = "✨ Estética", selected = selectedSegment == "ESTETICA", onClick = { selectedSegment = "ESTETICA" }, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    isLoading = true
                    onRegisterClick(salonName, address, phone, selectedSegment)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = salonName.isNotBlank() && address.isNotBlank()
            ) {
                Text("Criar meu Salão e Iniciar Teste")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ao clicar em criar, você inicia seu período de 10 dias de avaliação gratuita.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun SegmentButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier.height(48.dp)) {
            Text(label, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.height(48.dp)) {
            Text(label)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterSalonPreview() {
    Agendei_PROTheme {
        RegisterSalonScreen(onRegisterClick = { _, _, _, _ -> })
    }
}
