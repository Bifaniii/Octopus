# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com código neste repositório.

> Este arquivo é versionado e vale para toda a squad. Não coloque nele valores de `.env`, senhas ou tokens,
> só nomes de variáveis. A seção "Estado das branches" é um retrato datado: atualize quando mexer numa branch.
> Última revisão: 09/10/2026 (Sprint 2 em andamento).

## Visão geral do repositório

Projeto acadêmico da disciplina "Análise e Projeto de Sistemas II": o sistema **Plantão**, para a clínica
veterinária *VidaPet*. É um painel de medicação e internação que mostra, a qualquer hora do plantão, o que está
atrasado, o que vence na próxima hora e o que foi perdido. O `TAP.md` é o Termo de Abertura do Projeto. Não é
compilado nem testado, mas define o vocabulário e as regras que o código segue: nomes de entidades, RN-01 a
RN-08, cronograma das 5 sprints e os papéis da squad.

O sistema é feito de microsserviços Spring Boot independentes, um por área, cada um em sua pasta, com `pom.xml`,
`mvnw` e `src/` próprios. Não existe POM agregador.

| Módulo                  | Porta | Branch                         | Estado                                               |
| :---------------------- | :---- | :----------------------------- | :--------------------------------------------------- |
| `octopus-msusuario/`    | 8080  | `main`                         | Login/JWT, recuperação de senha, perfis, tutores, animais. 42 testes. |
| `ms-cadastro-baias/`    | 8081  | `feature/cadastro-baia`        | CRUD de baias, limite de 12 ativas, ativar/desativar. 14 testes. |
| `octopus-msmedications/`| 8082  | `feature/register_medications` | CRUD de medicamentos, esquema e interações. 12 testes. |
| `ms-internacao/`        | 8083  | `feature/internacao`           | Admissão, ciclo de vida, RN-01, RN-02 e RN-08. 50 testes. |
| `octopus-msprescricao/` | 8084  | `feature/prescricao`           | Prescrição e itens (sem doses), ligada à internação, RN-03. 37 testes. |

Os módulos estão publicados no free tier da AWS (EC2), com o MySQL hospedado no Aiven. O front é um projeto
Angular separado, no repositório `Octopus-front`, que também usa uma branch por funcionalidade e tem o seu
próprio `CLAUDE.md`.

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

O enunciado do professor (PDF "Projeto Octopus") é mais detalhado que o TAP e prevalece quando os dois
divergem. Dele vêm as regras que o código da internação segue:

- **Perfis:** a recepcionista cadastra tutor e animal e registra a internação; o veterinário prescreve e dá
  alta; o auxiliar registra a aplicação das doses.
- **RN-01:** coletiva e isolamento comportam 1 animal; ninhada, até 6 filhotes da mesma mãe.
- **RN-02:** antirrábica vencida (mais de 12 meses) ou sem registro, só baia de isolamento.
- **RN-08:** a baia só é liberada com a saída física do animal. Atenção: no TAP o par RN-07/RN-08 aparece ligado
  aos esquemas contínuo e sintomático; confira a numeração no PDF antes de citar uma RN.
- **Ciclo de vida da internação:** Admitida → Em tratamento → Alta autorizada → Encerrada, com os alternativos
  Isolamento e Alta a pedido do tutor (tabela completa em "Arquitetura do `ms-internacao`").

## Próximos módulos

1. `ms-internacao` (:8083): em construção na `feature/internacao`; ver "Arquitetura do `ms-internacao`".
2. `octopus-msprescricao` (:8084, Sprint 3): prescrição e itens, na `feature/prescricao`; ver "Arquitetura do
   `octopus-msprescricao`".
3. Módulo de doses (Sprint 3, ainda sem nome nem branch): gera os horários a partir dos itens prescritos e
   alimenta o painel (atrasadas, próximas, perdidas) e o relatório. A squad decidiu separar as doses da
   prescrição (09/10/2026). Como não há transação entre módulos (o TAP proíbe mensageria e transação
   distribuída), o contrato precisa ser: a prescrição grava e depois manda os itens ao módulo de doses; a geração
   é idempotente por id do item, e há um caminho para gerar de novo quando o módulo de doses estiver fora do ar.
   Sem isso, o painel mostra prescrição sem nenhuma dose.

