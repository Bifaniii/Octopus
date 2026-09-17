# Octopus — Sistema Plantão (VidaPet)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, regras de negócio (RN-01..RN-08), squad e cronograma das 5 sprints estão no [`TAP.md`](TAP.md).

## Microsserviços

O sistema é dividido em microsserviços Spring Boot independentes, um por área, cada um em sua própria pasta:

| Módulo                    | Área                            | Onde está                              | Estado                                            |
| :------------------------ | :------------------------------ | :------------------------------------- | :------------------------------------------------ |
| `octopus-msusuario/`      | Usuários, perfis, tutores, animais | `main`                              | Funcional (login JWT, CRUD de perfis, tutor, animal stub) |
| `ms-cadastro-baias/`      | Baias                           | `feature/cadastro-baia`                | Entidade + repository                             |
| `octopus-msmedications/`  | Medicamentos                    | `feature/register_medications`         | Entidade + repository                             |

Stack: **Java 17**, **Spring Boot 4.1.1**, Spring Data JPA, Spring Security + JWT (jjwt), Bean Validation,
Lombok, H2 (dev) / MySQL 8.4 (docker-compose), springdoc OpenAPI.

## Como rodar

### 1. Variáveis de ambiente

Copie `.env.example` para `.env` na raiz e preencha. Não há valores default no código — sem `.env` a aplicação
não sobe.

```bash
cp .env.example .env
openssl rand -base64 48   # use a saída como JWT_SECRET
```

Para rodar local com H2 em memória:

```properties
DB_URL=jdbc:h2:mem:octopus;DB_CLOSE_DELAY=-1
DB_USERNAME=sa
DB_PASSWORD=
```

### 2. Local, com Maven Wrapper (não precisa de Maven instalado)

```bash
cd octopus-msusuario
./mvnw spring-boot:run     # sobe em http://localhost:8080
./mvnw test                # roda os testes (usam H2 e config própria, não dependem do .env)
```

### 3. Com Docker Compose (MySQL + msusuario)

```bash
docker compose up --build
```

Sobe o MySQL 8.4 na porta `MYSQL_HOST_PORT` (default 3307) e o `msusuario` na 8080.

### 4. Usando a API

- Swagger: <http://localhost:8080/swagger-ui.html>
- Login: `POST /api/auth/login` com e-mail e senha → token JWT. No Swagger, use o botão **Authorize** (`bearerAuth`).
- Na primeira subida é criado um admin com `ADMIN_EMAIL` / `ADMIN_SENHA` do `.env`. Só admin cadastra
  veterinário, auxiliar, recepcionista e outros admins; admin e recepcionista cadastram tutores e animais.
- Console H2 (quando `DB_URL` for H2): <http://localhost:8080/h2-console>

## Convenções

- Branch por funcionalidade: `feature/<nome_da_funcionalidade>`, partindo da `main`.
- Cada microsserviço em sua própria pasta na raiz, com `pom.xml` e `mvnw` próprios.
- Pacote base `com.br.octopus_ms<area>`, entidades em português conforme o TAP, tabelas `tb_<plural>`, id `UUID`.
- Detalhes de arquitetura e convenções para novos módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 17/09/2026 — branch `main`_
- `CLAUDE.md` passa a ser versionado, com o estado atual das três branches, convenções para novos
  microsserviços e regras de configuração via `.env`.
- Credenciais e URLs de banco removidas do `application.properties`; tudo vem do `.env` (`.env.example`
  só com os nomes das variáveis). Testes usam `src/test/resources/application.properties` autocontido.
- Branch `feature/cadastro_login_usuario` mergeada (PR #1): login JWT, perfis Admin/Veterinário/Auxiliar/
  Recepcionista, cadastro de tutores e animais, admin inicial via bootstrap, Swagger, Dockerfile e docker-compose.
- Criado este `README.md`.

## Próximos passos
- [ ] Integrar `feature/cadastro-baia` na `main` sem remover `octopus-msusuario`; alinhar pacote, nome da
      entidade (`Baia`), id `UUID` e tabela `tb_baias` às convenções.
- [ ] Corrigir compilação de `octopus-msmedications` (`Medication.java` → `Medicacao.java`, tipos do repository)
      e integrar a branch órfã `feature/register_medications` sobre a `main`.
- [ ] Criar service, DTOs e controller de baias e de medicamentos (Sprint 1, telas 1–3).
- [ ] Completar a entidade `Animal` (espécie, data da última vacina antirrábica) e a entidade central de
      internação simples, sem validações (Sprint 1).
- [ ] Adicionar os novos módulos ao `docker-compose.yml` e definir como eles vão autenticar (validar o JWT do
      `msusuario` com o mesmo `JWT_SECRET` ou ficar sem segurança por enquanto).
- [ ] Apagar a branch remota `feature/cadastro_login_usuario` (já mergeada).
