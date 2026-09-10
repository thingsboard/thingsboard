# TWmonitor — White-label do ThingsBoard CE

Este documento registra tudo que foi feito para transformar este fork do
[ThingsBoard Community Edition](https://github.com/thingsboard/thingsboard) na versão
com a marca **TWmonitor** (Tecwise). Serve como referência para quem for continuar o
trabalho, revisar o que foi feito, ou reproduzir o ambiente de desenvolvimento.

## Contexto

- **Objetivo**: white-label do ThingsBoard CE com a marca TWmonitor/Tecwise. Já
  verificado internamente que isso é permitido pela licença Apache 2.0 do ThingsBoard
  (é preciso manter os avisos de copyright/licença nos arquivos-fonte, mas o nome do
  produto, logos e cores exibidos ao usuário final podem ser trocados).
- **Descoberta importante**: a feature de *White Labeling* dinâmico do ThingsBoard
  (trocar logo/paleta/título via tela, sem mexer em código) é **exclusiva da Professional
  Edition** — não existe nenhuma classe `WhiteLabel*` no código-fonte do CE público.
  Por isso, todo o trabalho aqui é feito por **edição direta do código-fonte** (fork
  mantido), não por configuração em runtime.
- **Repositórios**:
  - `origin` → https://github.com/Samuelvieria/ThigsBoardCE-TWmonitor (fork próprio)
  - `upstream` → https://github.com/thingsboard/thingsboard (oficial, para trazer
    atualizações futuras com `git fetch upstream && git merge upstream/master`)
  - Branch de trabalho: `twmonitor-whitelabel`

## Paleta de cores oficial

| Uso | Cor | Hex |
|---|---|---|
| Azul primário (tema/UI) | 🟦 | `#00316D` |
| Cinza secundário (tema/UI) | ⬜ | `#757575` |
| Azul do ícone/logo (extraído dos arquivos de marca fornecidos) | 🟦 | `#006EB3` |
| Amarelo de destaque (usado só na tela de login, link "esqueci senha") | 🟨 | `#F4AC20` |

> Nota: o texto "TWmonitor" desenhado nos logos SVG usa `#006EB3` (a cor real extraída
> do ícone do mapa da América Latina fornecido pela empresa), que é ligeiramente
> diferente do azul de tema `#00316D`. É intencional por ora — logo colorido +
> UI mais sóbria é um padrão comum — mas pode ser unificado se o time de design pedir.

## O que foi alterado (arquivo por arquivo)

### Frontend (`ui-ngx/`)

| Arquivo | O que mudou |
|---|---|
| `src/scss/constants.scss` | `$tb-primary-color`, `$tb-secondary-color`, `$tb-hue3-color` → paleta TWmonitor |
| `src/environments/environment.ts` e `.prod.ts` | `appTitle: 'TWmonitor'`, `defaultLang: 'pt_BR'` |
| `src/index.html` | `<title>TWmonitor</title>`, favicon → `favicon.svg` |
| `src/favicon.svg` (novo) | Ícone do mapa da América Latina, usado como favicon |
| `src/assets/twmonitor_logo.svg` (novo) | Logo completo (ícone azul + texto "TWmonitor"), usado no sidenav expandido |
| `src/assets/twmonitor_logo_collapsed.svg` (novo) | Só o ícone, usado no sidenav colapsado (20×20px) |
| `src/assets/twmonitor_logo_white.svg` (novo) | Logo em branco (lockup completo com ícone geométrico), usado na tela de login — o card de login usa a cor primária como fundo, então precisa da variante branca |
| `src/app/modules/home/home.component.ts` | `logo`/`collapsedLogo` apontando pros novos SVGs |
| `src/app/modules/home/components/dashboard-page/dashboard-page.component.ts` | `defaultDashboardLogo` idem |
| `src/app/shared/components/logo.component.ts` | `src` default → `twmonitor_logo.svg` |
| `src/app/modules/login/pages/login/login.component.html` | `<tb-logo>` usa a variante branca; link do logo aponta pra `www.tecwise.com.br` (em vez de thingsboard.io) |
| `src/app/modules/login/pages/login/login.component.scss` | Visual novo completo da tela de login (ver seção própria abaixo) |
| `src/app/modules/home/home.component.html` | Removido `<tb-github-badge>` (apontava pro repo oficial do ThingsBoard) |
| `src/app/modules/home/components/dashboard-page/dashboard-page.component.html` | Idem |
| `application/src/main/resources/templates/*.ftl` (10 arquivos) | Texto "Thingsboard"/"ThingsBoard" → "TWmonitor" nos e-mails transacionais (ativação, reset de senha, 2FA, avisos de limite de API, etc.). **Os cabeçalhos de licença Apache no topo de cada arquivo foram preservados intencionalmente.** |
| `ui-ngx/angular.json` | Adicionado `src/favicon.svg` no array `assets` (sem isso o favicon novo dava 404 — só `thingsboard.ico` estava listado) |

### Tela de login — visual novo

