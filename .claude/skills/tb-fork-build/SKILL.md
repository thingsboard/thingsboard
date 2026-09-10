---
name: tb-fork-build
description: 'Fork do ThingsBoard CE do código-fonte — white-label no Angular (ui-ngx), build via Maven, imagens Docker, manutenção do fork contra o upstream. Use ao modificar o código-fonte em vez de operar uma instância pronta: trocar logo/cores/título, mexer no menu lateral ou na tela de login, traduzir para PT-BR, compilar do zero, gerar imagem própria. Sintomas típicos - onde fica o SCSS do tema, o logo continua o do ThingsBoard depois de trocar o asset, "docker build" falha com erro de sintaxe no Dockerfile, JavaScript heap out of memory no ui-ngx, o .deb não é gerado, ng serve não sobe, os emails de ativação e reset ainda dizem ThingsBoard (templates .ftl no backend), qual tag do CE usar como base do fork, rebase do fork numa release nova sem perder as customizações. Escopo é código-fonte compilado por você; operar servidor instalado é tb-deploy-admin, widget feito pelo editor é tb-widgets-dashboards.'
---

# Fork do ThingsBoard CE: white-label e build do código-fonte

Esta skill é para quem **modifica e recompila** o ThingsBoard. Para operar uma instância
pronta (REST API, rule chains, widgets pelo editor, deploy de imagem oficial), use
`tb-rest-api`, `tb-rule-engine`, `tb-widgets-dashboards` e `tb-deploy-admin`.

## Antes de começar — fixe a versão base

Caminhos de arquivo e estrutura de build **mudam entre versões**. Todo caminho citado aqui
foi verificado contra uma ref específica (ver cabeçalho de
[references/whitelabel-map.md](references/whitelabel-map.md)).

```bash
# qual versão o fork realmente é? (nao confie no que o plano diz)
grep -m1 -A1 "<artifactId>thingsboard</artifactId>" pom.xml

# todo caminho citado nesta skill ainda existe nesta ref?
node scripts/verify-fork-paths.mjs --ref v4.3.1.4
node scripts/verify-fork-paths.mjs --repo <owner>/<fork> --ref <branch>
```

**Forkar `master` é armadilha.** `master` é branch de desenvolvimento e o `pom.xml` dela
traz versão `X.Y.Z-SNAPSHOT` — código não lançado. Para produto entregue a cliente e
mantido por anos, forke de uma **tag de release** (`v4.3.1.4`, etc.). Se o fork já saiu de
`master`, isso é dívida a resolver antes do go-live, não depois: rebase futuro fica
imprevisível.

## Como o build funciona de verdade

Este é o ponto onde planos de projeto costumam errar. **Não existe Dockerfile na raiz** e
não existe build multi-stage único.

```
build.sh
  └─ mvn -T6 license:format clean install -DskipTests -Dpkg.skip=true --also-make
       ├─ compila o backend Java inteiro (dao, rule-engine, transport, application, ...)
       ├─ compila o frontend Angular (ui-ngx) e o embute
       └─ empacota .deb por serviço        <- só se pkg.skip NÃO for true
            └─ msa/<serviço>/docker/Dockerfile  instala o .deb já pronto
```

Consequências práticas:

- **`-Dpkg.skip=true` está no `build.sh` padrão** — o build padrão **não gera o `.deb`**.
  Sem `.deb`, o Dockerfile do serviço não tem o que instalar.
- **A build da imagem Docker vem desligada.** As flags `-Ddockerfile.skip=false` e
  `-Dpush-docker-image=true` estão comentadas no `build.sh`; precisa habilitar.
- **Os Dockerfiles não são Dockerfiles válidos como estão.** Usam placeholders do Maven
  (`${pkg.name}`, `${pkg.installFolder}`, `${pkg.user}`, `${docker.base.image}`) e o Maven
  os filtra para o diretório de build (`target`) antes de construir a imagem. Rodar
  `docker build` direto no arquivo do repositório **falha** — não é bug, é template.
- **Compile só o que precisa**: `./build.sh msa/tb-node,msa/web-ui` com `--also-make`
  puxa as dependências. Compilar o repositório inteiro é desnecessário para white-label.
- **O `web-ui` é um servidor Node**, não Nginx — `server.js` servindo o bundle Angular
  já compilado. Não procure config de Nginx.

### Memória

