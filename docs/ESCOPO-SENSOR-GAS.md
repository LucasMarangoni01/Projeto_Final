# Mudança de escopo do protótipo: sensor de gás

**Decisão de projeto (17/11/2026):** o protótipo passa a prever **somente
sensor de gás para novos cadastros**. Esta decisão do projeto é posterior
aos textos originais do TAP e do TCC, que citam sensor térmico.

## Mudanças efetivas no software

- A página `/sensores` mostra **Sensor de gás (protótipo)** como único
  tipo possível em um novo cadastro.
- O backend rejeita tentativas de cadastrar novos sensores de outros tipos,
  mesmo que o navegador envie um tipo manualmente.
- Sensores antigos continuam acessíveis e editáveis com seu tipo original
  para **preservar o histórico** e a integridade do banco.
- Dados legados de limite térmico continuam disponíveis apenas para
  edição dos registros térmicos existentes.
- A telemetria MQTT atual **não foi adaptada para leituras de gás**:
  ela utiliza um contrato histórico térmico. O sistema rejeita mensagens
  térmicas de dispositivos classificados como gás.
- Não são emitidos tokens de telemetria para os novos sensores de gás
  antes da definição e validação da integração específica.
- Nenhuma tabela ou registro histórico foi apagado.

## Limitação técnica essencial

**Um detector de gás genérico não detecta automaticamente vazamento de
soda cáustica líquida (NaOH) e não impede fisicamente um vazamento.**
Dependendo do processo, pode ser necessário monitorar névoas/aerossóis ou
outra grandeza distinta. Um detector de gás pode servir apenas para um
gás-alvo que seu fabricante declare detectar, dentro das condições
corretas de uso.

Portanto, **não é seguro** chamar a simples presença do dispositivo de
“prevenção garantida de vazamento de soda cáustica”. A aplicação deve
continuar usando termos como “sinal de possível anormalidade”,
respeitando procedimentos de inspeção e os critérios de segurança.

## Pendências antes de implementar a telemetria de gás

1. Definir com o orientador ou responsável técnico **qual gás-alvo**
   existe no processo e sua relação efetiva com o risco que se deseja medir.
2. Escolher e documentar modelo de sensor com especificações verificáveis,
   limites de uso e compatibilidade com o ambiente.
3. Especificar contrato MQTT próprio (tipo de unidade, valor, timestamp,
   identificação do dispositivo, validade e tratamento de indisponibilidade).
4. Validar limites e ações do alerta com profissional competente.
5. Testar em **bancada didática supervisionada**, sem soda cáustica,
   tubulações pressurizadas ou exposição a atmosferas perigosas.
6. Atualizar formalmente a documentação TAP/TCC e o plano de testes
   quando o hardware e o gás-alvo forem definidos.

**Estado atual:** inventário de sensores de gás implementado e testado
automaticamente. **Detecção de gás com hardware real não implementada
nem homologada.** O estado “Instalado” é cadastral, não comprova conexão.

## Rastreabilidade

Esta decisão altera a proposta física descrita nos documentos originais,
mas não altera o texto desses documentos automaticamente. Conferir
`docs/MATRIZ-REQUISITOS-OFICIAIS.md` e
`docs/PLANO-DE-TESTES-OFICIAIS.md` para pendências do TCC.
