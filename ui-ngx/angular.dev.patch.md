# Konfiguracja `fast` dla `ui-ngx/angular.json`

Nie nadpisuje niczego istniejącego — dodaje **nową** konfigurację obok `development`
i `production`. Build produkcyjny pozostaje bez zmian.

## 1. `projects.thingsboard.architect.build.configurations` — dodaj wpis `fast`

Wstaw ten obiekt obok istniejącego `"development"`:

```json
"fast": {
  "optimization": false,
  "extractLicenses": false,
  "namedChunks": false,
  "vendorChunk": true,
  "buildOptimizer": false,
  "budgets": [],
  "sourceMap": {
    "scripts": true,
    "styles": false,
    "vendor": false
  },
  "outputHashing": "none",
  "progress": true,
  "statsJson": false,
  "define": {
    "ngDevMode": "true"
  }
}
```

Dlaczego to działa: `sourceMap.styles: false` wyłącza najdroższy etap dev builda w tym
projekcie — mapy źródeł dla ~18 globalnych arkuszy plus SCSS-y komponentów.
`budgets: []` usuwa analizę rozmiarów bundle'a przy każdej przebudowie.

## 2. `architect.serve.configurations` — dodaj wpis `fast`

```json
"fast": {
  "buildTarget": "thingsboard:build:fast",
  "prebundle": {
    "exclude": []
  }
}
```

`prebundle` (esbuild dependency pre-bundling) jest w Angular 20 domyślnie włączony i to
on odpowiada za różnicę „cold vs warm start". Cache trafia do `.angular/cache`, dlatego
compose trzyma ten katalog w wolumenie nazwanym.

## 3. Assets: przenieś TinyMCE i MDI z kopiowania per-build

W `architect.build.options.assets` te dwa wpisy kopiują **tysiące małych plików** przy
każdym cold starcie (`node_modules/tinymce/**/*` to ~3000 plików, `@mdi/svg/svg/*.svg`
to ~7500 plików). Na Docker Desktop/Windows to dziesiątki sekund.

Usuń je z listy `assets` **tylko dla konfiguracji `fast`**, dodając w sekcji `fast`
z punktu 1:

```json
"assets": [
  "src/thingsboard.ico",
  "src/assets",
  { "glob": "worker-html.js",       "input": "./node_modules/ace-builds/src-noconflict/", "output": "/" },
  { "glob": "worker-xml.js",        "input": "./node_modules/ace-builds/src-noconflict/", "output": "/" },
  { "glob": "worker-css.js",        "input": "./node_modules/ace-builds/src-noconflict/", "output": "/" },
  { "glob": "worker-json.js",       "input": "./node_modules/ace-builds/src-noconflict/", "output": "/" },
  { "glob": "worker-javascript.js", "input": "./node_modules/ace-builds/src-noconflict/", "output": "/" },
  { "glob": "worker-tbel.js",       "input": "./src/app/shared/models/ace/tbel/",         "output": "/" },
  { "glob": "marker-icon-2x.png",   "input": "node_modules/leaflet/dist/images/",         "output": "/" },
  { "glob": "marker-icon.png",      "input": "node_modules/leaflet/dist/images/",         "output": "/" },
  { "glob": "marker-shadow.png",    "input": "node_modules/leaflet/dist/images/",         "output": "/" }
]
```

a TinyMCE i MDI podaj dev serverowi jako statyczne katalogi przez proxy — dopisz do
`ui-ngx/proxy.conf.dev.js`:

```js
// serwuj ciezkie assety wprost z node_modules, bez kopiowania przy kazdym buildzie
const path = require('path');
PROXY_CONFIG['/assets/tinymce'] = {
  target: 'http://localhost:4200',
  bypass: (req, res) => {
    const rel = req.url.replace(/^\/assets\/tinymce\/?/, '');
    res.sendFile(path.resolve('node_modules/tinymce', rel));
    return true;
  },
};
PROXY_CONFIG['/assets/mdi'] = {
  target: 'http://localhost:4200',
  bypass: (req, res) => {
    const rel = req.url.replace(/^\/assets\/mdi\/?/, '');
    res.sendFile(path.resolve('node_modules/@mdi/svg/svg', rel));
    return true;
  },
};
```

Jeżeli wolisz prostsze rozwiązanie: jednorazowo zrób symlink
`ln -s ../node_modules/tinymce src/assets/tinymce` i
`ln -s ../node_modules/@mdi/svg/svg src/assets/mdi`, a wpisy z `assets` usuń.
Symlink jest kopiowany raz, nie per-build.

