# ADR 0002: ids `Long` em tudo, menos nos usuários

- **Status:** aceita
- **Data:** 11/10/2026

## Contexto

Os cadastros da Sprint 1 nasceram com id `UUID` (`BINARY(16)`). Em 09/10 a internação e a prescrição passaram
a `Long`, e o sistema ficou com os dois tipos misturados: a prescrição guardava `medicacaoId` como `Long` enquanto
o módulo de medicamentos devolvia `UUID`, o que quebrava a consulta de interações.

## Decisão

Toda entidade usa id `Long` com `GenerationType.IDENTITY` (`BIGINT AUTO_INCREMENT`), e toda referência a outro
módulo é uma coluna `BIGINT`. A exceção são os usuários: `Usuario` e os perfis (admin, veterinário, auxiliar,
recepcionista) continuam `UUID`.

## Consequências

- Ids curtos e legíveis nas telas, nos logs e nos casos de teste (`/api/internacoes/42`).
- As migrations de conversão (`V7` no msusuario, `V3` em baias, `V4` em medicamentos) preservam os cadastros e
  remapeiam as FKs internas. As internações e prescrições de teste foram descartadas (`V4` da internação, `V2` da
  prescrição), porque apontavam para UUIDs que deixaram de existir.
- Id sequencial é previsível. Isso não abre acesso: toda rota exige token e perfil.
