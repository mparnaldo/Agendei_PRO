# MEMÓRIA Agendei PRO - Registro de Contexto e Progresso

Este arquivo serve como uma "memória viva" de tudo o que foi construído, discutido e resolvido no projeto **Agendei PRO**. Guarde-o com segurança durante a formatação do seu PC. Quando retornarmos, basta pedir para que eu leia este arquivo e voltaremos exatamente de onde paramos.

## 📱 Estrutura do Projeto
O projeto consiste em duas aplicações principais no mesmo repositório, compartilhando o módulo `core`:
1. **Agendei_Cliente (App do Cliente):** Permite que os clientes se conectem (via conta Google), vinculem-se a um salão utilizando um código único e realizem agendamentos de serviços.
2. **Agendei_Salao (App do Salão/Admin):** Permite que donos de salões e profissionais configurem horários, dias de funcionamento, serviços, acompanhem a agenda (Dashboard e Visão de Grade), gerenciem profissionais e bloqueiem clientes indisciplinados.
3. **Agendei_Admin (App do Administrador - Retaguarda):** Aplicativo separado utilizado pela equipe de suporte para responder chamados e gerenciar a base de dados.

### 🛠 Tecnologias Utilizadas
- **Linguagem:** Kotlin
- **UI:** Jetpack Compose (Material Design 3)
- **Backend/DB:** Firebase Authentication (Google Sign-In) e Firebase Firestore.
- **Arquitetura:** MVVM (Model-View-ViewModel) utilizando Kotlin Coroutines e StateFlow.
- **Build System:** Gradle (Kotlin DSL).

## 🚀 Principais Funcionalidades e Regras de Negócio Implementadas

1. **Gestão de Agendamentos e Grade de Horários (`SchedulingViewModel` & `AgendaScreen`)**
   - Suporte a serviços com duração variável (ex: corte de 60min) consumindo múltiplos slots (ex: 2 slots de 30min).
   - Validação robusta de conflitos e sobreposições (`overlap`), impedindo agendamentos simultâneos para o mesmo profissional.
   - Opção "Tanto faz" onde o sistema varre profissionais qualificados e busca slots consecutivos livres.

2. **Bloqueio de Clientes**
   - O salão pode bloquear clientes problemáticos. A flag `isBlocked` é persistida na coleção `user_bindings` e verificada ativamente, bloqueando agendamentos se o cliente tentar burlar a interface. (Resolvido um bug com anotações `@PropertyName` para sincronia correta com o Firebase).

3. **Status e Cancelamentos de Agendamentos**
   - Diferenciação entre cancelamentos (`cancelledByParam`). Se o salão rejeitar/cancelar um agendamento pela Dashboard, o sistema registra `cancelledBy = "SALON"`. Se o cliente o fizer, registra como cliente.

4. **Integração com WhatsApp**
   - Compartilhamento nativo de links via Android Intent (o sistema abre o WhatsApp oficial com um texto pré-preenchido contendo o Link do App e o Código de Vinculação do salão).

5. **Gerenciamento do Salão (Configurações Personalizadas)**
   - Suporte para que profissionais tenham sua própria grade de horários individual (`hasCustomSchedule`), ou respeitem o horário geral do salão.

6. **Múltiplos Ramos de Atividade (Cadastro e Ajustes do Salão)**
   - O salão pode selecionar múltiplos segmentos (Barbearia, Cabeleireiro, Manicure, Estética, Tattoo). O principal fica gravado em `segment` e os adicionais em `additionalSegments: List<String>`.

7. **Suporte Técnico Híbrido com Triagem FAQ**
   - Chat de atendimento inteligente guiado por chips de 8 categorias de ajuda sobre todos os temas do app.
   - Opção de escalonar a conversa para atendentes humanos, caindo instantaneamente na retaguarda do Admin.

## 🐛 Histórico de Bugs Resolvidos

- **Erro de Chamados no Painel Admin (Julho/2026):** O painel de suporte do administrador não carregava os chamados devido à falta de índices compostos no console Firebase para queries compostas de igualdade + ordenação. **Solução:** Removido o `.orderBy()` direto da query do Firestore e implementada a ordenação de forma local na memória do Kotlin com `sortedByDescending { it.lastMessageTime }`, eliminando qualquer dependência de índices extras.
- **Perda de Mensagens no Início do Chat (Julho/2026):** As propriedades de credenciais do `SupportViewModel` eram estáticas e ficavam vazias se o FirebaseAuth demorasse a sincronizar no carregamento. **Solução:** Convertidos ID, Nome e E-mail para getters Kotlin dinâmicos e adicionado o salvamento do documento do chat principal via `.set(..., SetOptions.merge())`.
- **Preço de Serviços sem Máscara:** Preços de serviços eram difíceis de digitar e não mostravam cifrão e decimais. **Solução:** Criada uma máscara monetária dinâmica baseada em centavos que formata em tempo real no padrão `R$ XX,XX`.
- **Descrição Cortada na Tela de Serviços:** O campo observações na dialog de criação de serviço tinha altura física fixa de `70.dp`. **Solução:** Removido tamanho fixo, adicionado `minLines = 3` e `maxLines = 5` com `fillMaxWidth()`.
- **E-mail de Profissional com Caixa Alta:** Se o e-mail Google do colaborador possuísse letras maiúsculas, a busca de fotos falhava devido à case-sensitivity do Firestore. **Solução:** Normalizados todos os e-mails salvos para minúsculas e implementada busca dupla de segurança em `TeamViewModel` (minúsculas + grafia original).
- **Mapeamento de Posições no Telefone:** Crash ao cadastrar salão decorrente do offset dinâmico na formatação visual. **Solução:** Refatorado `PhoneVisualTransformation.kt` para criar mapa dinâmico de offsets com base na string digitada em tempo real.

## 📋 Status Atual e Próximos Passos
- **APKs Atualizados:** Os novos APKs já foram compilados com os fixes acima e movidos para a pasta `Agendei_Install/` na raiz do projeto.
- **Git Organizado:** Todos os arquivos novos e modificados foram commitados localmente na branch `main`.

**Aviso para o próximo assistente ou futura interação:** 
*Por favor, leia este documento na íntegra para assumir o contexto exato do repositório antes de realizar novas alterações.*
