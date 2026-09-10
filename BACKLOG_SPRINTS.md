# TWmonitor — Backlog por Sprints (ciclos de 2 semanas)

> Reorganiza o escopo do `PLANO_DESENVOLVIMENTO_FORK_TW_MONITOR.md` (18 semanas / 1.335 HH,
> dividido por Fase e por função) em um **backlog por sprint**, sem atribuição por
> papel/pessoa — cada sprint lista os itens de backlog a entregar, ponto final.
> A referência "Fase X / HH" fica só como rastreabilidade com o plano original.
>
> 10 sprints de 2 semanas = 20 semanas (Sprint 0 de planejamento + as 18 semanas
> originais do plano, reorganizadas em ciclos pares).

## Sprint 0 — Planejamento (2 semanas)

1. **Levantamento de backlog do produto** — este documento. Entregável: backlog
   fatiado por sprint, validado com o time.
2. **Estabelecer versionamento do projeto**:
   - Definir esquema de versão (SemVer: `MAJOR.MINOR.PATCH`, ex. `1.0.0`).
   - Definir convenção de branches (ex. `main`/`develop`/`feature/*`/`release/*`,
     ou trunk-based com `twmonitor-whitelabel` como base atual).
   - Definir convenção de tags de release e de nomes de imagem Docker
     (`tecwise/tw-monitor-core:<versão>`).
   - Criar `CHANGELOG.md` inicial no repositório.
   - Definir política de versionamento do banco (migrations) alinhada às versões do
     ThingsBoard upstream (rastrear de qual tag do CE cada release do TWmonitor parte).

---

## Sprint 1 — Setup do fork e pipeline de build (Fase 1 · 85 HH)

1. Fixar a tag/branch base estável do ThingsBoard CE usada como ponto de partida
   (documentar qual versão upstream, hoje branch `lts-4.4`/rc mergeados).
2. Expurgar plugins/dependências externas de nuvem que não serão usados
   (ex. avaliar `msa/vc-executor`, integrações não aplicáveis ao on-premise).
3. Mapear e validar o pipeline de build real do ThingsBoard (`build.sh`, módulos
   `msa/tb-node`, `msa/web-ui`) — já investigado e documentado em `PLANO_PROJETO.md`.
4. Decidir e configurar onde as imagens Docker serão construídas (GitHub Actions
   recomendado — não sobrecarrega máquina local; alternativas: Docker Desktop via TI,
   VM dedicada).
5. Criar workflow de CI que roda o build (`mvn ... --projects msa/tb-node,msa/web-ui`)
   e publica as imagens (`tecwise/tw-monitor-core`) num registry.
6. Validar `docker-compose.dev.yml` local usando as imagens geradas pelo CI (não
   buildando localmente).

---

## Sprint 2 — White-label: identidade visual e navegação (Fase 2, parte 1 · ~120 HH)

> Boa parte já prototipada nesta sessão (branch `twmonitor-whitelabel`) — itens
> marcados ✅ têm código funcional a validar/oficializar.

1. ✅ Definir e aplicar paleta de cores oficial (`#00316D` azul, `#757575` cinza) no
   tema SCSS.
2. ✅ Substituir logotipos, ícones SVG e favicon pela marca TWmonitor/TecWise
   (variante colorida e variante branca para fundos escuros).
3. ✅ Reestruturar sidenav e barra superior (dark navy, textos/ícones claros),
   seguindo a identidade visual real da instância PE.
4. ✅ Remover itens de menu não utilizados (`iot-hub`, `mobile-apps`) — pendente de
   validação/commit.
5. Revisar contraste e acessibilidade das cores customizadas (WCAG AA mínimo).
6. Ajustar tamanho do logo do header conforme padrão definido (60px, igual à
   configuração já usada na instância PE) — pendente de validação/commit.
7. Auditoria completa de assets remanescentes do ThingsBoard (imagens/ícones não
   trocados) e badges/links para `github.com/thingsboard` remanescentes.

---

## Sprint 3 — White-label: login, idioma e conteúdo (Fase 2, parte 2 · 120 HH)

1. ✅ Redesign completo da tela de login (gradiente, cartão com glassmorphism,
   campos e botão customizados).
2. ✅ Definir idioma padrão PT-BR (`defaultLang`) e revisar strings traduzidas.
3. Completar traduções PT-BR faltantes identificadas (`monitor.monitor`,
   `entity.devices-and-assets`, `iot-hub.iot-hub`, `customer.customers-and-users`,
   `entity.data-processing`, `image.images`, `javascript.scripts`, `resource.files`,
   `admin.platform`, `notification.notifications`).
4. Expurgar/redirecionar links institucionais externos (`thingsboard.io`,
   documentação pública, fórum) para domínio/canal próprio da TecWise ou remover.
5. Revisar e substituir textos de e-mails transacionais restantes, rodapés e
   diálogos "Sobre"/versão.
