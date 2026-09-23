# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com código neste repositório.

> Este arquivo é versionado e vale para toda a squad. Não coloque nele valores de `.env`, senhas ou tokens —
> só nomes de variáveis. A seção "Estado das branches" é um retrato datado: atualize-a ao integrar uma branch.
> Última revisão: 22/09/2026 (fim da Sprint 1).

## Visão geral do repositório

Projeto acadêmico da disciplina "Análise e Projeto de Sistemas II": o sistema **Plantão**, para a clínica
veterinária *VidaPet* — um painel de medicação/internação que mostra, a qualquer hora do plantão, o que está
atrasado, o que vence na próxima hora e o que foi perdido. O `TAP.md` na raiz é o Termo de Abertura do Projeto:
documentação de domínio/negócio, não é compilada nem testada, mas define o vocabulário e as regras que o código
deve seguir (nomes de entidades, RN-01..RN-08, cronograma das 5 sprints, squad).

O sistema é construído como **microsserviços Spring Boot independentes, um por área**, cada um em sua própria
pasta na raiz (com `pom.xml`, `mvnw` e `src/` próprios — não há POM agregador). Hoje:

| Módulo                  | Porta | Onde está                             | Estado                                     |
| :---------------------- | :---- | :------------------------------------ | :----------------------------------------- |
| `octopus-msusuario/`    | 8080  | `main` (em produção na AWS)           | Login/JWT, perfis, tutores, animais (stub). |
| `ms-cadastro-baias/`    | 8081  | branch `feature/cadastro-baia`        | CRUD completo de baias, pronto para merge.  |
| `octopus-msmedications/`| 8082  | branch `feature/register_medications` | CRUD completo de medicamentos, pronto para merge. |

Os três compartilham o **mesmo banco MySQL** e o **mesmo `JWT_SECRET`**: só o `msusuario` emite token; os
outros dois validam o token dele (ver "Segurança entre módulos").

### Domínio do produto (conforme TAP.md)

- **Cadastros (Sprint 1, telas 1–3):** animais e tutores (espécie, tutor, data da última vacina antirrábica),
  baias (coletiva, isolamento ou ninhada, com capacidade), medicamentos (esquema contínuo ou sintomático,
  interações proibidas). Também a entidade central de internação, ainda sem validações.
- **Internação (Sprint 2):** alocação de animal em baia, com validações de capacidade e de vacinação (RN-01, RN-02).
- **Prescrição e Painel de Doses (Sprint 3):** itens de medicação com intervalos e geração automática de
  horários; painel com doses atrasadas, próximas e perdidas; janela de aplicação de 30 minutos (RN-03..RN-06).
- **Relatório de Doses (Sprint 4):** doses aplicadas/atrasadas/perdidas, por turno e por auxiliar (RN-07, RN-08).
- **Restrições explícitas do TAP:** nada de integrações externas, notificações automáticas (e-mail/push) ou apps
  mobile; login simples baseado em perfis (sem infraestrutura de auth avançada).

## Estado das branches (22/09/2026 — fim da Sprint 1)

Os três módulos da Sprint 1 estão prontos, cada um na sua branch. `CLAUDE.md` é idêntico nas branches de
feature; ao mergear, fique com ele e reescreva as seções "Últimas alterações"/"Próximos passos" do README.

- **`main`** — `octopus-msusuario`, `docker-compose.yml`, `.env.example`, `TAP.md`, `CLAUDE.md`, `README.md` e
  `docs/mapas-processo.html` (mapas de processo UML; página HTML autocontida, gerada a partir de dados JS no
  próprio arquivo — ao mapear uma funcionalidade nova, acrescente um objeto ao array `MAPS`).
  **Está em produção no free-tier da AWS**: mudanças aqui afetam o ambiente publicado, então nada de push
  direto — abra PR e combine com a squad. O `CLAUDE.md`/`README.md` da `main` estão mais antigos que os das
  branches de feature; serão atualizados no merge da Sprint 1.
