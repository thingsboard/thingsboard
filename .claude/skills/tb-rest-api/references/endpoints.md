# Catálogo de endpoints (referência rápida)

> Confirme sempre contra `https://<host>/swagger-ui/` da instância alvo — esta tabela
> cobre os endpoints mais estáveis e usados, mas paths/parâmetros podem variar entre
> versões CE/PE.

## Testados de verdade contra uma instância PE (2026-08-26)

Os endpoints abaixo foram efetivamente chamados (somente `GET`/`POST /api/auth/login`,
nenhuma escrita) contra uma instância PE em produção e responderam corretamente — maior
confiança que o restante deste arquivo:

| Método | Path | Retorno |
|---|---|---|
| POST | `/api/auth/login` | `{token, refreshToken}` |
| GET | `/api/auth/user` | usuário logado (`email`, `authority`, `tenantId`, `customerId`) |
| GET | `/api/tenant/dashboards?pageSize=&page=` | lista de dashboards do tenant |
| GET | `/api/dashboard/{id}` | dashboard completo (`configuration.widgets`, `.states`, `.entityAliases`) |
| GET | `/api/ruleChains?pageSize=&page=` | lista de rule chains (`name`, `root`, `id`) |
| GET | `/api/ruleChain/{id}/metadata` | `{nodes, connections, firstNodeIndex, ruleChainConnections}` |
| GET | `/api/deviceProfiles?pageSize=&page=` | perfis de device (`transportType`, `defaultRuleChainId`) |
| GET | `/api/assetProfiles?pageSize=&page=` | perfis de asset |
| GET | `/api/tenant/devices?pageSize=&page=` | devices do tenant (`hasNext`, `totalElements`) |
| GET | `/api/tenant/assets?pageSize=&page=` | assets do tenant |
| GET | `/api/customers?pageSize=&page=` | customers do tenant |
| GET | `/api/relations/info?fromId=&fromType=` | relações a partir de uma entidade |
| GET | `/api/widgetsBundles?pageSize=&page=` | bundles de widget (sistema + tenant) — nota: é `widgetsBundles`, com "s", não `widgetBundles` |
| GET | `/api/widgetTypes?bundleAlias=&isSystem=false` | tipos de widget de um bundle **tenant** (não funcionou para bundles de sistema com esses parâmetros — testar variação para `isSystem=true` se precisar dos widgets de sistema) |

Dica: bundles de sistema (Charts, Tables, SCADA, Maps, ...) sempre aparecem com o mesmo
`tenantId` reservado `13814000-1dd2-11b2-8080-808080808080` — compare contra o
`tenantId` retornado por `/api/auth/user` para separar bundle de sistema de bundle
customizado pelo tenant sem depender de nome/alias.

## Autenticação

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/auth/login` | Login usuário/senha → JWT + refresh token |
| POST | `/api/auth/token` | Renovar access token a partir do refresh token |
| POST | `/api/auth/logout` | Invalida sessão |
| GET | `/api/auth/user` | Dados do usuário autenticado |

## Devices

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/device` | Criar/atualizar device |
| POST | `/api/device-with-credentials` | Criar device já com credenciais definidas |
| GET | `/api/device/{deviceId}` | Buscar device por id |
| DELETE | `/api/device/{deviceId}` | Remover device |
| GET | `/api/device/{deviceId}/credentials` | Obter access token/credenciais |
| POST | `/api/device/credentials` | Atualizar credenciais |
| GET | `/api/tenant/devices?deviceName=...` | Buscar por nome dentro do tenant |
| GET | `/api/customer/{customerId}/devices?pageSize=...&page=...` | Listar devices de um customer |
| POST | `/api/customer/{customerId}/device/{deviceId}` | Atribuir device a customer |

## Telemetria e atributos

> Prefixo `/api/plugins/telemetry` confirmado como constante `TELEMETRY_URL_PREFIX` no
> código-fonte (`TbUrlConstants.java`); paths abaixo conferidos contra os
> `@GetMapping`/`@PostMapping`/`@DeleteMapping` reais do `TelemetryController.java`.

