# Octopus — Sistema Plantão (VidaPet) · doses

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
doses, [`octopus-msdoses/`](octopus-msdoses/README.md) (porta 8085): guarda uma dose por horário gerado na
prescrição e registra a aplicação pelo auxiliar. Arquitetura e convenções de todos os módulos:
[`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 12/10/2026 na branch `feature/doses`_
- Agendar reserva no msmedications 1 unidade por dose nova; cancelar devolve só as que estavam pendentes. Se uma
  chamada de estoque falha no meio, as anteriores são desfeitas.
- `PATCH /api/doses/internacoes/{id}/cancelar` (recepção ou veterinário), chamado pelo ms-internacao quando o
  animal sai.
- Nova variável `MS_MEDICACOES_URL`. 28 testes.

## Próximos passos
- [ ] Acrescentar `MS_MEDICACOES_URL` ao `~/octopus-env/doses.env` na EC2 antes do deploy.
- [ ] Criar `~/octopus-env/doses.env` na EC2 e liberar a porta 8085 antes do primeiro deploy.
- [ ] Sprint 3: janela de 30 minutos no `aplicar` (RN-03), dose perdida (RN-04) e o painel de doses no front.
- [ ] Sprint 4: reaplicação por esquema (RN-07).
