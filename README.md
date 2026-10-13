# Octopus — Sistema Plantão (VidaPet) · prescrição

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
prescrição, [`octopus-msprescricao/`](octopus-msprescricao/README.md) (porta 8084): o veterinário prescreve
medicamentos para uma internação, com a RN-06 (interação proibida no mesmo horário), a ligação com o
`ms-internacao` e o agendamento das doses no `octopus-msdoses`.
Arquitetura e convenções de todos os módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 13/10/2026 na branch `feature/prescricao`_
- Item "se necessário" (mudança contratada de 01/11): intervalo mínimo, sem quantidade nem início; vigência em `SE_NECESSARIO_A_PARTIR_DE`.
- O agendamento leva o esquema do medicamento (contínuo ou sintomático) para a RN-07 no msdoses.
- O tratamento é iniciado depois de gravar e agendar, para o ms-internacao conferir a prescrição ativa; falha em qualquer passo desfaz a prescrição.
- Item sem intervalo, ou de horário fixo sem quantidade e início, recebe 400.
- Migration `V3__itens_se_necessario.sql`. 49 testes.

## Próximos passos
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
