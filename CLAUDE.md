# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com código neste repositório.

> Este arquivo é versionado e vale para toda a squad. Não coloque nele valores de `.env`, senhas ou tokens —
> só nomes de variáveis. A seção "Estado das branches" é um retrato datado: atualize-a ao integrar uma branch.
> Última revisão: 17/09/2026 (início da Sprint 1).

## Visão geral do repositório

Projeto acadêmico da disciplina "Análise e Projeto de Sistemas II": o sistema **Plantão**, para a clínica
veterinária *VidaPet* — um painel de medicação/internação que mostra, a qualquer hora do plantão, o que está
atrasado, o que vence na próxima hora e o que foi perdido. O `TAP.md` na raiz é o Termo de Abertura do Projeto:
documentação de domínio/negócio, não é compilada nem testada, mas define o vocabulário e as regras que o código
deve seguir (nomes de entidades, RN-01..RN-08, cronograma das 5 sprints, squad).

O sistema é construído como **microsserviços Spring Boot independentes, um por área**, cada um em sua própria
pasta na raiz (com `pom.xml`, `mvnw` e `src/` próprios — não há POM agregador). Hoje:

| Módulo                  | Onde está                                  | Estado                                              |
| :---------------------- | :----------------------------------------- | :-------------------------------------------------- |
| `octopus-msusuario/`    | `main`                                     | Funcional: login, perfis, tutores e animais (stub). |
| `ms-cadastro-baias/`    | branch `feature/cadastro-baia`             | Só entidade + repository, sem service/controller.   |
| `octopus-msmedications/`| branch `feature/register_medications`      | Só entidade + repository; não compila ainda.        |

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

## Estado das branches (17/09/2026)

- **`main`** — contém apenas `octopus-msusuario`, `docker-compose.yml`, `.env.example` e `TAP.md`. PR #1
  (`feature/cadastro_login_usuario`) já foi mergeada; essa branch pode ser apagada.
- **`feature/cadastro-baia`** (Douglas) — parte da `main` e adiciona `ms-cadastro-baias/`. Pontos a resolver antes
  do merge: o commit `fe7c49d` remove o diretório `octopus-msusuario` (precisa ser restaurado, senão o merge apaga
  o ms de usuário da `main`) e `.idea/` foi versionado (remover). Para alinhar com as convenções abaixo: pacote
  `com.ocptopus.ms_cadastro_baias` → `com.br.octopus_msbaias` (corrigindo "ocptopus"), entidade `Cadastro` →
  `Baia`, id `int`/`IDENTITY` → `UUID`, tabela `cadastro_baia` → `tb_baias`, enum de `domain.ENUM` → `domain.enums`,
  e avaliar se `spring-boot-starter-amqp` (RabbitMQ) é necessário — o TAP não prevê mensageria.
- **`feature/register_medications`** (Guilherme Bifani) — branch órfã: começou de um commit vazio, sem ancestral
  comum com a `main`. Contém só `.gitignore` e `octopus-msmedications/` (`Medicacao` + `MedicationRepository`).
  Para integrar: `git merge --allow-unrelated-histories` ou rebase/cherry-pick sobre a `main`. Pendências de
  compilação: `Medication.java` declara `class Medicacao` (renomear o arquivo); `MedicationRepository` estende
  `JpaRepository<Medication, Long>` mas retorna `Medicacao`, e falta o import de `Optional`. A versão anterior
  desse trabalho (`ms-medication/`, com `ApplicationDosage`, `SchemeType`, `StatusDosage`) está preservada
  em `refs/backup/register_medication` — ref só na máquina do Guilherme Bifani, não está no remoto.

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

Na raiz, `docker-compose up --build` sobe MySQL 8.4 (porta `MYSQL_HOST_PORT`, default 3307) e o `msusuario`
(porta 8080). Os outros módulos ainda não estão no compose.

### Configuração via `.env`

- Não há valores default de credenciais em nenhum `application.properties`: **tudo vem do `.env` da raiz**
  (`spring.config.import=optional:file:../.env[.properties]`). Sem `.env` a aplicação não sobe local
  (`JWT_SECRET`, `DB_URL`, `ADMIN_*` são obrigatórios). Copie `.env.example` para `.env` e preencha.
- O arquivo precisa se chamar exatamente `.env` (o `.gitignore` cobre `*.env*`; um arquivo chamado só `env`
  não é lido pela aplicação nem ignorado pelo git).
- `JWT_SECRET` é base64 com ≥ 48 bytes (`openssl rand -base64 48`).
- `DB_URL` para dev local: H2 em memória (ex.: `jdbc:h2:mem:octopus;DB_CLOSE_DELAY=-1`, user `sa`); no
  docker-compose o compose sobrescreve com o MySQL do container.
- Os testes usam `src/test/resources/application.properties`, autocontido (H2 + segredo fake), e não dependem do `.env`.

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
- Banco: `ddl-auto=update`, H2 console em `/h2-console` (quando `DB_URL` for H2); MySQL via `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`.
- Swagger em `/swagger-ui.html`; o botão Authorize aceita o token do `/api/auth/login` (esquema `bearerAuth`).
- `Dockerfile` multi-stage (maven → temurin 17 JRE, usuário não-root), usado pelo `docker-compose.yml`.

## Convenções para novos microsserviços

Use `octopus-msusuario` como referência ao criar ou revisar os outros módulos (os dois em branch ainda não seguem tudo isso):

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
- Não versionar `.idea/`, `HELP.md`, `target/` (já estão no `.gitignore` da raiz). Não adicionar dependências
  que o TAP não pede (mensageria, integrações externas).
- **Em aberto:** como os outros módulos vão autenticar. Hoje só o `msusuario` emite e valida JWT; os dois
  módulos em branch já incluem `spring-boot-starter-security` mas sem nenhuma configuração (o que, no Boot,
  bloqueia todas as rotas com senha gerada). Antes de expor controllers neles, decidir com a squad se cada
  módulo valida o token do `msusuario` (mesmo `JWT_SECRET`) ou se fica sem segurança por enquanto.

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
