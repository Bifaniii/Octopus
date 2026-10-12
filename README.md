# Octopus — Sistema Plantão (VidaPet) · prescrição

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
prescrição, [`octopus-msprescricao/`](octopus-msprescricao/README.md) (porta 8084): o veterinário prescreve
medicamentos para uma internação, com a RN-06 (interação proibida no mesmo horário), a ligação com o
`ms-internacao` e o agendamento das doses no `octopus-msdoses`.
Arquitetura e convenções de todos os módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 12/10/2026 na branch `feature/prescricao`_
- Antes de gravar, confere se há estoque para todas as doses de cada medicamento; sem saldo, 422 "Estoque
  insuficiente de X" e nada é gravado.
- Recusa de estoque vinda do agendamento no msdoses chega como 422 com a mesma mensagem (antes virava 503).
- 45 testes.

## Próximos passos
- [ ] Criar `~/octopus-env/doses.env` na EC2 e acrescentar `MS_DOSES_URL` ao `prescricao.env` antes de subir a versão.
- [ ] Sprint 4: prescrição "se necessário" (mudança contratada de 01/11).
