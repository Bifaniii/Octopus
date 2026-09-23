# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com código neste repositório.

> Este arquivo é versionado e vale para toda a squad. Não coloque nele valores de `.env`, senhas ou tokens,
> só nomes de variáveis. A seção "Estado das branches" é um retrato datado: atualize quando mexer numa branch.
> Última revisão: 24/09/2026 (fim da Sprint 1).

## Visão geral do repositório

Projeto acadêmico da disciplina "Análise e Projeto de Sistemas II": o sistema **Plantão**, para a clínica
veterinária *VidaPet*. É um painel de medicação e internação que mostra, a qualquer hora do plantão, o que está
atrasado, o que vence na próxima hora e o que foi perdido. O `TAP.md` é o Termo de Abertura do Projeto. Não é
compilado nem testado, mas define o vocabulário e as regras que o código segue: nomes de entidades, RN-01 a
RN-08, cronograma das 5 sprints e os papéis da squad.

O sistema é feito de microsserviços Spring Boot independentes, um por área, cada um em sua pasta, com `pom.xml`,
`mvnw` e `src/` próprios. Não existe POM agregador.

| Módulo                  | Porta | Branch                                | Estado                                      |
| :---------------------- | :---- | :------------------------------------ | :------------------------------------------ |
| `octopus-msusuario/`    | 8080  | `main` (em produção na AWS)           | Login/JWT, perfis, tutores, animais (stub).  |
| `ms-cadastro-baias/`    | 8081  | `feature/cadastro-baia`               | CRUD completo de baias.                      |
| `octopus-msmedications/`| 8082  | `feature/register_medications`        | CRUD completo de medicamentos.               |

Cada microsserviço vive na sua branch e fica lá. Não há merge para a `main` nem PR entre módulos, então cada um
precisa ser autocontido: `Dockerfile`, `docker-compose.yml` e `.env.example` dentro da pasta do módulo, subindo
com o seu próprio MySQL.

O que liga os módulos é o `JWT_SECRET`. Só o `msusuario` emite token e os outros validam o dele, como está em
"Segurança entre módulos". Em produção eles podem apontar para o mesmo banco; no desenvolvimento cada compose
sobe um MySQL separado.

### Domínio do produto (conforme TAP.md)

- **Cadastros (Sprint 1, telas 1 a 3):** animais e tutores (espécie, tutor, data da última vacina antirrábica),
  baias (coletiva, isolamento ou ninhada, com capacidade), medicamentos (esquema contínuo ou sintomático,
  interações proibidas). Também a entidade central de internação, ainda sem validações.
- **Internação (Sprint 2):** alocação de animal em baia, com validações de capacidade e de vacinação (RN-01, RN-02).
- **Prescrição e Painel de Doses (Sprint 3):** itens de medicação com intervalos e geração automática de
  horários; painel com doses atrasadas, próximas e perdidas; janela de aplicação de 30 minutos (RN-03 a RN-06).
- **Relatório de Doses (Sprint 4):** doses aplicadas, atrasadas e perdidas, por turno e por auxiliar (RN-07, RN-08).
- **Restrições do TAP:** nada de integrações externas, notificações automáticas (e-mail/push) ou apps mobile;
  login simples baseado em perfis, sem infraestrutura de auth avançada.

## Estado das branches (24/09/2026, fim da Sprint 1)

Branch por microsserviço, permanente. O `CLAUDE.md` é mantido igual em todas; o `README.md` de cada uma descreve
o próprio módulo.

- **`main`**: `octopus-msusuario`, `TAP.md`, `CLAUDE.md`, `README.md`, `docker-compose.yml` e `docs/`. Em `docs/`
  ficam os mapas de processo UML, numa página HTML autocontida gerada a partir de dados JS no próprio arquivo;
  para mapear algo novo, acrescente um objeto ao array `MAPS`. Esta branch está em produção no free-tier da AWS,
  então mudanças de código afetam o ambiente publicado: combine com a squad antes.
