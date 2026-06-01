# Projeto Agendei & Agendei PRO - Documentação Oficial

## Visão Geral
Sistema de agendamento para salões de beleza e estética.
- **Arquitetura:** Projeto único com **Product Flavors** (`pro` e `client`). Isso garante que o projeto seja leve para o computador e compartilhe o mesmo código-base.
- **Banco de Dados:** Firebase Firestore (Tempo Real).
- **Autenticação:** Google Auth (Exclusivo).

## Regras de Negócio
1. **Agendei PRO (Salões):**
   - Assinatura mensal via Google Play Billing.
   - Período de teste de 10 dias (Blindado via Firestore).
   - Geração de código único: `PRO-XXXXXX`.
   - Cadastro de serviços (Nome, Preço, Duração).
   - Agenda visual (Lista e Calendário).

2. **Agendei (Clientes):**
   - 100% Gratuito.
   - Vinculação ao salão via código PRO (Apenas um por vez nesta fase).
   - Agendamento em tempo real.

3. **Segurança e LGPD (Proteção de Dados):**
   - **Autenticação:** Dados gerenciados pelo Google Auth (Segurança Google).
   - **Transparência:** Uso de dados apenas para identificação no agendamento.
   - **Direito ao Esquecimento:** Botão "Excluir Conta" disponível no perfil, que remove todos os dados do Firestore e Auth.
   - **Isolamento:** Regras do Firestore garantem que um usuário não veja dados de outro.

## Fluxo de Notificações
- O Salão deve receber uma notificação PUSH sempre que um cliente realizar um novo agendamento.
- Implementação via Firebase Cloud Messaging (FCM).

## Status de Desenvolvimento
- [x] Configuração de Flavors e Gradle.
- [x] Modelos de dados (Salon, Service, Appointment, UserBinding).
- [x] Telas PRO (Dashboard, Serviços, Agenda).
- [x] Telas Cliente (Dashboard, Vincular Salão, Agendamento).
- [ ] Gestão de Perfil (Editar Nome do Google).
- [ ] Sistema de Notificações Ativo.
