# Design de widgets: HTML/CSS/cor com critério, não no olho

> Fontes: doc oficial de Widget Development do ThingsBoard (Overview, Widget Patterns,
> Advanced Topics) + método de data visualization (princípios de escolha de forma,
> cor-por-função, specs de marca, acessibilidade) adaptado ao contexto de widget
> embutido em dashboard. Objetivo: um widget custom ficar visualmente consistente com o
> resto do ThingsBoard e ser lido corretamente por qualquer pessoa, não só "parecer bom".

## O que o ThingsBoard já te dá (não reinvente)

O editor de widget tem 3 abas de código + resources:

- **HTML**: suporta diretivas Angular (`*ngIf`, `*ngFor`, `[ngStyle]`, `[ngClass]`),
  componentes Angular Material (`mat-icon`, `mat-button`, `mat-select`), componentes do
  próprio ThingsBoard (`tb-icon`, `tb-time-series-chart-widget`) e **Tailwind CSS
  disponível nativamente** — para widgets simples, usar classes utilitárias Tailwind no
  HTML é mais rápido e consistente do que escrever CSS do zero.
- **CSS**: escopado automaticamente à instância do widget (não vaza para outros widgets
  nem para o dashboard). Tokens de cor do tema ficam disponíveis como CSS custom
  properties, ex. `var(--tb-primary-500)`, `var(--tb-primary-50)`.
- **Resources**: para gráficos mais elaborados que os widgets nativos não cobrem, a
  biblioteca padrão de facto do ecossistema ThingsBoard é o **ECharts** (adicionar a URL
  do CDN aqui) — antes de escrever visualização customizada em Canvas/SVG puro, verificar
  se ECharts já resolve, porque ganha de graça: tooltip, legenda, resize, e temas.

**Regra prática**: para 80% dos casos (cards, listas, toggles, formulários), Tailwind +
Angular Material + tokens de tema do próprio ThingsBoard já bastam — só desça para CSS
manual detalhado quando precisar de um layout/comportamento que eles não cobrem
(como os exemplos de abas/cards feitos com DOM manual vistos em produção).

## 1. Escolha a forma antes da cor

A decisão de "que tipo de widget/visualização" vem antes de qualquer decisão de cor —
a maioria dos widgets feios erra aqui, não na paleta.

| O dado é... | Use | Não use |
|---|---|---|
| Um valor atual (+ talvez uma tendência) | **Card/Latest Values** com valor + sparkline | Um gráfico de barra com uma barra só |
| Um punhado de números-chave | Fileira de cards (KPI row) | Gráfico de barras agrupado |
| O número que a tela inteira gira em torno | Um "hero number" grande (fonte grande, sem serifa) | — |
| Uma razão contra um limite (ex: nível de tanque) | **Meter/gauge** com a mesma rampa de cor do limite | Pizza de 2 fatias |
| Mais de ~7 categorias que importam individualmente | **Tabela** (ou tabela + gráfico) | Mais cores tentando distinguir tudo |
| Comparar magnitude entre entidades | Barra/coluna; **heatmap** se for uma grade | — |
| Tendência ao longo do tempo | Linha; área só se for 1 série | — |
| Distinguir séries diferentes | Linha múltipla / barra agrupada | — |
| Acima/abaixo de uma referência (delta a um alvo) | Barra divergente, ou linha vs. linha de base | — |
| Uma série é o ponto, o resto é contexto | **Emphasis**: 1 série na cor de destaque, resto cinza | Colorir todas as séries igualmente |

**Emphasis é o padrão mais subutilizado**: se a história é "esse sensor específico
subiu", a resposta certa quase nunca é "colorir todas as N séries" — é destacar a que
importa e apagar o resto para cinza. Reduz ruído visual sem perder contexto.

## 2. Cor por função, nunca por gosto

Toda cor num widget cumpre um de quatro papéis. Confundir os papéis é o erro mais comum:

| Papel | O que codifica | Como aplicar no ThingsBoard |
|---|---|---|
| **Categórico** | identidade (qual série/entidade) | ordem fixa de cores por série, nunca gerada/aleatória — se o widget já usa `dataKeys[].color` do datasource, mantenha essa mesma cor em qualquer visualização adicional da mesma chave |
| **Sequencial** | magnitude (quanto) | uma única cor, claro→escuro conforme o valor sobe — é o padrão seguro para heatmap/gauge |
| **Divergente** | polaridade (de que lado de uma referência) | duas cores (ex. azul/vermelho) + um cinza neutro no meio — nunca uma cor só "no meio" da escala |
| **Status** | estado (bom→crítico) | escala fixa reservada (ex. verde/amarelo/laranja/vermelho para severidade de alarme) — **nunca reaproveitar essas cores pra "série 4"**, e sempre acompanhar de ícone + texto, nunca só a cor |

