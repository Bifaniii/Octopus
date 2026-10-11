# Octopus — Sistema Plantão (VidaPet) · doses

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
doses, [`octopus-msdoses/`](octopus-msdoses/README.md) (porta 8085): guarda uma dose por horário gerado na
prescrição e registra a aplicação pelo auxiliar. Arquitetura e convenções de todos os módulos:
[`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 11/10/2026 na branch `feature/doses`_
- Módulo novo, separado da prescrição por decisão da squad (09/10). Entidade `Dose` com status `PENDENTE`,
  `APLICADA` e `CANCELADA`, sem setters.
- Agendamento idempotente vindo do `octopus-msprescricao` (`POST /api/doses/agendamentos`) e cancelamento das
  pendentes quando a prescrição é desativada.
- RN-05: a aplicação grava o auxiliar e a hora uma vez só; a segunda tentativa recebe 422.
- Listagem com filtros de internação, prescrição, status e período. 23 testes.

## Próximos passos
- [ ] Criar `~/octopus-env/doses.env` na EC2 e liberar a porta 8085 antes do primeiro deploy.
- [ ] Sprint 3: janela de 30 minutos no `aplicar` (RN-03), dose perdida (RN-04) e o painel de doses no front.
- [ ] Sprint 4: reaplicação por esquema (RN-07).
