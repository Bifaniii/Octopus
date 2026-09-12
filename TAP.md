# Termo de Abertura do Projeto (TAP) — Projeto Sistema para VidaPet

**Projeto:** Um painel de medicação que mostra, a qualquer hora do plantão, o que está atrasado, o que vence na próxima hora e o que foi perdido.

## 1. Informações Gerais do Projeto
* **Nome do Produto:** Plantão (Sistema de Plantão para Clínicas Veterinárias com Internação).
* **Interlocutor de Negócio:** Dr. Rodrigo Salvatore (Veterinário Responsável).
* **Stakeholder:** Márcio da Silva Bastos.

---

## 2. Equipe do Projeto (Squad)
* Douglas Rocha
* Guilherme Bifani
* Guilherme Soares
* Gustavo Amaral
* Luigi Vasconcelos
* Marcos Vinicios
* Miliani Alves
* Vitor Kimany

---

## 3. Justificativa do Projeto
A clínica veterinária *VidaPet* possui 12 baias e opera em regime de 24 horas, mas atualmente depende de processos manuais e informais (como prescrições em papel presas nas baias). Essa realidade gerou métricas críticas no último trimestre:
* **9 doses registradas fora do horário** e **2 doses provavelmente não aplicadas**.
* Falhas graves de segurança física, como **3 animais colocados na mesma baia por engano** na virada de plantão.
* **Ausência de rastreabilidade**, sem registros de qual auxiliar aplicou qual dose.

O projeto justifica-se pela necessidade de eliminar erros humanos, garantir a segurança dos animais internados, automatizar o controle de plantão e fornecer rastreabilidade total das medicações por meio de um painel inteligente em tempo real.

---

## 4. Objetivo do Projeto
O projeto tem como objetivo principal o desenvolvimento e a entrega do sistema **Plantão**, uma solução voltada para a gestão integrada de plantões e internações em clínicas veterinárias. O escopo de desenvolvimento executado pela squad abrange a implementação dos seguintes módulos e funcionalidades centrais:

-   **Módulos de Cadastro:** Estruturação das bases de dados para o gerenciamento de animais, tutores, baias (coletivas, de isolamento e ninhada) e medicamentos (com controle de esquemas e interações proibidas).

-   **Gestão de Internação:** Automatização do fluxo de alocação de animais com aplicação de validações sanitárias (como controle de vacinação antirrábica) e capacidade física.

-   **Prescrição e Controle de Doses:** Criação de rotinas para prescrição médica com geração automatizada de horários, operando em conjunto com um **painel de doses** para monitoramento de status.

-   **Relatório de Doses:** Disponibilização de relatórios consolidados de aplicação, atrasos e perdas, filtrados por turnos e auxiliares.

---

## 5. Escopo do Produto

1. **Cadastro de Animais e Tutores:** Registro de espécie, tutor e data da última vacina antirrábica.
2. **Cadastro de Baias:** Definição do tipo (coletiva, isolamento ou ninhada) e capacidade.
3. **Cadastro de Medicamentos:** Nome, tipo de esquema (contínuo ou sintomático) e interações proibidas.
4. **Internação:** Alocação do animal na baia com aplicação de validações de capacidade e saúde.
5. **Prescrição:** Criação de itens de medicação associados à internação, definindo intervalos e gerando os horários automaticamente.
6. **Painel de Doses:** Visualização de doses atrasadas, próximas e perdidas, com interface para registro de aplicação.
7. **Relatório de Doses:** Consolidação de doses aplicadas, atrasadas e perdidas filtradas por turno e por auxiliar.

---

## 6. Fora do Escopo

* Funcionalidades avançadas de infraestrutura corporativa; em vez disso, será adotado um sistema de login simples baseado em perfis.
* Esforço excessivo em design visual.

---

## 7. Principais Stakeholders
| Papel / Nome | Interesse Principal / Impacto no Projeto |
| :--- | :--- |
| **Dr. Rodrigo Salvatore** (Cliente / Veterinário Responsável) | Garantir a segurança clínica dos animais internados e a fidelidade das prescrições. |
| **Equipe de Desenvolvimento (Squad Octopus)** | Entregar as 5 sprints com código limpo, modelagem correta e os 10 casos de teste validados. |
| **Márcio da Silva Bastos** (Patrocinador / "Dono" da Clínica) | Garantir a viabilidade estratégica do produto, o cumprimento rigoroso do contrato de engenharia e a rentabilidade/utilidade do sistema para o negócio. |
| **Recepcionista / Veterinário / Auxiliar** (Usuários Finais) | Operacionalizar cadastros, internações, prescrições e registros de aplicação sem atritos. |

---

## 8. Premissas
* A equipe do projeto terá disponibilidade e capacidade técnica de desenvolver as competências necessárias para a realização das atividades planejadas.
* Considera-se que o sistema lidará de forma segura com os registros inconsistentes fornecidos na carga inicial, evitando falhas ou erros de execução.

---

## 9. Restrições
* **Limites de Engenharia:** Proibido uso de integrações externas, notificações automáticas (e-mail/push) ou apps mobile; login restrito a formato simples por perfil.
* **Recursos:** O número de desenvolvedores da squad é fixo e não poderá ser alterado.

---

## 10. Riscos Preliminares
* **Resistência operacional:** Possível dificuldade ou resistência dos auxiliares e funcionários no registro rigoroso das informações corretas no painel de doses.
* **Complexidade nas regras de tempo:** Gestão de janelas temporais estritas (30 minutos) e deslocamento de esquemas contínuos vs. descarte de esquemas sintomáticos em caso de perda de dose.

---

## 11. Cronograma Macro
* **Sprint 1 (17/09 a 30/09):** Cadastros (Telas 1, 2 e 3) e criação da entidade central de internação simples sem validações.
* **Sprint 2 (01/10 a 14/10):** Tela 4 com validações de baia e vacinação (RN-01, RN-02) e modelagem do ciclo de vida da internação.
* **Sprint 3 (15/10 a 28/10):** Telas 5 e 6 (Prescrição, Painel, Janela de aplicação de 30 min, controle de doses perdidas e validação de interações medicamentosas - RN-03, RN-04, RN-05, RN-06).
* **Sprint 4 (29/10 a 11/11):** Tela 7 (Relatório), regras de reaplicação por tipo de esquema (RN-07, RN-08) e implementação da mudança contratada a partir de 01/11.
* **Sprint 5 (12/11 a 18/11):** Execução e validação dos 10 casos de teste oficiais, correções de bugs e ensaio geral.
* **Apresentação Final (19/11):** Demonstração ao vivo para a banca e entrega dos artefatos.

---

## 12. Critérios de Sucesso
* **Automatização de Processos:** Eliminação do uso de papéis nas baias, centralizando o fluxo na clínica veterinária.
* **Redução de Desperdícios:** Controle rigoroso de horários para mitigar perdas de doses e otimizar insumos.
* **Segurança na Internação:** Validação impeditiva de interações medicamentosas, controle estrito de capacidade de baias e isolamento obrigatório para animais com vacinação antirrábica irregular.
* **Conformidade de Testes:** Aprovação em 100% dos casos de teste definidos no contrato do projeto (com 6 demonstrações ao vivo e 4 evidenciadas no laudo).

---

## 13. Aprovação
* **Miliani Alves** *(Squad)*
* **Marcos Vinicios** *(Squad)*
* **Dr. Rodrigo Salvatore** *(Veterinário responsável)*
*  **Márcio da Silva Bastos** *(Stakeholder)*