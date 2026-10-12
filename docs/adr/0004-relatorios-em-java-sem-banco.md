# ADR 0004: relatórios em Java, sem banco próprio

- **Status:** aceita
- **Data:** 12/10/2026

## Contexto

O plano da Sprint 2 era fazer o relatório de doses (tela 7) num serviço Python à parte. Na prática isso traria
uma segunda stack para manter: CI, testes e validação do JWT reescritos, numa squad que trabalha em Spring Boot.
Também foi pedido exportar em PDF e XLSX outros dados do projeto, não só as doses, com um formato padrão e com o
nome das pessoas no lugar do e-mail.

## Decisão

- Módulo `octopus-msrelatorios` em Spring Boot, igual aos outros, na branch `feature/relatorios` (porta 8086).
- Sem banco: a cada pedido, lê os dados dos outros módulos repassando o token de quem pediu. A permissão de cada
  relatório segue a de quem lê aqueles dados no sistema, e os módulos de origem conferem de novo.
- PDF com OpenPDF 2.0.5 (licença LGPL/MPL; as versões seguintes exigem Java 21) e XLSX com Apache POI.
- Nomes vêm de `GET /api/usuarios/nomes` no `msusuario`, liberado a qualquer perfil, só com e-mail, nome e perfil.

## Consequências

- Nada é duplicado: o relatório mostra o estado atual de cada módulo, sem sincronização.
- Um relatório depende de todos os módulos que lê. Se um estiver fora do ar, a resposta é 503 e nada sai pela
  metade.
- Períodos longos trazem listas inteiras pela rede. Para o volume de uma clínica isso basta; se crescer, os
  módulos de origem precisam de filtros de período mais finos.
