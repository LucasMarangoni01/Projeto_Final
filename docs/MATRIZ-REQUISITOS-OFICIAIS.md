# Matriz de rastreabilidade — documentação oficial FlangeGuard

**Fontes válidas:**
- `TAP-Flangens.docx` (Termo de Abertura do Projeto, versão oficial enviada pelo autor).
- `TCC - Soda Caustica.docx` (TCC oficial, seções 3.3 a 3.6).

Este documento identifica o que existe **no código**. A presença de classes não comprova
que um dispositivo físico, o banco de produção ou um navegador tenha sido testado.
Situações: **implementado no código**, **parcial**, **pendente de validação**.

## Requisitos funcionais (TCC, RF01–RF15)

| ID | Obrigação no TCC | Situação verificada no repositório | Referência |
| --- | --- | --- | --- |
| RF01 | Cadastrar flanges | Implementado no código | `FlangeController`, `Flange` |
| RF02 | Consultar flanges | Implementado no código | `FlangeController`, `FlangeRepository` |
| RF03 | Editar flanges | Implementado no código | `FlangeController` |
| RF04 | Excluir flanges | Implementado como arquivamento lógico para preservar histórico (RN08); validar expectativa da banca | `FlangeController`, `Flange.ativo` |
| RF05 | Receber dados dos sensores | Implementado no código MQTT; **hardware real não validado** | `MqttTelemetriaSubscriber`, `TelemetriaService` |
| RF06 | Processar dados dos sensores | Implementado no código para sensor térmico | `TelemetriaService` |
| RF07 | Identificar condições de possível vazamento | Parcial: sinal do hardware e limite térmico configurável; não comprova vazamento | `TelemetriaService`, `Sensor` |
| RF08 | Emitir alertas | Implementado no código; tempo e LED físico pendentes de teste | `Incidente`, `AlertaController` |
| RF09 | Apresentar sensores no painel | Implementado no código; validar navegador e dados reais | `HomeController`, `home/index.html` |
| RF10 | Registrar ocorrências | Implementado no código | `TelemetriaService`, `IncidenteRepository` |
| RF11 | Consultar histórico de ocorrências | Implementado no código | `AlertaController`, `alerta/listagem.html` |
| RF12 | Registrar manutenções | Implementado no código | `ManutencaoController` |
| RF13 | Consultar histórico de manutenções | Implementado no código, inclusive arquivadas | `ManutencaoRepository` |
| RF14 | Armazenar dados em banco | JPA/MySQL implementado; integração automatizada com MySQL descartável no CI; ainda falta validar ambiente de implantação | `repository/`, `application.properties` |
| RF15 | Visualizar situação atual das flanges | Implementado no código | `HomeController`, `DashboardFlangeDTO` |

## Requisitos não funcionais (TCC, RNF01–RNF09)

| ID | Obrigação | Situação / teste necessário |
| --- | --- | --- |
| RNF01 | Interface simples | Páginas presentes; realizar teste de usabilidade com operadores/avaliadores |
| RNF02 | Informações claras | Dashboard, alertas e análises presentes; conferir contraste e nomenclaturas |
| RNF03 | Tempo de resposta adequado | Dashboard otimizado para consultas de última leitura e 10 recentes; **pendente:** medir em carga real |
| RNF04 | Integridade dos dados | Testes de consultas e isolamento por unidade com MySQL descartável no CI; **pendente:** restauração de backup e validação da instalação real |
| RNF05 | Usar banco de dados | Implementado no código com MySQL |
| RNF06 | Tecnologias web adequadas | Spring Boot, Thymeleaf, HTML/CSS/JS |
| RNF07 | Código de fácil manutenção | Divisão em controllers/services/repositories; revisar padrões e cobertura |
| RNF08 | Navegadores compatíveis | **Pendente:** executar testes visuais/funcionais nos navegadores-alvo |
| RNF09 | Alertas rápidos | Consulta periódica a cada 7 s no navegador implementada; **pendente:** medir latência MQTT → tela com hardware |

## Regras de negócio (TCC, RN01–RN08)