Regras que valem sempre, independente da paleta escolhida:

- **Nunca dual-axis** (dois eixos Y na mesma área de plot) — é o erro nº 1 de gráfico.
  Duas grandezas de escalas diferentes viram dois gráficos, small multiples, ou uma
  normalizada para base comum.
- **Nunca "rainbow"** numa escala sequencial — uma cor só, do claro ao escuro.
- **Até 3 séries**: cor sozinha já é confortável de ler, com rótulo direto. **4+
  séries**: rótulo direto passa a ser obrigatório (cores próximas ficam ambíguas),
  e formas tipo scatter/mapa de calor com todos-contra-todos deveriam parar em ~3 séries
  simultâneas — o resto vira "Outros" ou small multiples.
- **Nunca gerar uma N-ésima cor "na hora"** para uma série extra — uma cor gerada
  aleatoriamente tende a ficar indistinguível de alguma já usada para quem tem
  daltonismo. Prefira agrupar em "Outros" ou usar small multiples/facetas.
- Cores de **status/alarme são reservadas** — se uma métrica representa "bom/ruim"
  (ex. dentro/fora do limite), ela usa a paleta de status do próprio ThingsBoard, não uma
  cor categórica arbitrária.

### Onde conferir contraste/acessibilidade

Não existe (neste projeto) um validador automático de paleta como o usado para
artifacts — na prática, para widgets do ThingsBoard:

- Texto sobre fundo: mínimo **4.5:1** de contraste para texto normal, **3:1** para texto
  grande (título/hero number) — WCAG AA. Ferramentas de contraste online resolvem uma
  checagem manual rápida.
- Nunca codificar informação **só** por cor — sempre ter um segundo canal: ícone, texto,
  padrão de traço (tracejado vs. sólido), ou posição. Isso cobre daltonismo E telas
  monocromáticas/impressão.
- Marca (mark) sobre fundo do gráfico: pelo menos **3:1** de contraste — se a cor "clara
  demais" do tema não alcançar isso, complementar com rótulo visível ou usar a
  visualização em tabela como alternativa.

## 3. Specs de marca (linhas, barras, marcadores)

Detalhes pequenos que fazem um gráfico parecer "profissional" em vez de "gráfico
default de biblioteca":

- Linhas finas (~2px), não grossas — linha grossa demais esconde a forma da série.
- Extremidades arredondadas em barras/áreas ancoradas na base, não retas cortadas.
- Marcadores (pontos) com pelo menos ~8px de área de toque — em widgets menores do
  dashboard isso importa mais, porque a área útil já é pequena.
- Ao empilhar barras/áreas, manter um pequeno espaçamento (2px) entre segmentos — sem
  isso, séries adjacentes se fundem visualmente e parecem uma coisa só.
- Grid/eixos devem ser recessivos (cinza claro, fino) — quem deve chamar atenção é o
  dado, não a grade.
- Rotulagem seletiva: rotular o ponto que importa (último valor, pico, valor abaixo do
  limite), não todo ponto da série — rótulo em todo ponto vira poluição visual.

## 4. Interatividade (hover/tooltip) por padrão

Um widget custom em HTML/JS é interativo por natureza — não tratar isso como opcional:

- Gráfico de linha/área: crosshair + tooltip acompanhando o cursor (ECharts já entrega
  isso de graça via `tooltip: { trigger: 'axis' }`).
- Barra/ponto/célula de tabela: tooltip por marca individual
  (`tooltip: { trigger: 'item' }` no ECharts).
- Área de clique/toque maior que o elemento visual, especialmente em widgets pequenos
  no grid do dashboard (dedo em tela touch é bem maior que um cursor de mouse).
- Widget realmente estático (ex. um "hero number" sem gráfico nenhum) é a única forma
  que dispensa hover — qualquer coisa com plot deveria ter tooltip.
- Para ações (drill-down, abrir dialog, RPC), usar o sistema nativo de **Actions** do
  ThingsBoard (ver seção "Actions" no SKILL.md) em vez de reinventar navegação manual —
  ele já cobre navegação entre states, abrir dialogs com template próprio, e comandos
  RPC/mobile.

