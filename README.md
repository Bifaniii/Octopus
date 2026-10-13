# Octopus — Sistema Plantão (VidaPet) · doses

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
doses, [`octopus-msdoses/`](octopus-msdoses/README.md) (porta 8085): guarda uma dose por horário gerado na
prescrição e registra a aplicação pelo auxiliar. Arquitetura e convenções de todos os módulos:
[`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 13/10/2026 na branch `feature/doses`_
- Teto de 400 MB de memória por serviço: o `docker run` do `pipeline.yml` passa a usar
  `--memory 400m --memory-swap 400m`.
- `Dockerfile` com a JVM ajustada ao teto: heap em até 50%, metaspace até 128 MB, cache de código até
  48 MB, pilha de 512 KB por thread e `SerialGC`.
- `CLAUDE.md`: regra de memória na seção "Integração contínua".

## Próximos passos
- [ ] Subir a `<version>` do `pom.xml` para o teto de memória valer na EC2, ou aplicar no container atual
      com `docker update --memory 400m --memory-swap 400m <container>`.
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
