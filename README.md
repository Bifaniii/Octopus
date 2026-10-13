# Octopus · Sistema Plantão (VidaPet) · relatórios

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
relatórios, [`octopus-msrelatorios/`](octopus-msrelatorios/README.md) (porta 8086). Ele exporta em PDF e XLSX
doses, internações, tutores e animais, ocupação de baias e usuários. Arquitetura e convenções de todos os
módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 13/10/2026 na branch `feature/relatorios`_
- Dose perdida lida do msdoses (RN-04), com o cálculo como reserva.
- A observação do relatório de doses identifica a reaplicação (RN-07) e a dose "se necessário". 28 testes.

## Próximos passos
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