- **`feature/cadastro-baia`** (Douglas) — `ms-cadastro-baias/` completo: entidade `Baia`, DTOs, service,
  controller `/api/baias`, exceptions, JWT, `V1__baias.sql`, Dockerfile e entrada no `docker-compose.yml`.
  **Pendência de merge:** o commit `fe7c49d` (de 14/09) removeu o diretório `octopus-msusuario` nesta branch;
  antes de mergear é preciso restaurá-lo (`git checkout main -- octopus-msusuario/`), senão o merge apaga o ms
  de usuário da `main`.
- **`feature/register_medications`** (Guilherme Bifani) — `octopus-msmedications/` completo: entidade
  `Medicacao` com `TipoEsquema` e interações proibidas, DTOs, service, controller `/api/medicacoes`,
  exceptions, JWT, `V1__medicacoes.sql`, Dockerfile e 11 testes. **Pendência de merge:** a branch é órfã (sem
  ancestral comum com a `main`), então a integração exige `git merge --allow-unrelated-histories` ou
  cherry-pick; ela também não tem `TAP.md`, `.env.example` nem `docker-compose.yml` (vêm da `main` no merge) e
  o serviço ainda precisa ser acrescentado ao compose. A versão antiga (`ms-medication/`, com
  `ApplicationDosage`, `SchemeType`, `StatusDosage`) está em `refs/backup/register_medication` — ref só na
  máquina do Guilherme Bifani, não está no remoto.

## Comandos

Cada microsserviço tem seu próprio Maven Wrapper; execute a partir da pasta do módulo (`octopus-msusuario/`,
`ms-cadastro-baias/`, `octopus-msmedications/`). Não é necessário Maven instalado.

```bash
./mvnw clean install       # build completo
./mvnw test                 # roda todos os testes
./mvnw test -Dtest=NomeDaClasse            # roda uma classe de teste específica
./mvnw test -Dtest=NomeDaClasse#metodo     # roda um método de teste específico
./mvnw spring-boot:run      # sobe a aplicação localmente
```

Na raiz, `docker compose up --build` sobe MySQL 8.4 (porta `MYSQL_HOST_PORT`, default 3307) e o `msusuario`
(porta 8080). `docker compose up -d mysql` sobe só o banco, para rodar a aplicação com `./mvnw spring-boot:run`.
Os outros módulos ainda não estão no compose.

### Configuração via `.env`

- Não há valores default de credenciais em nenhum `application.properties`: **tudo vem do `.env` da raiz**
  (`spring.config.import=optional:file:../.env[.properties]`). Sem `.env` a aplicação não sobe local
  (`JWT_SECRET`, `DB_URL`, `ADMIN_*` são obrigatórios). Copie `.env.example` para `.env` e preencha.
- O arquivo precisa se chamar exatamente `.env` (o `.gitignore` cobre `*.env*`; um arquivo chamado só `env`
  não é lido pela aplicação nem ignorado pelo git).
- `JWT_SECRET` é base64 com ≥ 48 bytes (`openssl rand -base64 48`).
- `DB_URL` para dev local aponta para o MySQL do compose: `jdbc:mysql://localhost:3307/<MYSQL_DATABASE>`, com
  `DB_USERNAME`/`DB_PASSWORD` iguais a `MYSQL_USER`/`MYSQL_PASSWORD`. H2 **não serve mais para dev** (as
  migrations são SQL de MySQL); rodando via docker-compose, o compose sobrescreve `DB_*` sozinho.
- Os testes usam `src/test/resources/application.properties`, autocontido (H2 + `create-drop` + Flyway
  desligado + segredo fake), e não dependem do `.env`.

## Arquitetura — `octopus-msusuario`

- **Java 17**, **Spring Boot 4.1.1** (atenção: essa versão usa os novos nomes de starters "quebrados", ex.
  `spring-boot-starter-webmvc`, `spring-boot-h2console`, `spring-boot-starter-webmvc-test`, em vez dos nomes
  antigos do Boot 2/3 como `spring-boot-starter-web` — não "corrija" esses artifactIds para os nomes antigos).
- Pacote base é `com.br.octopus_msusuario` (com underscore, não hífen). Isso é intencional: o artifactId Maven
  `octopus-msusuario` não é um nome de pacote Java válido, então o pacote foi renomeado. Mantenha essa grafia
  em qualquer classe nova.
