# Padrão visual FlangeGuard — identidade do index e do dashboard

Esta documentação registra a padronização visual solicitada para o sistema.
**Não modifica os requisitos funcionais, operacionais ou de segurança do TAP/TCC**.

## Identidade

| Elemento | Referência cromática | Uso |
| --- | --- | --- |
| Azul-base | `#002456` | Fundo industrial e identidade do index |
| Azul dos painéis | `#0a2b61`, `#10387b` | Cards, formulários e áreas de análise |
| Azul de informação | `#60a5fa` | Links, foco e detalhes de monitoramento |
| Dourado | `#f4c45f` | Identificação de seções e ações em destaque |
| Verde | `#22c55e` | Estados operacionais positivos |
| Vermelho | `#ef4444` | Alertas e situações que exigem atenção |
| Fundo do tema claro | `#f3f6fc` | Interface clara |
| Superfície do tema claro | `#ffffff` | Cards e formulários |

Cor isolada não deve ser o único indicador de status; preservar sempre textos e rótulos.

## Hierarquia tipográfica e estrutura

- **Família de interface:** IBM Plex Sans, com fallback Segoe UI/Arial.
- **Dados, etiquetas e códigos:** JetBrains Mono, com fallback Consolas/monospace.
- **Títulos principais:** aproximadamente 27–38 px, ajustados à largura da tela.
- **Títulos de cartões:** aproximadamente 18–22 px.
- **Textos explicativos:** 13–15 px com altura de linha maior para leitura.
- **Etiquetas:** aproximadamente 10–11 px, em caixa alta apenas quando úteis.
- **Campos e botões:** alvo visual de pelo menos 41–44 px; bordas arredondadas,
  foco destacado para navegação pelo teclado.

## Arquivos de estilo

- `src/main/resources/static/style.css`: tokens originais e componentes-base.
- `src/main/resources/static/theme.css`: refinamentos transversais e modo claro/escuro.
- `src/main/resources/static/menu.css`: menu institucional, responsividade e dropdowns.
- `src/main/resources/static/relatorios.css`: central de relatórios redesenhada.
- CSS específico (`alertas.css`, `sensor.css`, `analises.css` etc.): componentes locais.

A personalização transversal do `theme.css` usa o escopo
`body:has(.main-nav)` para não atingir indevidamente a página de login.
Os módulos continuam respeitando autorização no backend; **links ocultos na
navegação não são um mecanismo de segurança**.

## Experiência e navegação

- Desktop: links essenciais no topo; rotas administrativas e de operação
  agrupadas em menus.
- Mobile: menu adaptado à largura, com acesso aos mesmos módulos autorizados.
- Página ativa destacada com `aria-current="page"` quando aplicável.
- Tabelas largas usam rolagem horizontal, sem esconder colunas de dados.
- Formulários preservam os mesmos `name`, `th:field`, `th:action`,
  validações e rotas anteriores à revisão.
- Relatórios preservam seis exportações CSV e o controle de acesso por unidade.
- Os estados de alerta mantêm textos e histórico; nenhum valor de sensor
  é substituído por dado demonstrativo nesta revisão.

## Testes e limitações

A validação automática `scripts/validar-visual.py` verifica presença de `main`,
`h1`, links de exportação, rotas da navegação e balanceamento de blocos CSS
em templates críticos. O CI também verifica a sintaxe de `menu.js`.
O pipeline Maven/Docker e os testes MySQL continuam em execução.

**Ainda exigem inspeção manual em navegador:** contraste, quebra de linha,
uso de teclado, modo claro/escuro, responsividade em 360/768/1366 px,
visualização dos gráficos e screenshots da tela real.
A revisão visual não confirma o funcionamento físico de Arduino nem a
implantação de produção.