Fornecido pelo time (paleta `#006caf #939196 #f4ac20 #00326f #1e120d #ffffff`),
aplicado em `login.component.scss` dentro de um bloco `::ng-deep` (padrão já usado
nas outras telas de login deste projeto, necessário porque os componentes internos
do Angular Material — `mat-form-field`, `mat-progress-bar`, etc. — têm encapsulamento
de estilo próprio e não são alcançados por CSS escopado comum):

- Fundo com gradiente azul + textura de grid sutil
- Card com efeito *glassmorphism* (fundo semi-transparente + `backdrop-filter: blur`)
- Faixa gradiente no topo do card
- Campos de input com fundo azulado translúcido e anel ciano no foco
- Botão "Entrar": inicialmente gradiente azul, **depois trocado para gradiente cinza**
  (`#a8a6aa → #939196 → #6e6c70`) a pedido
- Link "Esqueceu sua senha?" em amarelo (`#f4ac20`)
- Logo aumentado de 280×60px (padrão ThingsBoard) para **400×86px**

Ponto de atenção corrigido durante o desenvolvimento: o CSS fornecido originalmente
tinha o seletor `.tb-action-button button[mat-raised-button]`, mas o botão real do
template usa `mat-flat-button` com a classe `tb-action-button` diretamente no
`<button>` (não um `<button mat-raised-button>` dentro de um container). O seletor foi
ajustado para `.tb-action-button` puro.

### Idioma

`defaultLang` mudou de `en_US` para `pt_BR`. A maior parte da UI já aparecia em
português por causa da detecção automática do idioma do navegador, mas isso garante
que pt_BR seja o padrão real do produto independente do navegador do usuário.

**Achado não corrigido ainda**: vários itens de menu aparecem com a chave de tradução
"crua" em vez do texto (`monitor.monitor`, `entity.devices-and-assets`,
`iot-hub.iot-hub`, `customer.customers-and-users`, `entity.data-processing`,
`image.images`, `javascript.scripts`, `resource.files`, `admin.platform`,
`mobile.mobile-apps`, `notification.notifications`) — são traduções que **faltam no
arquivo `locale.constant-pt_BR.json` do próprio ThingsBoard**, não algo introduzido
por este trabalho. Confirmado via warnings no console do navegador
(`Translation for 'X' doesn't exist`). Pendente de decisão: completar essas traduções.

## O que NÃO foi mexido (decisões conscientes)

- **`helpBaseUrl`** (em `ui-ngx/src/app/shared/models/constants.ts`) continua apontando
  pra `thingsboard.io` — os links de ajuda contextual (ícones "?") ainda vão pra
  documentação oficial do ThingsBoard. Mantido de propósito: a documentação é
  funcionalmente válida (mesmas APIs/features do CE) e trocar sem ter um mirror próprio
  quebraria todos os links.
- **`docker/.env`** (`DOCKER_REPO` etc.) não foi alterado — ainda aponta pra imagens
  oficiais do Docker Hub. Só faz sentido mudar quando houver um registry próprio com
  imagens buildadas a partir deste fork.
- **`$tb-dark-primary-color`** e **`$tb-primary-color-light`** (usados no tema escuro)
  continuam com os valores padrão do Material (stock Indigo) — baixo risco/baixa
  visibilidade, não ajustados ainda.

## Ambiente de desenvolvimento local

### O que está instalado/configurado nesta máquina

- **Node.js** (já vinha instalado) — necessário pro `ui-ngx`.
- **Apache Maven 3.9.9**, instalado manualmente (sem precisar de admin) em
  `apache-maven-3.9.9/` na raiz do projeto (fora do repo git). Necessário pra rodar o
  backend Java a partir do código-fonte.
- **Java 21** (JetBrains Runtime, via Android Studio) — já estava disponível.

### O que falta pra rodar o stack completo (backend + banco)

**Não instalado** por falta de privilégio de administrador no ambiente onde este
trabalho foi feito (`net session` retornou "Acesso negado", e não há WSL2 instalado):

- **PostgreSQL** (nativo, ou via Docker) — banco de dados do ThingsBoard.
- **Docker Desktop** — exigiria primeiro habilitar WSL2 (`wsl --install`), que precisa
  de admin **e reinicialização do Windows**.

Recomendação: instalar o PostgreSQL nativo para Windows (não exige WSL2/reboot, só
elevação do instalador) e então rodar o backend via
`mvn spring-boot:run` (ou equivalente) no módulo `application`, usando o Maven já
disponível em `apache-maven-3.9.9/bin/mvn`.

### `npm install` neste projeto: usar Yarn, não npm

O repositório tem um `yarn.lock` versionado — **o fluxo oficial é `yarn install`**,
não `npm install`. Isso foi descoberta durante este trabalho: `npm install` (mesmo com
`--legacy-peer-deps`) resolve incorretamente duas dependências git do próprio
`package.json` do ThingsBoard —

```
"flot": "https://github.com/thingsboard/flot.git#0.9-work"
"ngx-flowchart": "https://github.com/thingsboard/ngx-flowchart.git#release/4.1.0"
```

