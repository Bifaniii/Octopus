# Octopus — Sistema Plantão (VidaPet) · prescrição

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. Esta branch contém o microsserviço de
prescrição, [`octopus-msprescricao/`](octopus-msprescricao/README.md) (porta 8084): o veterinário prescreve
medicamentos para uma internação, com a RN-06 (interação proibida no mesmo horário), a ligação com o
`ms-internacao` e o agendamento das doses no `octopus-msdoses`.
Arquitetura e convenções de todos os módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 27/10/2026 na branch `feature/prescricao`_
- A prescrição passa a se concluir sozinha. Ao listar ou buscar, o módulo confere se o animal já saiu (internação `ENCERRADA`) ou se o `msdoses` não tem mais dose pendente nem item "se necessário" ativo; se sim, ela fica `ativo = false` com `concluidaEm` preenchido (migration `V4`). A resposta ganhou `situacao` (`ATIVA`, `CONCLUIDA` ou `DESATIVADA`) e `concluidaEm`. As criadas há menos de 2 minutos ficam de fora, e se o `msdoses` ou o `ms-internacao` não responderem nada muda. Prescrição concluída não pode ser reativada (422): não há mais doses. Versão `0.0.3` para `0.0.4-SNAPSHOT`.
- Ajuste de memória do Douglas (24/10), mantido:
  - `subir()` do `pipeline.yml`: container com 256 MB de RAM e até 768 MB com swap
  (`--memory 256m --memory-swap 768m`) e a JVM configurada no `docker run` por `JAVA_TOOL_OPTIONS`
  (`-Xmx128m`, metaspace até 128 MB, pilha de 256 KB, `SerialGC`, `-XX:TieredStopAtLevel=1`).
  - `CLAUDE.md`: regra de memória atualizada para esses valores.

## Próximos passos
- [ ] Executar os 10 casos de teste do enunciado com os dados de carga (Sprint 5)
- [ ] Publicar em produção junto com os outros módulos (ver o documento de deploy)
