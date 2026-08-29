# Termo de Abertura de Projeto (TAP)

**Projeto:** Reestruturação do Processo e Ferramentas de Atendimento — Portal de Chamados da Nexa Software
**Disciplina:** Análise e Projeto de Sistemas II
**Data de elaboração:** 03/09
**Versão:** 1.0

---

## 1. Squad

| Integrante       | Papel        |
| ---------------- | ------------ |
| Guilherme Bifani | Dev Backend  |
| Guilherme Soares | Dev Backend  |
| Gustavo          | Dev Frontend |
| Douglas          | Tech Lead    |
| Kimany           | QA           |
| Luigi            | Fullstack    |
| Marcos           | PO           |
| Miliani          | Scrum Master |

---

## 2. Justificativa do Projeto

A Nexa Software atende 240 clínicas veterinárias sob um contrato de suporte com SLA de 4 horas úteis (chamados críticos) e 24 horas úteis (demais chamados). No último trimestre, de 1.847 chamados abertos, 312 estouraram o prazo contratual, gerando 18 mil reais em descontos concedidos.

O processo atual depende de dois sistemas que não conversam entre si (portal de chamados e quadro de desenvolvimento), com a ponte entre eles feita manualmente por uma pessoa copiando texto de um sistema para o outro. Isso gera perda de rastreabilidade (23 cards sem referência ao chamado de origem), retrabalho (o mesmo defeito de impressão de receituário corrigido duas vezes) e falhas de comunicação entre suporte e desenvolvimento (chamado que ficou 19 dias aberto após a correção já estar em produção).

Além disso, o formulário de abertura de chamado não possui campos estruturados (categoria, urgência, versão instalada, identificação da clínica), o que contribui para que 41 de 96 chamados analisados precisassem de rodadas extras de pedido de informação, com espera média de 1,5 dia — tempo que corre contra o prazo contratual.

Como conta o coordenador de suporte, Rafael: o desconto não é pago porque o time é lento, mas porque não é possível provar de quem era a vez de agir.

**Prioridade declarada pelo patrocinador do processo (Rafael):** eliminar o pagamento de descontos por estouro de prazo que não é de responsabilidade da Nexa.

---

## 3. Objetivo do Projeto

Redesenhar o processo de atendimento de chamados da Nexa Software e especificar as ferramentas de suporte necessárias, de forma a:

- Tornar possível identificar de quem é a responsabilidade pelo tempo decorrido em cada chamado (Nexa x cliente);
- Eliminar a perda de rastreabilidade entre chamado e card de desenvolvimento;
- Reduzir o volume de chamados que são dúvida de uso e não defeito;
- Dar visibilidade ao cliente sobre o andamento do seu chamado;
- Eliminar o pagamento de descontos indevidos gerados por prazos estourados durante a espera de resposta do cliente, e não por lentidão da Nexa;
- Implementar a medição da satisfação do cliente (CSAT/NPS) ao encerramento do atendimento, hoje inexistente;
- Fazer tudo isso sem alterar o contrato de SLA, sem trocar a ferramenta de quadro dos squads e sem aumento de headcount.

---

## 4. Escopo

### Dentro do escopo

- Processo de atendimento de chamados, do abrir ao encerrar;
- Formulário de abertura de chamado e seus campos;
- Integração/rastreabilidade entre portal de chamados e quadro de desenvolvimento;
- Regras de pausa e retomada do relógio do SLA;
- Controle de acesso à base de dados das clínicas durante o atendimento;
- Tratamento do caminho paralelo do WhatsApp das nove clínicas maiores;
- Base de conhecimento para dúvidas de uso recorrentes.

### Fora do escopo

- O produto de gestão veterinária vendido pela Nexa (sistema fim, usado pelas clínicas);
- Renegociação do contrato de SLA (4h/24h) com as clínicas;
- Troca da ferramenta de quadro usada pelos três squads de desenvolvimento;
- Contratação de novos analistas de suporte (orçamento de headcount fechado).

---

## 5. Principais Stakeholders

| Papel                              | Interesse principal                                       |
| ---------------------------------- | --------------------------------------------------------- |
| Letícia (Analista N1)              | Não repetir a mesma pergunta três vezes ao cliente        |
| Rafael (Coordenador de Suporte)    | Parar de pagar desconto por prazo estourado indevidamente |
| Bianca (Tech Lead de Squad)        | Proteger a sprint de interrupções                         |
| Camila (CS/Sucesso do Cliente)     | Reduzir cancelamento das clínicas                         |
| Otávio (Diretor de Produto)        | Time entregar funcionalidade nova, não só correção        |
| Cliente (recepcionista da clínica) | Saber quando o problema será resolvido                    |

---

## 6. Premissas

- O contrato de suporte com os prazos de 4h e 24h permanece válido durante todo o projeto;
- A ferramenta de quadro de desenvolvimento dos squads não muda;
- O time de suporte continua com 6 analistas + 1 coordenador;
- As 240 clínicas continuam rodando múltiplas versões do produto simultaneamente.

## 7. Restrições

- Orçamento de headcount fechado (não é possível contratar analistas);
- Contrato de SLA não será renegociado neste ano;
- Ferramenta de quadro de desenvolvimento não será substituída;
- Produto de gestão veterinária está fora do escopo do projeto.

---

## 8. Riscos Preliminares

| Risco                                                                                  | Impacto                                 |
| -------------------------------------------------------------------------------------- | --------------------------------------- |
| Resistência dos analistas a preencher campos extra no chamado                          | Baixa adoção do novo formulário         |
| Conflito de prioridade entre suporte (correção) e squads (roadmap)                     | Atraso nas entregas do TO-BE            |
| Pico de chamados de segunda-feira (18% do volume semanal) não ser tratado no redesenho | Solução não resolve o gargalo real      |
| Falta de dado histórico de satisfação do cliente                                       | Dificuldade em medir sucesso do projeto |

---

## 9. Cronograma Macro (entregas da disciplina)

| Entrega | Unidade | Descrição                                                  |
| ------- | ------- | ---------------------------------------------------------- |
| E1      | I       | Processo atual em 6 a 10 passos, com ator por passo        |
| E2      | I       | Três regras de negócio                                     |
| E3      | II      | BPMN do AS-IS com raias por ator                           |
| E4      | II      | BPMN do TO-BE                                              |
| E5      | III     | Mapa de stakeholders e roteiro de entrevista               |
| E6      | III     | Requisitos funcionais e não funcionais                     |
| E7      | III     | Histórias de usuário com critérios de aceite               |
| E8      | III     | Lista de prioridades/complexidades e Product Backlog       |
| E9      | IV      | Sprints, Definition of Done, cronograma e riscos           |
| E10     | V       | Diagrama de Casos de Uso e Caso de Uso Expandido           |
| E11     | V       | Diagrama de Classes e Diagrama de Sequência                |
| E12     | V e VI  | Mockups, mapa navegacional, documento final e apresentação |

---

## 10. Critérios de Sucesso

- Redução do número de chamados estourados sem responsabilidade clara da Nexa;
- Rastreabilidade completa entre chamado e card de desenvolvimento (0 cards órfãos);
- Redução de chamados de dúvida de uso via base de conhecimento;
- Cliente com visibilidade do status do seu chamado.

---

## 11. Aprovação

| Nome    | Papel        | Assinatura/Aceite |
| ------- | ------------ | ----------------- |
| Douglas | Tech Lead    |                   |
| Marcos  | PO           |                   |
| Miliani | Scrum Master |                   |