## Estado das branches (09/10/2026, Sprint 2 em andamento)

Branch por microsserviço, permanente. O `CLAUDE.md` é mantido igual em todas; o `README.md` de cada uma descreve
o próprio módulo.

- **`main`**: `octopus-msusuario`, `TAP.md`, `CLAUDE.md`, `README.md`, `docker-compose.yml`, `docs/` e o
  workflow de CI. Em `docs/` ficam os mapas de processo UML, numa página HTML autocontida gerada a partir de
  dados JS no próprio arquivo; para mapear algo novo, acrescente um objeto ao array `MAPS`. Esta branch está em
  produção, então mudanças de código afetam o ambiente publicado: combine com a squad antes.
- **`feature/cadastro-baia`** (Douglas): `ms-cadastro-baias/` completo, com entidade `Baia`, DTOs, service,
  controller `/api/baias`, exceptions, JWT, migrations e compose próprios. Em 24/09 entrou um merge de
  `origin/main` nesta branch, o que foge do modelo de uma branch por microsserviço: ela passou a carregar
  `TAP.md`, `docs/` e um diretório `.idea/` versionado. O `.idea/` deveria sair, já que está no `.gitignore`.
- **`feature/register_medications`** (Guilherme Bifani): `octopus-msmedications/` completo, com entidade
  `Medicacao` (incluindo `TipoEsquema`, interações proibidas e `quantidade`), DTOs, service, controller
  `/api/medicacoes`, exceptions, JWT, migrations, compose próprio e 12 testes. É uma branch órfã, sem ancestral
  comum com a `main`, o que é esperado aqui.
- **`feature/animal_mae`** (Douglas): parte da `main` e adiciona ao `octopus-msusuario` a mãe do animal
  (`mae_id`, migration V6) e o `GET /api/animais/{id}`, ambos para o `ms-internacao` (RN-01 da ninhada e
  RN-02). Ainda não foi para a `main`, que está em produção. Enquanto não for, o `ms-internacao` não consegue
  admitir contra o `msusuario` publicado: o endpoint que ele consulta não existe lá e a admissão responde
  "Animal não encontrado".
- **`feature/internacao`** (Douglas): `ms-internacao/` com entidade `Internacao` e histórico
  (`InternacaoEvento`), migrations V1 e V2, clients REST para `msbaias` e `msusuario`, service com RN-01, RN-02
  e RN-08, controller `/api/internacoes`, JWT, compose próprio e 50 testes. O esqueleto veio de uma cópia do
  módulo de baias (branch `copilot/ms-internacao-only-cadastro-baias-folders`, já mesclada e que pode ser
  apagada); o pacote, a porta e o histórico do Flyway já foram trocados, e o `.idea/` saiu do versionamento.
  Tem `pipeline.yml` próprio (container `internacao`, porta 8083). A pasta se chama `ms-internacao/`, e não
  `octopus-msinternacao/`, por herança desse esqueleto.
- **`feature/prescricao`** (Vitor Kimany e Guilherme Bifani; ajustes de Douglas): `octopus-msprescricao/` só com
  prescrição e itens; as doses foram tiradas daqui de propósito (ver "Próximos módulos"). Branch órfã, com
  `pipeline.yml` próprio (container `prescricao`, porta 8084); o primeiro deploy precisa do arquivo
  `~/octopus-env/prescricao.env` na EC2 e da porta 8084 liberada.

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

Portas de host default: 8080/3307 (usuário), 8081/3308 (baias), 8082/3309 (medicações), 8083/3310 (internação),
8084/3311 (prescrição).

### Configuração via `.env`

- Nenhum `application.properties` tem credencial default: tudo vem do `.env`, lido da pasta do próprio módulo ou
  da raiz do repositório (`spring.config.import=optional:file:./.env[.properties],optional:file:../.env[.properties]`).
  Sem `.env` a aplicação não sobe local. Copie o `.env.example` do módulo para `.env` e preencha.
