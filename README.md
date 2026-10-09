# Octopus — Sistema Plantão (VidaPet)

Projeto acadêmico da disciplina **Análise e Projeto de Sistemas II**. O **Plantão** é um sistema de gestão de
plantões e internações para a clínica veterinária *VidaPet*: um painel de medicação que mostra, a qualquer hora,
o que está atrasado, o que vence na próxima hora e o que foi perdido.

O escopo completo, regras de negócio (RN-01..RN-08), squad e cronograma das 5 sprints estão no [`TAP.md`](TAP.md).

## Microsserviços

O sistema é dividido em microsserviços Spring Boot independentes, um por área, cada um em sua própria pasta e
na sua própria branch. Esta branch contém o de internação:

| Módulo           | Área                                        | Branch               | Porta |
| :--------------- | :------------------------------------------ | :------------------- | :---- |
| `ms-internacao/` | Internação: admissão, ciclo de vida, RN-01, RN-02 e RN-08 | `feature/internacao` | 8083  |

Ele depende de dois outros módulos, consultados por REST com o token de quem chamou: `octopus-msusuario`
(animais e emissão do JWT, branch `main`) e `ms-cadastro-baias` (baias, branch `feature/cadastro-baia`).

Stack: **Java 17**, **Spring Boot 4.1.1**, Spring Data JPA, Spring Security + JWT (jjwt), Bean Validation,
Lombok, MySQL 8.4 com **Flyway**, H2 só nos testes, springdoc OpenAPI.

## Como rodar

### 1. Variáveis de ambiente

Dentro de `ms-internacao/`, copie `.env.example` para `.env` e preencha. Não há valores default no código: sem
`.env` a aplicação não sobe.

- `JWT_SECRET`: o mesmo do `octopus-msusuario`, que é quem emite o token.
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: para desenvolver, o MySQL do compose do módulo
  (`jdbc:mysql://localhost:3310/<MYSQL_DATABASE>`), nunca o banco publicado.
- `MS_USUARIO_URL` e `MS_BAIAS_URL`: endereços do `msusuario` e do `msbaias`.

### 2. Local, com Maven Wrapper (não precisa de Maven instalado)

```bash
cd ms-internacao
docker compose up -d mysql   # sobe só o banco (porta MYSQL_HOST_PORT, default 3310)
./mvnw spring-boot:run       # sobe em http://localhost:8083; o Flyway cria as tabelas na primeira subida
./mvnw test                  # 50 testes (H2 em memória e config própria, não dependem do .env)
```

No IntelliJ, a configuração de execução precisa de *Working directory* `$MODULE_WORKING_DIR$`, senão o `.env`
não é encontrado.

### 3. Usando a API

- Swagger: <http://localhost:8083/swagger-ui.html>, com o token do `POST /api/auth/login` do `msusuario` no
  botão **Authorize** (`bearerAuth`).

| Endpoint | Quem pode |
| :------- | :-------- |
| `POST /api/internacoes` (animal, baia, motivo) | `RECEPCIONISTA`, `ADMIN` |
| `GET /api/internacoes?status=&baiaId=&animalId=`, `GET /{id}`, `GET /{id}/eventos` | autenticado |
| `PATCH /{id}/iniciar-tratamento`, `/isolar`, `/autorizar-alta` | `VETERINARIO` |
| `PATCH /{id}/alta-a-pedido-do-tutor` (termo), `/encerrar` (saída física) | `RECEPCIONISTA`, `VETERINARIO` |

Nada é apagado: a internação termina em `ENCERRADA` e cada passo fica no histórico (`/eventos`).

## Convenções

- Branch por funcionalidade: `feature/<nome_da_funcionalidade>`, partindo da `main`.
- Cada microsserviço em sua própria pasta na raiz, com `pom.xml` e `mvnw` próprios.
- Pacote base `com.br.octopus_ms<area>`, entidades em português conforme o TAP, tabelas `tb_<plural>`, id `UUID`.
- Schema por migrations Flyway em `src/main/resources/db/migration/V<n>__<descricao>.sql`; o Hibernate roda em
  `validate` e nunca cria tabela. Migration aplicada não se edita — mudança de tabela é uma nova versão.
- Detalhes de arquitetura e convenções para novos módulos: [`CLAUDE.md`](CLAUDE.md).

## Últimas alterações
_Push de 09/10/2026 na branch `feature/internacao`_
- Id da internação e do histórico passa de `UUID` para `Long` (`BIGINT AUTO_INCREMENT`), por decisão da squad:
  migration `V3__internacoes_id_long.sql` recria as duas tabelas (só tinham dados de teste). `animalId`, `baiaId`
  e `maeId` continuam `UUID`.
- Endpoints passam a receber o id numérico (`/api/internacoes/42`); testes ajustados (50).
- Versão do `ms-internacao` em `0.0.2-SNAPSHOT`: este push publica na EC2 a V2 (nome e espécie do animal), o
  filtro por animal e a V3.
- `CLAUDE.md`: convenção de ids `Long` para internação, prescrição e dose; arquitetura do `octopus-msprescricao`
  (RN-03, ligação com a internação) e a decisão de separar as doses da prescrição.

## Próximos passos
- [ ] Levar a `feature/animal_mae` para a `main`: sem o `GET /api/animais/{id}` publicado, a admissão responde
      "Animal não encontrado".
- [ ] Testar a corrida de duas admissões simultâneas contra MySQL (o H2 não reproduz o `FOR UPDATE` do InnoDB).
- [ ] Criar a tela de animais e tutores no front, com o campo "mãe".
