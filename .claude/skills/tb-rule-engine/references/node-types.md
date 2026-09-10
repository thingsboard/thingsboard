# Catálogo de rule nodes — verificado no código-fonte oficial (CE)

<!-- PROVENANCE:BEGIN (gerado por scripts/verify-nodes.mjs — não editar à mão) -->
> **Proveniência fixada.** Fonte: [`thingsboard/thingsboard`](https://github.com/thingsboard/thingsboard/tree/a488a4c138971c0264bf7fdc879871f68eead1e4/rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/),
> ref **`v4.3.1.4`**, commit **`a488a4c13897`**, verificado em **2026-09-03**.
> Extraído da anotação `@RuleNode` de 77 classes (`type`, `name`, `relationTypes`).
>
> Reverificar contra um release mais novo: `node scripts/verify-nodes.mjs --ref vX.Y.Z`.
> CI mensal em `.github/workflows/verify-catalog.yml` abre issue quando o upstream diverge.
<!-- PROVENANCE:END -->

**Cobre só CE** (código público). Nodes exclusivos de PE/Cloud (Add to Group, Duplicate to
Group/Related, Generate Report, Twilio…) são closed-source e não aparecem neste repo — para
esses, a seção "Confirmado em instância PE real" no fim do arquivo é a fonte, e o caminho
definitivo é `node scripts/tb.mjs nodes` contra a própria instância, que lista os
descritores reais incluindo os de PE.

A categoria de cada node na paleta da UI vem do campo `type` da anotação (`ComponentType`),
que **nem sempre bate com o nome do pacote Java** — ex. `TbMsgTimeseriesNode` /
`TbMsgAttributesNode` moram no pacote `telemetry` mas são `ComponentType.ACTION`;
`TbMsgToEmailNode` mora em `mail` mas é `TRANSFORMATION`, enquanto `TbSendEmailNode` no
mesmo pacote é `EXTERNAL`. As tabelas abaixo agrupam pela categoria real da anotação, não
pelo pacote.

## Convenção de `relationTypes` (labels de conexão)

Quando a anotação não declara `relationTypes` explicitamente, o node usa o padrão do
framework: **`Success` / `Failure`**. Vale a exceção para nodes do tipo "Switch"
(`TbJsSwitchNode`, `TbMsgTypeSwitchNode`, `TbOriginatorTypeSwitchNode`), cujas conexões
são **dinâmicas** — calculadas em runtime a partir do script ou do tipo de
mensagem/entidade, não fixas. Nodes com `relationTypes` explícito na tabela abaixo usam
exatamente esses labels (nunca `Success`/`Failure` genérico) — usar o label errado numa
`connections[].type` faz a conexão nunca disparar.

## Filter

| Node (nome exibido) | Classe | `connections[].type` válidos |
|---|---|---|
| script | `org.thingsboard.rule.engine.filter.TbJsFilterNode` | `True`, `False` |
| switch | `org.thingsboard.rule.engine.filter.TbJsSwitchNode` | dinâmico (retorno do script) |
| message type filter | `org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode` | `True`, `False` |
| message type switch | `org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode` | dinâmico (nome do msgType) |
| entity type filter | `org.thingsboard.rule.engine.filter.TbOriginatorTypeFilterNode` | `True`, `False` |
| entity type switch | `org.thingsboard.rule.engine.filter.TbOriginatorTypeSwitchNode` | dinâmico (tipo de entidade) |
| asset profile switch | `org.thingsboard.rule.engine.filter.TbAssetTypeSwitchNode` | nome do asset profile, + `default` |
| device profile switch | `org.thingsboard.rule.engine.filter.TbDeviceTypeSwitchNode` | nome do device profile, + `default` |
| check fields presence | `org.thingsboard.rule.engine.filter.TbCheckMessageNode` | `True`, `False` |
| check relation presence | `org.thingsboard.rule.engine.filter.TbCheckRelationNode` | `True`, `False` |
| alarm status filter | `org.thingsboard.rule.engine.filter.TbCheckAlarmStatusNode` | `True`, `False` |
| gps geofencing filter | `org.thingsboard.rule.engine.geo.TbGpsGeofencingFilterNode` | `True`, `False` |

## Enrichment

| Node (nome exibido) | Classe |
|---|---|
| originator attributes | `org.thingsboard.rule.engine.metadata.TbGetAttributesNode` |
| originator fields | `org.thingsboard.rule.engine.metadata.TbGetOriginatorFieldsNode` |
| originator telemetry | `org.thingsboard.rule.engine.metadata.TbGetTelemetryNode` |
| calculate delta | `org.thingsboard.rule.engine.metadata.CalculateDeltaNode` |
| customer attributes | `org.thingsboard.rule.engine.metadata.TbGetCustomerAttributeNode` |
| customer details | `org.thingsboard.rule.engine.metadata.TbGetCustomerDetailsNode` |
| tenant attributes | `org.thingsboard.rule.engine.metadata.TbGetTenantAttributeNode` |
| tenant details | `org.thingsboard.rule.engine.metadata.TbGetTenantDetailsNode` |
| related device attributes | `org.thingsboard.rule.engine.metadata.TbGetDeviceAttrNode` |
| related entity data | `org.thingsboard.rule.engine.metadata.TbGetRelatedAttributeNode` |
| fetch device credentials | `org.thingsboard.rule.engine.metadata.TbFetchDeviceCredentialsNode` |

(Todos `Success`/`Failure`, **exceto `calculate delta`**, que declara também `Other`
— usado quando não há leitura anterior da chave para calcular o delta.)

## Transformation

| Node (nome exibido) | Classe | `connections[].type` |
|---|---|---|
| script | `org.thingsboard.rule.engine.transform.TbTransformMsgNode` | `Success`, `Failure` |
| change originator | `org.thingsboard.rule.engine.transform.TbChangeOriginatorNode` | `Success`, `Failure` |
| copy key-value pairs | `org.thingsboard.rule.engine.transform.TbCopyKeysNode` | `Success`, `Failure` |
| delete key-value pairs | `org.thingsboard.rule.engine.transform.TbDeleteKeysNode` | `Success`, `Failure` |
| rename keys | `org.thingsboard.rule.engine.transform.TbRenameKeysNode` | `Success`, `Failure` |
| json path | `org.thingsboard.rule.engine.transform.TbJsonPathNode` | `Success`, `Failure` |
| split array msg | `org.thingsboard.rule.engine.transform.TbSplitArrayMsgNode` | `Success`, `Failure` |
| to email | `org.thingsboard.rule.engine.mail.TbMsgToEmailNode` | `Success`, `Failure` |
| deduplication | `org.thingsboard.rule.engine.deduplication.TbMsgDeduplicationNode` | `Success`, `Failure` |

## Action

| Node (nome exibido) | Classe | `connections[].type` |
|---|---|---|
| create alarm | `org.thingsboard.rule.engine.action.TbCreateAlarmNode` | `Created`, `Updated`, `False` (**não** Success/Failure) |
| clear alarm | `org.thingsboard.rule.engine.action.TbClearAlarmNode` | `Cleared`, `False` |
| device profile (**deprecado**) | `org.thingsboard.rule.engine.profile.TbDeviceProfileNode` | `Alarm Created`, `Alarm Updated`, `Alarm Severity Updated`, `Alarm Cleared`, `Success`, `Failure` |
| device state | `org.thingsboard.rule.engine.action.TbDeviceStateNode` | `Success`, `Failure`, `Rate limited` |
| gps geofencing events | `org.thingsboard.rule.engine.geo.TbGpsGeofencingActionNode` | `Success`, `Entered`, `Left`, `Inside`, `Outside` |
| save time series | `org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode` | `Success`, `Failure` |
| save attributes | `org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode` | `Success`, `Failure` |
| delete attributes | `org.thingsboard.rule.engine.telemetry.TbMsgDeleteAttributesNode` | `Success`, `Failure` |
| calculated fields and alarm rules | `org.thingsboard.rule.engine.telemetry.TbCalculatedFieldsNode` | `Success`, `Failure` |
| log | `org.thingsboard.rule.engine.action.TbLogNode` | `Success`, `Failure` |
| message count | `org.thingsboard.rule.engine.action.TbMsgCountNode` | `Success`, `Failure` |
| math function | `org.thingsboard.rule.engine.math.TbMathNode` | `Success`, `Failure` |
| generator | `org.thingsboard.rule.engine.debug.TbMsgGeneratorNode` | `Success`, `Failure` |
| delay (**deprecado**) | `org.thingsboard.rule.engine.delay.TbMsgDelayNode` | `Success`, `Failure` |
| create relation | `org.thingsboard.rule.engine.action.TbCreateRelationNode` | `Success`, `Failure` |
| delete relation | `org.thingsboard.rule.engine.action.TbDeleteRelationNode` | `Success`, `Failure` |
| assign to customer | `org.thingsboard.rule.engine.action.TbAssignToCustomerNode` | `Success`, `Failure` |
| unassign from customer | `org.thingsboard.rule.engine.action.TbUnassignFromCustomerNode` | `Success`, `Failure` |
| copy to view | `org.thingsboard.rule.engine.action.TbCopyAttributesToEntityViewNode` | `Success`, `Failure` |
| save to custom table | `org.thingsboard.rule.engine.action.TbSaveToCustomCassandraTableNode` | `Success`, `Failure` |
| rpc call request | `org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode` | `Success`, `Failure` |
| rpc call reply | `org.thingsboard.rule.engine.rpc.TbSendRPCReplyNode` | `Success`, `Failure` |
| rest call reply | `org.thingsboard.rule.engine.rest.TbSendRestApiCallReplyNode` | `Success`, `Failure` |
| push to edge | `org.thingsboard.rule.engine.edge.TbMsgPushToEdgeNode` | `Success`, `Failure` |
| push to cloud (**Edge only**) | `org.thingsboard.rule.engine.edge.TbMsgPushToCloudNode` | `Success`, `Failure` |
| synchronization start | `org.thingsboard.rule.engine.transaction.TbSynchronizationBeginNode` | `Success`, `Failure` |
| synchronization end (**deprecado** — usar checkpoint) | `org.thingsboard.rule.engine.transaction.TbSynchronizationEndNode` | `Success`, `Failure` |

## External

| Node (nome exibido) | Classe |
|---|---|
| rest api call | `org.thingsboard.rule.engine.rest.TbRestApiCallNode` |
| send email | `org.thingsboard.rule.engine.mail.TbSendEmailNode` |
| send sms | `org.thingsboard.rule.engine.sms.TbSendSmsNode` |
| send notification | `org.thingsboard.rule.engine.notification.TbNotificationNode` |
| send to slack | `org.thingsboard.rule.engine.notification.TbSlackNode` |
| AI request | `org.thingsboard.rule.engine.ai.TbAiNode` |
| mqtt | `org.thingsboard.rule.engine.mqtt.TbMqttNode` |
| kafka | `org.thingsboard.rule.engine.kafka.TbKafkaNode` |
| rabbitmq | `org.thingsboard.rule.engine.rabbitmq.TbRabbitMqNode` |
| aws lambda | `org.thingsboard.rule.engine.aws.lambda.TbAwsLambdaNode` |
| aws sns | `org.thingsboard.rule.engine.aws.sns.TbSnsNode` |
| aws sqs | `org.thingsboard.rule.engine.aws.sqs.TbSqsNode` |
| gcp pubsub | `org.thingsboard.rule.engine.gcp.pubsub.TbPubSubNode` |
| azure iot hub | `org.thingsboard.rule.engine.mqtt.azure.TbAzureIotHubNode` |

(Todos `Success`/`Failure`.) Nota de segurança encontrada no código: o node **rest api
call** usa um `SsrfSafeAddressResolverGroup` — o ThingsBoard valida/restringe o endereço
de destino contra SSRF (não deixa apontar livremente para IPs internos/reservados) por
padrão.

## Flow

| Node (nome exibido) | Classe |
|---|---|
| rule chain | `org.thingsboard.rule.engine.flow.TbRuleChainInputNode` |
| output | `org.thingsboard.rule.engine.flow.TbRuleChainOutputNode` |
| acknowledge | `org.thingsboard.rule.engine.flow.TbAckNode` |
| checkpoint | `org.thingsboard.rule.engine.flow.TbCheckpointNode` |

## Confirmado em instância PE real (nodes fora do CE público)

Estes não existem no repositório CE (código fechado de PE/Cloud) — confirmados
inspecionando rule chains reais via `GET /api/ruleChain/{id}/metadata` numa instância PE:

| Node (nome exibido) | `type` confirmado |
|---|---|
| Add to Group | `org.thingsboard.rule.engine.action.TbAddToGroupNode` |
| Duplicate to Group | `org.thingsboard.rule.engine.transform.TbDuplicateMsgToGroupNode` |
| Entity Type Filter ("Is Entity Group") — via `TbOriginatorTypeFilterNode` acima também existe em CE; confirmado em uso real com esse propósito em PE. | — |

Demais nodes exclusivos de PE citados na doc oficial (Duplicate to Group by Name,
Duplicate to Related, Change Owner, Generate Report, Generate Dashboard Report,
Integration Downlink, Remove from Group, Twilio SMS/Voice) — nomes confirmados na doc,
`type` de classe não confirmado (nem no código público, nem visto em uso na instância
inspecionada). Exportar um rule chain real que os use para obter o `type` exato antes de
gerar JSON com eles via API.

## Analytics

Não encontrado como pacote separado no código público (`.../rule/engine/analytics` não
existe no repo em 2026-08-27, apesar da doc oficial descrever "aggregate stream",
"aggregate latest" e "alarms count" como nodes de Rule Engine) — os `type` de classe
para esses três seguem não confirmados; tratar como PE/Cloud-only até verificação
adicional (exportar rule chain real que os use, se disponível).

## Boas práticas ao escrever/editar rule chain JSON

- Usar exatamente os `connections[].type` da tabela acima para cada `type` de node —
  a maioria é `Success`/`Failure`, mas alarmes (`Created`/`Updated`/`Cleared`/`False`) e
  geofencing são exceções reais, não hipotéticas.
- TBEL é a linguagem recomendada sobre JavaScript puro nos Script nodes por rodar em
  sandbox mais restrito e com melhor performance (ver `references/tbel.md`).
- Nodes marcados **(deprecado)** (`device profile`, `delay`) ainda funcionam mas não
  devem ser usados em rule chains novas — a doc oficial já indica substitutos
  (Calculated Fields para lógica de alarme de profile; qualquer padrão de
  throttle/schedule para delay).
