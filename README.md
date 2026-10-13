# Octopus — Sistema Plantão (VidaPet) · doses

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
doses, [`octopus-msdoses/`](octopus-msdoses/README.md) (porta 8085): guarda uma dose por horário gerado na
prescrição e registra a aplicação pelo auxiliar. Arquitetura e convenções de todos os módulos:
[`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 13/10/2026 na branch `feature/doses`_
- RN-03: a aplicação só é aceita de 30 minutos antes a 30 minutos depois do horário previsto.
- RN-04 e RN-07: antes de cada consulta, a pendente fora da janela vira PERDIDA; contínuo é reaplicado na hora e desloca as seguintes, sintomático é pulado e devolve a unidade.
- Tela 6: `GET /api/doses/painel` com atrasadas, próximas, perdidas e itens "se necessário".
- Mudança contratada: `POST /api/doses/se-necessario/{item}/aplicacoes`, com intervalo mínimo e reserva de estoque na aplicação.
- Migration `V2__doses_perdidas_e_se_necessario.sql`, testada num MySQL com `sql_require_primary_key=ON`. 43 testes.

## Próximos passos
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
