# Octopus — Sistema Plantão (VidaPet)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, regras de negócio (RN-01..RN-08), squad e cronograma das 5 sprints estão no [`TAP.md`](TAP.md).

## Microsserviços

**Cada microsserviço vive na sua própria branch**, com `Dockerfile`, `docker-compose.yml` e `.env.example`
próprios — sobe sozinho, sem depender dos outros. Esta branch contém o **microsserviço de baias**.

| Módulo                    | Área                               | Porta | Branch                         |
| :------------------------ | :--------------------------------- | :---- | :----------------------------- |
| `octopus-msusuario/`      | Usuários, perfis, tutores, animais | 8080  | `main`                         |
| `ms-cadastro-baias/`      | Baias                              | 8081  | `feature/cadastro-baia`        |
| `octopus-msmedications/`  | Medicamentos                       | 8082  | `feature/register_medications` |

O que liga os três é o `JWT_SECRET`: o `msusuario` emite o token no login e os outros dois apenas validam,
sem tabela de usuários própria.

Stack: **Java 17**, **Spring Boot 4.1.1**, Spring Data JPA, Spring Security + JWT (jjwt), Bean Validation,
Lombok, MySQL 8.4 com **Flyway**, H2 só nos testes, springdoc OpenAPI.

## O que a Sprint 1 entrega (telas 1 a 3 do TAP)

| Tela do TAP | Onde | Endpoints |
| :---------- | :--- | :-------- |
| Login e perfis (base de tudo) | `msusuario` | `POST /api/auth/login`, `/api/admins`, `/api/veterinarios`, `/api/auxiliares`, `/api/recepcionistas` |
| 1 · Animais e tutores | `msusuario` | `/api/tutores`, `/api/animais` |
| 2 · Baias | `msbaias` | `/api/baias` — tipo (coletiva, isolamento, ninhada) e capacidade |
| 3 · Medicamentos | `msmedications` | `/api/medicacoes` — esquema (contínuo/sintomático) e interações proibidas |

Internação, prescrição, painel de doses e relatório são das Sprints 2 a 4.

## Como rodar

Tudo abaixo roda de dentro de `ms-cadastro-baias/`.

### 1. Variáveis de ambiente

```bash
cp .env.example .env
openssl rand -base64 48   # use a saída como JWT_SECRET
```

Não há valores default no código — sem `.env` a aplicação não sobe. O `JWT_SECRET` precisa ser **o mesmo do
`octopus-msusuario`**, senão o token emitido por ele é rejeitado aqui.

### 2. Tudo pelo Docker Compose

```bash
docker compose up --build
```

Sobe o MySQL 8.4 deste módulo e a aplicação; o compose injeta `DB_*` sozinho. As tabelas são criadas na
primeira subida pelo Flyway.

### 3. Local, com Maven Wrapper (não precisa de Maven instalado)

```bash
docker compose up -d mysql   # só o banco
./mvnw spring-boot:run       # http://localhost:8081
./mvnw test                  # testes com H2 em memória, não dependem do .env
```

### 4. Usando a API

- Swagger: <http://localhost:8081/swagger-ui.html>
- O token vem do `octopus-msusuario` (`POST /api/auth/login`). No Swagger, use o botão **Authorize** (`bearerAuth`).
- Leitura: qualquer usuário autenticado. Cadastrar, editar e remover: `ADMIN`.

| Endpoint | O que faz |
| :------- | :-------- |
| `GET /api/baias` | Lista as baias da clínica |
| `GET /api/baias/{id}` | Detalha uma baia |
| `POST /api/baias` | Cadastra baia com tipo e capacidade |
| `PUT /api/baias/{id}` | Edita a baia |
| `DELETE /api/baias/{id}` | Remove a baia |

## Convenções

- Uma branch por microsserviço, permanente: os módulos não se mesclam.
- Cada microsserviço em sua própria pasta, com `pom.xml`, `mvnw`, `Dockerfile` e compose próprios.
- Pacote base `com.br.octopus_ms<area>`, entidades em português conforme o TAP, tabelas `tb_<plural>`, id `UUID`.
- Schema por migrations Flyway em `src/main/resources/db/migration/V<n>__<descricao>.sql`; o Hibernate roda em
  `validate` e nunca cria tabela. Migration aplicada não se edita — mudança de tabela é uma nova versão.
- Detalhes de arquitetura e convenções para novos módulos: `CLAUDE.md`.

## Últimas alterações
_Push de 23/09/2026 — branch `feature/cadastro-baia`_
- Módulo autocontido: `docker-compose.yml` e `.env.example` próprios dentro de `ms-cadastro-baias/`, subindo a
  aplicação com o seu MySQL (host 3308). O compose antigo da raiz apontava para `octopus-msusuario/`, que não
  existe nesta branch, e não subia.
- Cadastro completo em `/api/baias`: entidade `Baia` com tipo (coletiva, isolamento, ninhada) e capacidade,
  DTOs, service, exceptions, JWT e `V1__baias.sql`.
- `CLAUDE.md` e `README.md` alinhados ao modelo de uma branch por microsserviço.

## Próximos passos
- [ ] Publicar este módulo no mesmo ambiente do `octopus-msusuario`, com o mesmo `JWT_SECRET`.
- [ ] Sprint 2: microsserviço de internação, que vai consumir este cadastro para alocar animais e aplicar
      RN-01 (capacidade) e RN-02 (isolamento por vacinação irregular).
- [ ] Expor a ocupação atual da baia, quando a internação existir.
