# Octopus — Sistema Plantão (VidaPet) · prescrição

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
prescrição, [`octopus-msprescricao/`](octopus-msprescricao/README.md) (porta 8084): o veterinário prescreve
medicamentos para uma internação, com a RN-06 (interação proibida no mesmo horário), a ligação com o
`ms-internacao` e o agendamento das doses no `octopus-msdoses`.
Arquitetura e convenções de todos os módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 17/11/2026 na branch `fix/prescricao-listagem-lenta` (para a `feature/prescricao`)_
- `GET /api/prescricoes` e `GET /api/prescricoes/{id}` respondem só com o banco do módulo. A conclusão automática
  (internação encerrada ou sem dose pendente) chamava o ms-internacao uma vez por internação e o msdoses antes de
  cada resposta, e a tela chegava a esperar 18 s. Agora ela roda em segundo plano, depois da resposta, com o token
  de quem pediu (`client/TokenRepassado`), e a prescrição concluída aparece assim na leitura seguinte.
- Executor próprio da conclusão (`config/ConclusaoConfig`): uma thread e uma vaga na fila, e o excedente é
  descartado, para as conferências não se acumularem quando um módulo estiver lento.
- Itens carregados em lote (`hibernate.default_batch_fetch_size=50`): a listagem faz 2 consultas ao banco em vez de
  1 por prescrição.
- 5 testes novos (76 no módulo). Versão `0.0.4-SNAPSHOT` para `0.0.5-SNAPSHOT`.

## Próximos passos
- [ ] Concluir a prescrição no momento do fato (saída do animal no ms-internacao, última dose no msdoses), em vez de
  conferir a cada leitura
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)