- **`feature/cadastro-baia`** (Douglas): `ms-cadastro-baias/` completo, com entidade `Baia`, DTOs, service,
  controller `/api/baias`, exceptions, JWT, migrations e compose próprios. Esta branch não tem o diretório
  `octopus-msusuario/`, removido no commit `fe7c49d`, o que é esperado no modelo de uma branch por microsserviço.
- **`feature/register_medications`** (Guilherme Bifani): `octopus-msmedications/` completo, com entidade
  `Medicacao` (incluindo `TipoEsquema` e interações proibidas), DTOs, service, controller `/api/medicacoes`,
  exceptions, JWT, migrations, compose próprio e 12 testes. É uma branch órfã, sem ancestral comum com a `main`,
  o que também é esperado aqui. A versão antiga desse trabalho (`ms-medication/`, com `ApplicationDosage`,
  `SchemeType`, `StatusDosage`) está em `refs/backup/register_medication`, uma ref que existe só na máquina do
  Guilherme Bifani e não foi para o remoto.

## Comandos

Cada microsserviço tem seu Maven Wrapper; execute a partir da pasta do módulo. Não é necessário Maven instalado.

```bash
./mvnw clean install       # build completo
./mvnw test                 # roda todos os testes
./mvnw test -Dtest=NomeDaClasse            # roda uma classe de teste específica
./mvnw test -Dtest=NomeDaClasse#metodo     # roda um método de teste específico
./mvnw spring-boot:run      # sobe a aplicação localmente
```

Cada módulo tem o próprio `docker-compose.yml`, dentro da pasta dele, que sobe o serviço e um MySQL só dele:

```bash
cd <modulo> && cp .env.example .env   # preencher os valores
docker compose up --build             # aplicação + MySQL
docker compose up -d mysql            # só o banco, para rodar com ./mvnw spring-boot:run
```

Portas de host default: 8080/3307 (usuário), 8081/3308 (baias), 8082/3309 (medicações).

### Configuração via `.env`

- Nenhum `application.properties` tem credencial default: tudo vem do `.env`, lido da pasta do próprio módulo ou
  da raiz do repositório (`spring.config.import=optional:file:./.env[.properties],optional:file:../.env[.properties]`).
  Sem `.env` a aplicação não sobe local. Copie o `.env.example` do módulo para `.env` e preencha.
- O arquivo precisa se chamar exatamente `.env`. O `.gitignore` cobre `*.env*`, e um arquivo chamado só `env`
  não é lido pela aplicação nem ignorado pelo git.
- `JWT_SECRET` é base64 com no mínimo 48 bytes (`openssl rand -base64 48`).
- `DB_URL` para dev local aponta para o MySQL do compose do módulo
  (`jdbc:mysql://localhost:<porta>/<MYSQL_DATABASE>`), com `DB_USERNAME` e `DB_PASSWORD` iguais a `MYSQL_USER` e
  `MYSQL_PASSWORD`. H2 não serve para dev, porque as migrations são SQL de MySQL. Rodando pelo compose, ele
  sobrescreve as variáveis `DB_*` sozinho.
- Os testes usam `src/test/resources/application.properties`, autocontido (H2, `create-drop`, Flyway desligado e
  segredo fake), e não dependem do `.env`.

## Arquitetura do `octopus-msusuario`

- Java 17 e Spring Boot 4.1.1. Atenção: essa versão usa os novos nomes de starters "quebrados", como
  `spring-boot-starter-webmvc`, `spring-boot-h2console` e `spring-boot-starter-webmvc-test`, no lugar dos nomes
  antigos do Boot 2/3 como `spring-boot-starter-web`. Não "corrija" esses artifactIds de volta.
- Pacote base é `com.br.octopus_msusuario`, com underscore. Isso é intencional: o artifactId Maven
  `octopus-msusuario` não é um nome de pacote Java válido, então o pacote foi renomeado. Mantenha essa grafia em
  qualquer classe nova.
- Jackson 3: o Spring Boot 4 usa `tools.jackson.databind.ObjectMapper`, e não `com.fasterxml.jackson...`. O bean
  auto-configurado é o do Jackson 3; importar o Jackson 2 compila, porque vem transitivo via jjwt, mas falha na
  injeção.