| Método | Path | Descrição |
|---|---|---|
| GET | `/api/plugins/telemetry/{entityType}/{entityId}/keys/attributes[/{scope}]` | Listar chaves de atributos existentes (opcionalmente por scope) |
| GET | `/api/plugins/telemetry/{entityType}/{entityId}/keys/timeseries` | Listar chaves de timeseries existentes |
| GET | `/api/plugins/telemetry/{entityType}/{entityId}/values/attributes[/{scope}]` | Valores de atributos |
| GET | `/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys=...` | Última(s) leitura(s), ou série no intervalo com `startTs`/`endTs` |
| GET | `/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries/history?keys=...&startTs=...&endTs=...` | Endpoint dedicado para consulta histórica (variante do endpoint acima) |
| POST | `/api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}` | Setar atributos |
| POST | `/api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}` | Inserir telemetria (uso admin/backfill) |
| POST | `/api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}/{ttl}` | Idem, com TTL (em segundos) para a telemetria inserida |
| POST | `/api/plugins/telemetry/{deviceId}/{scope}` | Atalho legado só para device (sem precisar `entityType`) |
| DELETE | `/api/plugins/telemetry/{entityType}/{entityId}/timeseries/delete?keys=...` | Apagar timeseries |
| DELETE | `/api/plugins/telemetry/{entityType}/{entityId}/{scope}?keys=...` | Remover atributos de uma scope |
| DELETE | `/api/plugins/telemetry/{deviceId}/{scope}?keys=...` | Idem, atalho legado só para device |

## API de transporte do device (usa access token, não JWT)

| Método | Path/Tópico | Descrição |
|---|---|---|
| POST | `/api/v1/{TOKEN}/telemetry` | Publicar telemetria (HTTP) |
| POST | `/api/v1/{TOKEN}/attributes` | Publicar atributos client-side (HTTP) |
| GET | `/api/v1/{TOKEN}/attributes` | Ler atributos shared/client (HTTP) |
| MQTT | `v1/devices/me/telemetry` | Publicar telemetria (MQTT) |
| MQTT | `v1/devices/me/attributes` | Publicar/assinar atributos (MQTT) |
| MQTT | `v1/devices/me/rpc/request/+` | Receber RPC do servidor (MQTT) |

## Alarmes

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/alarm` | Criar/atualizar alarme |
| GET | `/api/alarm/{alarmId}` | Buscar por id |
| GET | `/api/alarm/info/{alarmId}` | Buscar com info do originador |
| POST | `/api/alarm/{alarmId}/ack` | Reconhecer |
| POST | `/api/alarm/{alarmId}/clear` | Limpar |
| DELETE | `/api/alarm/{alarmId}` | Remover |
| GET | `/api/alarm/{entityType}/{entityId}?searchStatus=...` | Listar alarmes de uma entidade |

## Relações

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/relation` | Criar relação |
| DELETE | `/api/relation?fromId=...&toId=...&type=...` | Remover relação |
| GET | `/api/relations/info?fromId=...&fromType=...` | Relações a partir de uma entidade |

## RPC (comandos síncronos/assíncronos ao device)

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/plugins/rpc/twoway/{deviceId}` | RPC bloqueante (espera resposta do device) |
| POST | `/api/plugins/rpc/oneway/{deviceId}` | RPC fire-and-forget |
| POST | `/api/rpc/persistent/{deviceId}` | RPC persistente (fila até device conectar) |

## Entidades gerais (Assets, Customers, Users, Dashboards)

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/asset` | Criar/atualizar asset |
| GET | `/api/tenant/assets?assetName=...` | Buscar asset por nome |
| POST | `/api/customer` | Criar/atualizar customer |
| POST | `/api/user` | Criar/atualizar usuário |
| GET | `/api/dashboard/{dashboardId}` | Obter dashboard (config completa em JSON) |
| POST | `/api/dashboard` | Criar/atualizar dashboard |

## Somente PE (validar disponibilidade na instância)

- White-labeling: `/api/whiteLabel/*`
- Edge management: `/api/edge/*`
- Mobile Application Center: `/api/mobile/*`
- Scheduler: `/api/schedulerEvent/*`
- Roles customizadas (RBAC granular): `/api/role/*`
