# Octopus — Sistema Plantão (VidaPet) · doses

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
doses, [`octopus-msdoses/`](octopus-msdoses/README.md) (porta 8085): guarda uma dose por horário gerado na
prescrição e registra a aplicação pelo auxiliar. Arquitetura e convenções de todos os módulos:
[`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 24/10/2026 na branch `feature/doses`_
- Novo `PATCH /api/doses/prescricoes/{id}/reativar` (`VETERINARIO`): as doses canceladas que ainda estão na janela voltam a ficar pendentes e reservam o estoque de novo; reativa também o item "se necessário". Idempotente.
- Versão `0.0.2` para `0.0.3-SNAPSHOT`.

## Próximos passos
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
