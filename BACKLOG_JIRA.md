# TWmonitor — Backlog para Jira (por Sprint)

> Formato pensado para transcrição direta no Jira: cada Sprint tem 1 **Epic**
> (o objetivo da fase) e uma lista de **Stories/Tasks** com estimativa em horas e
> critério de aceite. Sem divisão por pessoa/função — só o backlog.
>
> Rebalanceei a distribuição em relação à primeira versão: a Fase 5 (Analytics,
> 280 HH) estava inteira num sprint só, o que não cabe em 2 semanas de capacidade de
> equipe. Redistribuí para que nenhum sprint passe de ~200 HH — ver a nota de
> rebalanceamento no fim do documento.

**Convenções usadas nas tabelas:**
- **Tipo**: `Epic` | `Story` | `Task`
- **Estimativa**: horas (HH) — some os itens do sprint para bater com o total da
  coluna "HH sprint"; convertam para Story Points conforme a escala do time, se
  preferirem.
- **Rótulos sugeridos** (labels do Jira): `frontend`, `backend`, `infra`,
  `docker`, `docs`, `qa`, `licenciamento`.

---

## Sprint 0 — Planejamento
**HH sprint:** — (atividade de organização, não estimada em HH de desenvolvimento)

**Epic:** `TWM-E0` — Planejamento e fundação do projeto

| # | Título | Tipo | Estimativa | Labels | Critério de aceite |
|---|---|---|---|---|---|
| 0.1 | Levantamento de backlog do produto | Task | — | `docs` | Backlog completo, dividido por sprint, revisado e aprovado pelo time/PO |
| 0.2 | Definir esquema de versionamento (SemVer) | Task | — | `docs`, `infra` | Documento de convenção `MAJOR.MINOR.PATCH` aprovado e publicado no repositório |
| 0.3 | Definir convenção de branches e tags de release | Task | — | `docs`, `infra` | Convenção de `main`/`develop`/`feature/*`/`release/*` (ou trunk-based) documentada; tags de release definidas |
| 0.4 | Criar `CHANGELOG.md` inicial | Task | — | `docs` | Arquivo criado no repositório com formato definido (ex. Keep a Changelog) |
| 0.5 | Definir convenção de nomes de imagem Docker | Task | — | `docs`, `docker` | Padrão `tecwise/tw-monitor-<serviço>:<versão>` documentado |
| 0.6 | Definir política de rastreio da versão upstream do ThingsBoard | Task | — | `docs` | Documentado de qual tag/branch do CE cada release do TWmonitor parte |

---

## Sprint 1 — Setup do fork e pipeline de build
**HH sprint:** 85 HH · *(ref. Fase 1 do plano original)*

**Epic:** `TWM-E1` — Fork estruturado e pipeline de build funcional

| # | Título | Tipo | Estimativa (HH) | Labels | Critério de aceite |
|---|---|---|---|---|---|
| 1.1 | Fixar tag/branch base estável do ThingsBoard CE | Task | 10 | `infra`, `docs` | Versão upstream documentada e branch base criada no fork |
| 1.2 | Expurgar plugins/dependências de nuvem não utilizados | Task | 15 | `backend`, `infra` | Módulos/dependências não aplicáveis ao on-premise removidos ou desabilitados, build continua passando |
| 1.3 | Validar pipeline de build real (`build.sh`, `msa/tb-node`, `msa/web-ui`) | Task | 15 | `infra`, `docker` | Build local (em ambiente com recursos adequados, não a estação de trabalho) roda com sucesso e gera artefatos `.deb` |
| 1.4 | Decidir e configurar onde as imagens Docker serão construídas | Task | 10 | `infra`, `docker` | Decisão registrada (GitHub Actions recomendado) com justificativa |
| 1.5 | Criar workflow de CI (build + push de imagem) | Task | 25 | `infra`, `docker` | Workflow no GitHub Actions builda `msa/tb-node` e `msa/web-ui` e publica em um registry |
| 1.6 | Validar `docker-compose.dev.yml` com as imagens geradas pelo CI | Task | 10 | `infra`, `docker`, `qa` | Stack sobe localmente via `docker compose up` usando as imagens publicadas, sem build local |

---

## Sprint 2 — White-label: identidade visual e navegação
**HH sprint:** ~120 HH · *(ref. Fase 2, parte 1)*

