# ADR 0001: MySQL com schema versionado pelo Flyway

- **Status:** aceita
- **Data:** 19/09/2026 (registrada em 11/10/2026)

## Contexto

O enunciado deixa a persistência livre, desde que o estado sobreviva entre sessões e a escolha esteja
justificada em ADR. O Plantão guarda registros que não podem se perder nem ser reescritos: quem aplicou qual
dose, em que horário, em qual baia o animal estava. Os dados são relacionais (tutor tem animais, prescrição tem
itens, internação tem histórico) e as regras dependem de consultas com restrição (vaga na baia, interação no
mesmo horário). O sistema é feito de microsserviços independentes, publicados no free tier da AWS.

## Decisão

- Banco relacional **MySQL 8.4**, hospedado no **Aiven** em produção e num container por módulo no
  desenvolvimento.
- Schema criado só por migrations **Flyway** (`V<n>__<descricao>.sql`); o Hibernate roda em `ddl-auto=validate` e
  nunca altera tabela. Migration aplicada não se edita: mudança é uma versão nova.
- Em produção os módulos dividem o mesmo banco, cada um com a sua tabela de histórico do Flyway
  (`flyway_schema_history_<modulo>`) e sem FK entre tabelas de módulos diferentes.
- Nada é apagado: o que sai de uso fica com `ativo = false` ou num status final (`ENCERRADA`, `CANCELADA`).

## Consequências

- Integridade e concorrência ficam com o banco: `UNIQUE`, `FOREIGN KEY` dentro do módulo, `SELECT ... FOR UPDATE`
  na disputa pela vaga e `@Version` contra edição simultânea.
- O schema de cada versão do código é reproduzível em qualquer máquina, e o `validate` derruba a subida se
  entidade e tabela divergirem.
- Testes automatizados usam H2 em memória com o schema gerado pelo Hibernate; o que depende do InnoDB (trava de
  vaga) só se verifica contra MySQL.
- Um banco só em produção reduz custo no free tier, mas os módulos continuam separados por tabela e por histórico
  de migrations, e qualquer um pode ir para um banco próprio sem mudar código.
