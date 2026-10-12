# Octopus · Sistema Plantão (VidaPet) · relatórios

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
relatórios, [`octopus-msrelatorios/`](octopus-msrelatorios/README.md) (porta 8086). Ele exporta em PDF e XLSX
doses, internações, tutores e animais, ocupação de baias e usuários. Arquitetura e convenções de todos os
módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 12/10/2026 na branch `feature/relatorios`_
- Módulo novo em Java, como os outros. O plano anterior era um serviço Python; em 12/10 ficou decidido manter uma stack só.
- Cinco relatórios em PDF (formato padrão com logo, cabeçalho e rodapé) e XLSX (uma aba por tabela).
- Relatório de doses da tela 7: por turno, por auxiliar e a lista de doses, com atrasadas e perdidas calculadas
  pela janela de 30 minutos.
- Nomes das pessoas no lugar dos e-mails, pelo `GET /api/usuarios/nomes` novo no `msusuario` (branch `main`).
- 26 testes.

## Próximos passos
- [ ] Criar `~/octopus-env/relatorios.env` na EC2 e liberar a porta 8086 antes do primeiro deploy.
- [ ] Publicar o `msusuario` com o `/api/usuarios/nomes` antes ou junto com este módulo.
- [ ] Confirmar com a squad os horários dos turnos (hoje 6h, 12h e 18h).