**Epic:** `TWM-E2` — Identidade visual TWmonitor aplicada

| # | Título | Tipo | Estimativa (HH) | Labels | Critério de aceite | Status |
|---|---|---|---|---|---|---|
| 2.1 | Aplicar paleta de cores oficial (`#00316D`, `#757575`) no tema | Story | 15 | `frontend` | Cores refletidas em botões, toolbar, sidenav, sem regressão visual | ✅ Prototipado |
| 2.2 | Substituir logotipos, ícones SVG e favicon | Story | 25 | `frontend` | Logo TWmonitor/TecWise aplicado em sidenav (claro/escuro), login e favicon | ✅ Prototipado |
| 2.3 | Reestruturar sidenav e barra superior (dark navy) | Story | 30 | `frontend` | Sidenav e toolbar em dark navy com texto/ícones legíveis, igual à instância PE de referência | ✅ Prototipado |
| 2.4 | Remover itens de menu não utilizados (`iot-hub`, `mobile-apps`) | Task | 5 | `frontend` | Itens não aparecem no menu para nenhum perfil de usuário | ✅ Feito, pendente de validação/commit |
| 2.5 | Ajustar tamanho do logo do header (60px) | Task | 5 | `frontend` | Logo exibido em 60px de altura, layout do header sem cortes | ✅ Feito, pendente de validação/commit |
| 2.6 | Revisar contraste e acessibilidade das cores customizadas | Task | 15 | `frontend`, `qa` | Contraste mínimo WCAG AA validado em texto sobre fundo escuro e claro |  |
| 2.7 | Auditoria de assets/badges/links remanescentes do ThingsBoard | Task | 25 | `frontend`, `qa` | Nenhuma referência visual a "ThingsBoard" ou link para `github.com/thingsboard` remanescente na UI |  |

---

## Sprint 3 — White-label: login, idioma e conteúdo
**HH sprint:** 120 HH · *(ref. Fase 2, parte 2)*

**Epic:** `TWM-E2` — Identidade visual TWmonitor aplicada (continuação)

| # | Título | Tipo | Estimativa (HH) | Labels | Critério de aceite | Status |
|---|---|---|---|---|---|---|
| 3.1 | Redesign completo da tela de login | Story | 35 | `frontend` | Tela de login com gradiente, cartão, campos e botão customizados, responsiva | ✅ Prototipado |
| 3.2 | Definir idioma padrão PT-BR | Task | 5 | `frontend` | `defaultLang` = `pt_BR`; UI carrega em português por padrão | ✅ Prototipado |
| 3.3 | Completar traduções PT-BR faltantes | Task | 30 | `frontend`, `docs` | Nenhuma chave de tradução "crua" visível no menu/telas principais |  |
| 3.4 | Expurgar/redirecionar links institucionais externos | Task | 20 | `frontend`, `docs` | Nenhum link para `thingsboard.io`/fórum público sem substituição por canal próprio ou remoção |  |
| 3.5 | Revisar textos de e-mails transacionais e diálogos "Sobre" | Task | 15 | `backend`, `docs` | Todos os e-mails/diálogos exibem marca TWmonitor, sem menção a ThingsBoard |  |
| 3.6 | Dashboard "Início" com identidade TWmonitor | Story | 15 | `frontend` | Dashboard padrão do tenant/sysadmin substituído por versão com a marca TWmonitor |  |

---

## Sprint 4 — Homologação, testes e Go-Live do 1º cliente
**HH sprint:** 200 HH · *(ref. Fase 3)*

**Epic:** `TWM-E3` — Go-Live do 1º cliente homologado

| # | Título | Tipo | Estimativa (HH) | Labels | Critério de aceite |
|---|---|---|---|---|---|
| 4.1 | Provisionar ambiente do 1º cliente | Task | 30 | `infra`, `docker` | Stack completo (core + Postgres + Caddy) rodando na VM do cliente via imagens do CI |
| 4.2 | Testes de carga/estresse com telemetria simulada | Task | 40 | `qa`, `backend` | Relatório de teste de carga (MQTT/HTTP) com resultados documentados |
| 4.3 | Testes de persistência do PostgreSQL | Task | 30 | `qa`, `backend` | Sistema recupera dados corretamente após queda de energia/reboot simulado |
| 4.4 | Deploy remoto na VM do cliente | Task | 30 | `infra` | Aplicação acessível e funcional na VM do cliente |
| 4.5 | Cadastro dos primeiros instrumentos de campo | Task | 30 | `qa` | Dispositivos reais enviando telemetria e aparecendo nos dashboards |
| 4.6 | Validação de dashboards operacionais com o cliente | Task | 20 | `qa`, `frontend` | Cliente valida visualmente os dashboards em reunião de acompanhamento |
| 4.7 | Aceite formal (UAT) | Task | 20 | `docs`, `qa` | Documento de aceite assinado pelo cliente, Milestone 1 encerrado |