## 5. Dark mode é um modo validado, não um filtro automático

O ThingsBoard tem dark mode nativo no dashboard. Duas formas de acompanhar, confirmadas
na doc oficial:

**CSS (preferível sempre que possível)** — usar os tokens de tema em vez de hex fixo, o
navegador/framework troca sozinho:
```css
.my-widget {
  color: var(--tb-primary-500);
  background: var(--tb-primary-50);
}
```

**JavaScript (necessário para bibliotecas externas como ECharts, que não seguem CSS
custom properties automaticamente)** — checar a classe do dashboard e escutar o evento
de troca de tema:
```javascript
function isDarkMode() {
  return document.querySelector('.tb-dashboard-page')?.classList.contains('dark') || false;
}

broadcastService.on('toggle-dark-mode', () => {
  let isDark = document.querySelector('.tb-dashboard-page')?.classList.contains('dark');
  // reconfigurar cores da lib externa (ex: option.textStyle.color) e chamar chart.setOption(option)
});
```

**Antes de dar o widget como pronto**: testar visualmente nos dois modos, não só um. Um
widget que só foi olhado no modo claro tipicamente quebra contraste no escuro (texto
escuro sobre fundo escuro é o erro mais comum) — não existe "inverte automático e já era
suficiente" quando há cores custom hardcoded misturadas com tokens de tema.

## 6. Responsividade dentro do grid do dashboard

Diferente de uma página web comum, um widget ThingsBoard vive dentro de uma célula de
grid redimensionável pelo usuário — o layout precisa reagir a isso, não assumir um
tamanho fixo:

- `width: 100%; height: 100%` no container raiz do widget, nunca px fixo.
- Implementar `self.onResize()` para redimensionar qualquer lib externa
  (`chart.resize()` no ECharts) — sem isso, o gráfico fica com o tamanho antigo até a
  próxima atualização de dados.
- Para widgets com múltiplos elementos (abas, cards, texto secundário), esconder
  progressivamente elementos secundários quando o widget for muito pequeno (ex. via
  `ResizeObserver` no container ou checando `ctx.width`/`ctx.height`) em vez de deixar
  o conteúdo transbordar ou virar ilegível.
- **Exceção**: widgets do tipo **SCADA Symbol** têm posição/tamanho fixos por design (não
  reagem ao viewport) — é a escolha certa para diagramas de processo/planta industrial
  onde o layout espacial em si é a informação, mas não é o padrão para o resto do
  dashboard.
- Preferir unidades relativas (`rem`, `%`, `clamp()`) a `px` fixo para tamanho de fonte,
  para o widget continuar legível tanto expandido em fullscreen quanto minimizado numa
  célula pequena do grid.

## 7. Tipografia e hierarquia

- Seguir a escala tipográfica do próprio tema do ThingsBoard (herdar `font-family` e
  pesos do dashboard) em vez de importar fonte própria — mantém consistência visual com
  o resto da UI e evita flash de fonte não carregada.
- Hierarquia clara em 3 níveis costuma bastar: título do widget (menor, cor secundária),
  valor principal (maior, cor primária/de destaque), rótulos/unidades (menor ainda, cor
  secundária/muted). Texto nunca deveria "competir" visualmente com o dado.
- Texto (rótulos, valores, legendas) usa sempre a cor de texto do tema (primária/
  secundária/muted) — a cor "de série"/categórica fica reservada para a marca (linha,
  barra, ponto) ao lado do texto, não para o texto em si. Texto colorido igual à série
  vira ruído e ainda quebra contraste em um dos dois modos de tema mais cedo ou mais tarde.

## Checklist rápido antes de considerar um widget pronto

1. A forma escolhida é a mais simples que resolve o job do dado (não virou gráfico só
   porque "parece mais chique")?
2. Cada cor usada tem um papel claro (categórica/sequencial/divergente/status) e nenhuma
   invade o papel da outra?
3. Testei nos dois temas (claro e escuro), não só um?
4. Testei redimensionando o widget bem pequeno e bem grande/fullscreen?
5. Tem hover/tooltip em qualquer coisa que seja um plot de dados?
6. Nenhuma informação depende só de cor — tem ícone/texto/padrão como reforço?
7. Textos e marcas usam os tokens de tema (`var(--tb-*)`) em vez de hex fixo, exceto
   onde uma cor de série específica é intencional?
