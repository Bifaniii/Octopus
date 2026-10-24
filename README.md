# Octopus · Sistema Plantão (VidaPet) · relatórios

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
relatórios, [`octopus-msrelatorios/`](octopus-msrelatorios/README.md) (porta 8086). Ele exporta em PDF e XLSX
doses, internações, tutores e animais, ocupação de baias e usuários. Arquitetura e convenções de todos os
módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 24/10/2026 na branch `feature/relatorios`_
- `subir()` do `pipeline.yml`: container com 256 MB de RAM e até 768 MB com swap
  (`--memory 256m --memory-swap 768m`) e a JVM configurada no `docker run` por `JAVA_TOOL_OPTIONS`
  (`-Xmx128m`, metaspace até 128 MB, pilha de 256 KB, `SerialGC`, `-XX:TieredStopAtLevel=1`).
- `CLAUDE.md`: regra de memória atualizada para esses valores.

## Próximos passos
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
