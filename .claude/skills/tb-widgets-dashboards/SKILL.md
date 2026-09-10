---
name: tb-widgets-dashboards
description: Widgets customizados (HTML/CSS/JS, controllerScript, ctx/self.ctx) e dashboards do ThingsBoard — bundles, datasources, entity alias, actions, time window, RPC de botão para device, SCADA symbol, dark mode, layout no grid. Use ao criar, editar, estilizar ou depurar um widget ou um dashboard. Sintomas típicos - o widget não atualiza com telemetria nova, ctx.data vazio ou undefined no controllerScript, entity alias não resolve o device certo, o gráfico não aparece nem renderiza, time window do dashboard não aplica no widget, widget ilegível no dark mode, como mandar RPC de um botão/slider, usar ECharts ou biblioteca de gráfico dentro do widget, deixar o widget responsivo no grid, latest values vs timeseries, editar defaultConfig/settingsSchema.
---

# ThingsBoard Widgets & Dashboards

## Antes de começar — parta de um export real

> **Se o MCP oficial do ThingsBoard estiver conectado** ([`thingsboard/thingsboard-mcp`](https://github.com/thingsboard/thingsboard-mcp),
> 120+ tools, funciona em CE e PE): use as tools dele para **ler e escrever dados** —
> devices, assets, telemetria, alarmes, relações, OTA. São melhores nisso que qualquer
> comando aqui. Use `scripts/tb.mjs` para o que o MCP **não** responde: qual endpoint
> existe nesta versão (`api`, `spec`), quais rule nodes esta instância tem (`nodes`), e
> se é CE ou PE (`check`). O MCP faz dado; esta skill faz contrato e conhecimento.

O JSON de widget/dashboard é grande, com muitos campos opcionais que variam por versão e
por tipo de widget. Montar do zero de memória é a forma mais rápida de produzir um JSON
que o ThingsBoard aceita e não renderiza.

```bash
node scripts/tb.mjs find dashboard                          # lista os dashboards + UUIDs
node scripts/tb.mjs export dashboard "Nome do Dashboard" --out base.json
node scripts/tb.mjs api "widget"                            # endpoints de widget desta versão
```

1. Comece de um export de widget/dashboard **similar** (comando acima, ou menu → Export na
   UI) e edite — não monte o JSON inteiro do zero.
2. Para customizações pequenas (texto, cor, threshold), prefira editar a
   `defaultConfig`/`settings` do widget existente a reescrever o `controllerScript`.
3. Teste incrementalmente: o editor de widgets do ThingsBoard tem preview ao vivo — sugira
   o usuário colar o JS/HTML aos poucos e checar no preview em vez de aplicar tudo de uma vez.
4. Antes de escrever HTML/CSS do zero, checar se **Tailwind CSS** (disponível nativamente
   no editor), Angular Material (`mat-icon`, `mat-button`, `mat-select`) ou componentes
   nativos do ThingsBoard (`tb-icon`, `tb-time-series-chart-widget`) já resolvem — só
   escrever CSS manual detalhado quando esses não cobrirem o layout desejado.
5. Para diretrizes de visual/UX (escolha de forma, cor, dark mode, responsividade dentro
   do grid) ver [references/widget-design-guide.md](references/widget-design-guide.md)
   antes de estilizar um widget do zero.

## Tipos de widget

| Tipo | Uso |
|---|---|
| **Timeseries** | Gráficos/tabelas de séries temporais (múltiplos datasources, eixo de tempo) |
| **Latest Values** | Cards/tabelas com o valor mais recente de atributos/telemetria |
| **RPC / Control** | Botões, sliders, switches que enviam comando ao device |
| **Alarm** | Tabela/lista de alarmes com filtro por status/severidade |
| **Static** | Conteúdo fixo (texto, imagem, iframe) sem datasource |

## Estrutura do descriptor de widget

Campos principais de um widget (bundle item):

- `type`: `timeseries` | `latest` | `rpc` | `alarm` | `static`
- `templateHtml`: markup do widget. Suporta diretivas Angular (`*ngIf`, `*ngFor`,
  `[ngStyle]`, `[ngClass]`), Angular Material e componentes nativos do ThingsBoard
  (`tb-icon`, `tb-time-series-chart-widget`, `tb-entities-table-widget`), além de
  **Tailwind CSS nativamente**. **Confirmado em widgets reais de produção**: para UI
  muito dinâmica (abas, cards gerados por dado), também é válido deixar o
  `templateHtml` como só uma div de "montagem" praticamente vazia (ex:
  `<div id="mtg"><div id="mtg-tabs"></div><div id="mtg-cards"></div></div>`) e fazer
  toda a renderização imperativamente dentro do `controllerScript` via DOM puro/jQuery
  — as duas abordagens (bindings Angular declarativos vs. montagem imperativa em JS)
  coexistem na mesma instância dependendo da complexidade do widget.
- `templateCss`: estilos escopados ao widget
- `controllerScript`: JS que define o comportamento (ver API do `ctx` abaixo)
- `settingsForm` / `dataKeySettingsForm` / `latestDataKeySettingsForm`: formato **atual**
  (confirmado em produção) do formulário de configurações — um **array** de campos, não
  mais o par `settingsSchema`+`settingsDirective` do AngularJS antigo:
  ```json
  [
    {
      "id": "cotaTubo",
      "name": "Cota do tubo (elevação absoluta, em metros)",
      "type": "number",
      "helpText": "Texto de ajuda exibido no editor."
    }
  ]
  ```
  Cada item vira um campo no editor de configurações do widget; `id` é a chave usada em
  `ctx.settings.<id>`. `hasBasicMode`/`basicModeDirective` também aparecem em widgets
  mais elaborados — controlam o alternador "Basic/Advanced" do editor.
- `defaultConfig`: JSON (como string) com valores default de `datasources`, `settings`,
  `title`, etc. Campos vistos em produção além do básico: `widgetStyle`, `widgetCss`,
  `titleStyle`, `titleFont`, `titleColor`, `titleIcon`/`showTitleIcon`/`iconColor`,
  `pageSize`, `units`, `decimals`, `noDataDisplayMessage`, `enableDataExport`,
  `useDashboardTimewindow`, `timewindowStyle`, `configMode`, `actions`, `mobileHeight`.

## API do controller (`self.ctx` / `ctx`)

Dentro do `controllerScript`, o objeto `ctx` expõe:

- `ctx.data`: array de séries já resolvidas (datasource + data points)
- `ctx.datasources`: metadados das entidades/aliases usados pelo widget
- `ctx.settings`: settings configuradas pelo usuário (schema acima)
- `ctx.widgetConfig`: config completa do widget na dashboard atual
- `ctx.$scope` / `ctx.$container`: acesso ao DOM/escopo Angular do widget
- `ctx.detectChanges()`: força re-render após mutar dados manualmente
- `ctx.controlApi.sendOneWayCommand(method, params)` /
  `ctx.controlApi.sendTwoWayCommand(method, params)`: dispara RPC para o device
  originador (widgets de controle)

Ciclo de vida (funções que o `controllerScript` pode definir):

| Hook | Quando roda |
|---|---|
| `onInit` | Uma vez, ao montar o widget |
| `onDataUpdated` | Toda vez que novos dados chegam (telemetria em tempo real, refresh) |
| `onResize` | Quando o widget é redimensionado no layout |
| `onEditModeChanged` | Ao entrar/sair do modo de edição do dashboard |
| `onMobileModeChanged` | Ao alternar entre layout desktop/mobile |
| `onDestroy` | Ao remover o widget da tela |

## Dashboard: estrutura geral

- `configuration.widgets`: dicionário `{ widgetId: { ... } }`. **Formato confirmado em
  produção**: cada entrada tem `typeFullFqn` (string única `"<scope>.<alias>"`, ex.
  `"system.line_chart"` ou `"tenant.custom_image_overlay"` — substitui o antigo par
  `bundleAlias`+`typeAlias`), `type`, `sizeX`/`sizeY`, `row`/`col` e `config` (`title`,
  `datasources`, `settings`, mais os campos de estilo listados acima).
- `config.datasources[]`: `{ type: "entity", entityAliasId: "<uuid>", dataKeys: [...] }`
  — o widget referencia o alias por **id**, não pelo nome. Cada `dataKeys[]` tem
  `{ name, type: "timeseries"|"attribute", label, color, settings }`.
- `configuration.entityAliases`: define aliases resolvidos em tempo de execução por um
  filtro — widgets referenciam o alias (por id), não a entidade diretamente, o que
  permite reusar o mesmo dashboard para clientes/entidades diferentes. Tipos de filtro
  de alias (doc oficial): **Single Entity** (uma entidade por tipo+nome), **Entity
  List** (lista curada manualmente — `{"type": "entityList", "entityType": "DEVICE",
  "entityList": [uuid, ...]}`, visto em produção), **Entity Name** (por prefixo de
  nome), **Entity Type** (todas as entidades de um tipo/profile, inclusive de
  customers — `{"type": "entityType", "entityType": "DEVICE", "entityTypeFilters":
  [...]}`, também visto em produção), **Entity from Dashboard State** (entidade vem de
  parâmetro de state — usado em drill-down), **Asset/Device/Entity View/Edge Type**
  (por profile específico), **API Usage State**, **Relations Query** (entidades
  conectadas por relação até N níveis de profundidade), **Asset/Device/Entity
  View/Edge Search Query** (busca via relação direcionada). Toggle "Resolve as
  multiple entities" controla se o alias devolve 1 ou N entidades.
- `configuration.filters`: mecanismo **diferente** de entity alias — filtra quais
  entidades de um alias aparecem no widget, baseado no **valor mais recente** de um
  atributo/telemetria/campo (ex: só mostrar devices com `status == "ATIVO"`). Suporta
  condições simples (AND) ou grupos aninhados AND/OR, e valores dinâmicos vindos de
  atributos do tenant/customer/usuário. Cada datasource de widget usa no máximo um
  filtro, configurado na aba "Data" do editor do widget.
- `states`: permite dashboards com múltiplas "telas" navegáveis (drill-down). **Padrão
  real visto em produção**: um único dashboard grande com dezenas de states (um por
  ponto/seção monitorada, ex. `cota_700`, `cota_703`, `alarmes`), cada um com seu próprio
  conjunto de widgets — útil para organizar muitos pontos de monitoramento sem multiplicar
  dashboards, mas fica difícil de navegar/manter passado algumas dezenas de states; para
  esse volume vale considerar nomear states de forma consistente (prefixo por categoria)
  para facilitar busca no seletor de state da UI.
- `layouts`: dentro de cada state, tipicamente `main` (desktop); `mobile` é opcional —
  se ausente, o dashboard usa o layout `main` reescalado em telas pequenas.

## Actions (interações de widget)

Configuradas na aba "Actions" do widget (básico: ícone de lápis; avançado: aba
dedicada). 8 tipos, confirmados na doc oficial:

| Tipo | Uso |
|---|---|
| Navigate to New Dashboard State | Vai para outro state do mesmo dashboard (pode abrir como dialog/popover) |
| Update Current Dashboard State | Atualiza contexto de entidade sem navegar — útil para widgets "companion" que reagem via alias "Entity from Dashboard State" |
| Navigate to Other Dashboard | Vai para outro dashboard, opcionalmente um state específico |
| Custom Action | Executa função JS livre (ex: deletar device) via `widgetContext.$scope.$injector` |
| Custom Action (HTML Template) | JS + template HTML próprio para dialogs de criar/editar (4 abas: Resources/CSS/HTML/JavaScript) |
| Mobile Action | Dispara câmera, QR scan, localização, chamada — só no app mobile |
| Open URL | Abre URL externa, opcionalmente em nova aba |
| Place Map Item | Cria entidade e posiciona no mapa — só em widgets de mapa |

Fontes que disparam uma action: botão no header do widget, botão de célula/coluna em
tabela, clique de linha/célula, seleção de nó (árvore), elemento HTML custom, elemento
de mapa (círculo/marcador/polígono).

## Time Window

Escopo temporal + regra de agregação aplicada a widgets de série temporal — pode ser
herdado do dashboard ou sobrescrito por widget ("Use widget time window").

- **Real-time**: atualização contínua para o intervalo corrente. **History**: carga
  única de um período fixo, sem atualização automática.
- Tipo de intervalo: **Last** (janela móvel, ex. "últimas 5 horas"), **Range** (datas
  explícitas), **Relative** (alinhado a limites de calendário, ex. "hoje").
- Agregação por bucket: None (bruto) / Min / Max / Average / Sum / Count — intervalo de
  agrupamento menor = mais granularidade e mais pontos retornados.
- **Max Values** limita pontos retornados para proteger performance do browser —
  importante em widgets com muitos datasources/séries simultâneas.

## SCADA Symbol Widget

Widget diferente dos demais: renderiza um símbolo SVG (válvula, bomba, tanque, etc. —
100+ símbolos prontos) conectado a uma entidade para dados em tempo real, com tags de
comportamento embutidas no próprio SVG. Diferença chave: **posição e tamanho são
fixos** (não reflow ao redimensionar o viewport, ao contrário dos widgets normais) e
usa layout em grid de coluna fixa (24 a 1008 colunas). Ideal para diagramas de processo/
planta industrial onde o layout espacial É a informação; não é a escolha certa para
dashboards com reflow responsivo tradicional.

## Boas práticas

- Evitar poll/consulta de dados pesada dentro de `onDataUpdated` — preferir agregação
  (`agg`) já na consulta de telemetria em vez de agregar no client.
- Para widgets com muitos pontos, configurar `datasources[].dataKeys[].postFuncBody` ou
  usar `agg=AVG/MAX` com `interval` em vez de trazer dados brutos em alta resolução.
- Não hardcodar `entityId` em `controllerScript` — sempre resolver via `ctx.datasources`,
  senão o widget quebra ao ser reusado em outro dashboard/aliase.
- Ao editar `settingsForm`, manter `defaultConfig.settings` coerente com os `id`s
  definidos — campo novo sem default causa `undefined` no controller.
- **Conversão de unidade simples (ex: °C→°F) não precisa de script nenhum**: o
  ThingsBoard tem um recurso nativo de **Unit Conversion** por data key (clicar no campo
  "Units" de uma data key, eixo Y, ou threshold) — define a unidade de origem (a que o
  device realmente envia) e as unidades alvo por sistema (Métrico/Imperial/Híbrido); a
  conversão acontece só na exibição, sem alterar o dado persistido, e os thresholds de
  cor continuam usando o valor na unidade de origem (evita inconsistência entre o
  limite configurado e a unidade exibida). Usar isso antes de escrever qualquer lógica
  de conversão manual em `controllerScript` ou rule chain.
- **Onde calcular um valor derivado mais complexo que unidade** (ex: fórmula de
  engenharia, combinação de múltiplas chaves): se o valor só é usado para exibição num
  único widget, calcular direto no `controllerScript` evita rule chain/calculated field
  extra e é mais simples de ajustar. Se o valor precisa ser persistido (histórico,
  export, usado por múltiplos widgets/relatórios) ou reutilizado por vários devices do
  mesmo profile, vale mais a pena centralizar via **Simple Calculated Field** (expressão
  matemática sem script — ver skill `tb-rule-engine`) ou Script Transformation/Script
  Calculated Field quando a lógica não cabe numa expressão simples.
- Para diretrizes de HTML/CSS, cor, dark mode e responsividade, seguir
  [references/widget-design-guide.md](references/widget-design-guide.md) — resume em
  que ordem decidir forma → cor → estilo, e o checklist antes de considerar um widget
  pronto.

## PE

Em uma instância PE real inspecionada, os bundles **de sistema** (Charts, Tables, SCADA
symbols, Maps, Gauges, Alarm widgets, etc. — cerca de 30 bundles) têm todos o mesmo
`tenantId` fixo `13814000-1dd2-11b2-8080-808080808080` (o "tenant" reservado que
representa escopo de sistema/sysadmin no ThingsBoard) — é um jeito confiável de
distinguir via API bundle de sistema vs. bundle customizado pelo tenant
(`GET /api/widgetsBundles`, comparar `tenantId` contra o `tenantId` do próprio usuário
logado, obtido em `GET /api/auth/user`). Bundles customizados aparecem com o `tenantId`
real do tenant. Ainda assim, confirmar disponibilidade de um bundle/widget específico na
galeria da instância antes de assumir que existe — a lista de bundles de sistema pode
variar por versão.

## Troubleshooting: widget "não atualiza" com telemetria nova

Tema recorrente no issue tracker oficial (várias issues distintas relatando o mesmo
sintoma — dados chegando no backend mas não aparecendo/atualizando no widget). Ordem de
verificação, da causa mais comum para a mais rara:

1. **Time window fixo no passado**: se o widget/dashboard está configurado com um
   intervalo `Range` (datas fixas) em vez de `Realtime`, dado novo cai fora da janela
   configurada por definição — não é bug, é comportamento correto de uma janela fixa.
   Checar se o Time Window realmente está em modo `Realtime`/`Last X` antes de investigar
   mais fundo.
2. **WebSocket bloqueado**: proxy/load balancer na frente do ThingsBoard sem suporte a
   upgrade de conexão HTTP→WebSocket derruba silenciosamente a subscription em tempo
   real — sintoma é a UI carregar normalmente mas nunca atualizar sozinha. Ver seção
   Troubleshooting da skill `tb-deploy-admin`.
3. **Widget custom sem `ctx.detectChanges()`**: se o `controllerScript` manipula o DOM
   ou estado manualmente fora do ciclo padrão do Angular (comum em widgets que fazem
   fetch adicional ou mutam `ctx.$scope` fora de `onDataUpdated`), a UI pode não
   re-renderizar sem uma chamada explícita — ver skill `tb-widgets-dashboards`, API do
   `ctx.detectChanges()`.
4. **Datasource/alias resolvendo para a entidade errada** (ou nenhuma): conferir se o
   entity alias do widget realmente resolve para o device que está enviando a
   telemetria — sintoma idêntico ("nada atualiza") mas causa totalmente diferente das
   anteriores.

## Referência

- [references/widget-controller-api.md](references/widget-controller-api.md) — mais
  detalhes da API do `ctx` e exemplos de `controllerScript`.
- [references/widget-design-guide.md](references/widget-design-guide.md) — como decidir
  forma/cor/estilo, specs de marca, dark mode, responsividade dentro do grid e
  acessibilidade para widgets custom.