- **Jackson 3**: Spring Boot 4 usa `tools.jackson.databind.ObjectMapper`, não `com.fasterxml.jackson...`. O bean
  auto-configurado é o do Jackson 3; importar o Jackson 2 compila (vem transitivo via jjwt) mas falha na injeção.
- Camadas (pacotes em `com.br.octopus_msusuario`): `domain` (entidades + `domain.enums`), `dto.request` /
  `dto.response` (records com Bean Validation; responses têm factory `from(entity)`), `repository`, `service`,
  `controller`, `exception` (`GlobalExceptionHandler` + `ErroResponse` + exceções `RecursoNaoEncontrado`,
  `RecursoDuplicado`, `LoginNaoPermitido`), `security`, `config`.
- **Modelo Usuario ↔ perfil por composição**: `Usuario` (`tb_usuarios`: email, senha BCrypt, `Role`, `ativo`) é
  uma entidade concreta. Cada perfil (`Admin`, `Veterinario`, `Auxiliar`, `Recepcionista`) é uma entidade própria
  com `@OneToOne(cascade = ALL) Usuario usuario`. Não há herança JPA. `Tutor` é separado, **sem** `Usuario` (não
  faz login); `Animal` é um stub mínimo (id, nome, tutor) até existir a tela de Animais.
- `Role` inclui `ROLE_TUTOR` (reservado, sem uso hoje); `AuthService.login` rejeita essa role explicitamente.
- **Segurança**: JWT stateless (jjwt 0.12, HS384). `JwtAuthenticationFilter` valida o token, recarrega o usuário e
  ignora tokens de usuários desativados — desativar (`ativo=false`) invalida tokens já emitidos. Autorização é por
  `@PreAuthorize` no controller: `hasRole('ADMIN')` para cadastro/desativação de veterinário, auxiliar,
  recepcionista e admin; `hasAnyRole('ADMIN','RECEPCIONISTA')` para tutores e animais. Erros da filter chain
  (401/403) são escritos em JSON por `SecurityErrorHandlers`, porque não passam pelo `@RestControllerAdvice`.
- **Admin inicial**: `config/AdminBootstrap` cria um admin na subida se não existir nenhum `ROLE_ADMIN`, com
  `ADMIN_EMAIL` / `ADMIN_SENHA` / `ADMIN_NOME` do `.env` (não há mais default no código). Sem isso ninguém
  consegue cadastrar o primeiro usuário.
- Banco: MySQL via `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`; schema gerido pelo Flyway com `ddl-auto=validate`
  (ver "Migrations"). O `spring-boot-h2console` continua no POM só por causa dos testes.
- É o **único módulo que emite JWT** (`/api/auth/login`); os outros só validam (ver "Segurança entre módulos").
- Swagger em `/swagger-ui.html`; o botão Authorize aceita o token do `/api/auth/login` (esquema `bearerAuth`).
- `Dockerfile` multi-stage (maven → temurin 17 JRE, usuário não-root), usado pelo `docker-compose.yml`.

## Convenções para novos microsserviços

Os três módulos existentes já seguem o padrão abaixo; use qualquer um deles como referência ao criar o próximo:

- Pasta na raiz e artifactId `octopus-ms<area>`; groupId `com.br`; pacote base `com.br.octopus_ms<area>`
  (underscore). Spring Boot 4.1.1 / Java 17 / Lombok, mesmos starters "quebrados" do Boot 4.
- Nomes de entidades em português, seguindo o TAP: `Baia`, `Medicamento`/`Medicacao`, `Internacao`,
  `Prescricao`, `Dose`... Tabelas `tb_<plural>` (`tb_baias`, `tb_medicacoes`), colunas `@Column(name = "snake_case")`.
- Id `UUID` com `GenerationType.UUID`; imports wildcard `jakarta.persistence.*` / `lombok.*`; `@Builder`,
  `@NoArgsConstructor`, `@AllArgsConstructor`. Enums em `domain.enums` (minúsculo).
