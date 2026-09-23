# Octopus — Sistema Plantão (VidaPet)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, regras de negócio (RN-01..RN-08), squad e cronograma das 5 sprints estão no [`TAP.md`](TAP.md).

## Microsserviços

O sistema é dividido em microsserviços Spring Boot independentes, um por área, cada um em sua própria pasta:

| Módulo                    | Área                               | Porta | Onde está                      | Estado |
| :------------------------ | :--------------------------------- | :---- | :----------------------------- | :----- |
| `octopus-msusuario/`      | Usuários, perfis, tutores, animais | 8080  | `main`                         | Completo — login JWT, CRUD de perfis, tutor e animal (stub) |
| `ms-cadastro-baias/`      | Baias                              | 8081  | `feature/cadastro-baia`        | Completo — CRUD de baias com tipo e capacidade |
| `octopus-msmedications/`  | Medicamentos                       | 8082  | `feature/register_medications` | Completo — CRUD de medicamentos, esquema e interações proibidas |

Os três dividem o mesmo banco MySQL e o mesmo `JWT_SECRET`: o `msusuario` emite o token no login e os outros
dois apenas validam, sem tabela de usuários própria.

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

cd octopus-msusuario  && ./mvnw spring-boot:run   # http://localhost:8080
cd ms-cadastro-baias  && ./mvnw spring-boot:run   # http://localhost:8081
cd octopus-msmedications && ./mvnw spring-boot:run # http://localhost:8082

./mvnw test                  # testes de cada módulo (H2 em memória, não dependem do .env)
```

Cada módulo cria as próprias tabelas na primeira subida (Flyway) e usa a sua tabela de histórico, por isso
podem dividir o mesmo banco. Faça login no `msusuario` e use o token nos outros dois.

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
_Push de 22/09/2026 — branch `feature/cadastro-baia`_
- `ms-cadastro-baias` concluído (Douglas): entidade `Baia` com tipo (coletiva, isolamento, ninhada) e
  capacidade, DTOs, service, controller `/api/baias`, exceptions, JWT, `V1__baias.sql` e Dockerfile.
- `CLAUDE.md` e `README.md` sincronizados com as demais branches: seção "Segurança entre módulos" (cada
  módulo valida o token do `msusuario`), regras de Flyway em banco compartilhado e estado das branches.

## Próximos passos
- [ ] **Antes do merge:** restaurar `octopus-msusuario/` nesta branch (`git checkout main -- octopus-msusuario/`);
      o commit `fe7c49d` removeu o diretório e o merge apagaria o ms de usuário da `main`.
- [ ] Integrar na `main` por PR, junto com `feature/register_medications`.
- [ ] Publicar o módulo de baias no mesmo ambiente do `msusuario`.
- [ ] Sprint 2: entidade de internação, alocação em baia e validações RN-01 (capacidade) e RN-02 (vacinação),
      que vão consumir este cadastro.
- [ ] Apagar a branch remota `feature/cadastro_login_usuario` (já mergeada).