- Os dois caminhos são relativos à pasta de trabalho. Pelo terminal, rode de dentro da pasta do módulo. No
  IntelliJ, a configuração de execução precisa de *Working directory* `$MODULE_WORKING_DIR$`; sem isso ele usa a
  raiz do repositório, não acha o `.env` e a subida falha com `Could not resolve placeholder 'JWT_SECRET'`.
- Para desenvolver, aponte o `DB_URL` para o MySQL do compose, não para o Aiven. Subir um módulo contra o banco
  publicado aplica as migrations nele, e a partir daí elas não podem mais ser editadas.
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
  porque não faz login. `Animal` tem id, nome, tutor, espécie e data da última vacina antirrábica; a data é
  anulável, e é ela que a RN-02 vai consultar na internação.
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
- Testes: 42 no total, entre unitários com JUnit 5 e Mockito (`service/`) e integração com MockMvc
  (`security/AutenticacaoEAutorizacaoTest` e `service/RecuperacaoSenhaTest`).
- `mail/EmailService` envia o e-mail de redefinição de senha via `JavaMailSender`. Ele lê
  `${spring.mail.username}` como remetente, e as propriedades `spring.mail.*` vêm do `.env`. O host tem default
  porque sem ele o Spring nem cria o bean e a aplicação não sobe; usuário e senha ficam vazios, então quem não
  configurou SMTP ainda consegue rodar o projeto.
- Recuperação de senha: `POST /api/auth/esqueci-senha` e `POST /api/auth/redefinir-senha`, os dois públicos na
  `SecurityConfig` e respondendo 204. Ver "Recuperação de senha".

## Arquitetura do `ms-internacao`

Mesmas camadas e convenções dos outros módulos, com pacote `com.br.octopus_msinternacao`. O que é próprio dele:

**Ciclo de vida.** `StatusInternacao` tem `ADMITIDA`, `EM_TRATAMENTO`, `ISOLAMENTO`, `ALTA_AUTORIZADA`,
`ALTA_A_PEDIDO_DO_TUTOR` e `ENCERRADA`, e o método `podeIrPara` é a tabela de transições inteira. São 8:

| De | Para | Guarda |
| :- | :--- | :----- |
| Admitida | Em tratamento | Prescrição ativa (quem chama é o `msplantao`); idempotente |
| Em tratamento | Isolamento | RN-02: antirrábica irregular, baia de isolamento com vaga |
| Em tratamento, Isolamento | Alta autorizada | — |
| Em tratamento, Isolamento | Alta a pedido do tutor | Termo de responsabilidade obrigatório |
| Alta autorizada, Alta a pedido | Encerrada | RN-08: saída física entre a alta e agora |

Isolamento não volta para Em tratamento. Um animal que já chega com a vacina vencida é admitido como
`ADMITIDA` numa baia de isolamento; o status `ISOLAMENTO` é só para quem já estava em tratamento.

**Entidade com invariantes.** `Internacao` não tem `@Setter` nem `@Builder`, o que foge da convenção de
propósito: o status só muda pelos métodos de negócio (`admitir`, `iniciarTratamento`, `isolar`, `autorizarAlta`,
`altaAPedidoDoTutor`, `encerrar`), que conferem a transição e gravam um `InternacaoEvento`
(`tb_internacao_eventos`) com status, baia, usuário (e-mail do token) e hora. O histórico nunca é editado. A
hora entra por parâmetro, vinda de um `Clock` fixo em `America/Sao_Paulo` (`config/ClockConfig`), porque o
`DATETIME` não guarda fuso e o container roda em UTC.

