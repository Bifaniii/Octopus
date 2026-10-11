# Octopus — Sistema Plantão (VidaPet) · prescrição

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
prescrição, [`octopus-msprescricao/`](octopus-msprescricao/README.md) (porta 8084): o veterinário prescreve
medicamentos para uma internação, com a RN-06 (interação proibida no mesmo horário), a ligação com o
`ms-internacao` e o agendamento das doses no `octopus-msdoses`.
Arquitetura e convenções de todos os módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 11/10/2026 na branch `feature/prescricao`_
- Recuperada a versão do Douglas (`90acb19`), apagada da branch pelo force-push de 10/10. O commit de merge tem
  os dois históricos como pais e mantém a correção do Vitor (`medicacaoId` `Long`).
- `dosagem` e `quantidadeDoses` continuam no item: sem a quantidade não há como gerar os horários, e a `V1` já
  aplicada em produção tem as duas colunas.
- RN-06 no lugar da antiga "RN-03": interação proibida só bloqueia quando os dois medicamentos cairiam no mesmo
  horário, como pede o enunciado (caso de teste 4).
- A resposta traz os `horarios` de cada item, e a prescrição agenda as doses no `octopus-msdoses`; se o
  agendamento falha, a prescrição é desativada e as doses criadas são canceladas.
- Clients com `JdkClientHttpRequestFactory`: o `HttpURLConnection` recusava PATCH e o `iniciar-tratamento` nunca
  chegava ao ms-internacao.
- `V2__itens_medicacao_long.sql`: `medicacao_id` vira `BIGINT`. 44 testes.

## Próximos passos
- [ ] Criar `~/octopus-env/doses.env` na EC2 e acrescentar `MS_DOSES_URL` ao `prescricao.env` antes de subir a versão.
- [ ] Sprint 4: prescrição "se necessário" (mudança contratada de 01/11).
