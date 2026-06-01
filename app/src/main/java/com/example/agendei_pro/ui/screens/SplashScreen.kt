package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

import com.example.agendei_pro.R

@Composable
fun SplashScreen(isPro: Boolean, onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500) // Retardo planejado kkk
        onFinish()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = if (isPro) R.drawable.agendeipro else R.drawable.agendei),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp)
            )
        }
    }
}
