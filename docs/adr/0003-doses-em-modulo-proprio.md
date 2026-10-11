# ADR 0003: doses num microsserviço próprio

- **Status:** aceita
- **Data:** 09/10/2026 (implementada em 11/10/2026)

## Contexto

A prescrição define o esquema (medicamento, intervalo, quantidade, início). As doses têm vida própria: são
aplicadas pelo auxiliar, perdidas fora da janela, deslocadas ou puladas conforme o esquema, e alimentam o
painel e o relatório. O TAP proíbe mensageria e integrações externas, então não há transação distribuída.

## Decisão

- O `octopus-msprescricao` gera os horários (`inicio + n * intervalo`) e, depois de gravar, chama
  `POST /api/doses/agendamentos` no `octopus-msdoses`, que guarda uma dose por horário.
- O agendamento é idempotente por prescrição, e o par (item, horário) é único.
- Se o agendamento falhar, a prescrição é desativada e as doses que tenham sido gravadas são canceladas. Desativar
  uma prescrição cancela as doses pendentes. Nenhum dos dois apaga registro.
- Aplicação, janela de 30 minutos, dose perdida e reaplicação ficam no `octopus-msdoses`.

## Consequências

- A prescrição não conhece o ciclo de vida da dose, e o módulo de doses não conhece interações nem esquema.
- Há uma dependência síncrona da prescrição para as doses: com o msdoses fora do ar, não se prescreve (503).
- Um timeout pode chegar depois de o msdoses ter gravado. A compensação cobre o caso comum; se o cancelamento
  também falhar, ficam doses pendentes de uma prescrição inativa, visíveis pelo `prescricaoId`.
