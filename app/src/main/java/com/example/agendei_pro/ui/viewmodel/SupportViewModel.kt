package com.example.agendei_pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class SupportMessage(
    val id: String = "",
    val sender: String = "", // USER, BOT, ADMIN
    val text: String = "",
    val timestamp: Date? = null
)

data class SupportChat(
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userType: String = "", // CLIENT, SALON
    val status: String = "BOT", // BOT, PENDING_HUMAN, RESOLVED
    val lastMessageText: String = "",
    val lastMessageTime: Date? = null
)

enum class SupportMenuState {
    MAIN, SERVICES, TEAM, SCHEDULE, LOYALTY, BILLING, WAITING, CLIENTS, THEMES
}

class SupportViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<SupportMessage>>(emptyList())
    val messages: StateFlow<List<SupportMessage>> = _messages

    private val _chatInfo = MutableStateFlow<SupportChat?>(null)
    val chatInfo: StateFlow<SupportChat?> = _chatInfo

    private val _menuState = MutableStateFlow(SupportMenuState.MAIN)
    val menuState: StateFlow<SupportMenuState> = _menuState

    // Getters dinâmicos para evitar inicialização vazia antes da conclusão do login
    private val userId get() = auth.currentUser?.uid ?: ""
    private val userName get() = auth.currentUser?.displayName ?: "Usuário"
    private val userEmail get() = auth.currentUser?.email ?: ""

    fun initChat(isProVersion: Boolean) {
        val currentUid = userId
        if (currentUid.isBlank()) return

        // 1. Escutar informações do chat
        db.collection("support_chats").document(currentUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _chatInfo.value = snapshot.toObject(SupportChat::class.java)
                } else {
                    // Criar o chat se não existir
                    val newChat = SupportChat(
                        userId = currentUid,
                        userName = userName,
                        userEmail = userEmail,
                        userType = if (isProVersion) "SALON" else "CLIENT",
                        status = "BOT",
                        lastMessageText = "Chat iniciado",
                        lastMessageTime = Date()
                    )
                    db.collection("support_chats").document(currentUid).set(newChat)
                }
            }

        // 2. Escutar as mensagens em tempo real
        db.collection("support_chats").document(currentUid).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val msg = doc.toObject(SupportMessage::class.java)
                        msg?.copy(id = doc.id)
                    }
                    _messages.value = list

                    // Se não houver mensagens, envia a de boas-vindas do Bot
                    if (list.isEmpty()) {
                        sendBotMessage("Olá! Sou o assistente virtual do Agendei PRO. Como posso ajudar você hoje?")
                    }
                }
            }
    }

    fun setMenuState(state: SupportMenuState) {
        _menuState.value = state
    }

    fun sendMessage(text: String) {
        val currentUid = userId
        if (text.isBlank() || currentUid.isBlank()) return

        val userMessage = mapOf(
            "sender" to "USER",
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        viewModelScope.launch {
            db.collection("support_chats").document(currentUid).collection("messages").add(userMessage)
            
            // Garante gravação segura com SetOptions.merge
            val updateData = mapOf(
                "lastMessageText" to text,
                "lastMessageTime" to FieldValue.serverTimestamp()
            )
            db.collection("support_chats").document(currentUid).set(updateData, SetOptions.merge())

            // Se ainda estiver em modo BOT, processa a resposta da Pseudo IA local
            val currentChat = _chatInfo.value
            if (currentChat != null && currentChat.status == "BOT") {
                processBotResponse(text)
            }
        }
    }

    fun selectFAQItem(question: String, answer: String) {
        val currentUid = userId
        if (currentUid.isBlank()) return
        
        val userMsg = mapOf(
            "sender" to "USER",
            "text" to question,
            "timestamp" to FieldValue.serverTimestamp()
        )

        val botMsg = mapOf(
            "sender" to "BOT",
            "text" to answer,
            "timestamp" to FieldValue.serverTimestamp()
        )

        viewModelScope.launch {
            db.collection("support_chats").document(currentUid).collection("messages").add(userMsg)
            db.collection("support_chats").document(currentUid).collection("messages").add(botMsg)
            
            val updateData = mapOf(
                "lastMessageText" to answer,
                "lastMessageTime" to FieldValue.serverTimestamp()
            )
            db.collection("support_chats").document(currentUid).set(updateData, SetOptions.merge())
            
            // Volta para o menu principal após responder uma FAQ para facilitar nova navegação
            _menuState.value = SupportMenuState.MAIN
        }
    }

    fun escalateToHuman() {
        val currentUid = userId
        if (currentUid.isBlank()) return

        val userMsg = mapOf(
            "sender" to "USER",
            "text" to "Falar com Atendente Humano 🙋‍♂️",
            "timestamp" to FieldValue.serverTimestamp()
        )

        val botMsg = mapOf(
            "sender" to "BOT",
            "text" to "Certo, registrei seu chamado! Repassei seu chat para nossos atendentes. Eles vão te responder o mais rápido possível por aqui. Fique atento às notificações!",
            "timestamp" to FieldValue.serverTimestamp()
        )

        viewModelScope.launch {
            db.collection("support_chats").document(currentUid).collection("messages").add(userMsg)
            db.collection("support_chats").document(currentUid).collection("messages").add(botMsg)
            
            val updateData = mapOf(
                "status" to "PENDING_HUMAN",
                "lastMessageText" to "Chamado escalado para suporte humano",
                "lastMessageTime" to FieldValue.serverTimestamp()
            )
            db.collection("support_chats").document(currentUid).set(updateData, SetOptions.merge())
        }
    }

    private fun sendBotMessage(text: String) {
        val currentUid = userId
        if (currentUid.isBlank()) return
        val botMsg = mapOf(
            "sender" to "BOT",
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("support_chats").document(currentUid).collection("messages").add(botMsg)
    }

    private fun processBotResponse(userInput: String) {
        val cleanInput = userInput.lowercase()
        val currentUid = userId
        if (currentUid.isBlank()) return

        val botResponse = when {
            cleanInput.contains("preço") || cleanInput.contains("valor") || cleanInput.contains("serviço") || cleanInput.contains("categoria") -> {
                "Dúvidas sobre Serviços/Preços:\n1. Acesse o menu lateral -> Ajustes do Salão -> Gerenciar Serviços.\n2. Para adicionar, clique no (+). Para editar, clique no serviço cadastrado.\n3. Digite o nome, preço formatado, duração e defina a categoria (crie uma nova se desejar!)."
            }
            cleanInput.contains("colaborador") || cleanInput.contains("profissional") || cleanInput.contains("equipe") || cleanInput.contains("funcionário") || cleanInput.contains("foto") -> {
                "Dúvidas sobre Equipe:\n1. No menu lateral, acesse 'Minha Equipe'.\n2. Para puxar a foto do profissional, informe o e-mail Google exato dele e clique no botão de link. Ele deve ter logado no app pelo menos uma vez!\n3. Você também pode definir especialidades individuais ou marcar 'Todos os Serviços'."
            }
            cleanInput.contains("funcionamento") || cleanInput.contains("agenda") || cleanInput.contains("slot") || cleanInput.contains("horário") || cleanInput.contains("almoço") -> {
                "Dúvidas sobre Horários:\n1. Vá em Ajustes do Salão.\n2. Configure o horário de abertura, fechamento e almoço.\n3. Defina os dias de trabalho marcando/desmarcando os dias.\n4. Altere o tempo do slot de agendamento (30min/60min) e o aceite automático se desejar."
            }
            cleanInput.contains("fidelidade") || cleanInput.contains("fidelômetro") || cleanInput.contains("selo") || cleanInput.contains("prêmio") -> {
                "Dúvidas sobre Fidelidade:\n1. Acesse Ajustes do Salão -> Ative a Fidelidade.\n2. Preencha os selos necessários, a descrição do prêmio e a expiração do cartão.\n3. Ative a 'Validação Automática' para dar selos logo na confirmação do agendamento."
            }
            cleanInput.contains("premium") || cleanInput.contains("assinatura") || cleanInput.contains("teste") || cleanInput.contains("trial") || cleanInput.contains("pagar") -> {
                "Dúvidas sobre Assinatura:\n1. Salões possuem 10 dias de avaliação gratuita (Trial) garantidos.\n2. Acesse o menu lateral e veja quantos dias restam.\n3. Clique em assinar para contratar o plano Premium e remover o limite de 2 profissionais na equipe."
            }
            cleanInput.contains("espera") || cleanInput.contains("fila") || cleanInput.contains("desistência") || cleanInput.contains("vaga") -> {
                "Dúvidas sobre Fila de Espera:\n1. A fila de espera permite que os clientes se registrem se o dia estiver lotado.\n2. Acesse o menu lateral -> Fila de Espera para visualizar os clientes que aguardam vaga.\n3. Você pode chamá-los rapidamente pelo atalho do WhatsApp."
            }
            cleanInput.contains("bloqueio") || cleanInput.contains("bloquear") || cleanInput.contains("indisciplinado") || cleanInput.contains("rejeitar") -> {
                "Dúvidas sobre Bloqueios:\n1. Para bloquear um cliente, acesse o menu do salão -> Clientes.\n2. Localize o cliente e ative o botão de bloqueio.\n3. Clientes bloqueados não conseguem mais efetuar agendamentos online."
            }
            cleanInput.contains("tema") || cleanInput.contains("cor") || cleanInput.contains("visual") || cleanInput.contains("layout") -> {
                "Dúvidas sobre Temas:\n1. No menu lateral do app do salão ou cliente, acesse 'Temas'.\n2. Escolha um dos 10 temas premium e clique em aplicar para alterar o visual do app instantaneamente."
            }
            else -> {
                "Desculpe, não captei sua dúvida. Você pode usar os chips de menu de ajuda acima por assunto, ou clicar em 'Falar com Atendente Humano' para falar conosco pessoalmente."
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            sendBotMessage(botResponse)
            
            val updateData = mapOf(
                "lastMessageText" to botResponse,
                "lastMessageTime" to FieldValue.serverTimestamp()
            )
            db.collection("support_chats").document(currentUid).set(updateData, SetOptions.merge())
        }
    }
}
