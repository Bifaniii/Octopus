# Octopus — Sistema Plantão (VidaPet)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, regras de negócio (RN-01..RN-08), squad e cronograma das 5 sprints estão no [`TAP.md`](TAP.md).

## Microsserviços

O sistema é dividido em microsserviços Spring Boot independentes, um por área, cada um em sua própria pasta:

| Módulo                    | Área                            | Onde está                              | Estado                                            |
| :------------------------ | :------------------------------ | :------------------------------------- | :------------------------------------------------ |
| `octopus-msusuario/`      | Usuários, perfis, tutores, animais | `main`                              | Funcional (login JWT, CRUD de perfis, tutor, animal stub); schema via Flyway |
| `ms-cadastro-baias/`      | Baias                           | `feature/cadastro-baia`                | Entidade + repository                             |
| `octopus-msmedications/`  | Medicamentos                    | `feature/register_medications`         | Entidade + repository + migration                 |

Stack: **Java 17**, **Spring Boot 4.1.1**, Spring Data JPA, Spring Security + JWT (jjwt), Bean Validation,
Lombok, MySQL 8.4 (docker-compose) com **Flyway**, H2 só nos testes, springdoc OpenAPI.

## Como rodar

### 1. Variáveis de ambiente

Copie `.env.example` para `.env` na raiz e preencha. Não há valores default no código — sem `.env` a aplicação
não sobe.

```bash
cp .env.example .env
openssl rand -base64 48   # use a saída como JWT_SECRET
```

O banco de desenvolvimento é o MySQL do docker-compose (o schema é criado por migrations Flyway em SQL de
MySQL). No `.env`, aponte a aplicação para ele:

```properties
DB_URL=jdbc:mysql://localhost:3307/<mesmo valor de MYSQL_DATABASE>
DB_USERNAME=<mesmo valor de MYSQL_USER>
DB_PASSWORD=<mesmo valor de MYSQL_PASSWORD>
```

### 2. Local, com Maven Wrapper (não precisa de Maven instalado)

```bash
docker compose up -d mysql   # sobe só o banco (porta MYSQL_HOST_PORT, default 3307)
cd octopus-msusuario
./mvnw spring-boot:run       # sobe em http://localhost:8080; o Flyway cria as tabelas na primeira subida
./mvnw test                  # roda os testes (H2 em memória e config própria, não dependem do .env)
```

Se o volume do MySQL ainda tiver tabelas de antes do Flyway, zere com `docker compose down -v` antes de subir.

### 3. Tudo com Docker Compose (MySQL + msusuario)

```bash
docker compose up --build
```

Sobe o MySQL 8.4 na porta `MYSQL_HOST_PORT` (default 3307) e o `msusuario` na 8080. O compose injeta `DB_*`
sozinho; não precisa configurar no `.env`.

### 4. Usando a API

- Swagger: <http://localhost:8080/swagger-ui.html>
- Login: `POST /api/auth/login` com e-mail e senha → token JWT. No Swagger, use o botão **Authorize** (`bearerAuth`).
- Na primeira subida é criado um admin com `ADMIN_EMAIL` / `ADMIN_SENHA` do `.env`. Só admin cadastra
  veterinário, auxiliar, recepcionista e outros admins; admin e recepcionista cadastram tutores e animais.

## Convenções

- Branch por funcionalidade: `feature/<nome_da_funcionalidade>`, partindo da `main`.
- Cada microsserviço em sua própria pasta na raiz, com `pom.xml` e `mvnw` próprios.
- Pacote base `com.br.octopus_ms<area>`, entidades em português conforme o TAP, tabelas `tb_<plural>`, id `UUID`.
- Schema por migrations Flyway em `src/main/resources/db/migration/V<n>__<descricao>.sql`; o Hibernate roda em
  `validate` e nunca cria tabela. Migration aplicada não se edita — mudança de tabela é uma nova versão.
- Detalhes de arquitetura e convenções para novos módulos: [`CLAUDE.md`](CLAUDE.md).

## Documentação

- [`TAP.md`](TAP.md) — Termo de Abertura do Projeto.
- [`docs/mapas-processo.html`](docs/mapas-processo.html) — mapas de processo (diagramas de atividade UML com raias) das
  funcionalidades já implementadas: usuários e acesso, medicamentos e baias. Baixe e abra no navegador, ou veja
  pelo GitHub Pages se estiver ativado para a pasta `docs/`.

## Últimas alterações
_Push de 20/09/2026 — branch `main`_
- `docs/mapas-processo.html`: mapas de processo (UML, raias por ator, sem raia "Sistema") de gestão de usuários e
  acesso, cadastro de medicamentos e cadastro de baias, com as regras do TAP e as premissas da squad marcadas.
- README com seção "Documentação".

## Próximos passos
- [ ] Integrar `feature/cadastro-baia` na `main` sem remover `octopus-msusuario`; alinhar pacote, nome da
      entidade (`Baia`), id `UUID`, tabela `tb_baias` e criar `V1__baias.sql` com Flyway.
- [ ] `octopus-msmedications`: adicionar Flyway ao módulo (`spring-boot-starter-flyway` + `flyway-mysql`,
      `ddl-auto=validate`), trocar id para `UUID`/`BINARY(16)` e tabela para `tb_medicacoes`, e integrar a branch
      órfã `feature/register_medications` sobre a `main`.
- [ ] Criar service, DTOs e controller de baias e de medicamentos (Sprint 1, telas 1–3).
- [ ] Completar a entidade `Animal` (espécie, data da última vacina antirrábica) via nova migration e criar a
      entidade central de internação simples, sem validações (Sprint 1).
- [ ] Adicionar os novos módulos ao `docker-compose.yml` e definir como eles vão autenticar (validar o JWT do
      `msusuario` com o mesmo `JWT_SECRET` ou ficar sem segurança por enquanto).
- [ ] Apagar a branch remota `feature/cadastro_login_usuario` (já mergeada).
