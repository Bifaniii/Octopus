# Octopus — Sistema Plantão (VidaPet) · doses

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
doses, [`octopus-msdoses/`](octopus-msdoses/README.md) (porta 8085): guarda uma dose por horário gerado na
prescrição e registra a aplicação pelo auxiliar. Arquitetura e convenções de todos os módulos:
[`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 24/11/2026 na branch `fix/admin-ti-doses` (para a `feature/doses`)_
- O `ADMIN` passa a ser o perfil de TI e pode fazer tudo o que os outros perfis fazem neste módulo.
- Agendar, cancelar e reativar doses de uma prescrição passam a aceitar `ADMIN` e `VETERINARIO`; cancelar as da internação, `ADMIN`, `RECEPCIONISTA` e `VETERINARIO`; aplicar a dose e aplicar o item "se necessário", `ADMIN` e `AUXILIAR`. A RN-05 continua gravando quem aplicou (`aplicadaPor`).
- Testes de permissão atualizados para o admin. Versão `0.0.5-SNAPSHOT` para `0.0.6-SNAPSHOT`.

## Próximos passos
- [ ] Publicar junto com os outros módulos da mesma mudança (internação, prescrição, doses e medicamentos)
- [ ] Liberar no front as ações que hoje aparecem só para cada perfil

