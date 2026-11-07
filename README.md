# Octopus — Sistema Plantão (VidaPet) · prescrição

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
prescrição, [`octopus-msprescricao/`](octopus-msprescricao/README.md) (porta 8084): o veterinário prescreve
medicamentos para uma internação, com a RN-06 (interação proibida no mesmo horário), a ligação com o
`ms-internacao` e o agendamento das doses no `octopus-msdoses`.
Arquitetura e convenções de todos os módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 07/11/2026 na branch `claude/medicamentos-500-error-gjvu1q` (para a `feature/prescricao`)_
- RN-06: "mesmo horário" passa a ser o cruzamento das janelas de aplicação da RN-03, e não o mesmo minuto. Duas doses
  com interação proibida a até 60 minutos uma da outra são recusadas (08:00 e 08:10 eram aceitas, embora o auxiliar
  aplicasse as duas juntas); com mais de 60 minutos o par continua aceito, como diz o enunciado. Vale entre os itens
  da prescrição nova e entre eles e as prescrições ativas da internação, e também ao reativar.
- A mensagem de erro mostra os dois horários em conflito.
- 4 testes novos no `PrescricaoServiceTest` (71 no módulo), com a borda de 60 minutos exatos recusada, porque os
  limites da janela são inclusivos.
- Substitui a proposta de 8 horas da `fix/validacao_medicacao`, que proibia o par em qualquer horário.

## Próximos passos
- [ ] Levar a mudança para a `feature/prescricao` e subir a versão do `pom.xml` para publicar
- [ ] Encerrar a `fix/validacao_medicacao`
- [ ] Avaliar a conferência da RN-06 na aplicação do item "se necessário" (msdoses), que não tem horário
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)

