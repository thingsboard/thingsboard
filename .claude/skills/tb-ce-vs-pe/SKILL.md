---
name: tb-ce-vs-pe
description: 'O que existe só no ThingsBoard PE e como resolver o mesmo problema no Community Edition — white-labeling, RBAC granular, Platform Integrations, relatórios em PDF, scheduler, Trendz Analytics, Edge, LoRaWAN. Use ao decidir entre CE e PE, ao descobrir que um recurso não existe na sua edição, ou ao planejar substituto em CE. Também cobre retenção e limpeza de dados, que em CE é configuração manual. Sintomas típicos - esse menu não aparece na minha instalação, endpoint da API devolve 404 mas está na documentação, como faço white label sem PE, como crio perfil de permissão customizado no CE, gerar relatório PDF sem PE, agendar tarefa recorrente, o banco está enchendo e não sei limpar, apagar telemetria antiga, remover eventos velhos, TTL de telemetria, quanto custa o PE, CE tem limite de devices.'
---

# CE vs PE: o que muda e como contornar

A confusão mais cara com ThingsBoard é usar a documentação da edição errada. O site
publica docs de PE e CE em caminhos diferentes (`/docs/pe/...` e `/docs/...`), e o
resultado é procurar por menu que não existe ou chamar endpoint que devolve 404.

## Antes de começar — confirme a edição

