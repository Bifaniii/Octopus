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