**Dados de outros módulos.** `animalId` e `baiaId` são `UUID` sem FK. Na admissão o service busca o animal no
`msusuario` (`GET /api/animais/{id}`) e a baia no `msbaias` (`GET /api/baias/{id}`) pelos clients do pacote
`client`, que repassam o JWT de quem chamou, têm timeout de 2 s para conectar e 3 s para ler, devolvem 404
quando o recurso não existe e 503 (`ServicoIndisponivelException`) em qualquer outra falha. O nome, a espécie e
a mãe do animal são copiados para a internação (`animal_nome`, `animal_especie`, `mae_id`): o painel mostra o
nome sem consultar o `msusuario`, cujo `GET /api/animais` é restrito, e a regra da ninhada é conferida sem
chamada remota. A capacidade usada na RN-01 é o menor valor entre a cadastrada e o máximo do tipo, o que protege
contra baia gravada antes da correção da coletiva para 1. URLs em `MS_USUARIO_URL` e `MS_BAIAS_URL`.

**Concorrência.** A admissão tem duas fases: as chamadas REST e a RN-02 acontecem fora da transação, e só a
disputa pela vaga fica dentro de um `TransactionTemplate`, para nenhuma trava esperar a rede.

- Mesma vaga ao mesmo tempo: `findByBaiaIdAndStatusNot` com `@Lock(PESSIMISTIC_WRITE)`. No InnoDB, com o índice
  `(baia_id, status)` e `REPEATABLE READ`, o `FOR UPDATE` trava a faixa da baia; com a baia vazia o MySQL pode
  resolver por deadlock, e o perdedor recebe 409.
- Mesmo animal admitido duas vezes: coluna gerada `animal_internado` (preenchida só enquanto a internação está
  aberta) com `UNIQUE`. Ela não é mapeada na entidade, então o `validate` a ignora e o H2 dos testes não a tem.
- Mesma internação editada ao mesmo tempo: `@Version` na coluna `versao`.
- O `GlobalExceptionHandler` transforma trava, deadlock, versão desatualizada e violação do `UNIQUE` em 409 "tente
  novamente", em vez de 500.

O H2 não reproduz o `FOR UPDATE` do InnoDB: os testes cobrem as regras, mas a corrida real só se verifica contra
MySQL.

**Endpoints.** Não há `DELETE`; a internação termina em `ENCERRADA`, que faz o papel do arquivamento.

| Endpoint | Quem pode |
| :------- | :-------- |
| `POST /api/internacoes` | `RECEPCIONISTA`, `ADMIN` |
| `GET /api/internacoes?status=&baiaId=&animalId=`, `GET /{id}`, `GET /{id}/eventos` | autenticado |
| `PATCH /{id}/iniciar-tratamento`, `/isolar`, `/autorizar-alta` | `VETERINARIO` |
| `PATCH /{id}/alta-a-pedido-do-tutor`, `/encerrar` | `RECEPCIONISTA`, `VETERINARIO` |

O `ADMIN` fica fora dos atos clínicos de propósito, para o histórico mostrar sempre um veterinário na alta.
O id da internação é `Long` (`/api/internacoes/42`); `animalId`, `baiaId` e `maeId` continuam `UUID`.

**Migrations.** `V1__internacoes.sql` cria as duas tabelas; `V2__internacoes_animal.sql` acrescenta nome e
espécie do animal; `V3__internacoes_id_long.sql` recria as duas com id `BIGINT AUTO_INCREMENT` (as tabelas só
tinham dados de teste). Histórico do Flyway em `flyway_schema_history_internacao`.

**Testes.** 50: `InternacaoTest` (máquina de estados), `InternacaoServiceTest` (RN-01 e RN-02 com clients
mockados e `Clock` fixo) e `InternacaoApiTest` (MockMvc com tokens de cada perfil e os clients como
`@MockitoBean`).

## Arquitetura do `octopus-msprescricao`

Pacote `com.br.octopus_msprescricao`, mesmas camadas dos outros módulos.

- `Prescricao` (`tb_prescricoes`): `internacaoId` (`Long`, sem FK), e-mail do veterinário (do token), observação,
  `ativo` e `criadoEm` (do `Clock` de São Paulo). `ItemPrescricao` (`tb_itens_prescricao`): `medicacaoId`
  (`UUID`, do msmedications), dosagem em texto, `intervaloHoras`, `quantidadeDoses`, `inicio` e observação. Não
  há dose aqui: os horários são `inicio + n * intervaloHoras` e serão gerados pelo módulo de doses.