6. Definir e aplicar template padrão de dashboard "Início" com a marca TWmonitor
   (substituindo o dashboard de sistema padrão do CE).

---

## Sprint 4 — Homologação, testes e Go-Live do 1º cliente (Fase 3 · 200 HH)

1. Provisionar ambiente do 1º cliente (Docker Compose com imagens geradas no CI,
   PostgreSQL, Caddy).
2. Testes de carga/estresse com telemetria simulada (MQTT/HTTP).
3. Testes de persistência do PostgreSQL (queda de energia, reboot, restauração).
4. Deploy remoto na VM do cliente (acesso via SSH/VPN/AnyDesk/RDP conforme
   combinado).
5. Cadastro dos primeiros instrumentos de campo e validação de comunicação.
6. Validação de dashboards operacionais junto ao cliente.
7. Aceite formal (UAT) e encerramento do Milestone 1.

---

## Sprint 5 — Relatórios offline: microsserviço e geração (Fase 4, parte 1 · 180 HH)

1. Criar microsserviço `tw-monitor-reports` (container desacoplado).
2. Implementar exportação assíncrona de relatórios em PDF.
3. Implementar exportação assíncrona de relatórios em Excel.
4. Implementar renderização headless de dashboards com dados históricos.
5. Definir contrato de API (endpoints, filas/eventos) entre core e o microsserviço
   de relatórios.

---

## Sprint 6 — RBAC granular e telas de gestão (Fase 4, parte 2 · 180 HH)

1. Estender autorização no backend para papéis customizados (*Custom Roles*).
2. Definir hierarquia de perfis operacionais por grupo de ativos.
3. Criar interface administrativa para gestão de permissões por grupo de ativos.
4. Criar componentes de interface para solicitar/baixar relatórios gerados.
5. Testes de segurança de permissões (acesso negado/concedido por perfil).
6. Validar geração de PDF/Excel end-to-end com dados reais de homologação.

---

## Sprint 7 — Analytics e modelos preditivos locais (Fase 5 · 280 HH)

1. Criar microsserviço `tw-monitor-analytics` (Python/FastAPI).
2. Implementar algoritmos de regressão linear/polinomial para tendências temporais.
3. Implementar detecção de outliers/anomalias.
4. Implementar cálculo de taxa de variação rápida (gradiente) para alertas
   precoces.
5. Criar widgets visuais customizados no frontend para plotagem de tendências e
   anomalias.
6. Documentar API do microsserviço (Swagger).
7. Testes de carga das APIs analíticas e validação matemática dos cálculos.

---

## Sprint 8 — Backups, healthchecks e updates remotos (Fase 6 · 100 HH)

1. Criar script de backup lógico automatizado do PostgreSQL
   (`backup_local.sh`), com retenção em NAS/SMB ou disco secundário.
2. Configurar healthchecks dos containers e políticas de auto-recuperação
   (*restart policies*).
3. Criar pipeline de atualização remota (`update_twmonitor.sh`/`.ps1`) que
   atualiza tags de imagem sem perda de dados.
4. Testes de restauração de backup (*disaster recovery*).
5. Escrever manuais técnicos de operação, contingência e disaster recovery.
6. Escrever release notes e guia visual para o usuário final.

---

## Sprint 9 — Licenciamento offline e encerramento do roadmap (Fase 7 · 70 HH + buffer)

1. Implementar motor de licença offline RSA 2048-bit / JWT RS256
   (`TwLicenseService`).
2. Implementar bloqueio suave de ingestão (*soft enforcement*) após expiração,
   mantendo login/consultas/histórico liberados.
3. Implementar mecanismo anti-tampering de relógio (checagem monotônica contra
   `MAX(ts)` no PostgreSQL).
4. Criar banner de notificação de status de licença na UI.
5. Criar gerador CLI de licenças TecWise (script Python de geração/assinatura).
6. Testes de expiração de licença e de adulteração de relógio.
7. Consolidação de métricas de engenharia e encerramento formal do roadmap.

---

## Rastreabilidade com o plano original

| Sprint | Semanas | Fase do plano formal | HH de referência |
|---|---|---|---|
| 0 | 1–2 | — (novo, planejamento) | — |
| 1 | 3–4 | Fase 1 | 85 HH |
| 2 | 5–6 | Fase 2 (parte 1) | ~120 HH |
| 3 | 7–8 | Fase 2 (parte 2) | ~120 HH |
| 4 | 9–10 | Fase 3 | 200 HH |
| 5 | 11–12 | Fase 4 (parte 1) | ~180 HH |
| 6 | 13–14 | Fase 4 (parte 2) | ~180 HH |
| 7 | 15–16 | Fase 5 | 280 HH |
| 8 | 17–18 | Fase 6 | 100 HH |
| 9 | 19–20 | Fase 7 | 70 HH |