- Mesma estrutura de camadas (`domain`, `dto.request/response` como records, `repository`, `service`,
  `controller`, `exception`) e o mesmo padrão de `GlobalExceptionHandler` + `ErroResponse`.
- Credenciais e URLs de banco só via `.env` (`spring.config.import=optional:file:../.env[.properties]`), com
  `src/test/resources/application.properties` autocontido para os testes.
- Schema por Flyway (`spring-boot-starter-flyway` + `flyway-mysql`, `ddl-auto=validate`, Flyway desligado nos
  testes), exatamente como no `msusuario` — ver "Migrations".
- Não versionar `.idea/`, `HELP.md`, `target/` (já estão no `.gitignore` da raiz). Não adicionar dependências
  que o TAP não pede (mensageria, integrações externas).
- Segurança: validar o token do `msusuario` com o mesmo `JWT_SECRET` (ver "Segurança entre módulos").
- Porta própria via `server.port=${PORT:<porta>}` (8080 usuário, 8081 baias, 8082 medicações), `Dockerfile`
  multi-stage igual ao do `msusuario` e entrada no `docker-compose.yml`.

## Segurança entre módulos

Decidido na Sprint 1 e implementado em `ms-cadastro-baias` e `octopus-msmedications`: **só o `octopus-msusuario`
emite token**; os demais módulos apenas validam o JWT dele, usando o mesmo `JWT_SECRET` (HS384). Nenhum outro
módulo tem tabela de usuários — identidade e papel vêm dos claims (`sub` = e-mail, `role` = `ROLE_*`).

Cada módulo novo copia de `ms-cadastro-baias`/`octopus-msmedications` quatro classes no pacote `security`:

- `JwtService` — valida a assinatura e devolve `DadosToken(email, role)`; não gera token.
- `JwtAuthenticationFilter` — lê o header `Authorization: Bearer`, autentica no contexto do Spring.
- `SecurityConfig` — stateless, CSRF off, Swagger liberado, `anyRequest().authenticated()`, `@EnableMethodSecurity`.
- `SecurityErrorHandlers` — escreve 401/403 como `ErroResponse` JSON (a filter chain não passa pelo `@RestControllerAdvice`).

Só `app.security.jwt.secret=${JWT_SECRET}` é necessário nas properties — sem `expiration-ms`, que é assunto de
quem emite. A autorização fina continua por `@PreAuthorize` no controller:

| Módulo        | Leitura                | Escrita                              |
| :------------ | :--------------------- | :----------------------------------- |
| `msusuario`   | conforme o recurso     | `ADMIN` (usuários), `ADMIN`/`RECEPCIONISTA` (tutor, animal) |
| `msbaias`     | autenticado            | `ADMIN`                              |
| `msmedications` | autenticado          | `ADMIN`/`VETERINARIO`; remover só `ADMIN` |

Desativar um usuário no `msusuario` **não** invalida na hora o acesso aos outros módulos: eles não consultam a
tabela de usuários, então o token continua válido até expirar. É aceitável para o escopo do TAP; se virar
problema, a saída é reduzir `JWT_EXPIRATION_MS`.

## Migrations (Flyway)

O schema de cada microsserviço é criado por migrations SQL em `src/main/resources/db/migration/`, nunca pelo
Hibernate. Configuração (já feita no `msusuario`, replicar nos outros):

- POM: `spring-boot-starter-flyway` + `org.flywaydb:flyway-mysql` (runtime). Versões vêm do parent do Boot.
- `application.properties`: `spring.jpa.hibernate.ddl-auto=validate` e `spring.flyway.enabled=true`. Com
  `validate`, o Hibernate confere na subida se cada entidade bate com a tabela e derruba a aplicação apontando a
  coluna errada — nunca cria nem altera nada.
- **Banco compartilhado:** os módulos dividem o mesmo MySQL, então cada um precisa da sua própria tabela de
  histórico e de baseline, senão o Flyway se recusa a rodar num schema que já tem tabelas de outro módulo:
  `spring.flyway.table=flyway_schema_history_<modulo>`, `spring.flyway.baseline-on-migrate=true` e
  `spring.flyway.baseline-version=0` (o padrão 1 faria o Flyway pular a `V1`). O `msusuario`, que foi o
  primeiro, usa a tabela padrão `flyway_schema_history`.
