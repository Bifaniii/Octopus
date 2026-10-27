# octopus-msrelatorios

Microsserviço de **relatórios** do Plantão (VidaPet). Exporta em PDF ou XLSX os dados dos outros módulos. Porta
**8086**. Não tem banco: busca os dados na hora, nos outros serviços, com o token de quem pediu o relatório.

Stack: Java 17, Spring Boot 4.1.1, Spring Web (webmvc), Spring Security, Lombok, jjwt (só validação), OpenPDF
2.0.5 (PDF), Apache POI 5.5 (XLSX), springdoc. Pacote base: `com.br.octopus_msrelatorios`.

## Como funciona

- **Autenticação:** só valida o JWT do `octopus-msusuario`, com o mesmo `JWT_SECRET`, e repassa o mesmo token aos
  módulos que consulta. Cada um aplica as próprias permissões, então o relatório nunca mostra o que o perfil não
  vê no sistema.
- **Nomes:** os módulos gravam o e-mail de quem fez cada operação. O relatório troca pelo nome com
  `GET /api/usuarios/nomes` do `msusuario`.
- **Formato padrão:** o PDF tem em toda página a logo (a pata do front), "VidaPet", o título e a data de emissão.
  No rodapé ficam quem emitiu e "Página X de Y". O XLSX tem uma aba por tabela, com o mesmo cabeçalho, filtro
  automático e datas como data.
- **Doses (tela 7):** a situação é calculada na consulta. Atrasada é pendente que passou do horário, ainda dentro
  dos 30 minutos (RN-03). Perdida é pendente fora da janela (RN-04). Os turnos (manhã 6h, tarde 12h, noite 18h)
  ficam em `app.relatorios.turnos.*`.

| Endpoint (`?formato=PDF\|XLSX`, padrão PDF) | Filtros | Quem pode |
| :------- | :------ | :-------- |
| `GET /api/relatorios/doses` | `de`, `ate` (padrão: hoje), `turno` (`MANHA`, `TARDE`, `NOITE`), `auxiliar` (e-mail), `situacao` (`APLICADA`, `ATRASADA`, `PERDIDA`, `PREVISTA`, `CANCELADA`) | qualquer autenticado |
| `GET /api/relatorios/internacoes` | `status`, `de`, `ate` (data de admissão) | `ADMIN`, `RECEPCIONISTA`, `VETERINARIO` |
| `GET /api/relatorios/tutores-animais` | nenhum | `ADMIN`, `RECEPCIONISTA`, `VETERINARIO` |
| `GET /api/relatorios/baias` | nenhum | qualquer autenticado |
| `GET /api/relatorios/usuarios` | `perfil` (`ROLE_*`), `ativo` | `ADMIN` |
| `GET /api/relatorios/prescricoes/{id}` | nenhum (só PDF) | `ADMIN`, `VETERINARIO` |
| `GET /api/relatorios/altas/{internacaoId}` | nenhum (só PDF) | `ADMIN`, `RECEPCIONISTA`, `VETERINARIO` |
| `POST /api/relatorios/altas/{internacaoId}/email` | nenhum | `ADMIN`, `RECEPCIONISTA`, `VETERINARIO` |

A prescrição sai em uma página: paciente, tutor, medicamentos e, no fim, o carimbo "Dr. <nome> ASSINADO DIGITALMENTE" com a data e a hora da emissão. É um carimbo, não certificado digital. Lê a prescrição no `msprescricao` (`MS_PRESCRICAO_URL`, padrão `http://localhost:8084`); na EC2 o `relatorios.env` precisa dessa variável. Prescrição que não existe responde 404.

**Resumo de alta.** Quando uma internação tem alta (autorizada, a pedido do tutor ou já encerrada), o `ms-internacao` chama o `POST .../email`, que descreve no corpo do e-mail ao tutor o atendimento, quem atendeu (admissão, veterinário e auxiliares, com o que cada um fez), as medicações aplicadas e os cuidados dos próximos 7 dias, em texto padrão por espécie (`resources/cuidados/cao.txt`, `gato.txt` e `outros.txt`, fáceis de editar; `## ` é título e `- ` é item). O envio passa pelo `POST /api/tutores/{id}/mensagens` do `msusuario`, que só escreve ao e-mail cadastrado. Tutor sem e-mail responde 422 e a recepção baixa o mesmo conteúdo em PDF no `GET .../altas/{id}`. Internação sem alta responde 422. Os cuidados são gerais e não substituem a orientação do veterinário.

Erros: filtro inválido responde 400; módulo fora do ar ou com erro responde 503 (o relatório não sai pela metade).

## Como rodar

Os outros cinco serviços precisam estar no ar (endereços no `.env`).

```bash
cd octopus-msrelatorios
cp .env.example .env                # JWT_SECRET igual ao do msusuario e as URLs dos módulos
./mvnw spring-boot:run              # ou: docker compose up --build
./mvnw test                         # 26 testes, clients simulados, não depende do .env
```

Swagger: <http://localhost:8086/swagger-ui.html>.

Deploy: `.github/workflows/pipeline.yml` roda os testes a cada push e publica na EC2 (container `relatorios`)
quando a `<version>` do `pom.xml` muda. Na EC2, `~/octopus-env/relatorios.env` precisa existir com o `JWT_SECRET`
e as `MS_*_URL` apontando para os outros containers.

## Últimas alterações
_Push de 27/10/2026 na branch `feature/relatorios`_
- Acompanha a `situacao` da prescrição (`ATIVA`, `CONCLUIDA`, `DESATIVADA`): só a desativada à mão é descrita como interrompida pelo veterinário no resumo de alta; a que acabou sozinha aparece como um tratamento normal, e o PDF da prescrição diz "CONCLUÍDA" ou "DESATIVADA".
- PDF e planilha saem com a logo nova (`resources/logo/logo-app.png`, a mesma do front), no lugar da pata desenhada em código.
- Resumo de alta ao tutor: `POST /api/relatorios/altas/{id}/email` (corpo do e-mail com atendimento, equipe, medicações e cuidados de 7 dias) e `GET /api/relatorios/altas/{id}` (PDF para tutor sem e-mail). Depende do e-mail do tutor no `msusuario` (`0.0.6`) e do gatilho no `ms-internacao` (`0.0.5`). Versão `0.0.3` para `0.0.4-SNAPSHOT`.
- Novo `GET /api/relatorios/prescricoes/{id}`: PDF da prescrição para o veterinário imprimir, com a assinatura digital carimbada. Versão `0.0.2` para `0.0.3-SNAPSHOT`.
- Nova variável `MS_PRESCRICAO_URL` (adicionar ao `relatorios.env` na EC2 antes do deploy).
