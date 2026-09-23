# Octopus — Sistema Plantão (VidaPet)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, regras de negócio (RN-01..RN-08), squad e cronograma das 5 sprints estão no [`TAP.md`](TAP.md).

## Microsserviços

**Cada microsserviço vive na sua própria branch**, com `Dockerfile`, `docker-compose.yml` e `.env.example`
próprios — sobe sozinho, sem depender dos outros. Esta branch (`main`) contém o **microsserviço de usuários**,
que é o único publicado.

| Módulo                    | Área                               | Porta | Branch                         |
| :------------------------ | :--------------------------------- | :---- | :----------------------------- |
| `octopus-msusuario/`      | Usuários, perfis, tutores, animais | 8080  | `main`                         |
| `ms-cadastro-baias/`      | Baias                              | 8081  | `feature/cadastro-baia`        |
| `octopus-msmedications/`  | Medicamentos                       | 8082  | `feature/register_medications` |

O que liga os três é o `JWT_SECRET`: o `msusuario` emite o token no login e os outros dois apenas validam,
sem tabela de usuários própria.

Stack: **Java 17**, **Spring Boot 4.1.1**, Spring Data JPA, Spring Security + JWT (jjwt), Bean Validation,
Lombok, MySQL 8.4 com **Flyway**, H2 só nos testes, springdoc OpenAPI.

## Documentação

- [Panorama da Sprint 1](https://claude.ai/artifact/AZfbHYkBdQNs2ZBWSrMZ8U) — o que está funcionando, como o
  login e o banco se encaixam, endpoints por perfil e o que falta publicar.
- [Mapas de processo](https://claude.ai/artifact/WyTGNbR9fVCytDNhxibdDq) — diagramas de atividade (UML, com
  raias) das funcionalidades prontas. Cópia versionada em [`docs/mapas-processo.html`](docs/mapas-processo.html).
- [`TAP.md`](TAP.md) — Termo de Abertura do Projeto.
- [`CLAUDE.md`](CLAUDE.md) — arquitetura, convenções e decisões técnicas.

## O que a Sprint 1 entrega (telas 1 a 3 do TAP)

| Tela do TAP | Onde | Endpoints |
| :---------- | :--- | :-------- |
| Login e perfis (base de tudo) | `msusuario` | `POST /api/auth/login`, `/api/admins`, `/api/veterinarios`, `/api/auxiliares`, `/api/recepcionistas` |
| 1 · Animais e tutores | `msusuario` | `/api/tutores`, `/api/tutores/{id}/animais`, `/api/animais` |
| 2 · Baias | `msbaias` | `/api/baias` — tipo (coletiva, isolamento, ninhada) e capacidade |
| 3 · Medicamentos | `msmedications` | `/api/medicacoes` — esquema (contínuo/sintomático) e interações proibidas |

Internação, prescrição, painel de doses e relatório são das Sprints 2 a 4.

## Como rodar

Nesta branch o compose e o `.env` ficam na raiz do repositório; os comandos abaixo saem de lá, menos onde
estiver indicado.

### 1. Variáveis de ambiente

```bash
cp .env.example .env
openssl rand -base64 48   # use a saída como JWT_SECRET
```

Não há valores default no código — sem `.env` a aplicação não sobe. O mesmo `JWT_SECRET` precisa ser usado
pelos outros módulos, senão o token emitido aqui é rejeitado lá.

### 2. Tudo pelo Docker Compose

```bash
docker compose up --build
```

Sobe o MySQL 8.4 e a aplicação; o compose injeta `DB_*` sozinho. As tabelas são criadas na primeira subida
pelo Flyway.

### 3. Local, com Maven Wrapper (não precisa de Maven instalado)

```bash
docker compose up -d mysql   # só o banco
cd octopus-msusuario
./mvnw spring-boot:run       # http://localhost:8080
./mvnw test                  # testes com H2 em memória, não dependem do .env
```

### 4. Usando a API

- Swagger: <http://localhost:8080/swagger-ui.html>
- Login: `POST /api/auth/login` com e-mail e senha → token JWT. No Swagger, use o botão **Authorize** (`bearerAuth`).
- Na primeira subida é criado um admin com `ADMIN_EMAIL` / `ADMIN_SENHA` do `.env`.

| Endpoint | O que faz | Quem pode |
| :------- | :-------- | :-------- |
| `POST /api/auth/login` | Troca e-mail e senha pelo token JWT | qualquer perfil, menos tutor |
| `POST /api/veterinarios` | Cadastra veterinário (CRMV único por UF) | `ADMIN` |
| `POST /api/auxiliares`, `/api/recepcionistas`, `/api/admins` | Cadastra os demais perfis | `ADMIN` |
| `PATCH /api/<perfil>/{id}/desativar` | Arquiva a conta e derruba os tokens dela | `ADMIN` |
| `POST /api/tutores` | Cadastra tutor | `ADMIN`, `RECEPCIONISTA` |
| `POST /api/tutores/{id}/animais` | Cadastra animal sob o tutor | `ADMIN`, `RECEPCIONISTA` |
| `GET /api/animais`, `/api/tutores` | Lista animais e tutores | `ADMIN`, `RECEPCIONISTA` |

O sistema **não apaga registros**: o que sai de uso é desativado (`ativo = false`) e continua no banco, para
preservar o histórico.

## Convenções

- Uma branch por microsserviço, permanente: os módulos não se mesclam.
- Cada microsserviço em sua própria pasta, com `pom.xml`, `mvnw`, `Dockerfile` e compose próprios.
- Pacote base `com.br.octopus_ms<area>`, entidades em português conforme o TAP, tabelas `tb_<plural>`, id `UUID`.
- Schema por migrations Flyway em `src/main/resources/db/migration/V<n>__<descricao>.sql`; o Hibernate roda em
  `validate` e nunca cria tabela. Migration aplicada não se edita — mudança de tabela é uma nova versão.
- Detalhes de arquitetura e convenções para novos módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 24/09/2026 — branch `main`_
- `CLAUDE.md` e `README.md` atualizados para o modelo de **uma branch por microsserviço** (sem merge nem PR
  entre módulos) e para a política de **arquivamento no lugar de exclusão**.
- Seção "Documentação" com o panorama da Sprint 1 e os mapas de processo.
- Nenhuma mudança de código nesta branch: o `octopus-msusuario` publicado na AWS continua como está.

## Próximos passos
- [ ] Publicar `ms-cadastro-baias` e `octopus-msmedications` no mesmo ambiente, com o mesmo `JWT_SECRET`.
- [ ] Decidir o banco de produção: um por módulo ou um só para os três (as duas opções funcionam, cada módulo
      já tem tabela de histórico própria no Flyway).
- [ ] Sprint 2: microsserviço de internação, com alocação em baia e validações RN-01 (capacidade) e RN-02
      (vacinação antirrábica irregular exige isolamento).
- [ ] Completar a entidade `Animal` (espécie e data da última vacina antirrábica) via nova migration.
- [ ] Apagar a branch remota `feature/cadastro_login_usuario` (já mergeada).
