# Conectores do ThingsBoard IoT Gateway

<!-- PROVENANCE:BEGIN (gerado por scripts/verify-gateway.mjs — não editar à mão) -->
> **Proveniência fixada.** Verificado contra
> [`thingsboard/thingsboard-gateway`](https://github.com/thingsboard/thingsboard-gateway/tree/a735a2d654a218c007b5db7759ceed44794253f9/thingsboard_gateway/connectors/),
> release **`3.8.4`**, commit **`a735a2d654a2`**, em **2026-09-04**.
> 15 conectores distribuídos nessa release; 3 chaves de config conferidas.
>
> Reverificar noutra versão: `node scripts/verify-gateway.mjs --ref X.Y.Z`.
<!-- PROVENANCE:END -->

Conector que não existe na versão instalada é o erro mais barato de cometer e mais chato de
diagnosticar: o gateway sobe, ignora a entrada desconhecida, e nada acontece.
`scripts/verify-gateway.mjs` confere esta tabela contra o código-fonte da release alvo.

## Catálogo

| Conector | Protocolo / uso | Arquivo de exemplo |
|---|---|---|
| `modbus` | Modbus TCP e RTU (serial) — instrumentação industrial | `modbus.json`, `modbus_serial.json` |
| `opcua` | OPC-UA — PLC e SCADA modernos | `opcua.json` |
| `bacnet` | BACnet — automação predial (HVAC) | `bacnet.json` |
| `mqtt` | Broker MQTT externo (bridge) | `mqtt.json` |
| `snmp` | SNMP — equipamento de rede, nobreak | `snmp.json` |
| `knx` | KNX — automação predial | `knx.json` |
| `can` | CAN bus — automotivo, veicular | `can.json` |
| `ble` | Bluetooth Low Energy | `ble.json` |
| `ocpp` | OCPP — estação de recarga de veículo elétrico | `ocpp.json` |
| `odbc` | Banco de dados via ODBC | `odbc.json` |
| `ftp` | Arquivos em servidor FTP | `ftp.json` |
| `rest` | Gateway expõe endpoint REST para o equipamento chamar | `rest.json` |
| `request` | Gateway chama endpoint HTTP externo (polling) | `request.json` |
| `socket` | TCP/UDP cru | `socket.json` |
| `xmpp` | XMPP | `xmpp.json` |

## Disponibilidade por versão

| Conector | Situação |
|---|---|
| `s7` | Siemens S7 (PLC Siemens). **Existe no `master`, ausente do release `3.8.4`.** Confirme na versão que você vai instalar antes de planejar em cima dele. |

Esta seção é o motivo de o verificador existir. A lista de conectores do branch de
desenvolvimento não é a da versão que você instala via `pip`.

## Arquivo principal — `tb_gateway.json`

Chaves de topo: `thingsboard`, `storage`, `grpc`, `connectors`.

### `thingsboard`

| Chave | Nota |
|---|---|
| `host` / `port` | `1883` é MQTT. Apontar para a porta HTTP (8080) falha sem mensagem clara |
| `security.type` | `accessToken`, `tls` (com `caCert` / `privateKey`), `usernamePassword` |
| `security.accessToken` | token do **device gateway**, com a flag "É gateway" ligada |
| `qos` | QoS do MQTT para a plataforma |
| `checkingDeviceActivity` | marca device como inativo após `inactivityTimeoutSeconds` |
| `remoteConfiguration` | permite editar a config pela UI; o arquivo local deixa de ser a fonte da verdade |
| `maxPayloadSizeBytes` | payload maior é rejeitado |
| `rateLimits` / `dpRateLimits` | limites de envio; batem com os limites do lado da plataforma |

### `storage`

| `type` | Implementação | Perde dado ao reiniciar? |
|---|---|---|
| `memory` | `memory_event_storage.py` | **sim** |
| `file` | `file_event_storage.py` | não |
| `sqlite` | `sqlite_event_storage.py` | não |

O exemplo distribuído vem com `memory`. Para instalação em campo, troque para `sqlite`.

## Campos de mapeamento do Modbus

Cada entrada de `timeseries`, `attributes`, `rpc` e `attributeUpdates` usa:

| Campo | Nota |
|---|---|
| `tag` | vira a chave da telemetria/atributo no ThingsBoard |
| `address` | endereço do registrador |
| `objectsCount` | quantos registradores ler; precisa bater com `type` |
| `functionCode` | `3` holding, `4` input, `1`/`2` coil/discrete, `5`/`6`/`15`/`16` escrita |
| `type` | `16int`, `32int`, `32float`, `bits`, `string` |
| `divider` | valor lido é dividido por ele (`253` com divider `10` → `25.3`) |
| `multiplier` | inverso do `divider` |

Do lado do escravo (`slaves[]`): `type` (`tcp`/`udp`/`serial`), `host`, `port`, `unitId`,
`deviceName`, `deviceType`, `pollPeriod`, `byteOrder`, `wordOrder`, `timeout`, `retries`,
`connectAttemptCount`, `waitAfterFailedAttemptsMs`.

## Checklist de instalação em campo

- [ ] `storage.type` **não** é `memory`
- [ ] `port` é 1883 (MQTT), não a porta da UI
- [ ] O device gateway existe no ThingsBoard com a flag "É gateway" ligada
- [ ] `unitId` confirmado com o fabricante do equipamento (raramente é `0`)
- [ ] `byteOrder`/`wordOrder` validados contra uma leitura conhecida
- [ ] `pollPeriod` sustentável pelo barramento (serial é mais sensível)
- [ ] Blocos de `rpc`/`attributeUpdates` usam `functionCode` de escrita
- [ ] Conector confirmado como presente na versão instalada (`verify-gateway.mjs`)
