---
name: tb-rest-api
description: REST API do ThingsBoard (CE/PE) — autenticação JWT, provisionamento de devices em massa, envio/consulta de telemetria e atributos, alarmes, relações, RPC. Use ao escrever, revisar ou depurar script/client (Python, Node.js, curl, Postman) que consome a API, ou ao integrar sistema externo com o ThingsBoard. Sintomas típicos - "erro 401 na API", "403 forbidden", "Authentication failed", "Invalid access token", token JWT expirou, "não sei qual endpoint usar", como pegar o access token do device, timestamp da telemetria em segundos vs milissegundos, provisionar devices de um CSV/planilha, paginação pageSize/page, header X-Authorization Bearer, POST /api/v1/token/telemetry, consultar swagger da instância.
---

# ThingsBoard REST API

## Antes de começar — verifique, não adivinhe

> **Se o MCP oficial do ThingsBoard estiver conectado** ([`thingsboard/thingsboard-mcp`](https://github.com/thingsboard/thingsboard-mcp),
> 120+ tools, funciona em CE e PE): use as tools dele para **ler e escrever dados** —
> devices, assets, telemetria, alarmes, relações, OTA. São melhores nisso que qualquer
> comando aqui. Use `scripts/tb.mjs` para o que o MCP **não** responde: qual endpoint
> existe nesta versão (`api`, `spec`), quais rule nodes esta instância tem (`nodes`), e
> se é CE ou PE (`check`). O MCP faz dado; esta skill faz contrato e conhecimento.

Endpoints e schemas variam entre versões e entre CE/PE. **Não gere código de integração a
partir das tabelas deste arquivo sem confirmar contra a instância alvo.** Existe uma
ferramenta para isso — use antes de escrever a primeira linha:

```bash
export TB_URL=https://host TB_USER=... TB_PASSWORD=...   # ou crie .tb.env (nunca commitar)

node scripts/tb.mjs check                     # alcançável? autenticou? CE ou PE? qual versão?
node scripts/tb.mjs api "telemetry|attribute" # quais paths existem MESMO nesta instância
node scripts/tb.mjs spec "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}" --method post
node scripts/tb.mjs get /api/auth/user        # GET autenticado, JWT gerenciado sozinho
```

`api` e `spec` leem o OpenAPI da própria instância (`/v3/api-docs`, cache de 24h) — é a
fonte da verdade, e mais barato que paginar a doc. `tb.mjs help` lista tudo.

Se **não** houver acesso à instância: use as convenções abaixo como ponto de partida e
**diga explicitamente ao usuário** que o contrato não foi verificado, apontando
`node scripts/tb.mjs api <termo>` como o passo de validação antes de rodar em produção.

PE expõe endpoints que CE não tem (white-labeling, Edge, Scheduler, Integrations) — o
`check` reporta a edição detectada, não presuma.

## Conceitos gerais

- **Duas "faces" da API**: a REST API de usuário (JWT, usada por UI/backoffice/integrações
  server-to-server) e a API de transporte de device (HTTP/MQTT/CoAP, autenticada com o
  *device access token*, usada pelo próprio device para publicar telemetria/atributos).
  Não confundir as duas — device nunca deveria carregar credenciais de usuário JWT.
- Todos os endpoints são via HTTPS, corpo em JSON (`Content-Type: application/json`).
- Timestamps são epoch **milissegundos**, não segundos.
- `entityType` é uma string maiúscula do enum (`DEVICE`, `ASSET`, `CUSTOMER`, `TENANT`,
  `USER`, `DASHBOARD`, ...) e aparece tanto em paths quanto em bodies de `EntityId`.
- Scopes de atributos: `CLIENT_SCOPE` (setado pelo device), `SHARED_SCOPE` (setado pelo
  backend, visível ao device), `SERVER_SCOPE` (interno, nunca vai ao device).
- Listagens paginadas usam `pageSize`, `page`, `sortProperty`, `sortOrder` (`ASC`/`DESC`)
  como query params e retornam `{ data: [...], totalPages, totalElements, hasNext }`.

## Autenticação (JWT)

```
POST /api/auth/login
{ "username": "...", "password": "..." }
→ { "token": "<JWT>", "refreshToken": "<JWT>" }
```

- Enviar o token nas chamadas seguintes no header `X-Authorization: Bearer <token>`.
- Access token expira (tipicamente minutos); usar `POST /api/auth/token` com o
  `refreshToken` para renovar sem novo login.
- Para automações/serviços, prefira gerar um usuário técnico com role restrita em vez de
  reusar credenciais de admin humano.

## Devices

- Criar: `POST /api/device` com body `{ "name": "...", "type": "...", "deviceProfileId": {...} }`
  (ou variante `/api/device?accessToken=...` para setar o token no ato).
- Obter credenciais (access token do device): `GET /api/device/{deviceId}/credentials`.
- Buscar por nome: `GET /api/tenant/devices?deviceName=...`.
- Deletar: `DELETE /api/device/{deviceId}`.
- device profiles controlam regras de transporte, rule chain padrão, alarmes e provisionamento.

## Telemetria

Consultar (via API de usuário, JWT):
```
GET /api/plugins/telemetry/{entityType}/{entityId}/values/timeseries
    ?keys=temperature,humidity&startTs=...&endTs=...&agg=NONE&interval=...&limit=100
```
```
GET /api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys=temperature   (latest)
```

Enviar telemetria em nome de uma entidade via API de usuário (uso administrativo/backfill,
não é o caminho normal de um device):
```
POST /api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}
{ "ts": 1700000000000, "values": { "temperature": 21.5 } }
```

`{scope}` é **path param**, não query string — o enum no OpenAPI de 4.3 tem um único valor
aceito, `ANY`, então na prática o path é `.../timeseries/ANY` (verificado com
`tb.mjs spec ... --method post` contra TB 4.3). Existe a variante
`.../timeseries/{scope}/{ttl}` quando se quer TTL explícito.

Caminho normal de um **device real** publicando telemetria (usa o access token do device,
não JWT):
```
POST /api/v1/{DEVICE_ACCESS_TOKEN}/telemetry
{ "temperature": 21.5, "humidity": 55 }
```
(equivalentes existem via MQTT tópico `v1/devices/me/telemetry` e via CoAP).

## Atributos

```
GET  /api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}
POST /api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}
DELETE /api/plugins/telemetry/{entityType}/{entityId}/{scope}?keys=k1,k2
```

## Alarmes

```
POST /api/alarm
{ "originator": {"entityType":"DEVICE","id":"..."}, "type": "High Temperature",
  "severity": "CRITICAL", "status": "ACTIVE_UNACK" }

GET  /api/alarm/{alarmId}
POST /api/alarm/{alarmId}/ack
POST /api/alarm/{alarmId}/clear
GET  /api/alarm/{entityType}/{entityId}?searchStatus=ACTIVE&pageSize=...&page=...
```

Normalmente alarmes são criados/limpos automaticamente por um rule chain (node
"Create Alarm"/"Clear Alarm") em vez de chamada manual via API — ver skill
`tb-rule-engine` para esse fluxo.

## Relações de entidades

```
POST /api/relation
{ "from": {...}, "to": {...}, "type": "Contains", "typeGroup": "COMMON" }
GET  /api/relations/info?fromId=...&fromType=...
```

## Erros comuns

- 401/403: token expirado (renovar) ou usuário sem permissão na role/tenant/customer.
- 400 em telemetria: `ts` fora de ms, ou `values` com tipos não suportados (evitar
  aninhar objetos complexos como valor de timeseries).
- Confundir `entityId` (UUID) com `id` de outro recurso (ex: deviceId vs device
  credentialsId são objetos diferentes).
- Rate limiting: instâncias PE costumam ter limites de API por tenant — para bulk
  operations, dar preferência a batch/telemetria via MQTT em vez de laços de POST via REST.

## Referência completa

Ver [references/endpoints.md](references/endpoints.md) para uma tabela mais ampla de
endpoints agrupados por recurso.