- **RN01:** identificação de flange via ID; verificar se a definição da banca exige outro código único.
- **RN02:** leitura contém referências obrigatórias ao sensor e à flange.
- **RN03 e RN04:** o serviço de telemetria cria ocorrência e alarme quando identifica anormalidade.
- **RN05:** ocorrências não devem ser apagadas durante resolução do alerta.
- **RN06 e RN07:** manutenção associada a flange e registros persistidos em MySQL.
- **RN08:** preservar histórico ao editar/arquivar. Excluir definitivamente ativos relacionados é incompatível com essa exigência sem uma política de retenção específica.

## TAP — entregas adicionais verificadas

| Entrega | Situação |
| --- | --- |
| CRUD de flanges, funcionários e manutenções | Implementado no código |
| Fotos das flanges com acesso restrito | Implementado no código; testar permissões e persistência após implantação |
| Dashboard de monitoramento | Implementado; verificação periódica de alertas por unidade e consulta de leituras limitada; validação em navegador pendente |
| Sensores térmicos/Arduino | Software MQTT presente; integração e ensaio **físico** pendentes |
| LED de alerta | **Pendente:** demonstração/testes com circuito de Arduino |
| Alertas visuais e histórico | Implementado no código |
| **Relatórios detalhados** | Integrado à branch main: exportações CSV por escopo; validação de uso real pendente |
| Testes unitários e integração | Maven e testes automatizados com MySQL descartável no CI; hardware, navegador e aceite continuam pendentes |
| Segurança de acesso | Spring Security com ADMIN/USER; ampliar testes de isolamento entre unidades |
| Backup e recuperação | Scripts Windows e procedimento documentados em `docs/BACKUP-E-RECUPERACAO.md`; **execução, agendamento e restauração real pendentes** |
| Desempenho e disponibilidade | **Pendente:** medição e validação em ambiente de implantação |
| Manual/guia de uso | `docs/GUIA-DO-SISTEMA.md` e página `/guia` existentes; revisar coerência com novas telas |
| Testes de aceitação | Casos e evidências esperadas em `docs/PLANO-DE-TESTES-OFICIAIS.md`; sessões presenciais pendentes |
| Documentação arquitetural e diagramas | Fluxo e arquitetura de código documentados em `docs/ARQUITETURA-E-INTEGRACAO.md`; diagramas acadêmicos do TCC ainda precisam de revisão |
| Evidência de implantação em produção | **Pendente:** comprovação funcional do backend, banco e autenticação |

### Observações da documentação

1. O TAP menciona, no item «Gerenciamento de Funcionários», atividades como
   inscrição de **jogadores**, tabelas de **jogos** e **rankings**. Esses trechos
   parecem ter sido colados de outro projeto e conflitam com o objetivo industrial
   e com o TCC. **Não devem virar funcionalidades sem correção formal da documentação.**
2. O sistema identifica **anormalidades ou sinais de possível vazamento**, e não
   confirma sozinho a presença de soda cáustica.
3. Não definir limiares universais de temperatura, pH ou pressão. Os parâmetros
   do processo devem ser especificados e aprovados por responsável técnico.
4. Nenhum requisito dependente de Arduino, MySQL de produção, sensores ou
   usuários reais pode ser declarado concluído sem o teste correspondente.

## Próximas entregas, na ordem indicada pelos riscos e pelo TAP

1. Validar os CSVs com testes automatizados, navegador e dados de diferentes unidades.
2. Criar teste de integração do fluxo de telemetria/alertas com banco MySQL.
3. Preparar protocolo de testes com Arduino, sensor térmico e LED (simulação de bancada,
   sem exposição à soda cáustica).
4. Definir e exercitar backup/recuperação do banco e validar política de retenção.
5. Registrar resultados dos testes de desempenho, aceitação, navegadores e implantação.

## Entregas de software da rodada de validação

- Endpoint seguro `/api/monitoramento/resumo` com escopo por unidade (RF09,
  RF11 e RNF09). A página apresenta mudança de estado e sugere atualização.
- Otimização das leituras para dashboard (RNF03), sem descarregar todo o histórico.
- Testes de unidade do endpoint e do carregamento do dashboard.
- Plano verificável de aceite, procedimento de backup/restauração e
  documentação arquitetural.
- **Ainda pendentes:** integração real com hardware/LED, teste com MySQL
  separado, backup agendado, validação de navegadores e hospedagem funcional.

**Regra de atualização:** ajustar esta matriz após cada entrega implementada,
sem marcar como validado algo que só existe no código.
