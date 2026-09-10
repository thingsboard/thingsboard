---
name: tb-gateway
description: 'ThingsBoard IoT Gateway e a API MQTT de gateway — conectar equipamento industrial que não fala MQTT/HTTP nativo (Modbus, OPC-UA, BACnet, S7, SNMP, KNX, CAN, BLE, OCPP, ODBC, FTP, socket) ou escrever seu próprio publicador nos tópicos v1/gateway/connect, telemetry e attributes. Use ao configurar tb_gateway.json ou um conector, mapear registrador Modbus para chave de telemetria, escrever converter, ou depurar envio de dados. Em CE o Gateway é obrigatório, porque Platform Integrations é só do PE. Sintomas típicos - o device não aparece no ThingsBoard, timeout no slave Modbus, valor lido vem multiplicado por 10, byteOrder e wordOrder, unitId errado, perde dados quando cai a rede, publisher lento porque manda um ponto por mensagem em vez de lote, tamanho máximo de payload MQTT, handshake TLS falha contra instância on-premise com certificado self-signed, RPC do dashboard não chega no equipamento.'
---

# ThingsBoard IoT Gateway

Serviço **separado** (Python) que fica entre o equipamento de campo e o ThingsBoard. Fala
o protocolo do equipamento de um lado e MQTT do ThingsBoard do outro, aparecendo na
plataforma como um device "gateway" que publica em nome de vários devices filhos.

**Em CE isso não é opcional.** *Platform Integrations* — os adaptadores de protocolo
embutidos na plataforma — é recurso exclusivo do PE. Num CE, todo equipamento que não fala
MQTT/HTTP/CoAP nativo passa pelo Gateway. Ver `tb-ce-vs-pe`.

## Antes de começar — fixe a versão