> **Se o MCP oficial do ThingsBoard estiver conectado** ([`thingsboard/thingsboard-mcp`](https://github.com/thingsboard/thingsboard-mcp),
> 120+ tools, funciona em CE e PE): use as tools dele para **ler e escrever dados** —
> devices, assets, telemetria, alarmes, relações, OTA. São melhores nisso que qualquer
> comando aqui. Use `scripts/tb.mjs` para o que o MCP **não** responde: qual endpoint
> existe nesta versão (`api`, `spec`), quais rule nodes esta instância tem (`nodes`), e
> se é CE ou PE (`check`). O MCP faz dado; esta skill faz contrato e conhecimento.

```bash
node scripts/tb.mjs check      # reporta CE ou PE e a versão exata
```

A detecção é por presença de endpoints exclusivos de PE no OpenAPI da instância
(`/api/whiteLabel`, `/api/scheduler`, `/api/integration`, `/api/converter`,
`/api/entityGroup`, `/api/report`…). Se um endpoint da documentação devolve 404, a
primeira hipótese é edição, não bug.

## O que CE **não** perde

Vale dizer primeiro, porque é fonte comum de erro de planejamento:

- **Não há limite de devices, assets nem de mensagens processadas.** Os limites de CE são
  de hardware e banco, não de licença.
- Ingestão de telemetria, Rule Engine, dashboards, multi-tenancy, MQTT/HTTP/CoAP,
  Calculated Fields e alarmes são todos de CE.
- **OAuth 2.0 e OIDC são de CE.** SSO com Keycloak, Authentik, Okta, Auth0 ou Azure AD
  funciona sem PE — o que é PE é o RBAC granular, não a autenticação federada.
- CE é Apache 2.0: pode usar comercialmente, modificar e redistribuir.

## Tabela de substituição

| Recurso do PE | Como resolver em CE |
|---|---|
| **White-labeling** (logo, cores, domínio) | Fork do código-fonte e recompilação. Não existe API nem tela — em CE é alteração de código. Ver `tb-fork-build` |
| **Platform Integrations** (adaptadores de protocolo na plataforma) | **ThingsBoard IoT Gateway**, serviço separado. É o caminho padrão de CE para Modbus, OPC-UA, BACnet, SNMP. Ver `tb-gateway` |
| **RBAC granular / Custom Roles** | Hierarquia tenant → customer → sub-customer para separar dados, mais OAuth2/OIDC para um IdP externo. Não dá para criar papel arbitrário; dá para segmentar por customer |
| **Entity Groups** | Relações de entidade (`tb-rest-api`) e atributos como marcador, com filtro por alias no dashboard |
| **Relatórios em PDF** | Serviço externo disparado por node "REST API Call" do rule chain, ou export do dashboard pela UI. Não há geração server-side nativa |
| **Scheduler** | Node `generator` no rule chain para período fixo, ou cron externo chamando a REST API. O `generator` é o mais simples e roda dentro da plataforma |
| **Trendz Analytics** | Calculated Fields cobrem janela rolante (`avg`, `mean`, `std`, `median`, `count`, `first`, `last`) sem serviço extra — ver `tb-rule-engine`. Para ML de verdade, microsserviço externo lendo pela REST API |
| **Edge** | Instância CE completa na ponta, sincronizada por rule chain com node REST/MQTT. Perde a sincronização gerenciada |
| **LoRaWAN** | Network server dedicado na frente (ChirpStack), publicando no ThingsBoard via MQTT ou Gateway. Custa manutenção do decoder |
| **Ciclo de vida do dado** | Configuração manual — ver a seção abaixo |

## Retenção e limpeza de dados

A dor mais reportada de CE, porque o banco cresce e não há tela unificada para isso.
São **duas camadas** independentes, e mexer só numa é o erro comum.

**1. TTL da telemetria — no rule chain.** O node **"Save Time Series"** tem um campo
*Default TTL*, em segundos, que define quanto tempo cada registro vive. `0` significa
guardar para sempre, e é o padrão — motivo pelo qual muita instalação nunca apaga nada.

Dá para sobrescrever por mensagem: a propriedade `TTL` na **metadata** tem precedência
sobre o valor do node. Isso permite reter telemetria crítica por anos e leitura de alta
frequência por semanas, no mesmo chain — enriqueça a metadata antes do Save Time Series.

**2. Limpeza no banco — variáveis de ambiente.** As `SQL_TTL_*` controlam a rotina
periódica que efetivamente remove os registros expirados no PostgreSQL/TimescaleDB. Há
TTLs separados para telemetria, alarmes, estatísticas de fila e exceções do Rule Engine.

**A tabela de eventos é o vilão silencioso.** Debug ligado num rule node grava evento a
cada mensagem. Esquecer debug ligado em produção enche o banco mais rápido que a própria
telemetria. Por isso o `debugSettings` de node tem `allEnabledUntil`, com expiração
automática — use, em vez de `allEnabled` permanente. Ver `tb-rule-engine`.

Ordem prática para uma instalação que já está grande:

1. Descubra o que ocupa espaço (`ts_kv`, `event`, `audit_log` são os suspeitos)
2. Desligue debug em todos os rule nodes
3. Defina TTL no Save Time Series — passa a valer para dado **novo**
4. Configure as `SQL_TTL_*` para a rotina apagar o que expirou
5. Só então considere agregação ou downsampling do histórico antigo

## Ao ler documentação

- `thingsboard.io/docs/pe/...` é PE; `thingsboard.io/docs/...` sem `pe` é CE.
- Muita página de recurso comum só existe sob `/pe/` mesmo valendo para os dois. A regra
  segura: **confirme contra a sua instância**, não contra a URL.
  ```bash
  node scripts/tb.mjs api "<termo>"     # esse endpoint existe aqui?
  node scripts/tb.mjs nodes             # esse rule node existe aqui?
  ```
- Rule nodes de PE (Add to Group, Duplicate to Group, Generate Report, Twilio) não estão
  no código-fonte público e não aparecem em instalação CE. O catálogo em `tb-rule-engine`
  marca quais são.

## Referência

- `tb-fork-build` — white-label por fork, o substituto de CE
- `tb-gateway` — o substituto de CE para Platform Integrations
- `tb-rule-engine` — Calculated Fields, `debugSettings`, catálogo de nodes por edição
- `tb-deploy-admin` — variáveis de ambiente e operação do banco
