# Octopus — Sistema Plantão (VidaPet) · prescrição

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
prescrição, [`octopus-msprescricao/`](octopus-msprescricao/README.md) (porta 8084): o veterinário prescreve
medicamentos para uma internação, com a RN-06 (interação proibida no mesmo horário), a ligação com o
`ms-internacao` e o agendamento das doses no `octopus-msdoses`.
Arquitetura e convenções de todos os módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 24/11/2026 na branch `fix/admin-ti-prescricao` (para a `feature/prescricao`)_
- O `ADMIN` passa a ser o perfil de TI e pode fazer tudo o que os outros perfis fazem neste módulo.
- Prescrever, desativar e reativar prescrição passam a aceitar `ADMIN` e `VETERINARIO`. O e-mail gravado na prescrição continua sendo o de quem prescreveu.
- Testes de permissão atualizados para o admin. Versão `0.0.5-SNAPSHOT` para `0.0.6-SNAPSHOT`.

## Próximos passos
- [ ] Publicar junto com os outros módulos da mesma mudança (internação, prescrição, doses e medicamentos)
- [ ] Liberar no front as ações que hoje aparecem só para cada perfil

