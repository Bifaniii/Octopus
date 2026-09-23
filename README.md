# Octopus: Sistema Plantão (VidaPet)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, as regras de negócio (RN-01 a RN-08), a squad e o cronograma das 5 sprints estão no `TAP.md`, na branch `main`.

## Microsserviços

Cada microsserviço vive na sua própria branch, com `Dockerfile`, `docker-compose.yml` e `.env.example` próprios,
e sobe sozinho. Esta branch contém o **microsserviço de medicamentos**.

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

Os comandos abaixo saem de dentro de `octopus-msmedications/`.

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
./mvnw spring-boot:run       # http://localhost:8082
./mvnw test                  # testes com H2 em memória, não dependem do .env
```

### 4. Usando a API

- Swagger: <http://localhost:8082/swagger-ui.html>
- O token vem do `octopus-msusuario`, em `POST /api/auth/login`. No Swagger, use o botão **Authorize** (`bearerAuth`).
- Leitura: qualquer usuário autenticado. Cadastrar e editar: `ADMIN` ou `VETERINARIO`. Desativar: `ADMIN`.

| Endpoint | O que faz |
| :------- | :-------- |
| `GET /api/medicacoes` | Lista tudo, ou filtra com `?fabricante=` e `?nomeComercial=` |
| `GET /api/medicacoes/{id}` | Detalha o medicamento e suas interações proibidas |
| `POST /api/medicacoes` | Cadastra com tipo de esquema e lista de interações |
| `PATCH /api/medicacoes/{id}` | Altera só os campos enviados |
| `PATCH /api/medicacoes/{id}/desativar` | Arquiva o medicamento |

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
_Push de 24/09/2026 na branch `feature/register_medications`_
- `DELETE /api/medicacoes/{id}` virou `PATCH /api/medicacoes/{id}/desativar`. O medicamento sai de uso
  arquivado, com o campo `ativo` e a migration `V2__medicacoes_ativo.sql`.
- Cadastro completo em `/api/medicacoes`: listar com filtro, buscar, criar, alterar por PATCH e desativar.
- `Medicacao` com `UUID`, tipo de esquema (`CONTINUO` ou `SINTOMATICO`) e interações proibidas simétricas entre
  medicamentos, que são os dois itens que o TAP pede nesta tela.
- Spring Security com JWT validando o token do `octopus-msusuario`, e erros padronizados pelo
  `GlobalExceptionHandler` (400, 401, 403, 404, 409 e 422).
- Flyway com `V1__medicacoes.sql` e tabela de histórico própria. São 12 testes automatizados.
- O módulo ficou autocontido: `docker-compose.yml` e `.env.example` próprios, subindo a aplicação com o seu
  MySQL na porta 3309.

## Próximos passos
- [ ] Publicar este módulo no mesmo ambiente do `octopus-msusuario`, com o mesmo `JWT_SECRET`.
- [ ] Sprint 3: microsserviço de prescrição, que vai consultar este cadastro para validar interações (RN-03).
- [ ] Sprint 3 ou 4: microsserviço de aplicação de doses, separado deste.
- [ ] Decidir se o item de prescrição terá `tipoEsquema` próprio, nascendo com o valor do catálogo.