O `package-lock.json` gerado por `npm` (que não é o lockfile oficial do projeto) tinha
entradas `"resolved": "git+ssh://git@github.com/thingsboard/flot.git"` **sem
branch/commit fixado**, fazendo o npm baixar a branch padrão (errada) desses repositórios
em vez da branch pedida em `package.json`. Isso quebrava o build do `ng serve`
(`Could not resolve "flot/src/jquery.flot.js"` e `Could not resolve "ngx-flowchart"`).
Correção aplicada localmente (não commitada, pois é specific de ter usado `npm` em vez de
`yarn`):

```bash
rm -rf node_modules/flot node_modules/ngx-flowchart
npm install --legacy-peer-deps \
  "flot@https://github.com/thingsboard/flot.git#0.9-work" \
  "ngx-flowchart@https://github.com/thingsboard/ngx-flowchart.git#release/4.1.0"
```

Se o time seguir usando `yarn install` (o correto), esse problema não deve ocorrer.

### Rodando só o frontend (`ng serve`)

```bash
cd ui-ngx
yarn install   # ou o workaround de npm acima
node --max_old_space_size=8048 ./node_modules/@angular/cli/bin/ng.js serve \
  --configuration development --host 0.0.0.0 --port 4200
```

O `proxy.conf.js` já existente no projeto redireciona `/api`, `/oauth2` e o WebSocket
`/api/ws` para `http://localhost:8080` (onde o backend Java deveria estar rodando).
Sem backend, a tela de **login** renderiza normalmente (é só HTML/CSS/Angular, não
depende de API pra desenhar a tela) — dá pra validar logo/cores/layout direto assim.

### Visualizando o sidenav/home sem backend real (`dev-tools/mock-backend-preview.js`)

Pra ver as telas *depois* do login (sidenav, menu, home) sem precisar subir
PostgreSQL/Java, foi criado um backend HTTP mínimo que responde só o suficiente pra
passar pela autenticação do Angular:

```bash
node ui-ngx/dev-tools/mock-backend-preview.js
```

Isso sobe na porta 8080 e imprime no terminal um trecho de código para colar no
Console do navegador (F12) em `http://localhost:4200`. O trecho seta um JWT
"fake" (com claims plausíveis, mas sem assinatura válida) e as chaves de expiração no
`localStorage`, depois redireciona para `/home`.

**Por que isso funciona**: o `AuthGuard` do Angular decodifica o JWT localmente (sem
verificar assinatura — isso é papel do backend real numa requisição de verdade) via
`angular-jwt`. Contanto que o token não esteja expirado, ele só faz duas chamadas reais
à API antes de liberar a navegação: `GET /api/user/{userId}` e `GET /api/system/params`.
O mock server responde essas duas com dados plausíveis (usuário "Preview TWmonitor",
tenant admin) e qualquer outra chamada com `200 {}` (fallback permissivo, pra não
travar a UI com erro de rede) — dados reais (dispositivos, dashboards, telemetria) não
existem, então widgets/listas aparecem vazios, e o WebSocket de tempo real vai dar erro
de conexão (esperado, sem backend real).

**Isso é só uma ferramenta de desenvolvimento/preview** — não usar como substituto de
autenticação real em nenhum ambiente compartilhado.

## Como continuar o trabalho de white-label

Itens identificados mas não resolvidos:

1. **Completar traduções pt_BR faltando** nos itens de menu (ver lista acima).
2. **Decidir sobre `helpBaseUrl`**: manter apontando pro thingsboard.io, ou criar/linkar
   uma documentação própria.
3. **Alinhar a cor do texto do logo** (`#006EB3`) com o azul de tema (`#00316D`), se o
   design pedir consistência total.
4. **Publicar imagem Docker própria**: buildar a partir deste fork e publicar num
   registry próprio (hoje `docker/.env` ainda aponta pro Docker Hub oficial do
   ThingsBoard).
5. **Configurar backend + banco completos** (local ou em servidor) para testar o
   produto de ponta a ponta com dados reais — ver seção "Ambiente de desenvolvimento".
6. **Badge do GitHub removido, mas não há substituto** — avaliar se faz sentido um link
   de suporte/contato da Tecwise no lugar.
7. Revisar os demais pontos já mapeados mas não alterados nesta rodada: Swagger
   (`SWAGGER_TITLE` etc., configurável via variável de ambiente sem editar código),
   nome do pacote npm (`ui-ngx/package.json`, cosmético/interno), nomes de imagem
   Docker (`docker/.env`).

## Skill de referência

O repositório https://github.com/Samuelvieria/ThingsBoard-Skill (skills de Claude Code
para ThingsBoard) foi consultado no início deste trabalho. Nenhuma das 4 skills
existentes (`tb-rest-api`, `tb-rule-engine`, `tb-widgets-dashboards`, `tb-deploy-admin`)
cobre white-labeling — é um gap identificado. Uma 5ª skill (`tb-whitelabel`) documentando
o que está neste arquivo poderia ser adicionada lá seguindo o mesmo padrão
(`SKILL.md` + `references/`).