## 4. Globalne CSS z `node_modules` → `index.html` (tylko dev)

W konfiguracji `fast` skróć `styles` do plików projektu:

```json
"styles": [
  "src/styles.scss",
  "src/form.scss",
  "src/app/modules/home/components/widget/lib/maps/map.scss",
  "src/app/modules/home/components/widget/lib/maps-legacy/markers.scss",
  "src/app/modules/home/components/widget/lib/home-page/home-page.scss"
]
```

a pozostałe (gotowe, zminifikowane CSS z `node_modules`, które nigdy się nie zmieniają)
wstaw raz do `src/index.html` wewnątrz warunku dev — dev server serwuje je z
`node_modules` przez proxy z punktu 3 albo z `src/assets/vendor` po jednorazowym
skopiowaniu:

```html
<!-- dev-only vendor css: nie przechodzi przez pipeline Sass przy kazdym rebuildzie -->
<link rel="stylesheet" href="/assets/vendor/jquery.terminal.min.css">
<link rel="stylesheet" href="/assets/vendor/tooltipster.bundle.min.css">
<link rel="stylesheet" href="/assets/vendor/tooltipster-sideTip-shadow.min.css">
<link rel="stylesheet" href="/assets/vendor/jstree-proton.min.css">
<link rel="stylesheet" href="/assets/vendor/leaflet.css">
<link rel="stylesheet" href="/assets/vendor/MarkerCluster.css">
<link rel="stylesheet" href="/assets/vendor/MarkerCluster.Default.css">
<link rel="stylesheet" href="/assets/vendor/leaflet-geoman.css">
<link rel="stylesheet" href="/assets/vendor/prism.css">
<link rel="stylesheet" href="/assets/vendor/prism-line-numbers.css">
<link rel="stylesheet" href="/assets/vendor/ace-diff.min.css">
<link rel="stylesheet" href="/assets/vendor/photoswipe.css">
```

Jednorazowe skopiowanie (dodaj jako `npm run dev:vendor-css`):

```bash
mkdir -p src/assets/vendor
cp node_modules/jquery.terminal/css/jquery.terminal.min.css \
   node_modules/tooltipster/dist/css/tooltipster.bundle.min.css \
   node_modules/tooltipster/dist/css/plugins/tooltipster/sideTip/themes/tooltipster-sideTip-shadow.min.css \
   node_modules/leaflet/dist/leaflet.css \
   node_modules/leaflet.markercluster/dist/MarkerCluster.css \
   node_modules/leaflet.markercluster/dist/MarkerCluster.Default.css \
   node_modules/@geoman-io/leaflet-geoman-free/dist/leaflet-geoman.css \
   node_modules/prismjs/themes/prism.css \
   node_modules/prismjs/plugins/line-numbers/prism-line-numbers.css \
   node_modules/ace-diff/dist/ace-diff.min.css \
   node_modules/photoswipe/dist/photoswipe.css \
   src/assets/vendor/
cp node_modules/jstree-bootstrap-theme/dist/themes/proton/style.min.css src/assets/vendor/jstree-proton.min.css
```

## 5. `src/tsconfig.app.json` — build incremental

```json
{
  "compilerOptions": {
    "incremental": true,
    "tsBuildInfoFile": "../.angular/cache/tsconfig.app.tsbuildinfo",
    "skipLibCheck": true
  }
}
```

`skipLibCheck: true` pomija typecheck plików `.d.ts` z `node_modules` — w projekcie
tej wielkości to zauważalna część czasu TypeScriptu.

## 6. `package.json` — nowe skrypty

```json
"start:fast": "node --max_old_space_size=4096 ./node_modules/@angular/cli/bin/ng serve --configuration fast --host 0.0.0.0 --hmr",
"dev:vendor-css": "bash ./dev-vendor-css.sh",
"bench": "bash ../dev/bench.sh"
```

Zwróć uwagę: `--max_old_space_size` spada z 8048 na 4096. Ośmiogigabajtowy heap nie
przyspiesza builda, a wydłuża start V8 i zwiększa pauzy GC przy dużej liczbie żywych
obiektów.

## Czego świadomie NIE zmieniamy

- Builder pozostaje `@angular-builders/custom-esbuild:application` — pluginy z
  `./esbuild/tb-esbuild-plugins.ts` są potrzebne do builda i nie ma powodu ich obchodzić.
- `production` i `development` bez zmian, więc CI i `build:prod` działają identycznie.
- `stylePreprocessorOptions.includePaths` i `silenceDeprecations` zostają — usunięcie
  zepsułoby kompilację SCSS.
