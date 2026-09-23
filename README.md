# Octopus: Sistema Plantão (VidaPet)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, as regras de negócio (RN-01 a RN-08), a squad e o cronograma das 5 sprints estão no [`TAP.md`](TAP.md).

## Microsserviços

Cada microsserviço vive na sua própria branch, com `Dockerfile`, `docker-compose.yml` e `.env.example` próprios,
e sobe sozinho. Esta branch contém o **microsserviço de baias**.

| Módulo                    | Área                               | Porta | Branch                         |
| :------------------------ | :--------------------------------- | :---- | :----------------------------- |
| `octopus-msusuario/`      | Usuários, perfis, tutores, animais | 8080  | `main`                         |
| `ms-cadastro-baias/`      | Baias                              | 8081  | `feature/cadastro-baia`        |
| `octopus-msmedications/`  | Medicamentos                       | 8082  | `feature/register_medications` |

O que liga os três é o `JWT_SECRET`. O `msusuario` emite o token no login e os outros dois apenas validam, sem
tabela de usuários própria.

Stack: Java 17, Spring Boot 4.1.1, Spring Data JPA, Spring Security com JWT (jjwt), Bean Validation, Lombok,
MySQL 8.4 com Flyway, H2 só nos testes e springdoc OpenAPI.

## O que a Sprint 1 entrega (telas 1 a 3 do TAP)

| Tela do TAP | Onde | Endpoints |
| :---------- | :--- | :-------- |
| Login e perfis (base de tudo) | `msusuario` | `POST /api/auth/login`, `/api/admins`, `/api/veterinarios`, `/api/auxiliares`, `/api/recepcionistas` |
| 1 · Animais e tutores | `msusuario` | `/api/tutores`, `/api/tutores/{id}/animais`, `/api/animais` |
| 2 · Baias | `msbaias` | `/api/baias`, com tipo (coletiva, isolamento, ninhada) e capacidade |
| 3 · Medicamentos | `msmedications` | `/api/medicacoes`, com esquema (contínuo ou sintomático) e interações proibidas |

Internação, prescrição, painel de doses e relatório ficam para as Sprints 2 a 4.

## Como rodar

Os comandos abaixo saem de dentro de `ms-cadastro-baias/`.

### 1. Variáveis de ambiente

```bash
cp .env.example .env
openssl rand -base64 48   # use a saída como JWT_SECRET
```

Não há valores default no código, então sem `.env` a aplicação não sobe. O `JWT_SECRET` precisa ser o mesmo do
`octopus-msusuario`, senão o token emitido por ele é rejeitado aqui.

### 2. Tudo pelo Docker Compose

```bash
docker compose up --build
```

Sobe o MySQL 8.4 e a aplicação. O compose preenche as variáveis `DB_*` sozinho, e o Flyway cria as tabelas na
primeira subida.

### 3. Local, com Maven Wrapper (não precisa de Maven instalado)

```bash
docker compose up -d mysql   # só o banco
./mvnw spring-boot:run       # http://localhost:8081
./mvnw test                  # testes com H2 em memória, não dependem do .env
```

### 4. Usando a API

- Swagger: <http://localhost:8081/swagger-ui.html>
- O token vem do `octopus-msusuario`, em `POST /api/auth/login`. No Swagger, use o botão **Authorize** (`bearerAuth`).
- Leitura: qualquer usuário autenticado. Cadastrar, editar e desativar: `ADMIN`.

| Endpoint | O que faz |
| :------- | :-------- |
| `GET /api/baias` | Lista as baias da clínica |
| `GET /api/baias/{id}` | Detalha uma baia |
| `POST /api/baias` | Cadastra baia com tipo e capacidade |
| `PUT /api/baias/{id}` | Edita a baia |
| `PATCH /api/baias/{id}/desativar` | Arquiva a baia |

Nada é apagado no sistema: o que sai de uso é desativado (`ativo = false`) e continua no banco, para preservar
o histórico.

## Convenções

- Uma branch por microsserviço, permanente. Os módulos não se mesclam.
- Cada microsserviço na sua pasta, com `pom.xml`, `mvnw`, `Dockerfile` e compose próprios.
- Pacote base `com.br.octopus_ms<area>`, entidades em português conforme o TAP, tabelas `tb_<plural>`, id `UUID`.
- Schema por migrations Flyway em `src/main/resources/db/migration/V<n>__<descricao>.sql`. O Hibernate roda em
  `validate` e nunca cria tabela. Migration aplicada não se edita: mudança de tabela é uma nova versão.
- Arquitetura e convenções para novos módulos: `CLAUDE.md`.

## Últimas alterações
_Push de 24/09/2026 na branch `feature/cadastro-baia`_
- `DELETE /api/baias/{id}` virou `PATCH /api/baias/{id}/desativar`. A baia sai de uso arquivada, com o campo
  `ativo` e a migration `V2__baias_ativo.sql`.
- O módulo ficou autocontido: `docker-compose.yml` e `.env.example` dentro de `ms-cadastro-baias/`, subindo a
  aplicação com o seu MySQL na porta 3308. O compose antigo, na raiz, apontava para `octopus-msusuario/`, que
  não existe nesta branch, e por isso não subia.
- `mvnw` voltou a ser executável. Estava sem a permissão no git, e `./mvnw` falhava.
- `CLAUDE.md` e `README.md` reescritos junto com as outras branches.

## Próximos passos
- [ ] Publicar este módulo no mesmo ambiente do `octopus-msusuario`, com o mesmo `JWT_SECRET`.
- [ ] Sprint 2: microsserviço de internação, que vai consumir este cadastro para alocar animais e aplicar a
      RN-01 (capacidade) e a RN-02 (isolamento por vacinação irregular).
- [ ] Expor a ocupação atual da baia, quando a internação existir.