- Criar prescrição: busca a internação no `ms-internacao` (`InternacaoClient`, mesmo padrão de clients com token
  repassado, timeouts, 404 e 503), aceita só `ADMITIDA`, `EM_TRATAMENTO` ou `ISOLAMENTO` (senão 422), chama
  `PATCH /api/internacoes/{id}/iniciar-tratamento` e só então grava. O `iniciar-tratamento` é idempotente, então
  vem antes da gravação: se falhar, nada é gravado. O método não é `@Transactional`, para nenhuma transação
  esperar a rede. URL em `MS_INTERNACAO_URL`.
- Só o `VETERINARIO` prescreve e desativa (o enunciado diz que é ele quem prescreve, e é o único perfil que o
  `ms-internacao` aceita no `iniciar-tratamento`). Leitura liberada para qualquer perfil autenticado.
- RN-03: antes de iniciar o tratamento e gravar, o `MedicacaoClient` busca cada medicamento novo no
  msmedications (`GET /api/medicacoes/{id}`, uma vez por medicamento) e a prescrição é recusada com 422 se dois
  deles têm interação proibida entre si, ou se algum tem interação proibida com um medicamento das prescrições
  ativas da mesma internação (o animal recebe todas). Basta olhar a lista do medicamento novo, porque o
  msmedications grava o par nos dois sentidos. URL em `MS_MEDICACOES_URL`.
- Falta: o item "se necessário" da mudança contratada de 01/11, sem intervalo nem quantidade.

| Endpoint | Quem pode |
| :------- | :-------- |
| `POST /api/prescricoes` | `VETERINARIO` |
| `GET /api/prescricoes?internacaoId=`, `GET /{id}` | autenticado |
| `PATCH /{id}/desativar` | `VETERINARIO` |

## Convenções para novos microsserviços

Os módulos existentes já seguem o padrão abaixo; use qualquer um deles como referência ao criar o próximo.

- Pasta na raiz e artifactId `octopus-ms<area>`, groupId `com.br`, pacote base `com.br.octopus_ms<area>` com
  underscore. Spring Boot 4.1.1, Java 17, Lombok e os mesmos starters "quebrados" do Boot 4.
- Nomes de entidades em português, seguindo o TAP: `Baia`, `Medicacao`, `Internacao`, `Prescricao`, `Dose`.
  Tabelas `tb_<plural>` (`tb_baias`, `tb_medicacoes`) e colunas `@Column(name = "snake_case")`.
- Id `UUID` com `GenerationType.UUID`, imports wildcard `jakarta.persistence.*` e `lombok.*`, mais `@Builder`,
  `@NoArgsConstructor` e `@AllArgsConstructor`. Enums em `domain.enums`, em minúsculo.
- Exceção decidida pela squad em 09/10/2026: internação, prescrição (com seus itens) e dose usam id `Long` com
  `GenerationType.IDENTITY` (`BIGINT AUTO_INCREMENT`), assim como as referências entre eles (`internacaoId`,
  `itemId`). Usuários, tutores, animais, baias e medicamentos continuam `UUID`, e as referências a eles também.
  Com `IDENTITY` o id só existe depois do `INSERT`.
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
- Um `.github/workflows/pipeline.yml` próprio, copiado de outra branch, trocando o nome da branch no filtro de
  `push` e o bloco `env` (ver "Integração contínua").

## Integração contínua

Cada branch tem o seu `.github/workflows/pipeline.yml`. A cada push ele roda os testes do módulo daquela branch;
o deploy (imagem no GHCR e troca do container na EC2, com volta automática para a versão anterior se o serviço
não subir) só acontece quando a `<version>` do `pom.xml` muda. Para publicar uma alteração, suba a versão no
mesmo push. O arquivo é praticamente igual em todas as branches, mudando o nome da branch no filtro de `push` e
o bloco `env` (`MODULO`, `CONTAINER`, `PORTA`).

