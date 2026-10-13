# Octopus · Sistema Plantão (VidaPet) · relatórios

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
relatórios, [`octopus-msrelatorios/`](octopus-msrelatorios/README.md) (porta 8086). Ele exporta em PDF e XLSX
doses, internações, tutores e animais, ocupação de baias e usuários. Arquitetura e convenções de todos os
módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 13/10/2026 na branch `feature/relatorios`_
- Versão do módulo de `0.0.1-SNAPSHOT` para `0.0.2-SNAPSHOT`: este push publica na EC2 a imagem nova, com o teto de
  400 MB de memória (`--memory 400m --memory-swap 400m`) e a JVM ajustada a ele no `Dockerfile` (heap em
  até 50%, metaspace até 128 MB, cache de código até 48 MB, `SerialGC`).

## Próximos passos
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
