# Painel de monitoramento e simulação de flanges

## Funcionalidade

A tela principal `/` exibe um **Painel de monitoramento** abaixo do mapa da planta.
Nele aparecem:
- Total de alertas **reais e ativos** registrados no MySQL.
- Os **6 alertas individuais mais recentes**, inclusive os já normalizados.
- Um aviso vermelho pulsante enquanto houver alertas reais ativos.
- Destaque vermelho nos pontos do mapa e cartões relacionados a alertas reais.
- Link para o histórico completo e o fluxo de reconhecimento/normalização.
- Consulta automática de dados a cada **7 segundos** enquanto a aba está visível.
- Aviso de nova informação, sem ocultar as ocorrências existentes.

### Origem dos dados

- `HomeController` fornece uma renderização inicial do histórico de até seis alertas.
- `GET /api/monitoramento/eventos` retorna somente os campos de visualização
  (identificador, flange, horário, descrição e situação), IDs das flanges com alarmes
  ativos, contagem total e estados visuais de cada flange autorizada
  (rótulo, prioridade, categoria e mensagem).
- A API exige autenticação. ADMIN acessa todas as unidades; USER só sua unidade
  ativa. O resultado não pode ser armazenado em cache.
- O alerta real depende de **registros persistidos**; não é a comprovação física
  de que soda cáustica vazou. Sensores de gás ainda precisam de especificação
  do gás-alvo e teste da integração.

## Simulador visual do Dashboard

ADMIN encontra o botão **Simular flange** junto ao título do painel.
1. Selecionar uma flange cadastrada e visível para o administrador.
2. Escolher `Anormalidade em observação`, `Suspeita de vazamento`,
   `Flange danificada` ou `Falha de sensor/comunicação`.
3. Iniciar a demonstração.
4. O cartão e o ponto correspondente no mapa mudam de estilo. Em cenário crítico
   surge faixa **vermelha pulsante** com texto **SIMULAÇÃO**.
5. Usar **Encerrar simulação** para restaurar a aparência anterior.

A simulação roda **somente no navegador atual**: não faz chamadas HTTP de
escrita, não grava leituras, não altera a flange no MySQL e não envia MQTT.
Ao recarregar a página o cenário visual desaparece. O histórico real
continua apresentado em bloco separado.

A simulação **não** representa a operação de um sensor nem a confirmação
de vazamento real, não é teste de segurança de processo e não deve ser
usada para avaliar detecção efetiva de NaOH.

## Interface e acessibilidade

- Usar rótulos `DADOS DO SISTEMA`, `ALERTA` e `SIMULAÇÃO`
  para diferenciar o que foi observado do que é demonstração.
- Reduzir animações em `prefers-reduced-motion`; o alerta permanece
  destacado por cor, borda e texto mesmo sem piscar.
- Renderizar os campos das ocorrências como **texto**, sem injetar HTML.
- Seguir temas claro (cinza-azulado) e escuro (azul industrial).
- Uma conexão falha não zera artificialmente o número de alarmes mostrado.

## Aceite / validação

- [ ] Conferir no navegador 360, 768 e 1366 px em ambos os temas.
- [ ] ADMIN consegue iniciar e encerrar simulação numa flange específica.
- [ ] USER vê painel real mas não tem acesso ao simulador.
- [ ] Simular condição crítica destaca **somente a flange selecionada**
      e não altera banco, histórico e contagens reais.
- [ ] Caso haja alerta real em outra flange, a indicação real permanece.
- [ ] IDs de unidades diferentes não chegam ao USER pela API.
- [ ] Testar que alerta normalizado permanece entre os últimos 6 registros.
- [ ] Testar tecla Escape e navegação de teclado na janela do simulador.
- [ ] Confirmar animação reduzida com preferência de movimento reduzido.
- [ ] Com hardware real, validar **separadamente** sensor, modelo,
      gás-alvo e procedimentos técnicos com o orientador.

**Situação:** código e testes automatizados disponíveis; não substituem
ensaio físico ou validação de detecção industrial.

## Alertas e ordenação

O mapa e a lista recebem estados do **mesmo classificador** no carregamento
inicial e nas consultas periódicas. Flanges em alerta sobem automaticamente
na lista, com seleção cruzada mapa → cartão e cartão → mapa. O alerta
crítico pulsa em círculo, borda e ícone; o conteúdo textual permanece estável.
Há estados distintos para dano explícito, suspeita, falha de sensor e ausência
de leitura. Sinais não verificados nunca aparecem como confirmação automática.

A simulação tem rótulo explícito e contador separado. Encerrá-la restaura
os dados reais mais recentes — não declara normalização operacional.
Regras técnicas e cenários de aceite: [Alertas visuais dinâmicos](ALERTAS-VISUAIS-DINAMICOS.md).

## Status de disponibilidade das leituras

No mapa e na lista lateral, o antigo rótulo genérico **SEM LEITURA** foi
substituído por estados distinguíveis:
- **SEM SENSOR:** nenhuma instrumentação cadastrada;
- **SENSOR NÃO INSTALADO:** existe cadastro, mas sem marcação de instalação;
- **AGUARDANDO DADOS DO SENSOR:** cadastrado como instalado e ainda sem
  primeira leitura própria;
- **SEM DADOS RECENTES:** há histórico de leitura do instrumento instalado,
  mas a última informação ultrapassou o prazo visual configurado;
- **NORMAL:** status cadastral normal com leitura registrada recentemente;
  isso não equivale à confirmação de segurança operacional.

O prazo padrão de referência é 15 minutos, configurável por
`MONITORAMENTO_PRAZO_LEITURA_MINUTOS`. Ele precisa ser validado com a
frequência de envio do sensor real. Não há alarme físico automático gerado
somente por ausência de dados. Falhas registradas de comunicação e
ocorrências críticas continuam como categorias distintas.
