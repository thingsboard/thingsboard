# API do controller de widget — detalhes e exemplos

## Exemplo mínimo de `controllerScript` (latest values)

```javascript
self.onInit = function() {
    var ctx = self.ctx;
    ctx.$container.html('<div class="tb-value">--</div>');
};

self.onDataUpdated = function() {
    var ctx = self.ctx;
    if (ctx.data && ctx.data.length > 0) {
        var latest = ctx.data[0].data[ctx.data[0].data.length - 1];
        var value = latest ? latest[1] : '--';
        ctx.$container.find('.tb-value').text(value);
    }
};

self.onResize = function() {};
self.onDestroy = function() {};
```

## Exemplo de widget de controle (RPC)

```javascript
self.onInit = function() {
    var ctx = self.ctx;
    ctx.$container.find('.tb-toggle-btn').on('click', function() {
        var newState = !self.currentState;
        ctx.controlApi.sendOneWayCommand('setState', { state: newState })
            .then(function() {
                self.currentState = newState;
                ctx.detectChanges();
            })
            .catch(function(err) {
                console.error('RPC falhou', err);
            });
    });
};
```

## Formato de `ctx.data`

Cada item de `ctx.data` corresponde a um par (datasource, dataKey):

```javascript
{
  datasource: { entityId, entityName, entityType, ... },
  dataKey: { name: 'temperature', type: 'timeseries', color: '#...', ... },
  data: [ [timestampMs, value], [timestampMs, value], ... ]
}
```

Para "latest values", `data` normalmente tem um único par `[ts, value]` (o mais recente).

## `settingsSchema` (formato legado) — exemplo

> Formato antigo (AngularJS schema-form). Widgets recentes usam `settingsForm`, um
> array — ver exemplo no `SKILL.md` principal desta skill. Este formato ainda pode
> aparecer em widgets/bundles mais antigos exportados de instâncias já existentes.

```json
{
  "schema": {
    "type": "object",
    "properties": {
      "threshold": { "title": "Threshold de alerta", "type": "number", "default": 30 },
      "showLabel": { "title": "Mostrar rótulo", "type": "boolean", "default": true }
    },
    "required": ["threshold"]
  },
  "form": ["threshold", "showLabel"]
}
```

E o correspondente em `defaultConfig`:

```json
{
  "settings": { "threshold": 30, "showLabel": true },
  "datasources": [],
  "title": "Meu Widget"
}
```

## Entity alias — exemplo de filtro por tipo de device

```json
{
  "id": "alias-uuid",
  "alias": "Sensores de Temperatura",
  "filter": {
    "type": "entityType",
    "resolveMultiple": true,
    "entityType": "DEVICE",
    "entityTypeFilters": ["Temperature Sensor"]
  }
}
```

## Notas de performance

- `ctx.data` já vem resolvido pelo framework do dashboard — evitar refazer chamadas REST
  manuais dentro do controller para os mesmos dados; usar `ctx.data` sempre que possível.
- Para widgets que precisam de dados adicionais fora do datasource padrão, usar
  `ctx.http` (wrapper interno de `$http`/API) em vez de `fetch`/`XMLHttpRequest` direto,
  para herdar automaticamente auth/headers da sessão do dashboard.

## Integração com biblioteca externa de gráfico (ECharts) — confirmado na doc oficial

Padrão recomendado para visualizações que os widgets nativos não cobrem: adicionar a URL
do CDN na aba **Resources** do editor (ex.
`https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js`) e seguir o ciclo de vida:

```javascript
var chart;

self.onInit = function() {
  var element = self.ctx.$container[0].querySelector('#my-chart');
  chart = echarts.init(element);
  chart.setOption(option);
};

self.onDataUpdated = function() {
  if (self.ctx.data.length && self.ctx.data[0].data.length) {
    var newValue = self.ctx.data[0].data[0][1];
    option.series.forEach(function(s) { s.data[0].value = newValue; });
    chart.setOption(option);
  }
};

self.onResize = function() {
  chart.resize();                 // essencial — sem isso o gráfico não acompanha o grid
};

self.onDestroy = function() {
  chart.clear();                  // libera memória ao remover o widget
};
```

```css
.chart-wrapper { width: 100%; height: 100%; }
#my-chart { width: 100%; height: 100%; }
```

Bibliotecas externas **não seguem os tokens de tema CSS automaticamente** — para dark
mode, escutar o evento de troca de tema e reconfigurar cores manualmente (ver
[references/widget-design-guide.md](widget-design-guide.md), seção "Dark mode").

## Comunicação entre widgets

Widgets no mesmo dashboard podem trocar eventos via `broadcastService` (útil para um
widget "mestre" atualizar um ou mais widgets "companion" sem depender só de entity
alias/state):

```javascript
self.onInit = function() {
  var $injector = self.ctx.$scope.$injector;
  var broadcastService = $injector.get(self.ctx.servicesMap.get('broadcastService'));

  broadcastService.on('my-custom-event', function(event, data) {
    self.ctx.$scope.receivedData = data;
    self.ctx.detectChanges();
  });
};
```

## Árvore de entidades via relações (padrão comum para hierarquia)

Consulta recursiva de relações para montar visualização em árvore (ex: tenant → site →
equipamento → sensor):

```javascript
var relationService = $injector.get(self.ctx.servicesMap.get('entityRelationService'));

var query = {
  filters: [{ relationType: 'Contains', entityTypes: [] }],
  parameters: {
    rootId: rootEntity.id.id,
    rootType: rootEntity.id.entityType,
    direction: 'FROM',
    maxLevel: 10
  }
};

relationService.findInfoByQuery(query).subscribe(function(relations) {
  buildTree(relations, rootEntity);
});
```

No HTML, usar `ngTemplateOutlet` recursivo para renderizar níveis arbitrários de
profundidade sem duplicar template por nível — ver "Widget Patterns" na doc oficial do
ThingsBoard para o exemplo completo de template + toggle expandir/colapsar.

## Embutir um dashboard state inteiro num diálogo

Útil para drill-down ou wizard multi-etapa sem sair do widget atual:

```html
<tb-dashboard-state
  [stateId]="selectedStateId"
  [entityId]="selectedEntityId"
  [entityName]="selectedEntityName">
</tb-dashboard-state>
```
