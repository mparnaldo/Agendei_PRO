package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agendei_pro.ui.viewmodel.SupportViewModel
import com.example.agendei_pro.core.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    userProfile: UserProfile?,
    isProVersion: Boolean,
    viewModel: SupportViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val chatInfo by viewModel.chatInfo.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.initChat(isProVersion)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val faqItems = listOf(
        "Como alterar preços de serviços?" to "Para alterar ou criar preços de serviços:\n1. Acesse o menu lateral do aplicativo.\n2. Se for salão, clique em 'Ajustes do Salão' ou 'Gerenciar Serviços'.\n3. Clique em um serviço ou no botão (+).\n4. Informe o valor do serviço e clique em Salvar.",
        "Como alterar a logo do salão?" to "Para alterar a logo do salão:\n1. Acesse Ajustes do Salão.\n2. Clique no círculo da foto no topo para abrir a galeria.\n3. Escolha a imagem e aguarde o upload terminar. Caso dê erro de permissão, verifique se salvou as regras no console.",
        "Como adicionar profissionais?" to "Para adicionar profissionais à sua equipe:\n1. Abra o menu lateral do aplicativo.\n2. Selecione a opção 'Minha Equipe'.\n3. Clique no botão azul de adicionar (+).\n4. Preencha o nome e salve."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suporte & Ajuda") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Banner de Status do Chamado
            chatInfo?.let { chat ->
                when (chat.status) {
                    "PENDING_HUMAN" -> {
                        Surface(
                            color = Color(0xFFFFF9C4),
                            contentColor = Color(0xFFF57F17),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⏳ Chamado na fila do atendimento humano. Aguarde...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    "RESOLVED" -> {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ Este chamado foi marcado como Resolvido. Envie uma nova mensagem para reabrir.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Lista de Mensagens
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.sender == "USER"
                    val bubbleColor = if (isMe) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else if (msg.sender == "ADMIN") {
                        Color(0xFFE3F2FD) // Azul claro para o Admin
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    val contentColor = if (isMe) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else if (msg.sender == "ADMIN") {
                        Color(0xFF0D47A1)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val align = if (isMe) Alignment.End else Alignment.Start
                    val senderName = when (msg.sender) {
                        "USER" -> userProfile?.name ?: "Você"
                        "ADMIN" -> "Suporte Agendei PRO 👑"
                        else -> "Assistente Virtual 🤖"
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = align
                    ) {
                        Text(
                            text = senderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    )
                                )
                                .background(bubbleColor)
                                .padding(12.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = contentColor,
                                fontSize = 15.sp
                            )
                        }
                        msg.timestamp?.let {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            }

            val menuState by viewModel.menuState.collectAsState()

            // FAQ Rápido e Botão Escalar (Exibe somente quando o status for "BOT")
            if (chatInfo?.status == "BOT") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val title = when (menuState) {
                        com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN -> "💡 Dúvidas Frequentes (Selecione um assunto):"
                        com.example.agendei_pro.ui.viewmodel.SupportMenuState.SERVICES -> "💰 Dúvidas sobre Serviços & Preços:"
                        com.example.agendei_pro.ui.viewmodel.SupportMenuState.TEAM -> "👥 Dúvidas sobre Profissionais & Equipe:"
                        com.example.agendei_pro.ui.viewmodel.SupportMenuState.SCHEDULE -> "⏰ Dúvidas sobre Horários & Agenda:"
                        com.example.agendei_pro.ui.viewmodel.SupportMenuState.LOYALTY -> "🎁 Dúvidas sobre Fidelômetro (Fidelidade):"
                        com.example.agendei_pro.ui.viewmodel.SupportMenuState.BILLING -> "👑 Dúvidas sobre Assinatura & Premium:"
                        com.example.agendei_pro.ui.viewmodel.SupportMenuState.WAITING -> "⏳ Dúvidas sobre Fila de Espera:"
                        com.example.agendei_pro.ui.viewmodel.SupportMenuState.CLIENTS -> "🚫 Dúvidas sobre Clientes & Bloqueio:"
                        com.example.agendei_pro.ui.viewmodel.SupportMenuState.THEMES -> "🎨 Dúvidas sobre Temas & Visual:"
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Chips de Dúvidas Horizontais com Scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (menuState) {
                            com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN -> {
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.SERVICES) },
                                    label = { Text("Serviços & Preços 💰") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.TEAM) },
                                    label = { Text("Profissionais & Equipe 👥") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.SCHEDULE) },
                                    label = { Text("Horários & Agenda ⏰") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.LOYALTY) },
                                    label = { Text("Fidelômetro (Fidelidade) 🎁") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.BILLING) },
                                    label = { Text("Assinatura & Premium 👑") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.WAITING) },
                                    label = { Text("Fila de Espera ⏳") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.CLIENTS) },
                                    label = { Text("Clientes & Bloqueio 🚫") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.THEMES) },
                                    label = { Text("Temas & Visual 🎨") }
                                )
                            }
                            com.example.agendei_pro.ui.viewmodel.SupportMenuState.SERVICES -> {
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como cadastrar serviços?", "Para cadastrar serviços:\n1. Acesse o menu lateral do app.\n2. Vá em Ajustes do Salão -> Gerenciar Serviços.\n3. Clique no botão de adicionar (+).\n4. Preencha nome, preço formatado, duração e selecione a categoria.") },
                                    label = { Text("Como cadastrar serviços?") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como mudar foto de serviço?", "Para fotos de serviços:\n1. No cadastro do serviço, selecione 'Galeria local' ou clique em 'Buscar Outra' para puxar do LoremFlickr.\n2. As buscas do LoremFlickr pesquisam dinamicamente a palavra-chave do serviço!") },
                                    label = { Text("Como mudar foto de serviço?") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como criar categorias?", "Para criar categorias de serviços:\n1. Na dialog do serviço, no campo Categoria, selecione 'Nova Categoria...'.\n2. Escreva o nome da nova categoria e salve.") },
                                    label = { Text("Como criar categorias?") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN) },
                                    label = { Text("Voltar ↩️") }
                                )
                            }
                            com.example.agendei_pro.ui.viewmodel.SupportMenuState.TEAM -> {
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como cadastrar colaboradores?", "Para cadastrar equipe:\n1. Abra o menu lateral.\n2. Clique em 'Minha Equipe'.\n3. Clique em (+) para inserir nome, e-mail Google e as especialidades.") },
                                    label = { Text("Como cadastrar colaboradores?") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como puxar a foto do Google?", "Para obter a foto do profissional:\n1. No cadastro da equipe, preencha o E-mail Google dele.\n2. Clique no ícone de Link. O profissional deve ter logado no app uma vez para que o perfil exista no Firebase!") },
                                    label = { Text("Como puxar a foto do Google?") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Vincular serviços ao profissional", "Para vincular serviços habilitados:\n1. Clique no profissional na lista de equipe.\n2. Selecione as especialidades dele ou marque o chip 'Todos os Serviços' para automatizar.") },
                                    label = { Text("Vincular especialidades") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Horário individual do profissional", "Para horários customizados:\n1. Clique no profissional na lista de equipe.\n2. Ative a flag 'Definir Horário Customizado'.\n3. Configure dias de trabalho e horários individuais do colaborador.") },
                                    label = { Text("Horário individual do profissional") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN) },
                                    label = { Text("Voltar ↩️") }
                                )
                            }
                            com.example.agendei_pro.ui.viewmodel.SupportMenuState.SCHEDULE -> {
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Horário de funcionamento do salão", "Para horários gerais do salão:\n1. Vá no menu lateral -> Ajustes do Salão.\n2. Configure os horários de abertura, fechamento e almoço/intervalo.") },
                                    label = { Text("Horário de funcionamento") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Configurar slot de agendamento", "Para configurar o slot de tempo:\n1. Vá em Ajustes do Salão.\n2. Defina os minutos de slot (ex: 30min ou 60min) para controle de grade.") },
                                    label = { Text("Duração do slot") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Ativar aceite automático", "Para aceitar agendamentos automaticamente:\n1. Vá em Ajustes do Salão.\n2. Habilite a flag 'Aceite Automático' para que o app confirme agendamentos sem triagem manual na dashboard.") },
                                    label = { Text("Aceite automático") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN) },
                                    label = { Text("Voltar ↩️") }
                                )
                            }
                            com.example.agendei_pro.ui.viewmodel.SupportMenuState.LOYALTY -> {
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como ativar o Fidelômetro?", "Para ativar fidelidade:\n1. Vá em Ajustes do Salão.\n2. Ligue a flag do Programa de Fidelidade.\n3. Digite o número de selos para pontuação e o prêmio (ex: 'Corte Grátis').") },
                                    label = { Text("Como ativar o Fidelômetro?") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como pontuar o cliente?", "Para pontuar o fidelômetro do cliente:\n1. Na Dashboard, conclua o agendamento do cliente.\n2. Ou ative 'Validação Automática' nos Ajustes do Salão para pontuar o cliente assim que você confirmar o horário dele!") },
                                    label = { Text("Como pontuar?") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Prazo de resgate do prêmio", "Para resgate de fidelidade:\n1. Vá em Ajustes do Salão.\n2. Defina em dias a validade do prêmio acumulado para resgate do cliente.") },
                                    label = { Text("Validade do prêmio") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN) },
                                    label = { Text("Voltar ↩️") }
                                )
                            }
                            com.example.agendei_pro.ui.viewmodel.SupportMenuState.BILLING -> {
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Período de teste (Trial)", "Sobre o Trial:\n1. Todo salão possui 10 dias de avaliação Premium grátis no primeiro cadastro.\n2. O menu lateral exibe o contador regressivo dos dias restantes.") },
                                    label = { Text("Sobre o Trial") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como assinar o plano?", "Para assinar o premium:\n1. Abra o menu lateral do app do salão.\n2. Caso expire ou deseje assinar, clique em 'Assinar Premium' e confirme a assinatura do Google Play Billing.") },
                                    label = { Text("Como assinar") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN) },
                                    label = { Text("Voltar ↩️") }
                                )
                            }
                            com.example.agendei_pro.ui.viewmodel.SupportMenuState.WAITING -> {
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como funciona a fila de espera?", "Sobre a Fila de Espera:\n1. Ative a flag 'Fila de Espera' nos Ajustes do Salão.\n2. Se um dia estiver lotado, clientes podem se cadastrar na lista de espera do app.") },
                                    label = { Text("Como funciona a fila?") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como avisar de desistências?", "Para gerenciar desistências:\n1. No menu lateral, acesse 'Fila de Espera'.\n2. Você verá quem aguarda vaga.\n3. Use o botão do WhatsApp ao lado do nome para avisar que abriu um slot!") },
                                    label = { Text("Avisar desistência") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN) },
                                    label = { Text("Voltar ↩️") }
                                )
                            }
                            com.example.agendei_pro.ui.viewmodel.SupportMenuState.CLIENTS -> {
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Ver clientes vinculados", "Para gerenciar clientes vinculados:\n1. Vá no menu lateral -> Clientes.\n2. A tela exibe nome, e-mail e telefone de todos os clientes vinculados ao seu salão.") },
                                    label = { Text("Ver clientes") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como bloquear cliente indisciplinado?", "Para bloquear clientes:\n1. No menu lateral -> Clientes.\n2. Selecione o cliente desejado e ative o botão de bloqueio.\n3. Ele ficará impedido de agendar novos horários no seu salão.") },
                                    label = { Text("Bloquear cliente") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN) },
                                    label = { Text("Voltar ↩️") }
                                )
                            }
                            com.example.agendei_pro.ui.viewmodel.SupportMenuState.THEMES -> {
                                SuggestionChip(
                                    onClick = { viewModel.selectFAQItem("Como mudar o tema de cores?", "Para mudar o visual do app:\n1. Abra o menu lateral -> Temas.\n2. Escolha um dos 10 temas premium e clique em aplicar para redefinir as cores do app.") },
                                    label = { Text("Mudar tema de cores") }
                                )
                                SuggestionChip(
                                    onClick = { viewModel.setMenuState(com.example.agendei_pro.ui.viewmodel.SupportMenuState.MAIN) },
                                    label = { Text("Voltar ↩️") }
                                )
                            }
                        }
                    }

                    // Botão para Falar com Humano
                    Button(
                        onClick = { viewModel.escalateToHuman() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Falar com Atendente Humano 🙋‍♂️")
                    }
                }
            }

            // Barra de Digitação
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { if (it.length <= 250) messageText = it },
                        placeholder = { Text("Descreva sua dúvida...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        supportingText = {
                            Text(
                                text = "${messageText.length}/250",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        viewModel.sendMessage(messageText)
                                        messageText = ""
                                    }
                                },
                                enabled = messageText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Enviar",
                                    tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
