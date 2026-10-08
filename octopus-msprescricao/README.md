# octopus-msprescricao

Microsserviço de **prescrição** do Plantão (VidaPet): o veterinário prescreve medicamentos para uma
internação, define o intervalo e a quantidade de doses, e o sistema gera os horários automaticamente
(TAP, item 5). Porta **8084**, MySQL na porta de host **3311**.

Stack: Java 17, Spring Boot 4.1.1, Spring Web (webmvc), Spring Data JPA, Spring Security, Bean Validation,
Lombok, MySQL 8.4 com Flyway, jjwt (só validação), springdoc, H2 só nos testes.
Pacote base: `com.br.octopus_msprescricao`.

## Como funciona

- **Autenticação:** este módulo não tem login nem usuários. O `octopus-msusuario` emite o JWT e aqui só se
  valida, com o mesmo `JWT_SECRET`. A identidade vem dos claims: `sub` = e-mail, `role` = `ROLE_*`.
- **Referências a outros módulos** (`internacaoId`, `medicacaoId`) são `UUID` sem `@ManyToOne` e sem FK,
  porque as tabelas pertencem a outros microsserviços.
- **Nada é apagado:** não há `DELETE`. A prescrição sai de uso com `PATCH /api/prescricoes/{id}/desativar`.

### Endpoints

| Endpoint | O que faz | Quem pode |
| :------- | :-------- | :-------- |
| `POST /api/prescricoes` | Cria a prescrição com os itens e gera as doses (o veterinário é o do token) | `ADMIN`, `VETERINARIO` |
| `GET /api/prescricoes?internacaoId=` | Lista ativas e inativas; o filtro é opcional | qualquer autenticado |
| `GET /api/prescricoes/{id}` | Prescrição com itens e doses | qualquer autenticado |
| `PATCH /api/prescricoes/{id}/desativar` | Marca como inativa | `ADMIN`, `VETERINARIO` |

Exemplo de corpo do `POST`:

```json
{
  "internacaoId": "0b6f6a52-2f5a-4f0e-9f57-1d2a3c4b5d6e",
  "observacao": "Animal agitado",
  "itens": [
    {
      "medicacaoId": "7c1e2d3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f",
      "dosagem": "5 mg",
      "intervaloHoras": 8,
      "quantidadeDoses": 3,
      "inicio": "2026-10-10T08:00:00"
    }
  ]
}
```

Esse item gera 3 doses `PENDENTE`: 10/10 08:00, 10/10 16:00 e 11/10 00:00.

### Modelo

`Prescricao` (`tb_prescricoes`) 1:N `ItemPrescricao` (`tb_itens_prescricao`) 1:N `Dose` (`tb_doses`).
Schema nas migrations `V1__prescricoes.sql` e `V2__doses.sql`.

## Como rodar

```bash
cd octopus-msprescricao
cp .env.example .env                # preencher os valores
openssl rand -base64 48             # use a saída como JWT_SECRET (o MESMO do msusuario)
docker compose up --build           # aplicação + MySQL
# ou: docker compose up -d mysql && ./mvnw spring-boot:run
./mvnw test                         # H2 em memória, não depende do .env
```

Swagger: <http://localhost:8084/swagger-ui.html>. Pegue o token em `POST /api/auth/login` do msusuario
(porta 8080) e use o botão **Authorize**.

## Últimas alterações
_Push de 07/10/2026 na branch `feature/prescricao`_
- Módulo criado: entidades `Prescricao`, `ItemPrescricao` e `Dose`, migrations V1 e V2, DTOs, service e
  `PrescricaoController` em `/api/prescricoes`.
- Geração automática das doses a partir de `inicio`, `intervaloHoras` e `quantidadeDoses`.
- Segurança por JWT do msusuario (`JwtService`, filtro, `SecurityConfig`, erros 401/403 em JSON).
- Testes: `JwtService`, `PrescricaoService` (Mockito) e integração com MockMvc (401, 403, 400, 404, fluxo completo).
- Dockerfile, `docker-compose.yml`, `.env.example` e `ci.yml` próprios.

## Próximos passos
- [ ] Rodar `./mvnw clean install` e subir contra o MySQL: o esqueleto foi escrito sem compilar.
- [ ] RN-03: bloquear interação proibida entre medicamentos da mesma prescrição (decidir como consultar o msmedications).
- [ ] Validar que a internação existe e está `ATIVA` quando o `octopus-msinternacao` existir.
- [ ] Decidir se painel e registro de aplicação de doses (RN-04 a RN-08) ficam aqui ou em `octopus-msplantao`.
- [ ] Definir o que acontece com as doses `PENDENTE` quando a prescrição é desativada.
- [ ] Tratar esquema sintomático (sem horário fixo), que depende do `TipoEsquema` do medicamento.