`build.sh` fixa `MAVEN_OPTS="-Xmx1024m"` e `NODE_OPTIONS="--max_old_space_size=4096"`.
**4 GB é o teto que o próprio time do ThingsBoard usa** para o frontend — subir esse valor
costuma piorar (mais GC, mais swap), não resolver. `JavaScript heap out of memory` no
`ui-ngx` quase nunca se resolve com mais heap; investigue disco cheio e `node_modules`
corrompido primeiro.

### Onde rodar

Build completo é pesado (múltiplos módulos Maven em paralelo + Angular). Não é trabalho
para máquina de desenvolvimento com restrição corporativa. Ordem de preferência:

1. **CI (GitHub Actions)** — desbloqueia sem depender de TI, publica em GHCR/ACR, e já
   vira o pipeline de release. Melhor primeiro passo.
2. **VM Linux dedicada** — ambiente nativo dos scripts; separa build de máquina pessoal.
3. **Docker Desktop local** — só muda onde a build roda, não deixa de ser pesada.

Para iterar em white-label você **não precisa** de build completo: `ng serve` dentro de
`ui-ngx` com proxy para um backend já rodando dá ciclo de segundos em vez de dezenas de
minutos. Use build completo só para gerar release.

## White-label

Mapa completo de arquivo por elemento em
[references/whitelabel-map.md](references/whitelabel-map.md). Ordem que evita retrabalho:

1. **Cores** primeiro (`ui-ngx/src/scss/constants.scss` → `theme.scss`) — muda a base de
   tudo; fazer depois obriga a revisitar telas já ajustadas.
2. **Assets e título** (logo, favicon, `index.html`) — mecânico, sem dependência.
3. **Telas específicas** (login, menu lateral, header).
4. **Textos** (`locale.constant-pt_BR.json`) e templates de e-mail `.ftl`.
5. **Expurgo** de links externos (docs, fórum, diálogos de upgrade) — por último, porque
   é varredura ampla e você quer fazer uma vez só.

Armadilhas que custam tempo:

- **Trocar o SVG do logo não basta.** `logo.component.ts` referencia o asset; o `index.html`
  tem o favicon separado; e o `ui-ngx/angular.json` lista o arquivo
  `ui-ngx/src/thingsboard.ico` pelo nome, nos `assets` (escrito lá como caminho relativo
  ao `ui-ngx/`). São três lugares.
- **`pt_BR` já existe** em `locale.constant-pt_BR.json` — o trabalho é revisar e
  sobrescrever, não traduzir do zero. Idioma padrão é decisão de código à parte.
- **Templates de e-mail `.ftl` ficam no backend** (`application/src/main/resources/templates/`),
  não no `ui-ngx`. É fácil fazer white-label completo na UI e esquecer que os e-mails de
  ativação e reset ainda dizem "ThingsBoard".
- **CE não tem API de white-labeling.** O endpoint `/api/whiteLabel*` é exclusivo do PE.
  Num fork CE tudo é alteração de código-fonte, recompilada. Não procure tela de admin.

## Manutenção do fork

O custo real de um fork não é a customização inicial — é acompanhar o upstream.

- **Isole as mudanças.** Quanto mais os arquivos alterados se concentrarem em poucos
  pontos, mais barato é o rebase. Preferir sobrescrever variável SCSS a espalhar
  `!important` por componentes.
- **Documente cada alteração** e o motivo, num arquivo versionado. Sem isso, em seis meses
  ninguém sabe se um conflito de merge pode ser resolvido a favor do upstream.
- **Rebase por tag**, uma de cada vez. Pular de `v4.3` direto para `v4.6` acumula
  conflitos que se mascaram entre si.
- **Antes de subir de versão**, rode `node scripts/verify-fork-paths.mjs --ref <nova-tag>`:
  ele diz quais caminhos que você customizou deixaram de existir.

## Licenciamento e enforcement (contexto CE)

CE é Apache 2.0: pode forkar, modificar, redistribuir e cobrar, **desde que preserve os
avisos de copyright e a licença**. O que não pode é remover atribuição de copyright dos
arquivos. Fazer white-label visual é permitido; apagar cabeçalho de licença dos `.java`
não é.

Para bloqueio de ingestão por licença expirada, o ponto de interceptação natural não é o
Rule Engine e sim a camada de transporte — mas ambos são possíveis. Ver `tb-rule-engine`
para a estrutura do Rule Engine se optar por lá.

## Referência

- [references/whitelabel-map.md](references/whitelabel-map.md) — arquivo por elemento de
  branding, com proveniência fixada e verificável por script.
