# Plano de testes e aceite — TAP e TCC oficiais

Estado inicial: **roteiros definidos, evidências de hardware e ambiente real pendentes**.
Nenhuma linha abaixo deve ser marcada como aprovada sem execução observada.

## Software / banco de dados

| Caso | Fonte | Ação | Resultado esperado | Estado |
| --- | --- | --- | --- | --- |
| TC01 | RF01–RF04 | Criar, listar, editar e arquivar flange | Dados íntegros e registro histórico preservado | Pendente |
| TC02 | RN01 | Tentar cadastrar identificador duplicado (se aplicável ao modelo) | Regra de unicidade aplicada | Revisar definição |
| TC03 | RF12–RF13/RN06/RN08 | Registrar, atualizar, arquivar e consultar manutenção | Vínculos e histórico permanecem | Pendente |
| TC04 | RF05–RF06/RN02 | Enviar telemetria de sensor autorizado em ambiente de teste | Leitura persistida e associada à flange correta | Pendente |
| TC05 | RF07–RF10/RN03–RN04 | Simular condição térmica anormal configurada pelo responsável técnico | Alerta, ocorrência e status coerentes | Pendente |
| TC06 | RF11/RN05/RN08 | Reconhecer e normalizar evento, depois consultar histórico | Evento preservado com autor, data e motivo | Pendente |
| TC07 | RNF04 | Reiniciar aplicação e consultar dados persistidos | Nenhum histórico perdido | Pendente |
| TC08 | TAP backup | Backup e restauração em banco isolado | Contagens e vínculos conferem | Pendente |
| TC09 | TAP segurança | Acessar dados de outra unidade como USER | Nenhum registro de outra unidade retornado | Pendente |
| TC10 | TAP relatórios | Exportar as seis categorias com USER e ADMIN | Escopo de acesso correto, CSV íntegro | Pendente |
| TC11 | RNF03 | Consultar dashboard com 0, 1, 10, 100 e mais flanges | Consultas responsivas, sem carregar todo histórico em memória | Pendente |
| TC12 | RNF08 | Usar Chrome, Edge e Firefox nas telas principais | Fluxos e layout corretos | Pendente |
| TC13 | RNF01–RNF02 | Realizar tarefas com operador/professor | Compreensão dos controles e alertas, feedback anotado | Pendente |
| TC14 | RNF09 | Medir sinal de anormalidade até alerta visível | Tempo aferido e avaliado pela equipe | Pendente |

## Atualização de escopo — sensor de gás

A decisão de 17/11/2026 substitui os **novos cadastros de sensor térmico**
por **sensor de gás (protótipo)**. O contrato MQTT disponível no código
ainda é térmico e não representa detecção real de gás.

Para validar a interface enquanto o hardware específico não for definido:
- [ ] Cadastrar novo sensor de gás com código único e referência de flange.
- [ ] Confirmar que novo sensor de tipo diferente é rejeitado pelo backend.
- [ ] Verificar que sensores antigos continuam listados e editáveis.
- [ ] Garantir que dados térmicos não sejam aceitos como dados de gás.
- [ ] Definir gás-alvo, modelo do sensor e critério de leitura com orientador.
- [ ] Revisar a documentação TAP/TCC antes de tratar a mudança como validada.

**Os passos abaixo descrevem o procedimento legado para sensores térmicos**
e ficam registrados somente como referência histórica; **não** devem ser
executados ou divulgados como comprovação da nova integração de gás.
Ver `docs/ESCOPO-SENSOR-GAS.md`.

## Integração acadêmica do protótipo

Somente **bancada didática de baixa tensão**, supervisionada pela instituição.
Não conectar o protótipo a linhas industriais, tubulações pressurizadas ou soda cáustica.

1. Revisar os requisitos do sensor térmico e limite de alerta com o orientador.
2. Confirmar sensor registrado, flange vinculada, credencial de dispositivo e broker MQTT
   de testes acessível. Não expor token de hardware em print, repositório ou log.
3. Enviar leitura sintética normal (temperatura dentro do critério aprovado); conferir
   apenas armazenamento, sem novo alerta.
4. Enviar leitura sintética anormal (limiar previamente aprovado no sistema);
   conferir registro de ocorrência e alerta.
5. Repetir identificador da mesma mensagem para verificar idempotência.
6. Simular interrupção da comunicação e confirmar que ausência de dados **não seja
   apresentada como comprovação de condição segura**.
7. Em bancada, observar o LED e medir atraso; registrar foto, vídeo, horário e resultado.
8. Validar comportamento após retorno do broker, reinicialização e normalização manual.

**Exemplo de contrato MQTT, com valores ilustrativos de comunicação, não limiares
industriais:** tópico `monitoramento/flanges/CODIGO_SENSOR/telemetria`.

```json
{
  "idMensagem": "evento_teste_00001",
  "token": "TOKEN_INDIVIDUAL_FORNECIDO_PELO_SISTEMA",
  "temperaturaC": 25.0,
  "vazamentoDetectado": false
}
```

O valor da temperatura é apenas um exemplo de payload de teste e **não** é o
limiar de segurança. O campo `vazamentoDetectado` é um sinal do firmware, não
uma confirmação química ou física do evento.

## Evidências a guardar após cada ensaio

- Data e local do teste, responsáveis e versão/commit do software.
- Cenário, dados **não sensíveis** da amostra e resultado esperado.
- Resultado observado, latência, captura do painel e do alerta.
- Passou/falhou, correção aplicada e reteste quando necessário.
- Aprovação por professor/avaliador. Nenhuma evidência foi fabricada neste arquivo.

## Checklist de aceite

- [ ] Testes Maven executados sem falha.
- [ ] Integração da aplicação com banco validada.
- [ ] Integração didática Arduino / sensor / LED comprovada.
- [ ] Autorização de acesso por unidade testada.
- [ ] Relatórios e histórico conferidos.
- [ ] Backup e restauração realizados em instância isolada.
- [ ] Testes de desempenho e navegador executados.
- [ ] Relatório de aceite validado pelo orientador.
