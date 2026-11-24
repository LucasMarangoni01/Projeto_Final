# Central de alarmes e testes de emergência — relatório técnico

## Escopo e arquitetura
A Central de Alarmes integra o Dashboard existente com `GET /api/monitoramento/alarmes`.
O serviço projeta ocorrências persistidas em `incidentes` e exercícios de software em
`teste_emergencia` para um contrato de apresentação comum (`AlarmePainelDTO`).
A simulação visual anterior continua local, sem persistência nem som.
O teste de emergência é persistido e exercita classificação visual, apresentação,
notificação, seleção do som, reconhecimento, atendimento, encerramento e auditoria.
Não injeta mensagens MQTT, não falsifica leituras, não usa credenciais de sensores,
não chama equipamentos e não executa o caminho físico de detecção. Assim, não é um
teste de ponta a ponta da instrumentação.

## Operação do protótipo
- Ativar som por ação explícita no navegador; a política de autoplay impede som garantido.
- Sinais distintos de vazamento/suspeita, condição crítica e falha de comunicação
  registrada. Som local, uma sequência por vez, com prioridade a ocorrências reais.
- Silenciar registra somente a lista de identificadores pendentes nesta aba/sessão;
  uma nova ocorrência não herda esse silêncio. Reconhecimento persistido interrompe
  o som correspondente, mas mantém o alerta visual ativo.
- Consultas periódicas a cada 7 segundos com aba visível. Falha de consulta preserva
  último estado e informa indisponibilidade. Abas ocultas e páginas fechadas não
  garantem reprodução nem atualização de alertas.
- ADMIN confirma flange ativa, tipo e gravidade antes de iniciar exercício.
  O identificador `teste:ID` nunca é confundido com `real:ID`.
- Encerramento altera somente o registro de teste. Estado cadastral, sensores,
  incidentes reais e relatórios oficiais não são escritos pelo exercício.
- Histórico de exercícios encerrados limitado aos 20 mais recentes na central;
  registros completos permanecem no banco. Ações registradas também em
  `usuario_auditoria`, com autor e detalhes. USER só recebe ocorrências de sua unidade,
  nunca exercícios administrativos.
- Estado detectado é marcado pelo horário de detecção; DISPARADO representa a
  espera de reconhecimento, RECONHECIDO mantém a ocorrência ativa, EM_MANUTENCAO
  representa atendimento, NORMALIZADO indica encerramento registrado.
  A normalização real continua na página Alertas, exclusiva de ADMIN, com
  justificativa e regras existentes. Não representa confirmação automática de segurança.
- O mapa, cartões, contadores e gráficos exibem estados reais e exercícios
  identificados separadamente. Uma condição real tem prioridade sobre o exercício
  na mesma flange.

## Arquivos
- Modelo: `CicloAlarme.java`, `AlarmePainelDTO.java`, `TesteEmergencia.java`,
  `Incidente.java` (campos de atendimento e atualização seletiva).
- Repositórios: `TesteEmergenciaRepository.java`, `IncidenteRepository.java`.
- Serviço: `CentralAlarmesService.java`.
- Controllers: `CentralAlarmesController.java`, `AlertaController.java`.
- Segurança: `SecurityConfig.java` (rotas ADMIN e CSRF preservado).
- Interface: `home/index.html`, `alerta/listagem.html`,
  `js/central-alarmes.js`, `css/central-alarmes.css`, `dashboard-simulacao.js`.
- Testes: `CentralAlarmesServiceTest.java`, `MySqlPersistenceIntegrationTest.java`,
  `scripts/test-central-alarmes.cjs`, workflow Maven.

## Validação
O pipeline executa Java/Maven, MySQL descartável, Node e validação estática da
interface e build Docker. Testes de lógica cobrem permissão, confirmação,
isolamento de teste, reconhecimento, atendimento, prioridade sonora, novos
alarmes após mute e persistência. Resultados devem ser verificados na execução
do GitHub Actions do commit correspondente; não equivalem a teste audível ou
validação em navegador real.

## Limitações e riscos identificados
- A integração MQTT de gás ainda não está habilitada/validada. Sinais térmicos
  legados ou genéricos não confirmam vazamento de NaOH. O software não é uma
  barreira de segurança industrial; nenhuma mudança no caminho de sensores.
- O atraso de leitura permanece diagnóstico de disponibilidade, não falha
  confirmada nem novo incidente automático. O som de comunicação é usado para
  ocorrências explicitamente registradas como falha.
- Não há garantia de tempo real ou disponibilidade contínua do navegador. O
  mute é local e não é replicado entre abas/dispositivos; reconhecimento é persistido.
  Eventos de telemetria agrupados no mesmo incidente não constituem um novo
  identificador de alarme.
- Gravidade real é categorizada para apresentação segundo registros disponíveis;
  não é medição de severidade física. Testes possuem gravidade selecionada.
- Revisar tema claro/escuro, resolução móvel, áudio efetivamente audível,
  navegadores, simultaneidade e fluxo de autenticação em sessão real.
- Novas tabelas/colunas usam a configuração JPA de atualização já existente.
  Antes de implantação fora do ambiente de desenvolvimento, revisar migração e
  backup com responsável técnico; o CI utiliza banco descartável.