> **Se o MCP oficial do ThingsBoard estiver conectado** ([`thingsboard/thingsboard-mcp`](https://github.com/thingsboard/thingsboard-mcp),
> 120+ tools, funciona em CE e PE): use as tools dele para **ler e escrever dados** —
> devices, assets, telemetria, alarmes, relações, OTA. São melhores nisso que qualquer
> comando aqui. Use `scripts/tb.mjs` para o que o MCP **não** responde: qual endpoint
> existe nesta versão (`api`, `spec`), quais rule nodes esta instância tem (`nodes`), e
> se é CE ou PE (`check`). O MCP faz dado; esta skill faz contrato e conhecimento.

Conectores e chaves de config mudam entre releases, e **a lista de conectores do `master`
não é a do último release**.

```bash
node scripts/verify-gateway.mjs                 # confere a skill contra o último release
node scripts/verify-gateway.mjs --ref 3.8.4     # contra uma versão específica
```

Exemplo real dessa armadilha: o conector **`s7`** (PLC Siemens) existe no `master` e
**não** existe no release `3.8.4`. Quem lê a documentação do repositório e assume que está
disponível instala a versão estável e não encontra o conector.

Catálogo verificado em [references/connectors.md](references/connectors.md).

## Arquitetura de configuração

O arquivo principal é **`tb_gateway.json`** (JSON, não YAML — documentação e tutoriais
antigos ainda dizem `tb_gateway.yaml`). Quatro chaves de topo:

```
tb_gateway.json
├── thingsboard   → como falar com a plataforma (host, port, security, rate limits)
├── storage       → fila local que segura dados quando a plataforma cai
├── grpc          → conectores externos por gRPC (opcional)
└── connectors[]  → lista que aponta para os arquivos de conector
```

Cada conector tem **arquivo próprio** (`modbus.json`, `mqtt.json`, `opcua.json`…). O
`tb_gateway.json` só referencia; a configuração do protocolo vive no arquivo do conector.

### `thingsboard`

```json
{
  "host": "meu-tb.local",
  "port": 1883,
  "security": { "type": "accessToken", "accessToken": "TOKEN_DO_DEVICE_GATEWAY" },
  "qos": 1,
  "checkingDeviceActivity": { "checkDeviceInactivity": false, "inactivityTimeoutSeconds": 300 }
}
```

- O `accessToken` é o do **device que representa o gateway** no ThingsBoard, com a flag
  *"É gateway"* ligada no cadastro. Não é credencial de usuário e não é um token por
  equipamento de campo.
- `security.type` também aceita `tls` (com `caCert`/`privateKey`) e `usernamePassword`.
- `port` **1883 é MQTT**, não 8080. Apontar para a porta HTTP é erro comum e o sintoma é
  conexão recusada sem mensagem clara.

### `storage` — o que decide se você perde dado

Três tipos, e a escolha importa mais do que parece:

| `type` | Comportamento | Quando usar |
|---|---|---|
| `memory` | fila em RAM; **perde tudo se o processo reiniciar** | teste, bancada |
| `file` | grava em arquivos no disco | produção sem SQLite |
| `sqlite` | banco local com TTL | **produção** — mais robusto |

O default do arquivo de exemplo é `memory`. Em campo, com rede instável — que é o caso de
instalação industrial — isso significa perder telemetria em cada reinício. Trocar para
`sqlite` é provavelmente a primeira alteração que você deve fazer.

## Modbus

O conector com mais problemas relatados no repositório oficial. Estrutura:

```json
{ "master": { "slaves": [ {
  "type": "tcp", "host": "192.168.0.10", "port": 502, "unitId": 1,
  "deviceName": "Piezometro-01", "deviceType": "default",
  "pollPeriod": 5000, "byteOrder": "BIG", "wordOrder": "BIG",
  "timeseries": [
    { "tag": "pressao", "type": "16int", "address": 0,
      "objectsCount": 1, "functionCode": 3, "divider": 10 }
  ],
  "attributes": [], "rpc": [], "attributeUpdates": []
} ] } }
```

Campos que causam os erros mais comuns:

- **`divider`** — o valor lido é dividido por ele. Sensor que envia `253` para 25,3 °C usa
  `"divider": 10`. Se o valor chega multiplicado por 10, é aqui. Existe também
  `multiplier` para o caso inverso.
- **`byteOrder` / `wordOrder`** (`BIG` ou `LITTLE`) — valor vindo absurdo (ex. `1.2e-38`
  em vez de `25.3`) num `32float` é quase sempre `wordOrder` trocado, não sensor com
  defeito. Teste as duas combinações antes de suspeitar do equipamento.
- **`unitId`** — endereço do escravo. Em Modbus TCP muita gente deixa `0`; vários
  equipamentos só respondem em `1`. Timeout sem erro de rede costuma ser isto.
- **`functionCode`** — `3` holding register (o mais comum), `4` input register,
  `1`/`2` coils/discrete inputs, `5`/`6`/`15`/`16` para escrita.
- **`type`** — `16int`, `32int`, `32float`, `bits`, `string`. Incompatibilidade entre o
  tipo declarado e `objectsCount` produz leitura silenciosamente errada, não erro.
- **`pollPeriod`** em ms. Agressivo demais em RS-485 serial derruba o barramento; comece
  em `5000` e reduza medindo.

Serial (RTU) usa o mesmo esquema com `"type": "serial"`, mais `port` (ex. `/dev/ttyUSB0`),
`baudrate`, `bytesize`, `parity`, `stopbits` — ver `modbus_serial.json` no repositório.

## MQTT e o modelo de converter

Conectores baseados em mensagem (MQTT, REST, socket, request) usam **converter** com
substituição por `${expressao}`:

```json
{ "mapping": [ {
  "topicFilter": "sensores/+/dados",
  "subscriptionQos": 1,
  "converter": {
    "type": "json",
    "deviceInfo": {
      "deviceNameExpressionSource": "topic",
      "deviceNameExpression": "(?<=sensores/)(.*?)(?=/dados)"
    },
    "timeseries": [ { "type": "double", "key": "temperatura", "value": "${temp}" } ]
  }
} ] }
```

- `deviceNameExpressionSource` decide de onde sai o nome do device: `topic` (regex sobre o
  tópico), `message` (campo do payload) ou `constant`. Errar isso faz **todos** os
  equipamentos virarem um device só na plataforma.
- `${campo}` lê do payload JSON; a regex lê do tópico. São mecanismos diferentes.
- `"type": "custom"` permite converter em Python próprio, para payload binário ou
  proprietário. É a saída quando o payload não é JSON.

## Publicar direto na API MQTT de gateway (sem usar o serviço)

Não é obrigatório rodar o Gateway em Python. Qualquer programa pode falar o **mesmo
protocolo MQTT** e publicar em nome de vários devices — é o que faz um conversor próprio
que lê arquivo de instrumento e envia para a plataforma.

Tópicos, conforme `MqttTopics.java` no fonte do ThingsBoard (prefixo `v1/gateway`):

| Tópico | Payload |
|---|---|
| `v1/gateway/connect` | `{"device": "Piezometro-01"}` — registra o device filho |
| `v1/gateway/telemetry` | `{"Piezometro-01": [{"ts": 1773585000000, "values": {...}}, …]}` |
| `v1/gateway/attributes` | `{"Piezometro-01": {"serial": "...", "versao": "..."}}` |
| `v1/gateway/disconnect` | `{"device": "Piezometro-01"}` |
| `v1/gateway/rpc` | RPC vindo da plataforma para o device filho |
| `v1/gateway/attributes/request` / `/response` | consulta de atributo do lado do device |

O **username** do MQTT é o access token do device gateway; não há senha. Porta 1883, ou
8883 com TLS.

### Envie em lote, não ponto a ponto

O array de `v1/gateway/telemetry` aceita **vários pontos por device e vários devices por
mensagem**. Publicar um ponto por mensagem, esperando o ack de cada, é o erro de
desempenho mais comum em publicador próprio: um mês de leitura horária vira 720 mensagens
e 720 round-trips, quando caberia em poucas.

O teto é o **payload máximo do transporte MQTT do ThingsBoard: 65536 bytes**
(`NETTY_MAX_PAYLOAD_SIZE` no `thingsboard.yml`). Mensagem maior é rejeitada.

Por isso **agrupe por bytes acumulados, não por contagem fixa**. Um ponto com 5 chaves tem
~150 bytes; um ponto de ShapeArray com 150 chaves passa de 3 KB. Um `chunk_size` fixo que
funciona num caso estoura no outro. Feche o lote quando o JSON serializado passar de
~48 KB, deixando folga para overhead.

Se o publicador passar pelo **serviço** Gateway em vez de falar direto com a plataforma,
vale também o `maxPayloadSizeBytes` do `tb_gateway.json`, que tem default bem menor.

### Reconexão no meio do envio

Publicação longa cai. Verifique a conexão a cada lote e reconecte antes de publicar, em
vez de descobrir pelo timeout do ack — com QoS 1 e ack de dezenas de segundos, uma queda
no meio de um envio grande custa o restante do lote.

### TLS contra instância on-premise

`cert_reqs=CERT_REQUIRED` sem informar a CA valida contra o trust store do sistema
operacional. Instalação on-premise atrás de Caddy/Nginx com certificado corporativo ou
self-signed **não está nesse trust store**, e a conexão falha com erro de handshake pouco
descritivo. Aponte a CA da planta explicitamente (em paho-mqtt, `tls_set(ca_certs=...)`).
Desligar a verificação resolve o sintoma e cria um problema pior.

## Sentido inverso: RPC e attributeUpdates

O Gateway não é só leitura.

- **`rpc`** — comando vindo do dashboard/rule chain chega no equipamento. É como um botão
  no dashboard aciona um relé. Ver `tb-widgets-dashboards` para o lado do widget.
- **`attributeUpdates`** — atributo `SHARED_SCOPE` alterado na plataforma é escrito no
  equipamento. Útil para setpoint.

Ambos precisam de `functionCode` de **escrita** no Modbus (`5`, `6`, `15`, `16`). Declarar
`3` num bloco de RPC é erro silencioso: o gateway aceita a config e a escrita nunca ocorre.

## Depuração

1. **O device apareceu no ThingsBoard?** Não → problema entre gateway e plataforma
   (token, host, porta 1883, firewall). Sim → problema no lado do protocolo.
2. **Logs**: `logs.json` controla os níveis. Suba o conector para `DEBUG` antes de
   adivinhar — ele registra cada leitura e o valor bruto antes da conversão.
3. **Isole o protocolo.** Antes de culpar o gateway, leia o registrador com uma ferramenta
   independente (`modpoll`, cliente OPC-UA). Se ela também não lê, o problema é rede ou
   equipamento.
4. **`remoteConfiguration`** permite editar a config pela UI do ThingsBoard. Conveniente,
   mas significa que o arquivo local deixa de ser a fonte da verdade — ao depurar, confirme
   qual das duas está valendo.

## Referência

- [references/connectors.md](references/connectors.md) — catálogo de conectores com
  proveniência fixada e disponibilidade por versão.
