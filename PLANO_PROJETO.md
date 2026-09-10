# TWmonitor — Plano de Projeto (Docker, Fases, Status)

> Complementa o [`TWMONITOR.md`](TWMONITOR.md) (o que já foi alterado no código) e o
> documento formal `PLANO_DESENVOLVIMENTO_FORK_TW_MONITOR.md` da TecWise (o plano de
> 18 semanas / 1.335 HH). Este arquivo registra o que aprendemos investigando o
> pipeline real de Docker do ThingsBoard CE, os bloqueios técnicos encontrados nesta
> máquina, e como isso ajusta as fases do plano formal.

## 1. Por que este documento existe

Numa sessão de trabalho anterior, o ambiente local (Windows, sem privilégio de admin)
ficou instável: `ng serve` + processos órfãos de automação de browser consumiram
memória excessiva, seguido de corrupção do `node_modules` e, depois, do **próprio
npm global** (`C:\Program Files\nodejs\node_modules\npm\...`), com comandos básicos
passando a falhar (`EUNKNOWN: unknown error, uv_spawn`, erros de `cygheap`).

Isso motivou uma pausa para diagnóstico calmo (sem rodar nada) e, agora, a investigação
de como o Docker realmente funciona neste projeto — em vez de assumir a arquitetura
descrita no plano formal, fomos conferir o que existe de fato no repositório.

## 2. Diagnóstico do ambiente local (para não esquecer)

- **Sem privilégio de administrador** nesta máquina (`net session` → "Acesso negado").
- **Sem WSL2 instalado** — pré-requisito do Docker Desktop no Windows; instalar exige
  admin **e reinicialização**.
- **PowerShell em modo de linguagem restrita** — sinal de política corporativa de TI
  gerenciando esta máquina.
- **`npm` global possivelmente corrompido** — última tentativa de reinstalar
  `node_modules` falhou dentro do próprio pacote `tar` do npm (não do projeto).
  Suspeitas mais prováveis: espaço em disco baixo (builds/instalações repetidas
  gravaram vários GB) ou exaustão de handles/processos acumulados na sessão.
- **Ação recomendada antes de qualquer novo `npm install`/build**: verificar espaço
  livre em disco e reiniciar o Windows. Não tentar reinstalar de novo sem isso.

## 3. Como o build/Docker do ThingsBoard CE funciona de verdade

O plano formal da TecWise assume um **"Dockerfile Multi-Stage oficial (Node builder →
Maven builder → Runtime JRE Alpine)"** — ou seja, um único Dockerfile que compila tudo
de forma isolada. **Não é assim que o ThingsBoard CE funciona.** Conferimos o
repositório real:

- **Não existe nenhum Dockerfile "raiz"** no projeto. O que existe em `docker/` são só
  arquivos `docker-compose.yml` que **baixam imagens já prontas** do Docker Hub oficial
  (`thingsboard/tb-node`, etc.) — não constroem nada.
- As imagens oficiais são construídas via **Maven**, não via um Dockerfile isolado:
  1. [`build.sh`](build.sh) roda `mvn -T6 license:format clean install -DskipTests -Dpkg.skip=true --also-make`
     — isso **compila o backend Java inteiro** (todos os módulos: `dao`, `rule-engine`,
     `transport`, `application`, etc.) e gera pacotes `.deb`.
  2. Cada serviço tem seu próprio Dockerfile **fino**, que só instala o `.deb` já
     pronto — ex: [`msa/tb-node/docker/Dockerfile`](msa/tb-node/docker/Dockerfile)
     (backend) e [`msa/web-ui/docker/Dockerfile`](msa/web-ui/docker/Dockerfile)
     (frontend — espera o `ui-ngx` já compilado, servido por um pequeno servidor Node,
     não Nginx).
  3. A construção da imagem Docker em si fica **desligada por padrão** no `build.sh`
     (as flags `-Dpush-docker-amd-arm-images` / `-Ddockerfile.skip=false` estão
     comentadas) — precisa ser habilitada explicitamente.
- `NODE_OPTIONS="--max_old_space_size=4096"` no próprio `build.sh` confirma: **4GB é o
  teto de memória que o time oficial do ThingsBoard usa** para o build do frontend —
  bate com o ajuste que já fizemos por conta própria depois de ver a máquina travar
  com 8GB configurados.

**Conclusão prática**: mesmo COM Docker instalado, gerar uma imagem oficial exige
compilar o backend Java inteiro via Maven — uma build pesada (múltiplos módulos,
threads paralelas), não só "rodar `docker build`". Isso significa que o bloqueio real
não é só "falta Docker" — é que **esse tipo de build pesado não deveria rodar nesta
máquina de trabalho de qualquer forma**, com ou sem Docker.