- `src/test/resources/application.properties`: `spring.flyway.enabled=false` + `ddl-auto=create-drop` (H2).

Regras:

- Nome do arquivo: `V<n>__<descricao>.sql` (**dois underscores**), ex. `V1__usuarios.sql`, `V2__perfis.sql`.
- **Migration já aplicada é imutável.** O Flyway guarda o checksum em `flyway_schema_history`; editar um `V<n>`
  já rodado faz a próxima subida falhar. Toda mudança de tabela é uma nova versão com `ALTER TABLE`. Enquanto o
  banco de dev é descartável, `docker compose down -v` zera tudo (necessário também se o volume ainda tiver
  tabelas criadas pelo antigo `ddl-auto=update`).
- Uma migration por unidade lógica (uma tabela ou um grupo coeso), com comentário `--` no topo explicando.
- Não há herança JPA: perfil ↔ `Usuario` é uma FK 1:1 (`usuario_id BINARY(16) NOT NULL` + `UNIQUE KEY` +
  `FOREIGN KEY`). Relação `@ManyToOne` vira coluna + `KEY` (índice) + `FOREIGN KEY`.
- Mapeamento Java → MySQL que passa no `validate`:

| Java                               | MySQL                     |
| :--------------------------------- | :------------------------ |
| `UUID`                             | `BINARY(16)`              |
| `String` com `length = N`          | `VARCHAR(N)` (sem length → `VARCHAR(255)`) |
| enum `@Enumerated(STRING)`         | `VARCHAR(<length>)`       |
| `LocalDate`                        | `DATE`                    |
| `LocalDateTime`                    | `DATETIME(6)`             |
| `boolean`                          | `BOOLEAN` (não `TINYINT(1)`, gera warning de deprecação) |
| `@Column(unique = true)`           | `UNIQUE KEY uk_<tabela>_<coluna> (coluna)` |
| `@UniqueConstraint` composta       | `UNIQUE KEY uk_<tabela>_<nome> (col1, col2)` |

## README.md (obrigatório antes de todo push)

O `README.md` da raiz é a "cara" do projeto no GitHub e precisa refletir o último push. **Sempre que for fazer
`git push`, antes de enviar:**

1. Se o `README.md` não existir, crie-o. Se existir, atualize — nunca reescreva do zero as seções estáveis
   (visão geral, como rodar, tabela de microsserviços); só corrija o que mudou.
2. Reescreva a seção **"Últimas alterações"** com: data, branch, e um resumo em bullets do que este push
   entrega (funcionalidades, endpoints, entidades, configs). Substitui o conteúdo anterior, não acumula —
   o histórico completo está no `git log`.
3. Reescreva a seção **"Próximos passos"** com o que vem a seguir nos próximos commits (pendências desta
   feature, o que falta para fechar a sprint, dívidas conhecidas). Bullets curtos, verbos no infinitivo.
4. Inclua o `README.md` **no mesmo commit** da funcionalidade (não faça um commit separado só para ele).
5. Se este `CLAUDE.md` tiver a seção "Estado das branches" desatualizada pelo push, atualize-a também.

Formato fixo das duas seções (mantenha os títulos exatos para facilitar a leitura no GitHub):

```markdown
## Últimas alterações
_Push de DD/MM/AAAA — branch `feature/xxx`_
- ...

## Próximos passos
- [ ] ...
```

Esta regra só se aplica a pushes feitos pelo Claude; se você pushar manualmente, atualize o README por conta.

## Git

- Branch por funcionalidade: `feature/<nome_da_funcionalidade>`. Commit com funcionalidade `feature/<nome_da_funcionalidade>`.
- Antes de todo push, atualizar o `README.md` (ver seção acima).
- Toda branch de feature deve partir da `main` atual (nada de branch órfã) e **não** deve apagar módulos de
  outras pessoas; cada microsserviço vive na sua própria pasta.
- **Nunca** adicionar trailer `Co-Authored-By` nem qualquer trailer extra.