---

## Sprint 5 — Relatórios offline: microsserviço e geração
**HH sprint:** 180 HH · *(ref. Fase 4, parte 1)*

**Epic:** `TWM-E4` — Relatórios offline e RBAC granular

| # | Título | Tipo | Estimativa (HH) | Labels | Critério de aceite |
|---|---|---|---|---|---|
| 5.1 | Criar microsserviço `tw-monitor-reports` | Task | 40 | `backend`, `infra` | Container novo, com healthcheck, integrado ao `docker-compose` |
| 5.2 | Exportação assíncrona de relatórios em PDF | Story | 50 | `backend` | Usuário solicita relatório e recebe PDF gerado com dados corretos |
| 5.3 | Exportação assíncrona de relatórios em Excel | Story | 45 | `backend` | Usuário solicita relatório e recebe Excel gerado com dados corretos |
| 5.4 | Renderização headless de dashboards com dados históricos | Task | 30 | `backend` | Dashboard renderizado sem navegador visível, com intervalo de datas customizado |
| 5.5 | Definir contrato de API entre core e microsserviço de relatórios | Task | 15 | `backend`, `docs` | Endpoints/eventos documentados (Swagger ou equivalente) |

---

## Sprint 6 — RBAC granular e telas de gestão
**HH sprint:** 180 HH · *(ref. Fase 4, parte 2)*

**Epic:** `TWM-E4` — Relatórios offline e RBAC granular (continuação)

| # | Título | Tipo | Estimativa (HH) | Labels | Critério de aceite |
|---|---|---|---|---|---|
| 6.1 | Autorização para papéis customizados (*Custom Roles*) | Story | 55 | `backend` | Sistema aceita criação de papéis além dos padrões (Tenant Admin/Customer User) |
| 6.2 | Hierarquia de perfis operacionais por grupo de ativos | Story | 40 | `backend` | Permissões aplicadas corretamente por grupo de ativos/dispositivos |
| 6.3 | Interface administrativa de gestão de permissões | Story | 40 | `frontend` | Tela permite criar/editar/excluir papéis e associar a grupos de ativos |
| 6.4 | Componentes de interface para solicitar/baixar relatórios | Story | 25 | `frontend` | Usuário consegue solicitar e baixar PDF/Excel pela UI |
| 6.5 | Testes de segurança de permissões | Task | 15 | `qa` | Casos de acesso negado/concedido testados e documentados |
| 6.6 | Validação end-to-end de relatórios com dados de homologação | Task | 5 | `qa` | Relatório gerado com dados reais do cliente piloto, sem erros |

---

## Sprint 7 — Analytics e modelos preditivos locais (parte 1)
**HH sprint:** 140 HH · *(ref. Fase 5, parte 1 — redistribuída)*

**Epic:** `TWM-E5` — Analytics e ML local

| # | Título | Tipo | Estimativa (HH) | Labels | Critério de aceite |
|---|---|---|---|---|---|
| 7.1 | Criar microsserviço `tw-monitor-analytics` (Python/FastAPI) | Task | 40 | `backend`, `infra` | Container novo, com healthcheck, integrado ao `docker-compose` |
| 7.2 | Algoritmos de regressão linear/polinomial para tendências | Story | 50 | `backend` | Endpoint retorna tendência calculada a partir de série histórica real |
| 7.3 | Detecção de outliers/anomalias | Story | 50 | `backend` | Endpoint sinaliza pontos fora do padrão em uma série de teste conhecida |

---

## Sprint 8 — Analytics e modelos preditivos locais (parte 2)
**HH sprint:** 140 HH · *(ref. Fase 5, parte 2 — redistribuída)*

**Epic:** `TWM-E5` — Analytics e ML local (continuação)