- Camadas, em `com.br.octopus_msusuario`: `domain` (entidades e `domain.enums`), `dto.request` e `dto.response`
  (records com Bean Validation; as responses têm factory `from(entity)`), `repository`, `service`, `controller`,
  `exception` (`GlobalExceptionHandler`, `ErroResponse` e as exceções `RecursoNaoEncontrado`, `RecursoDuplicado`
  e `LoginNaoPermitido`), `security` e `config`.
- Usuário e perfil se ligam por composição, não por herança. `Usuario` (`tb_usuarios`: email, senha BCrypt,
  `Role`, `ativo`) é uma entidade concreta, e cada perfil (`Admin`, `Veterinario`, `Auxiliar`, `Recepcionista`)
  é uma entidade própria com `@OneToOne(cascade = ALL) Usuario usuario`. `Tutor` é separado e não tem `Usuario`,
  porque não faz login. `Animal` é um stub mínimo (id, nome, tutor) até existir a tela de Animais.
- `Role` inclui `ROLE_TUTOR`, reservado e sem uso hoje; o `AuthService.login` rejeita essa role explicitamente.
- Segurança: JWT stateless (jjwt 0.12, HS384). O `JwtAuthenticationFilter` valida o token, recarrega o usuário e
  ignora tokens de usuários desativados, então `ativo=false` invalida os tokens já emitidos. A autorização é por
  `@PreAuthorize` no controller: `hasRole('ADMIN')` para cadastro e desativação de veterinário, auxiliar,
  recepcionista e admin; `hasAnyRole('ADMIN','RECEPCIONISTA')` para tutores e animais. Os erros da filter chain
  (401 e 403) são escritos em JSON pelo `SecurityErrorHandlers`, porque não passam pelo `@RestControllerAdvice`.
- Admin inicial: o `config/AdminBootstrap` cria um admin na subida se não existir nenhum `ROLE_ADMIN`, com
  `ADMIN_EMAIL`, `ADMIN_SENHA` e `ADMIN_NOME` do `.env`. Sem isso ninguém consegue cadastrar o primeiro usuário.
