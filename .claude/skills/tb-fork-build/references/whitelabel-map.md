# Mapa de white-label do ThingsBoard CE

<!-- PROVENANCE:BEGIN (gerado por scripts/verify-fork-paths.mjs — não editar à mão) -->
> **Proveniência fixada.** Todos os caminhos abaixo verificados contra
> [`thingsboard/thingsboard`](https://github.com/thingsboard/thingsboard/tree/a488a4c138971c0264bf7fdc879871f68eead1e4),
> ref **`v4.3.1.4`**, commit **`a488a4c13897`**, em **2026-09-04**.
> 26 caminhos conferidos contra a árvore real do repositório.
>
> Reverificar noutra versão: `node scripts/verify-fork-paths.mjs --ref vX.Y.Z`.
> Conferir contra o seu fork: `--repo <owner>/<fork> --ref <branch>`.
<!-- PROVENANCE:END -->

Todo caminho desta tabela é checado por `scripts/verify-fork-paths.mjs`, que falha se
algum deixar de existir na ref alvo. Caminho de arquivo inventado é a alucinação mais cara
em trabalho de fork: parece certo, e só falha depois de um build de dezenas de minutos.

## Identidade visual

| Elemento | Arquivo | Nota |
|---|---|---|
| Paleta / cores base | `ui-ngx/src/scss/constants.scss` | define `$tb-primary-color` e variantes; é a raiz da cor |
| Tema Angular Material | `ui-ngx/src/theme.scss` | monta a palette a partir das constantes (`mat.m2-define-palette`) |
| Ajustes sobre o tema | `ui-ngx/src/theme-overwrites.scss` | ponto de override sem tocar no tema base — **prefira aqui** |
| Estilos globais | `ui-ngx/src/styles.scss` | primeiro item de `styles` no `angular.json` |
| Constantes de layout | `ui-ngx/src/scss/constants.scss` | alturas de toolbar, breakpoints |
| Mixins SCSS | `ui-ngx/src/scss/mixins.scss` | |
| Fontes | `ui-ngx/src/scss/fonts.scss` | |

## Logo, ícone e título

| Elemento | Arquivo | Nota |
|---|---|---|
| Componente de logo | `ui-ngx/src/app/shared/components/logo.component.ts` | referencia o asset; trocar só o SVG não basta |
| Template do logo | `ui-ngx/src/app/shared/components/logo.component.html` | |
| Estilo do logo | `ui-ngx/src/app/shared/components/logo.component.scss` | tamanho/posição no header |
| Título da aba | `ui-ngx/src/index.html` | `<title>ThingsBoard</title>` |
| Favicon | `ui-ngx/src/index.html` + `ui-ngx/src/thingsboard.ico` | o `.ico` está listado nos `assets` do `angular.json` pelo nome — renomear exige editar os dois |
| Registro de assets | `ui-ngx/angular.json` | `projects.thingsboard.architect.build.options.assets` |
| Assets próprios | `ui-ngx/src/assets/` | destino dos SVG da marca |

## Telas

| Elemento | Arquivo |
|---|---|
| Login (markup) | `ui-ngx/src/app/modules/login/pages/login/login.component.html` |
| Login (estilo) | `ui-ngx/src/app/modules/login/pages/login/login.component.scss` |
| Shell / header | `ui-ngx/src/app/modules/home/home.component.html` |
| Shell (estilo) | `ui-ngx/src/app/modules/home/home.component.scss` |
| Menu lateral (estilo) | `ui-ngx/src/app/modules/home/menu/side-menu.component.scss` |
| Toggle do menu | `ui-ngx/src/app/modules/home/menu/menu-toggle.component.scss` |
| Página de dashboard | `ui-ngx/src/app/modules/home/components/dashboard-page/dashboard-page.component.html` |

## Itens de menu

| Elemento | Arquivo | Nota |
|---|---|---|
| Definição dos itens | `ui-ngx/src/app/core/services/menu.models.ts` | remover entrada aqui some com o item da barra lateral |
| Montagem por papel | `ui-ngx/src/app/core/services/menu.service.ts` | quem monta o menu por authority (sysadmin / tenant / customer) |

Remover um item do menu **não** remove a rota. Se o objetivo é bloquear acesso e não só
esconder, a rota também precisa sair.

## Textos e traduções

| Elemento | Arquivo | Nota |
|---|---|---|
| Português | `ui-ngx/src/assets/locale/locale.constant-pt_BR.json` | **já existe** — é revisão, não tradução do zero |
| Inglês (referência) | `ui-ngx/src/assets/locale/locale.constant-en_US.json` | use para achar a chave a sobrescrever |

## E-mails (backend, não é `ui-ngx`)

Todos em `application/src/main/resources/templates/`:

| Arquivo | Quando dispara |
|---|---|
| `activation.ftl` | ativação de conta |
| `account.activated.ftl` | confirmação de ativação |
| `reset.password.ftl` | pedido de redefinição |
| `password.was.reset.ftl` | confirmação de redefinição |
| `account.lockout.ftl` | conta bloqueada |
| `2fa.verification.code.ftl` | código de 2FA |
| `state.enabled.ftl` / `state.disabled.ftl` / `state.warning.ftl` | mudança de estado |
| `test.ftl` | e-mail de teste da config de SMTP |

É a parte de white-label mais esquecida: a UI fica 100% na marca nova e o e-mail de
ativação continua dizendo ThingsBoard.

## Checklist de conclusão

- [ ] Cores: `constants.scss` e `theme.scss` (ou `theme-overwrites.scss`)
- [ ] Logo: componente + assets + `angular.json`
- [ ] Favicon e `<title>` no `index.html`
- [ ] Login redesenhado
- [ ] Menu lateral: itens removidos em `menu.models.ts` **e** rotas correspondentes
- [ ] `locale.constant-pt_BR.json` revisado; idioma padrão definido
- [ ] Os 10 templates `.ftl` de e-mail
- [ ] Links externos expurgados (docs, fórum, diálogos de upgrade)
- [ ] `node scripts/verify-fork-paths.mjs` passa na ref alvo
- [ ] Cabeçalhos de licença Apache 2.0 preservados nos arquivos originais