| # | Título | Tipo | Estimativa (HH) | Labels | Critério de aceite |
|---|---|---|---|---|---|
| 8.1 | Cálculo de taxa de variação rápida (gradiente) para alertas | Story | 35 | `backend` | Alerta disparado quando variação excede limite configurado |
| 8.2 | Widgets visuais customizados para tendências e anomalias | Story | 55 | `frontend` | Widget plota tendência/anomalia calculada pelo microsserviço no dashboard |
| 8.3 | Documentar API do microsserviço (Swagger) | Task | 15 | `backend`, `docs` | Swagger acessível e cobrindo todos os endpoints do serviço |
| 8.4 | Testes de carga das APIs analíticas e validação matemática | Task | 35 | `qa`, `backend` | Resultados dos algoritmos validados contra cálculo de referência; API suporta carga esperada |

---

## Sprint 9 — Backups, updates remotos e licenciamento offline
**HH sprint:** 170 HH · *(ref. Fase 6 + Fase 7 — combinadas)*

**Epic:** `TWM-E6` — Operação sustentável e licenciamento

| # | Título | Tipo | Estimativa (HH) | Labels | Critério de aceite |
|---|---|---|---|---|---|
| 9.1 | Script de backup lógico automatizado do PostgreSQL | Task | 20 | `infra`, `backend` | Backup roda agendado e grava em NAS/SMB/disco secundário |
| 9.2 | Healthchecks e políticas de auto-recuperação dos containers | Task | 15 | `infra`, `docker` | Container reiniciado automaticamente após falha simulada |
| 9.3 | Pipeline de atualização remota (`update_twmonitor.sh`/`.ps1`) | Task | 25 | `infra`, `docker` | Atualização de versão executada sem perda de dados em ambiente de teste |
| 9.4 | Testes de restauração de backup (*disaster recovery*) | Task | 15 | `qa`, `infra` | Restauração completa validada em ambiente separado |
| 9.5 | Manuais técnicos de operação e disaster recovery | Task | 15 | `docs` | Manuais revisados e publicados |
| 9.6 | Motor de licença offline RSA/JWT (`TwLicenseService`) | Story | 30 | `backend`, `licenciamento` | Licença válida/expirada corretamente reconhecida sem conexão externa |
| 9.7 | Bloqueio suave de ingestão (*soft enforcement*) | Story | 15 | `backend`, `licenciamento` | Ingestão de telemetria suspensa após expiração; login/consultas continuam liberados |
| 9.8 | Anti-tampering de relógio | Task | 10 | `backend`, `licenciamento` | Manipulação de data do SO detectada via checagem contra `MAX(ts)` |
| 9.9 | Banner de status de licença na UI | Story | 10 | `frontend`, `licenciamento` | Banner exibe corretamente "ativa até DD/MM" ou "expirada" |
| 9.10 | Gerador CLI de licenças TecWise | Task | 10 | `backend`, `licenciamento` | Script gera e assina licença anual válida |
| 9.11 | Testes de expiração de licença e adulteração de relógio | Task | 5 | `qa`, `licenciamento` | Casos de teste executados e documentados |

---

## Nota sobre o rebalanceamento

A primeira versão do backlog seguia as durações originais de cada Fase do plano
formal (Fase 5 = 3 semanas inteiras num sprint só = 280 HH), o que ultrapassa a
capacidade razoável de um sprint de 2 semanas. Rebalanceei assim:

- **Fase 5** (Analytics, 280 HH) → dividida em **Sprint 7 + Sprint 8** (140 HH cada).
- **Fase 6** (100 HH) + **Fase 7** (70 HH) → combinadas no **Sprint 9** (170 HH),
  já que juntas cabem confortavelmente numa única iteração.
- Resultado: **9 sprints de entrega** (Sprint 1 a 9), mesma contagem de antes, com
  a carga por sprint variando entre **85 HH e 200 HH** (média ≈148 HH) — bem mais
  equilibrado do que a distribuição anterior (que tinha um pico de 280 HH e um vale
  de 70 HH).

| Sprint | HH | Sprint | HH |
|---|---|---|---|
| 1 | 85 | 6 | 180 |
| 2 | 120 | 7 | 140 |
| 3 | 120 | 8 | 140 |
| 4 | 200 | 9 | 170 |
| 5 | 180 | **Total** | **1.335 HH** |