## 4. Caminhos possíveis a partir daqui

| Opção | Como funciona | Prós | Contras |
|---|---|---|---|
| **A. CI no GitHub Actions (recomendado)** | Workflow no próprio fork (`Samuelvieria/ThigsBoardCE-TWmonitor`) roda o `mvn`/build/Docker nos runners do GitHub, não na máquina local. Publica em GitHub Container Registry (GHCR) inicialmente. | Zero carga na máquina local. Não depende de admin/TI. Já temos o fork no GitHub. Fica pronto pra virar pipeline de CI/CD oficial (Fase 6 do plano formal). | Precisa configurar o workflow (`.github/workflows/*.yml`) e resolver credenciais de registry. Build na nuvem tem seus próprios limites de tempo/recursos (mas Actions grátis já dá conta de projetos deste porte). |
| **B. Instalar Docker Desktop nesta máquina** | Pedir pra TI habilitar WSL2 + instalar Docker Desktop com admin. | Alinhado ao "Docker-first" do plano formal, sem depender de nuvem. | Depende de aprovação/ação de TI, fora do nosso controle nesta sessão. Reinicialização necessária. Não resolve o problema de a build ser pesada — só muda onde ela roda. |
| **C. VM/servidor Linux dedicado pro build** | O próprio plano formal já prevê "acesso remoto à VM" para deploy — a mesma VM (ou uma separada) pode ser usada para builds via Maven+Docker nativos do Linux. | Ambiente Linux é o nativo do ThingsBoard (scripts/Dockerfiles assumem isso). Separa build de deploy de máquina de trabalho pessoal. | Exige provisionar/ter acesso a essa VM antes da Fase 1 formalmente começar. |

**Recomendação**: seguir com **A (GitHub Actions)** agora, como forma de desbloquear
sem depender de TI/admin, e negociar **B ou C** em paralelo para quando o projeto
formal (com equipe completa) começar de fato — o plano já assume Docker como
requisito de arquitetura, então essa conversa com TI precisa acontecer de qualquer
forma antes da Fase 1 oficial.

## 5. Status atual do que já foi feito (spike da Fase 2)

Branch `twmonitor-whitelabel` no fork, commits já enviados ao GitHub:

| Commit | Conteúdo |
|---|---|
| `bbdedd7457` | White-label inicial: cores, logos, título, e-mails |
| `15bbe4da41` | Correção do logo invisível na tela de login |
| `641d8a6177` | Correção das cores oficiais (#00316D azul, #757575 cinza) |
| `bc239d5cb6` | Visual avançado da tela de login (gradiente, glassmorphism) |
| `85284ee7b2` | Idioma padrão pt_BR, botão cinza, logo maior no login |
| `1fb7497b2a` | Correção do 404 do favicon |
| `4d97001da9` | Documentação (`TWMONITOR.md`) + ferramenta de preview sem backend |
| `63092996f1` | Sidenav e toolbars em dark navy, replicando a instância PE real |

**Pendente, feito mas ainda NÃO commitado** (mudanças no working tree, nunca chegaram
a ser validadas por build antes da máquina travar):
- `ui-ngx/src/app/core/services/menu.models.ts` — remoção dos itens de menu
  `iot-hub.iot-hub` e `mobile.mobile-apps` (sysadmin e tenant admin).
- `ui-ngx/src/app/modules/home/home.component.scss` — logo do header do sidenav
  aumentado de 30px para 60px (ajuste de altura do toolbar e posição do botão de
  colapsar junto).

Isso corresponde ao conteúdo esperado da **Fase 2** do plano formal (paleta de cores,
logos, redesign de login, menu lateral, PT-BR) — feito como protótipo exploratório
validado visualmente (screenshots), não como a execução formal da fase com o time
completo.

## 6. Próximos passos concretos

- [ ] Verificar espaço em disco livre e reiniciar o Windows antes de qualquer novo
      `npm`/build local.
- [ ] Depois do reboot, confirmar que `npm --version` funciona normalmente antes de
      tentar rodar o frontend de novo.
- [ ] Validar (build local leve, só `ng serve` do frontend) as duas mudanças
      pendentes (menu e logo 60px) e commitar.
- [ ] Decidir entre as opções A/B/C da seção 4 para desbloquear builds Docker sem
      sobrecarregar a máquina local.
- [ ] Se optar por (A): criar workflow de GitHub Actions que roda `build.sh` com os
      projetos `msa/tb-node,msa/web-ui` e publica em GHCR.
- [ ] Levar a decisão de infraestrutura Docker (opção B ou C) para alinhamento com TI,
      já que o plano formal assume isso como pré-requisito da Fase 1.
