# FlangeGuard — alertas visuais dinâmicos

## Implementação (Dashboard)

As flanges do **mapa da planta** e os cartões da **lista lateral** compartilham
o mesmo `EstadoVisualFlangeDTO`. O servidor deriva o estado de registros
existentes e a API `GET /api/monitoramento/eventos` fornece o estado atualizado
das flanges acessíveis a cada usuário. A página consulta os dados a cada
**7 segundos**, somente com a aba visível; por isso não existe garantia de
detecção em tempo real ou de latência máxima.

| Código | Texto de apresentação | Efeito | Significado |
| --- | --- | --- | --- |
| NORMAL | NORMAL | Círculo verde, nome regular | Status cadastral, não comprova segurança |
| VAZAMENTO | VAZAMENTO CONFIRMADO | Vermelho pulsante | Somente ocorrência explicitamente cadastrada como `VAZAMENTO_CONFIRMADO`, nunca inferida de um sensor genérico |
| SUSPEITA | POSSÍVEL VAZAMENTO | Vermelho pulsante | Sinal de telemetria ainda não verificado |
| DANIFICADA | FLANGE DANIFICADA | Vermelho pulsante | Ocorrência explícita `FLANGE_DANIFICADA` ou `DANO_FLANGE` |
| CRITICO | ALERTA/ESTADO CRÍTICO | Vermelho pulsante | Estado crítico ou ocorrência ativa sem diagnóstico específico |
| SEM_SENSOR | SEM SENSOR | Cinza, sem animação | Não há instrumento cadastrado nesta flange |
| NAO_INSTALADO | SENSOR NÃO INSTALADO | Cinza, sem animação | Existe cadastro, mas nenhum sensor consta como instalado |
| AGUARDANDO_DADOS | AGUARDANDO DADOS DO SENSOR | Azul, sem animação | Há sensor marcado como instalado que nunca enviou uma primeira leitura própria |
| LEITURA_DESATUALIZADA | SEM DADOS RECENTES | Âmbar, sem vermelho pulsante | Sensor previamente enviou leituras, mas ultrapassou o prazo visual de atualização |
| FALHA_SENSOR | FALHA DE SENSOR | Âmbar de falha registrada | Ocorrência explícita de falha ou de comunicação, independente do prazo de dados |
| ATENCAO | ATENÇÃO | Amarelo | Status cadastral de atenção |
| MANUTENCAO | MANUTENÇÃO | Azul | Status cadastral de manutenção |

O estado visual mais prioritário é aplicado quando existem **várias
ocorrências ativas** para a mesma flange. A normalização só elimina
o destaque correspondente quando não restar evento ativo nem status
crítico persistido. Nunca chamar uma ausência de alertas de “seguro”.

## Sincronização de interface

- Mesmo estado no círculo SVG, nome, rótulo abaixo do ponto, bolinha
  lateral, borda, badge e mensagem do cartão.
- Críticos acima dos demais, mantendo a ordem original entre prioridades iguais.
- Indicador **Em alerta: N** conta as flanges com alerta real ou estado de
  atenção; simulação não entra nessa contagem.
- Clique ou **Enter/Espaço** no mapa seleciona e localiza o cartão; clique
  ou teclado no cartão destaca o ponto no mapa.
- Busca por flange continua ativa; ao selecionar um ponto oculto pela busca
  o filtro é limpo para revelar o cartão.
- **Não animar o conteúdo inteiro do card**: apenas borda, indicador,
  símbolo e luz/nome no SVG. Preferência `prefers-reduced-motion` desativa
  as animações mantendo textos, símbolos e cores.
- Tema claro cinza-azulado e escuro azul industrial são preservados.

## Demonstração segura

O botão **Simular flange** do ADMIN oferece atenção, suspeita, dano e
falha fictícia do sensor. A simulação atua somente no navegador:
não grava incidentes, não altera o MySQL, não envia MQTT, não modifica
dados reais e **não confirma vazamento**. O evento aparece em bloco
separado com rótulo **SIMULAÇÃO**, enquanto o histórico real permanece.
A contagem real de incidentes não é alterada. Se surgir uma ocorrência
real importante na flange em demonstração, a simulação é encerrada e o
estado real assume a prioridade.

## Limitações de segurança do protótipo

O sistema não verifica fisicamente danos ou vazamento confirmado.
A tipificação `VAZAMENTO_CONFIRMADO` só é exibida se já existir evento
explicitamente classificado assim no banco; **nenhum código de sensor
existente gera confirmação automática**. A integração de sensor de gás
ainda não está homologada nem prova detecção de NaOH líquido.
Um detector de gás comum não deve ser tratado como barreira de proteção
industrial. A operação real exige engenharia, inspeção e procedimentos
de segurança independentes do software.

## Testes e aceite

Testes automatizados incluem:
- JUnit: estado normal, falha de sensor, ausência de leitura, sinal suspeito,
  dano explícito, múltiplos incidentes e normalização.
- Node.js: prioridade estável de múltiplas flanges, cenários fictícios,
  classificação de simulação, presença da navegação bidirecional,
  regras de animação e movimento reduzido.
- CI: Java/Maven, MySQL descartável, Docker, CSS/HTML e sintaxe JS.

**Ainda dependem de conferência em navegador real:** aparência e
legibilidade em modo claro/escuro, selecionar e localizar flanges,
abrir/fechar simulação, condições simultâneas, responsividade
(360/768/1366 px), teclado e redução de movimento. Teste físico de
sensor/segurança industrial é uma etapa separada.

## Ciclo de leitura do instrumento

O estado visual não depende apenas da presença de algum registro na flange:
o sistema consulta a **última leitura de cada sensor marcado como instalado**,
evitando confundir registros de instrumentos antigos/desativados com os do
sensor atual. O mesmo classificador alimenta o Dashboard inicial e a API de
atualização sem recarregar a página.

O prazo para indicar **SEM DADOS RECENTES** é provisoriamente de
**15 minutos** e pode ser configurado por
`MONITORAMENTO_PRAZO_LEITURA_MINUTOS`
(`app.monitoramento.prazo-leitura-minutos`). Este intervalo é somente uma
referência visual de protótipo; precisa ser revisto quando a periodicidade
do hardware for definida e validada pelo orientador.

Um atraso não confirma que a comunicação foi interrompida ou que o sensor
está defeituoso. Da mesma forma, leitura recente não comprova ausência de
vazamento e não transforma um sensor de gás não homologado em detector de NaOH.
Ocorrências explícitas e status críticos continuam tendo prioridade visual
sobre os diagnósticos de leitura.