A duplicação é necessária, não descuido. O GitHub só executa um workflow que exista **na branch empurrada**, e
como nenhuma branch é mesclada com outra, o arquivo da `main` não cobriria as demais. Também não há gatilho de
`pull_request`, porque o projeto não usa PR: cada microsserviço fica na sua branch. O `workflow_dispatch`
permite rodar manualmente pela aba Actions.

Ao criar um módulo novo, copie o arquivo de qualquer branch e troque a branch e o bloco `env`.

## Segurança entre módulos

Decidido na Sprint 1 e implementado em `ms-cadastro-baias`, `octopus-msmedications` e `ms-internacao`: só o `octopus-msusuario`
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
| `msinternacao`  | autenticado        | por transição; ver "Arquitetura do `ms-internacao`" |
| `msprescricao`  | autenticado        | `VETERINARIO` (prescrever e desativar)  |

Desativar um usuário no `msusuario` não corta na hora o acesso dele aos outros módulos: como eles não consultam
a tabela de usuários, o token continua válido até expirar. Isso é aceitável para o escopo do TAP; se virar
problema, a saída é reduzir `JWT_EXPIRATION_MS`.

## Recuperação de senha

A senha mora no `Usuario`, que é compartilhado pelos quatro perfis, então o fluxo não toca em `Veterinario`,
`Auxiliar` nem `Recepcionista`. Um caminho só atende todo mundo.

`TokenRecuperacaoSenha` (`tb_tokens_recuperacao_senha`) guarda o código, com `StatusTokenSenha` em `ATIVO`,
`USADO` ou `ARQUIVADO`, prazo de 30 minutos e `usadoEm`. Nada é apagado aqui também: pedir um código novo
arquiva o anterior, então fica um válido por vez e o histórico de pedidos permanece.

Dois cuidados que estão no código de propósito:

- O `esqueci-senha` responde 204 exista ou não o e-mail, e também quando a conta está desativada. Responder
  diferente entregaria a quem perguntasse a lista de quem tem conta na clínica.
- A falha no envio do e-mail é registrada em log e engolida. Um SMTP fora do ar não pode virar erro 500 nem
  desfazer o token já gravado.

Limitação conhecida: trocar a senha não derruba os tokens JWT já emitidos, porque o filtro não tem como saber
que a senha mudou. Quem estava logado continua até o token expirar. Se virar problema, o caminho é uma coluna
de versão da senha no `Usuario`, gravada como claim e conferida no filtro.

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
- Quando faz sentido reativar, o módulo expõe também `PATCH /api/<recurso>/{id}/ativar`. As baias fazem isso, e
  a reativação respeita o limite de 12 baias ativas que a clínica tem.
- A internação é a exceção: não tem `ativo`, tem ciclo de vida. Ela termina em `ENCERRADA`, que faz o papel do
  arquivamento, e cada passo fica gravado em `tb_internacao_eventos`.

## Migrations (Flyway)

O schema de cada microsserviço é criado por migrations SQL em `src/main/resources/db/migration/`, nunca pelo
Hibernate. A configuração já está feita em todos os módulos:

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
  banco de dev for descartável, `docker compose down -v` zera tudo. Uma migration que nunca rodou em banco
  nenhum ainda pode ser editada; na dúvida, crie a próxima versão.
- Coluna que só o banco usa (gerada, de controle) pode ficar fora do mapeamento: o `validate` confere as colunas
  das entidades, não as que sobram na tabela.
- Uma migration por unidade lógica, uma tabela ou um grupo coeso, com um comentário `--` no topo quando a
  intenção não for evidente.
- Não há herança JPA: a ligação entre perfil e `Usuario` é uma FK 1:1 (`usuario_id BINARY(16) NOT NULL`, mais
  `UNIQUE KEY` e `FOREIGN KEY`). Uma relação `@ManyToOne` vira coluna, índice e `FOREIGN KEY`.
- Mapeamento de Java para MySQL que passa no `validate`:

| Java                               | MySQL                     |
| :--------------------------------- | :------------------------ |
| `UUID`                             | `BINARY(16)`              |
| `Long` com `IDENTITY`              | `BIGINT NOT NULL AUTO_INCREMENT` |
| `Long` (referência)                | `BIGINT`                  |
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
