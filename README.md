# Octopus — Sistema Plantão (VidaPet) · doses

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
doses, [`octopus-msdoses/`](octopus-msdoses/README.md) (porta 8085): guarda uma dose por horário gerado na
prescrição e registra a aplicação pelo auxiliar. Arquitetura e convenções de todos os módulos:
[`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 21/11/2026 na branch `fix/antirrabica-na-aplicacao` (para a `feature/doses`)_
- Aplicar a vacina antirrábica atualiza a data da última antirrábica no cadastro do animal (msusuario), que é o que a
  RN-02 confere na internação. A vacina é cadastrada, prescrita e agendada como qualquer medicamento e é reconhecida
  pelo nome: "antirrábica" no nome comercial, sem diferença de acento, maiúscula ou hífen (`service/Antirrabica`).
- A conferência do nome é guardada por medicamento enquanto o serviço roda: só a primeira aplicação de cada um
  pergunta ao msmedications. Aplicar a antirrábica faz mais duas chamadas: o animal da internação (ms-internacao) e
  a data no cadastro (`PATCH /api/animais/{id}/antirrabica` do msusuario).
- A data é gravada antes da aplicação. Se o msusuario não responder, a aplicação não é registrada (503) e o auxiliar
  tenta de novo: como a aplicação não pode ser editada (RN-05), a data da vacina nunca fica desatualizada. Vale também
  para o item "se necessário".
- Variáveis novas: `MS_INTERNACAO_URL` e `MS_USUARIO_URL` (`client/CadastrosClient`). Sem elas o serviço não sobe.
- 8 testes novos (61 no módulo). Versão `0.0.4-SNAPSHOT` para `0.0.5-SNAPSHOT`.

## Próximos passos
- [ ] Publicar o msusuario com o `PATCH /api/animais/{id}/antirrabica` antes deste módulo
- [ ] Acrescentar `MS_INTERNACAO_URL=http://172.17.0.1:8083` e `MS_USUARIO_URL=http://172.17.0.1:8080` no `doses.env` da EC2
- [ ] Cadastrar a vacina com "antirrábica" no nome

