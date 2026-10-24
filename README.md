# Octopus — Sistema Plantão (VidaPet) · prescrição

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
prescrição, [`octopus-msprescricao/`](octopus-msprescricao/README.md) (porta 8084): o veterinário prescreve
medicamentos para uma internação, com a RN-06 (interação proibida no mesmo horário), a ligação com o
`ms-internacao` e o agendamento das doses no `octopus-msdoses`.
Arquitetura e convenções de todos os módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 24/10/2026 na branch `feature/prescricao`_
- Novo `PATCH /api/prescricoes/{id}/ativar` (`VETERINARIO`): reativa a prescrição, refaz o RN-06 só nos horários que ainda não passaram e pede ao msdoses que traga as doses de volta. Recusa se a internação teve alta ou foi encerrada, se faltar estoque ou se nenhuma dose ainda vale.
- Versão `0.0.2` para `0.0.3-SNAPSHOT`.

## Próximos passos
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
