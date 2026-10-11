# octopus-msdoses

Microsserviço de **doses** do Plantão (VidaPet). Recebe da prescrição os horários gerados para cada item e guarda
uma dose por horário; o auxiliar registra a aplicação. Porta **8085**, MySQL na porta de host **3312**.

Stack: Java 17, Spring Boot 4.1.1, Spring Web (webmvc), Spring Data JPA, Spring Security, Bean Validation,
Lombok, MySQL 8.4 com Flyway, jjwt (só validação), springdoc, H2 só nos testes.
Pacote base: `com.br.octopus_msdoses`.

## Como funciona

- **Autenticação:** só valida o JWT do `octopus-msusuario`, com o mesmo `JWT_SECRET`.
- **Agendamento:** o `octopus-msprescricao`, ao gravar uma prescrição, chama `POST /api/doses/agendamentos` com o
  token do veterinário. Repetir o agendamento da mesma prescrição devolve as doses que já existem.
- **Aplicação (RN-05):** `PATCH /api/doses/{id}/aplicar`, só `AUXILIAR`. Grava o e-mail do token e a hora da
  clínica; não há edição depois.
- **Cancelamento:** desativar a prescrição cancela as doses pendentes. As aplicadas ficam como estão.
- **Nada é apagado:** a dose termina `APLICADA` ou `CANCELADA`.

| Endpoint | Quem pode |
| :------- | :-------- |
| `POST /api/doses/agendamentos` | `VETERINARIO` |
| `PATCH /api/doses/prescricoes/{prescricaoId}/cancelar` | `VETERINARIO` |
| `GET /api/doses?internacaoId=&prescricaoId=&status=&de=&ate=`, `GET /api/doses/{id}` | qualquer autenticado |
| `PATCH /api/doses/{id}/aplicar` | `AUXILIAR` |

## Como rodar

```bash
cd octopus-msdoses
cp .env.example .env                # preencher os valores; JWT_SECRET igual ao do msusuario
docker compose up --build           # aplicação + MySQL
# ou: docker compose up -d mysql && ./mvnw spring-boot:run
./mvnw test                         # 23 testes, H2 em memória, não depende do .env
```

Swagger: <http://localhost:8085/swagger-ui.html>.

Deploy: `.github/workflows/pipeline.yml` roda os testes a cada push e publica na EC2 (container `doses`) quando
a `<version>` do `pom.xml` muda. Na EC2, o arquivo `~/octopus-env/doses.env` precisa existir.
