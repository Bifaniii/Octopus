# octopus-msprescricao

Microsserviço de **prescrição** do Plantão (VidaPet): o veterinário prescreve medicamentos para uma
internação, com intervalo e horário de início (TAP, item 5). Porta **8084**, MySQL na porta de host **3311**.

Stack: Java 17, Spring Boot 4.1.1, Spring Web (webmvc), Spring Data JPA, Spring Security, Bean Validation,
Lombok, MySQL 8.4 com Flyway, jjwt (só validação), springdoc, H2 só nos testes.
Pacote base: `com.br.octopus_msprescricao`.

## Como funciona

- **Autenticação:** este módulo não tem login nem usuários. O `octopus-msusuario` emite o JWT e aqui só se
  valida, com o mesmo `JWT_SECRET`. A identidade vem dos claims: `sub` = e-mail, `role` = `ROLE_*`.
- **Ids:** prescrição e item são `Long` (`BIGINT AUTO_INCREMENT`), como a internação. Referências a outros
  módulos não têm `@ManyToOne` nem FK: `internacaoId` e `medicacaoId` são `Long`.
- **Ligação com a internação:** ao criar, a internação é buscada no `ms-internacao` e precisa estar `ADMITIDA`,
  `EM_TRATAMENTO` ou `ISOLAMENTO` (senão 422). Depois o módulo chama `PATCH /api/internacoes/{id}/iniciar-tratamento`
  (idempotente) e só então grava.
- **RN-03:** cada medicamento novo é consultado no msmedications; a prescrição é recusada com 422 se dois deles
  têm interação proibida entre si, ou se algum tem interação proibida com um medicamento das prescrições ativas
  da mesma internação.
- **Outros módulos fora do ar:** 503. Internação ou medicamento inexistente: 404. As chamadas repassam o token de
  quem prescreveu.
- **Nada é apagado:** não há `DELETE`. A prescrição sai de uso com `PATCH /api/prescricoes/{id}/desativar`.

### Endpoints

| Endpoint | O que faz | Quem pode |
| :------- | :-------- | :-------- |
| `POST /api/prescricoes` | Cria a prescrição com os itens (o veterinário é o do token) | `VETERINARIO` |
| `GET /api/prescricoes?internacaoId=` | Lista ativas e inativas; o filtro é opcional | qualquer autenticado |
| `GET /api/prescricoes/{id}` | Prescrição com os itens | qualquer autenticado |
| `PATCH /api/prescricoes/{id}/desativar` | Marca como inativa | `VETERINARIO` |

Exemplo de corpo do `POST`:

```json
{
  "internacaoId": 42,
  "observacao": "Animal agitado",
  "itens": [
    {
      "medicacaoId": 7,
      "intervaloHoras": 8,
      "inicio": "2026-10-10T08:00:00"
    }
  ]
}
```

### Modelo

`Prescricao` (`tb_prescricoes`) 1:N `ItemPrescricao` (`tb_itens_prescricao`), na migration `V1__prescricoes.sql`.

## Como rodar

```bash
cd octopus-msprescricao
cp .env.example .env                # preencher os valores, inclusive MS_INTERNACAO_URL e MS_MEDICACOES_URL
openssl rand -base64 48             # use a saída como JWT_SECRET (o MESMO do msusuario)
docker compose up --build           # aplicação + MySQL
# ou: docker compose up -d mysql && ./mvnw spring-boot:run
./mvnw test                         # 37 testes, H2 em memória, não depende do .env
```

Swagger: <http://localhost:8084/swagger-ui.html>. Pegue o token em `POST /api/auth/login` do msusuario
(porta 8080) e use o botão **Authorize**.

Deploy: `.github/workflows/pipeline.yml` roda os testes a cada push e publica na EC2 (container `prescricao`)
quando a `<version>` do `pom.xml` muda. Na EC2, o arquivo `~/octopus-env/prescricao.env` precisa existir.

Últimas alterações e próximos passos: ver o [`README.md`](../README.md) da raiz.
