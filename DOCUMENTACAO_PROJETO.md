# Projeto Agendei & Agendei PRO - Documentação Oficial

## Visão Geral
Sistema de agendamento online e gerenciamento de fidelidade em tempo real para salões de beleza, barbearias, manicures, estética e estúdios de tatuagem.
- **Arquitetura:** Projeto modular único em Kotlin utilizando **Product Flavors** (`pro` para salões e `client` para clientes) no aplicativo principal e um módulo separado (`admin`) para a retaguarda do suporte. Isso garante compartilhamento do core-business e leveza na compilação.
- **Banco de Dados:** Firebase Firestore (Realtime).
- **Autenticação:** Google Sign-In integrado à base de perfis do Firestore.

## Regras de Negócio e Recursos do Salão (PRO)
1. **Modelos de Negócio e Assinatura:**
   - Período de avaliação de 10 dias gratuitos (Trial) a partir do primeiro cadastro (blindado e verificado via Firestore).
   - Assinatura premium via Google Play Billing para habilitar mais de 2 profissionais na equipe e uso ilimitado.
   - Geração automática de código único de compartilhamento e vinculação rápida (`PRO-XXXXXX`).

2. **Gerenciamento de Serviços:**
   - Cadastro de serviços contendo nome, duração, categoria (com criação dinâmica de novas categorias) e descrição.
   - Máscara monetária em tempo real no padrão nacional brasileiro (`R$ XX,XX`) durante a digitação de valores.
   - Imagens dinâmicas de serviços obtidas opcionalmente via web (LoremFlickr) com busca automática por palavra-chave ou upload da galeria.

3. **Gerenciamento de Equipe (Minha Equipe):**
   - Cadastro de profissionais com e-mail Google para obter automaticamente a foto de perfil da conta Google vinculada (busca case-insensitive).
   - Vinculação de especialidades individuais por colaborador ou uso do chip universal **"🌟 Todos os Serviços"** (que o habilita para todos os serviços cadastrados no salão, inclusive serviços criados futuramente).
   - Configuração de horários de trabalho individuais e dias específicos para cada colaborador (`hasCustomSchedule`).

4. **Agenda & Grade de Horários:**
   - Controle de horários de funcionamento geral do salão, intervalos para almoço e dias de funcionamento.
   - Slot de tempo de agendamento configurável (30 minutos, 60 minutos, etc.).
   - Confirmação automática de horários (`autoAccept`) ou aprovação manual pela Dashboard.

5. **Programa de Fidelidade (Fidelômetro):**
   - Configuração dinâmica de número de selos para recompensa e descrição do prêmio (ex: "Corte Grátis").
   - Ativação de expiração do cartão fidelidade em dias e validação de selos automática (`autoValidateLoyalty`) no momento em que o salão aceita/confirma o agendamento.

6. **Fila de Espera:**
   - Lista para registro de clientes interessados em datas com slots já esgotados.
   - Atalho rápido do WhatsApp para contato manual imediato do salão com o cliente da fila em caso de desistências.

7. **Bloqueio de Clientes:**
   - Recurso de bloqueio online de clientes indisciplinados. Clientes bloqueados perdem a capacidade de agendar novos horários no salão.

## Suporte Técnico Híbrido (Chatbot FAQ e Admin)
- **Autoatendimento inteligente:** Menu de ajuda do robô com 8 categorias de navegação rápida cobrindo todas as funcionalidades reais do app.
- **Transição Humana:** Opção de escalonamento para falar com atendentes humanos. O chamado altera o status de `BOT` para `PENDING_HUMAN` no Firestore.
- **Painel de Suporte (Retaguarda Admin):**
  - Divisão de chamados em abas direcionadas por remetente ("Salões" e "Clientes") para uma triagem organizada do suporte, além da aba "Resolvidos".
  - Badges dinâmicos no canto de cada aba exibindo a quantidade de chamados ativos e o total de mensagens não lidas (`unreadCount`), atualizados em tempo real.
  - Indicadores visuais nos cards dos chamados sobre o status ("Triagem Bot" vs "Suporte Humano") e badge de "novas" mensagens.
  - Controle de chamados: botões explícitos para "Finalizar" e "Reabrir" chamados para o Admin e usuário, travando a digitação de ambos os lados e liberando a reabertura via novo chamado.
  - Sincronização automatizada: o contador de mensagens não lidas do chamado é zerado instantaneamente no Firestore quando o Admin abre a tela de conversa.
  - Consulta otimizada por ordenação de memória para dispensar índices compostos do Firebase, tornando o suporte ativo e imediato em qualquer base de dados.
- **Notificações Silenciosas e Sincronia:**
  - Redução drástica da invasividade na barra de status superior através do uso de prioridade mínima no canal persistente do Android (`IMPORTANCE_MIN`).
  - Listener inteligente do suporte em background no `NotificationService` que avisa sobre novas mensagens do suporte mesmo com o app fechado.

## Segurança e LGPD (Proteção de Dados)
- **Autenticação Segura:** Dados de login gerenciados inteiramente pelo Google Auth.
- **Direito ao Esquecimento:** Opção de excluir conta permanentemente dentro de "Meu Perfil", removendo dados e registros do Firestore e Auth de forma imediata.
- **Isolamento de Dados:** Regras de segurança do Firestore que restringem acessos e evitam vazamento de dados de salões e agendamentos concorrentes.

## Status de Desenvolvimento
- [x] Configuração de Flavors e Gradle unificado.
- [x] Modelos de dados completos (Salon, Service, Appointment, UserBinding, SupportChat).
- [x] Cadastro de serviços com máscara de R$ e altura de layout corrigida.
- [x] Múltiplos ramos de atividade no cadastro e ajustes.
- [x] Especialidade universal de equipe ("Todos os Serviços").
- [x] Chat de suporte com FAQ completo de 8 tópicos e getters dinâmicos de Auth.
- [x] Painel Admin de retaguarda de suporte sem dependência de índices compostos.
- [x] Abas direcionadas de suporte e sub-abas secundárias (Fila do Bot e Suporte Humano), contadores de não lidas e controle de finalização/novo chamado.
- [x] Histórico de chamados finalizados agrupados por data e numerados, com visualização in-app no cliente e salão.
- [x] Ícone de fone de ouvido (support_agent) nas notificações de suporte.
- [x] Logo linear do app na TopAppBar da Dashboard do Cliente ao lado do menu.
- [x] Busca automática de foto Google pelo e-mail e persistência na equipe.
- [x] Cópia automatizada de APKs de distribuição e testes.
