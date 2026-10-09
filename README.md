# Octopus — Sistema Plantão (VidaPet) · prescrição

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
prescrição, [`octopus-msprescricao/`](octopus-msprescricao/README.md) (porta 8084): o veterinário prescreve
medicamentos para uma internação, com a RN-03 (interações proibidas) e a ligação com o `ms-internacao`.
Arquitetura e convenções de todos os módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 09/10/2026 na branch `feature/prescricao`_
- Módulo volta a compilar e passa nos testes (37): os commits anteriores misturavam ids `Long` e `UUID`.
- Só prescrição e itens: doses saem deste módulo (sai a `V2__doses.sql` e o `StatusDose`); serão geradas por um
  módulo próprio.
- Ids `Long` (`BIGINT AUTO_INCREMENT`) na prescrição, no item e no `internacaoId`; `medicacaoId` continua `UUID`.
- Integração com o `ms-internacao`: valida a internação (`ADMITIDA`, `EM_TRATAMENTO` ou `ISOLAMENTO`) e chama
  `iniciar-tratamento` antes de gravar (`MS_INTERNACAO_URL`).
- RN-03: recusa par com interação proibida na mesma prescrição ou com medicamento já prescrito na internação,
  consultando o msmedications (`MS_MEDICACOES_URL`).
- Só `VETERINARIO` prescreve e desativa; `criadoEm` pelo relógio de São Paulo (`ClockConfig` com `@Configuration`);
  erros 503 para outro módulo fora do ar.
- `pipeline.yml` no lugar do `ci.yml`: deploy na EC2 (container `prescricao`, porta 8084) quando a versão muda.
- `CLAUDE.md` da squad incluído nesta branch.

## Próximos passos
- [ ] Criar `~/octopus-env/prescricao.env` na EC2 e liberar a porta 8084 antes do primeiro deploy.
- [ ] Definir e implementar o módulo de doses e o contrato com a prescrição (idempotente por item).
- [ ] Aceitar item "se necessário" (mudança contratada de 01/11), sem intervalo nem quantidade.
- [ ] Rodar contra MySQL para validar a `V1` no `ddl-auto=validate`.
