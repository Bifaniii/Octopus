# Octopus — Sistema Plantão (VidaPet) · doses

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
doses, [`octopus-msdoses/`](octopus-msdoses/README.md) (porta 8085): guarda uma dose por horário gerado na
prescrição e registra a aplicação pelo auxiliar. Arquitetura e convenções de todos os módulos:
[`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 24/10/2026 na branch `feature/doses`_
- `subir()` do `pipeline.yml`: container com 256 MB de RAM e até 768 MB com swap
  (`--memory 256m --memory-swap 768m`) e a JVM configurada no `docker run` por `JAVA_TOOL_OPTIONS`
  (`-Xmx128m`, metaspace até 128 MB, pilha de 256 KB, `SerialGC`, `-XX:TieredStopAtLevel=1`).
- `CLAUDE.md`: regra de memória atualizada para esses valores.

## Próximos passos
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
