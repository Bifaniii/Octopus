# Octopus: Sistema Plantão (VidaPet)

![CI](https://github.com/Bifaniii/Octopus/actions/workflows/ci.yml/badge.svg)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, as regras de negócio (RN-01 a RN-08), a squad e o cronograma das 5 sprints estão no [`TAP.md`](TAP.md).

## Microsserviços

Cada microsserviço vive na sua própria branch, com `Dockerfile`, `docker-compose.yml` e `.env.example` próprios,
e sobe sozinho. Esta branch contém o **microsserviço de usuários**, que é o único publicado.

| Módulo                    | Área                               | Porta | Branch                         |
| :------------------------ | :--------------------------------- | :---- | :----------------------------- |
| `octopus-msusuario/`      | Usuários, perfis, tutores, animais | 8080  | `main`                         |
| `ms-cadastro-baias/`      | Baias                              | 8081  | `feature/cadastro-baia`        |
| `octopus-msmedications/`  | Medicamentos                       | 8082  | `feature/register_medications` |

O que liga os três é o `JWT_SECRET`. O `msusuario` emite o token no login e os outros dois apenas validam, sem
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
| `POST /api/tutores/{id}/animais` | Cadastra animal (nome, espécie e data da última antirrábica) sob o tutor | `ADMIN`, `RECEPCIONISTA` |
| `GET /api/animais`, `/api/tutores` | Lista animais e tutores | `ADMIN`, `RECEPCIONISTA` |

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
- Pacote base `com.br.octopus_ms<area>`, entidades em português conforme o TAP, tabelas `tb_<plural>`, id `UUID`.
- Schema por migrations Flyway em `src/main/resources/db/migration/V<n>__<descricao>.sql`. O Hibernate roda em
  `validate` e nunca cria tabela. Migration aplicada não se edita: mudança de tabela é uma nova versão.
- Arquitetura e convenções para novos módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 04/10/2026 na branch `main`_
- `MAIL_DEBUG` liga a conversa SMTP no log (`spring.mail.properties.mail.debug`), sem mexer em código. Serve
  para saber se o servidor de e-mail aceitou a mensagem, que é a pergunta que o log normal não responde:
  quando o envio dá certo, o serviço não registra nada.
- Entrada malformada passa a devolver 400 em vez de 500. Faltavam dois handlers no
  `GlobalExceptionHandler`: `HttpMessageNotReadableException`, que cobre JSON quebrado, data fora do padrão
  ISO e valor inexistente no enum, e `MethodArgumentTypeMismatchException`, para um id na URL que não é UUID.
- 5 testes novos em `EntradaInvalidaTest`, cobrindo esses casos. São 47 no total.

## Próximos passos
- [ ] Melhorar o corpo do e-mail de redefinição, que hoje é texto puro com um UUID solto e tem cara de spam
      para os filtros. Assunto mais específico e um texto com o nome da clínica ajudam na entrega.
- [ ] Completar o `AdminController`: faltam `GET /{id}` e `PATCH /{id}/desativar`, que os outros três perfis já
      têm. Hoje não existe como desativar um admin pela API.
- [ ] Avaliar guardar o código de redefinição com hash em vez de texto puro.
- [ ] Sprint 2: `octopus-msinternacao`, com alocação em baia e as validações RN-01 (capacidade) e RN-02
      (vacinação antirrábica irregular exige isolamento).
- [ ] Apagar a branch remota `feature/cadastro_login_usuario`, que já foi mergeada.