- Banco: MySQL via `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, com schema gerido pelo Flyway e `ddl-auto=validate`
  (ver "Migrations"). O `spring-boot-h2console` continua no POM só por causa dos testes.
- É o único módulo que emite JWT, em `/api/auth/login`. Os outros só validam.
- Swagger em `/swagger-ui.html`. O botão Authorize aceita o token do `/api/auth/login`, no esquema `bearerAuth`.
- `Dockerfile` multi-stage (maven, depois temurin 17 JRE, usuário não-root), usado pelo `docker-compose.yml`.

## Convenções para novos microsserviços

Os três módulos existentes já seguem o padrão abaixo; use qualquer um deles como referência ao criar o próximo.

- Pasta na raiz e artifactId `octopus-ms<area>`, groupId `com.br`, pacote base `com.br.octopus_ms<area>` com
  underscore. Spring Boot 4.1.1, Java 17, Lombok e os mesmos starters "quebrados" do Boot 4.
- Nomes de entidades em português, seguindo o TAP: `Baia`, `Medicacao`, `Internacao`, `Prescricao`, `Dose`.
  Tabelas `tb_<plural>` (`tb_baias`, `tb_medicacoes`) e colunas `@Column(name = "snake_case")`.
- Id `UUID` com `GenerationType.UUID`, imports wildcard `jakarta.persistence.*` e `lombok.*`, mais `@Builder`,
  `@NoArgsConstructor` e `@AllArgsConstructor`. Enums em `domain.enums`, em minúsculo.
- Mesma estrutura de camadas (`domain`, `dto.request`, `dto.response`, `repository`, `service`, `controller`,
  `exception`) e o mesmo par `GlobalExceptionHandler` e `ErroResponse`.
- Credenciais e URLs de banco só via `.env`, com `src/test/resources/application.properties` autocontido para os
  testes.
- Schema por Flyway (`spring-boot-starter-flyway`, `flyway-mysql`, `ddl-auto=validate` e Flyway desligado nos
  testes), como está em "Migrations".
- Não versionar `.idea/`, `HELP.md` nem `target/`, que já estão no `.gitignore`. Não adicionar dependências que o
  TAP não pede, como mensageria ou integrações externas.
- Validar o token do `msusuario` com o mesmo `JWT_SECRET`, como está em "Segurança entre módulos".
- Porta própria via `server.port=${PORT:<porta>}`, `Dockerfile` multi-stage e `docker-compose.yml` com
  `.env.example` dentro da pasta do módulo, usando portas de host que não colidam com as dos outros.

## Segurança entre módulos

Decidido na Sprint 1 e implementado em `ms-cadastro-baias` e `octopus-msmedications`: só o `octopus-msusuario`
emite token, e os demais módulos apenas validam o JWT dele, com o mesmo `JWT_SECRET` (HS384). Nenhum outro
módulo tem tabela de usuários, então identidade e papel vêm dos claims: `sub` é o e-mail e `role` é a `ROLE_*`.

Cada módulo novo copia de `ms-cadastro-baias` ou `octopus-msmedications` quatro classes no pacote `security`:

- `JwtService` valida a assinatura e devolve `DadosToken(email, role)`. Não gera token.
- `JwtAuthenticationFilter` lê o header `Authorization: Bearer` e autentica no contexto do Spring.
- `SecurityConfig` deixa a aplicação stateless, com CSRF desligado, Swagger liberado,
  `anyRequest().authenticated()` e `@EnableMethodSecurity`.
- `SecurityErrorHandlers` escreve 401 e 403 como `ErroResponse` JSON, já que a filter chain não passa pelo
  `@RestControllerAdvice`.

Nas properties basta `app.security.jwt.secret=${JWT_SECRET}`, sem `expiration-ms`, que é assunto de quem emite.
A autorização fina continua por `@PreAuthorize` no controller:

| Módulo          | Leitura            | Escrita                              |
| :-------------- | :----------------- | :----------------------------------- |
| `msusuario`     | conforme o recurso | `ADMIN` (usuários), `ADMIN`/`RECEPCIONISTA` (tutor, animal) |
| `msbaias`       | autenticado        | `ADMIN`                              |
| `msmedications` | autenticado        | `ADMIN`/`VETERINARIO`; desativar só `ADMIN` |

Desativar um usuário no `msusuario` não corta na hora o acesso dele aos outros módulos: como eles não consultam
a tabela de usuários, o token continua válido até expirar. Isso é aceitável para o escopo do TAP; se virar
problema, a saída é reduzir `JWT_EXPIRATION_MS`.

## Nada é apagado

O que sai de uso é arquivado, para preservar o histórico de quem aplicou qual dose, em qual baia, com qual
medicamento. Toda entidade que pode sair de circulação tem um campo `ativo` com default `true` e um endpoint
`PATCH /api/<recurso>/{id}/desativar`, que devolve o recurso atualizado.

- Não crie `@DeleteMapping` nem chame `repository.delete(...)`.
- No `msusuario` o `ativo` mora no `Usuario`, compartilhado com o perfil. Nos demais módulos é uma coluna da
  própria entidade.
- Desativar um usuário no `msusuario` derruba os tokens dele naquele módulo. Nos outros, o token vale até
  expirar, como está em "Segurança entre módulos".
- O `GET` de listagem devolve ativos e inativos, com o campo `ativo` na resposta. Quem consome decide o que
  mostrar.

## Migrations (Flyway)

O schema de cada microsserviço é criado por migrations SQL em `src/main/resources/db/migration/`, nunca pelo
Hibernate. A configuração já está feita nos três módulos:

- POM: `spring-boot-starter-flyway` e `org.flywaydb:flyway-mysql` (runtime). As versões vêm do parent do Boot.
- `application.properties`: `spring.jpa.hibernate.ddl-auto=validate` e `spring.flyway.enabled=true`. Com
  `validate`, o Hibernate confere na subida se cada entidade bate com a tabela e derruba a aplicação apontando a
  coluna errada. Ele não cria nem altera nada.
- Como os módulos podem dividir o mesmo MySQL em produção, cada um precisa da sua tabela de histórico e de
  baseline, senão o Flyway se recusa a rodar num schema que já tem tabelas de outro módulo:
  `spring.flyway.table=flyway_schema_history_<modulo>`, `spring.flyway.baseline-on-migrate=true` e
  `spring.flyway.baseline-version=0`. A versão de baseline precisa ser zero, porque o default 1 faria o Flyway
  pular a `V1`. O `msusuario`, que foi o primeiro, usa a tabela padrão `flyway_schema_history`.
- `src/test/resources/application.properties`: `spring.flyway.enabled=false` e `ddl-auto=create-drop`, no H2.

Regras de escrita das migrations:

- Nome do arquivo: `V<n>__<descricao>.sql`, com dois underscores. Por exemplo `V1__usuarios.sql` e
  `V2__perfis.sql`.
- Migration já aplicada é imutável. O Flyway guarda o checksum na tabela de histórico, e editar um `V<n>` já
  rodado faz a próxima subida falhar. Toda mudança de tabela é uma nova versão com `ALTER TABLE`. Enquanto o
  banco de dev for descartável, `docker compose down -v` zera tudo.
- Uma migration por unidade lógica, uma tabela ou um grupo coeso, com um comentário `--` no topo quando a
  intenção não for evidente.
- Não há herança JPA: a ligação entre perfil e `Usuario` é uma FK 1:1 (`usuario_id BINARY(16) NOT NULL`, mais
  `UNIQUE KEY` e `FOREIGN KEY`). Uma relação `@ManyToOne` vira coluna, índice e `FOREIGN KEY`.
- Mapeamento de Java para MySQL que passa no `validate`:

| Java                               | MySQL                     |
| :--------------------------------- | :------------------------ |
| `UUID`                             | `BINARY(16)`              |
| `String` com `length = N`          | `VARCHAR(N)`; sem `length`, `VARCHAR(255)` |
| enum `@Enumerated(STRING)`         | `VARCHAR(<length>)`       |
| `LocalDate`                        | `DATE`                    |
| `LocalDateTime`                    | `DATETIME(6)`             |
| `boolean`                          | `BOOLEAN`; `TINYINT(1)` gera warning de deprecação |
| `@Column(unique = true)`           | `UNIQUE KEY uk_<tabela>_<coluna> (coluna)` |
| `@UniqueConstraint` composta       | `UNIQUE KEY uk_<tabela>_<nome> (col1, col2)` |

## README.md (obrigatório antes de todo push)

O `README.md` é a página que o GitHub abre no repositório, então ele precisa refletir o último push. Antes de
cada `git push`:

1. Se o `README.md` não existir, crie. Se existir, atualize só o que mudou. As seções estáveis (visão geral,
   como rodar, tabela de microsserviços) não são reescritas do zero.
2. Reescreva a seção "Últimas alterações" com a data, a branch e um resumo em bullets do que este push entrega:
   funcionalidades, endpoints, entidades, configuração. Ela substitui o conteúdo anterior em vez de acumular,
   já que o histórico completo está no `git log`.
3. Reescreva a seção "Próximos passos" com o que vem nos próximos commits: pendências desta feature, o que falta
   para fechar a sprint, dívidas conhecidas. Bullets curtos, verbos no infinitivo.
4. Inclua o `README.md` no mesmo commit da funcionalidade, sem um commit separado só para ele.
5. Se o push deixar a seção "Estado das branches" deste arquivo desatualizada, atualize-a também.

Mantenha os títulos das duas seções exatamente assim, para facilitar a leitura no GitHub:

```markdown
## Últimas alterações
_Push de DD/MM/AAAA na branch `feature/xxx`_
- ...

## Próximos passos
- [ ] ...
```

Esta regra vale para pushes feitos pelo Claude. Se você pushar manualmente, atualize o README por conta.

## Git

- Uma branch por microsserviço, permanente. Nada de merge entre elas nem para a `main`, já que cada módulo é
  autocontido e roda sozinho. Trabalhe sempre na branch do módulo que está mexendo.
- Commit com funcionalidade `feature/<nome_da_funcionalidade>`.
- Antes de todo push, atualizar o `README.md`, como está na seção acima.
- Nunca adicionar trailer `Co-Authored-By` nem qualquer trailer extra.
