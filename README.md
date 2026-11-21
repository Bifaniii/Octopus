# Octopus: Sistema Plantão (VidaPet)

![CI](https://github.com/Bifaniii/Octopus/actions/workflows/ci.yml/badge.svg)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, as regras de negócio (RN-01 a RN-08), a squad e o cronograma das 5 sprints estão no [`TAP.md`](TAP.md).

## Microsserviços

Cada microsserviço vive na sua própria branch, com `Dockerfile`, `docker-compose.yml` e `.env.example` próprios,
e sobe sozinho. Esta branch contém o **microsserviço de usuários**.

| Módulo                    | Área                               | Porta | Branch                         |
| :------------------------ | :--------------------------------- | :---- | :----------------------------- |
| `octopus-msusuario/`      | Usuários, perfis, tutores, animais | 8080  | `main`                         |
| `ms-cadastro-baias/`      | Baias                              | 8081  | `feature/cadastro-baia`        |
| `octopus-msmedications/`  | Medicamentos                       | 8082  | `feature/register_medications` |
| `ms-internacao/`          | Internação e ciclo de vida         | 8083  | `feature/internacao`           |
| `octopus-msprescricao/`   | Prescrição e horários              | 8084  | `feature/prescricao`           |
| `octopus-msdoses/`        | Doses e aplicação                  | 8085  | `feature/doses`                |
| `octopus-msrelatorios/`   | Relatórios em PDF e XLSX           | 8086  | `feature/relatorios`           |

O que liga os módulos é o `JWT_SECRET`. O `msusuario` emite o token no login e os outros apenas validam, sem
tabela de usuários própria.

Stack: Java 17, Spring Boot 4.1.1, Spring Data JPA, Spring Security com JWT (jjwt), Bean Validation, Lombok,
MySQL 8.4 com Flyway, H2 só nos testes e springdoc OpenAPI.

## Documentação

- [Panorama da Sprint 1](https://claude.ai/artifact/AZfbHYkBdQNs2ZBWSrMZ8U): o que está funcionando, como o
  login e o banco se encaixam, endpoints por perfil e o que falta publicar.
- [Mapas de processo](docs/mapas-processo.html): diagramas de atividade em UML, com raias, das
  funcionalidades prontas. Página HTML autocontida, versionada no repositório.
- [`TAP.md`](TAP.md), o Termo de Abertura do Projeto.
- [`CLAUDE.md`](CLAUDE.md), com arquitetura, convenções e decisões técnicas.

## O que a Sprint 1 entrega (telas 1 a 3 do TAP)

| Tela do TAP | Onde | Endpoints |
| :---------- | :--- | :-------- |
| Login e perfis (base de tudo) | `msusuario` | `POST /api/auth/login`, `/api/admins`, `/api/veterinarios`, `/api/auxiliares`, `/api/recepcionistas` |
| 1 · Animais e tutores | `msusuario` | `/api/tutores`, `/api/tutores/{id}/animais`, `/api/animais` |
| 2 · Baias | `msbaias` | `/api/baias`, com tipo (coletiva, isolamento, ninhada) e capacidade |
| 3 · Medicamentos | `msmedications` | `/api/medicacoes`, com esquema (contínuo ou sintomático) e interações proibidas |

Internação, prescrição, painel de doses e relatório ficam para as Sprints 2 a 4.

## Como rodar

Nesta branch o compose e o `.env` ficam na raiz do repositório, e é de lá que saem os comandos.

### 1. Variáveis de ambiente

```bash
cp .env.example .env
openssl rand -base64 48   # use a saída como JWT_SECRET
```

Não há valores default no código, então sem `.env` a aplicação não sobe. Os outros módulos precisam usar o mesmo
`JWT_SECRET`, senão o token emitido aqui é rejeitado lá.

### 2. Tudo pelo Docker Compose

```bash
docker compose up --build
```

Sobe o MySQL 8.4 e a aplicação. O compose preenche as variáveis `DB_*` sozinho, e o Flyway cria as tabelas na
primeira subida.

### 3. Local, com Maven Wrapper (não precisa de Maven instalado)

```bash
docker compose up -d mysql   # só o banco
cd octopus-msusuario
./mvnw spring-boot:run       # http://localhost:8080
./mvnw test                  # testes com H2 em memória, não dependem do .env
```

### 4. Usando a API

- Swagger: <http://localhost:8080/swagger-ui.html>
- Login em `POST /api/auth/login`, com e-mail e senha, devolve o token JWT. No Swagger, use o botão **Authorize** (`bearerAuth`).
- Na primeira subida é criado um admin com `ADMIN_EMAIL` e `ADMIN_SENHA` do `.env`.

| Endpoint | O que faz | Quem pode |
| :------- | :-------- | :-------- |
| `POST /api/auth/login` | Troca e-mail e senha pelo token JWT | qualquer perfil, menos tutor |
| `POST /api/veterinarios` | Cadastra veterinário, com CRMV único por UF | `ADMIN` |
| `POST /api/auxiliares`, `/api/recepcionistas`, `/api/admins` | Cadastra os demais perfis | `ADMIN` |
| `PATCH /api/<perfil>/{id}/desativar` | Arquiva a conta e derruba os tokens dela | `ADMIN` |
| `POST /api/tutores` | Cadastra tutor, opcionalmente já com os animais | `ADMIN`, `RECEPCIONISTA` |
| `POST /api/tutores/{id}/animais` | Cadastra animal (nome, espécie, data da última antirrábica e, opcionalmente, a mãe) sob o tutor | `ADMIN`, `RECEPCIONISTA` |
| `GET /api/animais`, `/api/tutores` | Lista animais e tutores | `ADMIN`, `RECEPCIONISTA` |
| `GET /api/animais/{id}` | Busca um animal (usado pelo `ms-internacao` nas RN-01 e RN-02) | `ADMIN`, `RECEPCIONISTA`, `VETERINARIO` |

Nada é apagado no sistema: o que sai de uso é desativado (`ativo = false`) e continua no banco, para preservar
o histórico.

## Testes

```bash
cd octopus-msusuario && ./mvnw test
```

- Unitários com JUnit 5 e Mockito para os services (login, cadastro de usuário e veterinário, tutores, animais)
  e para o `JwtService`.
- `AutenticacaoEAutorizacaoTest` sobe a aplicação inteira com H2 e MockMvc e cobre login, 401/403 em JSON,
  `@PreAuthorize` por perfil, e-mail duplicado (409), desativação derrubando tokens já emitidos e o cadastro de
  tutor com animais.
- O GitHub Actions roda a suíte a cada push na `main` e em pull requests.

## Convenções

- Uma branch por microsserviço, permanente. Os módulos não se mesclam.
- Cada microsserviço na sua pasta, com `pom.xml`, `mvnw`, `Dockerfile` e compose próprios.
- Pacote base `com.br.octopus_ms<area>`, entidades em português conforme o TAP, tabelas `tb_<plural>`, id `Long` (só usuários e perfis usam `UUID`).
- Schema por migrations Flyway em `src/main/resources/db/migration/V<n>__<descricao>.sql`. O Hibernate roda em
  `validate` e nunca cria tabela. Migration aplicada não se edita: mudança de tabela é uma nova versão.
- Arquitetura e convenções para novos módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 21/11/2026 na branch `fix/antirrabica-na-aplicacao-usuario` (para a `main`)_
- `PATCH /api/animais/{id}/antirrabica` (corpo `{"data": "aaaa-MM-dd"}`): grava a data da última antirrábica do
  animal. É chamado pelo octopus-msdoses quando o auxiliar aplica a vacina, com o token dele, então aceita
  `ADMIN`, `RECEPCIONISTA`, `VETERINARIO` e `AUXILIAR`. Só mexe nessa data e só para a frente: uma data mais antiga
  que a do cadastro é ignorada. Data futura ou ausente recebe 400; animal inexistente, 404.
- O auxiliar continua sem poder editar o resto do animal (`PUT /api/animais/{id}` segue só admin e recepção).
- 4 testes novos (118 no módulo). Versão `0.0.9-SNAPSHOT` para `0.0.10-SNAPSHOT`.

## Próximos passos
- [ ] Publicar antes do msdoses, que passa a chamar este endpoint ao aplicar a antirrábica

