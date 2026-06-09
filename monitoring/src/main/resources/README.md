# tb-monitoring — RPC companion check

Each `monitoring.transports.{mqtt,http,coap,lwm2m}.targets[N]` accepts an optional
`rpc:` sub-block. When `rpc.enabled` is `true`, the monitoring loop runs an
extra round-trip on the same target per cycle, after the existing telemetry
uplink + WebSocket validation has succeeded.

```yaml
monitoring:
  transports:
    mqtt:
      request_timeout_ms: 4000
      targets:
        - base_url: 'tcp://${monitoring.domain}:1883'
          queue: 'Main'
          rpc:
            enabled: true
            request_timeout_ms: 4000
        - base_url: 'ssl://${monitoring.domain}:8883'   # secure variant
          queue: 'Main'
          rpc:
            enabled: true                                # inherits ssl + creds
```

## YAML keys

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `rpc.enabled` | bool | `false` | Opt in to the per-target RPC sub-check. |
| `rpc.request_timeout_ms` | int | transport `request_timeout_ms` | Sent through to ThingsBoard as the RPC `timeout`. Must be `< monitoring.rest.request_timeout_ms`. |

For LwM2M the default is `6000` ms because the Read goes through rule engine +
Leshan registration; the other transports default to `4000`. The default
`monitoring.rest.request_timeout_ms` is `8000` ms, leaving 2 s margin above
LwM2M; if you raise any per-target `rpc.request_timeout_ms` near or above the
REST timeout, raise `REST_REQUEST_TIMEOUT_MS` first or `initialize()` will
fail-fast at startup.

## Per-transport behaviour

| Transport | Device side | Cloud side | Assertion |
|-----------|-------------|------------|-----------|
| MQTT | Same Paho client subscribes `v1/devices/me/rpc/request/+` and publishes echoed `params` to `v1/devices/me/rpc/response/{id}` | `POST /api/rpc/twoway/{deviceId}` `{method:"monitoringCheck", params:{value:<uuid>}}` | echoed `value == uuid` |
| HTTP | Daemon long-poll thread per target: `GET /api/v1/{token}/rpc?timeout=1000`; on 200, `POST /api/v1/{token}/rpc/{id}` with the params | same as MQTT | echoed `value == uuid` |
| CoAP | Separate `CoapClient` opens an OBSERVE on `coap://host/api/v1/{token}/rpc`; on each notification, posts echoed params to `coap://host/api/v1/{token}/rpc/{id}` | same as MQTT | echoed `value == uuid` |
| LwM2M | None — the existing `Lwm2mClient` already serves `/3/0/0` via the standard LwM2M Read | `POST /api/rpc/twoway/{deviceId}` `{method:"Read", params:{key:"/3/0/0"}}` | response is non-blank |

## Failure attribution

RPC failures use a dedicated `RpcInfo` service key whose `getShortName()`
returns `"<transport.shortName> RPC"` (`"MQTT RPC"`, `"MQTT Foo RPC"` for
non-default queues, etc.). The IncidentManager header therefore distinguishes
`":red_circle: MQTT (n)"` from `":red_circle: MQTT RPC (m)"`. A telemetry
uplink failure short-circuits the RPC sub-check for that cycle, so a broken
transport will not double-count against both rows.

## Latencies

When the RPC check succeeds, the round-trip is reported under
`<key>RpcRoundTrip` (e.g. `mqttRpcRoundTrip`, `mqttFooRpcRoundTrip` for
non-default queues), alongside the existing `<key>Request` and `<key>WsUpdate`
keys for the same target. No latencies dashboard schema change is needed.

## Troubleshooting

- **`rpc.request_timeout_ms` ordering**: the RPC HTTP call goes through the
  same `monitoring.rest.request_timeout_ms` REST client. Keep
  `rpc.request_timeout_ms` strictly smaller so the device-side timeout fires
  first; otherwise the REST call will fail with `ResourceAccessException`
  before the RPC has a chance to time out cleanly.
- **HTTP polling thread leaks**: the long-poll thread is daemon and lifecycle-
  bound to `initClient` / `destroyClient`. If you see leaked threads, check
  whether a custom shutdown hook is bypassing `@PreDestroy`.
- **CoAP OBSERVE behind NAT**: the OBSERVE client uses the same Californium
  configuration as the telemetry client; if the telemetry check works but the
  RPC observe never delivers, check NAT/firewall on the CoAP UDP path.
- **LwM2M Read on `/3/0/0` returns blank**: the monitoring `Lwm2mClient`
  serves resource `0` from whatever was last sent via telemetry, so a blank
  result usually means telemetry never reached the device this cycle. Confirm
  the telemetry row is green first.
