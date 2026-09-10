---
name: tb-deploy-admin
description: Infraestrutura do ThingsBoard PE/CE self-hosted — Docker Compose (monolito e microsserviços), PostgreSQL, Cassandra, TimescaleDB, Kafka, licença PE, upgrade de versão, backup e restore, HA, reverse proxy Nginx/Traefik. Use ao instalar, subir, atualizar, escalar ou consertar a plataforma em si (não a lógica dentro dela). Sintomas típicos - o container não sobe ou reinicia em loop, "Port 443 is already in use", "connection refused" no banco, docker pull da imagem PE dá unauthorized, licença PE inválida ou expirada, lag crescente na fila do Kafka, dashboard não atualiza em tempo real porque o websocket está bloqueado pelo proxy, escolher banco para telemetria, logs em /var/log/thingsboard, migrar de versão sem pular major.
---

# ThingsBoard Deploy & Administração (self-hosted, CE e PE)

## Antes de começar — fixe a versão e a edição primeiro

Praticamente toda resposta desta skill depende de duas variáveis: **qual versão** e
**CE ou PE**. Determine-as antes de propor qualquer passo concreto.

```bash
node scripts/tb.mjs check     # edição CE/PE, versão da API, build, e se a instância responde
```

Se a instância não estiver de pé (o caso comum aqui — é justamente o que se quer
consertar), caia para: tag da imagem em `docker compose config`, `docker image ls`,
`Help → About` na UI, ou `docker logs <container> | head -50`.

1. Confirme a versão atual instalada (comando acima, ou tag da imagem Docker em uso).
2. Se for uma operação de upgrade, confirme a versão de destino e avise o usuário para
   checar as **release/upgrade notes oficiais** daquele intervalo de versões — em geral
   ThingsBoard recomenda upgrade sequencial (não pular versões major) e o script de
   upgrade de banco muda por versão.
3. **PE usa registry Docker privado** (requer login com credenciais associadas à licença),
   diferente do CE que fica no Docker Hub público — não assumir que `docker pull` de uma
   imagem PE funciona sem `docker login` prévio.

## Modos de instalação

| Modo | Quando usar |
|---|---|
| **Monolítico** (`tb` único container/processo) | Ambientes pequenos/médios, PoC, single-tenant, menor complexidade operacional |
| **Microsserviços** (`tb-core`, `tb-rule-engine`, `tb-transport-*`, `tb-web-ui` separados + Kafka/Zookeeper) | Alta escala, necessidade de escalar componentes independentemente, HA real |

Fila (`queue`) e cache são configuráveis independente do modo. **Confirmado direto no
`thingsboard.yml` do repositório oficial** (`TB_QUEUE_TYPE`, comentário do próprio
arquivo): no **CE público, as únicas opções são `in-memory` ou `kafka`** — RabbitMQ, AWS
SQS, GCP Pub/Sub e Azure Service Bus **não aparecem na configuração CE**, então tratar
como recursos de PE/Cloud, não assumir disponibilidade em instalação CE.

- Fila: `in-memory` (só monolito/dev — mensagens **se perdem** em restart), `kafka`
  (padrão em produção/microsserviços — durável, persiste entre restarts, obrigatório em
  cluster). RabbitMQ/AWS SQS/GCP Pub/Sub/Azure Service Bus — só PE/Cloud.
- Cache: `caffeine` (in-process, só single instance, default) ou `redis` (compartilhado
  entre instâncias — obrigatório se houver mais de um `tb-core`). Env var: `CACHE_TYPE`.

### Filas do Rule Engine (dentro do backend de queue escolhido)

Confirmado na doc oficial: as filas garantem entrega sob carga, controlam picos e
ordenam mensagens submetidas aos rule chains. Configuração só por administrador de
sistema (não por tenant admin).

- **Filas padrão**: `Main` (entrada padrão, estratégia Burst), `HighPriority` (alarmes/
  notificações, Burst com retry), `SequentialByOriginator` (garante ordem por entidade).
- Um rule node individual pode ser roteado para uma fila específica via o campo
  `queueName` na configuração do node (visto em produção como `null` = usa a fila padrão
  do rule chain) — útil para isolar processamento de alarme/notificação numa fila de
  alta prioridade sem competir com o volume de telemetria normal na fila `Main`.
- **Estratégia de submit** (como mensagens chegam ao rule chain): Burst (tudo de uma
  vez), Batch (lotes fixos aguardando ack), Sequential by originator/by tenant/global
  (ordem garantida, mais lento quanto mais global).
- **Estratégia de retry**: Skip all failures, Skip all failures and timeouts, Retry
  failed, Retry timeout, Retry failed and timeout, Retry all (reenvia o lote inteiro se
  1 mensagem falhar).
- **Cuidado**: a fila `HighPriority` reententa indefinidamente por padrão — um erro
  lógico persistente num node dessa fila pode travar o processamento de alarmes
  permanentemente até alguém corrigir a causa raiz; monitorar lag dessa fila
  especificamente.
- Partições maiores = mais paralelismo; ajustar junto com intervalo de polling e timeout
  de processamento por fila.

## Docker Compose — pontos de atenção

Serviços típicos: `postgres` (ou banco escolhido), `thingsboard` (monolito) ou
`tb-core`/`tb-rule-engine`/`tb-mqtt-transport`/`tb-http-transport`/`tb-coap-transport`
(microsserviços), `kafka`+`zookeeper` (se fila = kafka), `redis` (se cache/sessão
compartilhada).

Portas padrão a mapear:

| Porta | Protocolo |
|---|---|
| 8080 | HTTP (UI + REST API) |
| 1883 | MQTT |
| 5683/5684 | CoAP / CoAP+DTLS |
| 7070 | Edge RPC (se usar ThingsBoard Edge) |
| 9090 | gRPC transport (comunicação interna entre microsserviços) |

Variáveis de ambiente centrais — **confirmadas direto no `thingsboard.yml` do
repositório oficial** (nomes e defaults exatos, não aproximação):

- `DATABASE_TS_TYPE` (default `sql`): `sql` | `cassandra` | `timescale` — onde fica
  timeseries. `DATABASE_TS_LATEST_TYPE` (default `sql`) é uma variável **separada** para
  onde ficam os últimos valores (latest) — pode divergir de `DATABASE_TS_TYPE` em modo
  híbrido.
- `SPRING_DATASOURCE_URL` (default `jdbc:postgresql://localhost:5432/thingsboard`) /
  `SPRING_DATASOURCE_USERNAME` (default `postgres`) / `SPRING_DATASOURCE_PASSWORD`
  (default `postgres`): conexão com o banco relacional. `SPRING_DATASOURCE_MAXIMUM_POOL_SIZE`
  (default `16`) controla o pool de conexões (HikariCP).
- `TB_QUEUE_TYPE` (default `in-memory`): só `in-memory` ou `kafka` no CE — ver nota
  acima. `TB_QUEUE_PREFIX` prefixa todos os tópicos/consumer groups (útil para múltiplas
  instâncias no mesmo cluster Kafka).
- `CACHE_TYPE` (default `caffeine`): `caffeine` | `redis`.
- `TB_SERVICE_ID` (default vazio): identifica esta instância dentro de um cluster —
  relevante tanto para coordenação entre nós quanto para ativação de licença PE. Nome
  exato de outras variáveis de licença PE varia por versão — checar doc oficial de
  instalação PE correspondente.

Ver [references/docker-compose-example.yml](references/docker-compose-example.yml) para
um esqueleto ilustrativo (monolítico, Postgres).

## Licenciamento (PE)

- Instância PE precisa de chave de licença ativa; sem ela, funcionalidades PE ficam
  bloqueadas ou o serviço recusa subir, dependendo da versão.
- Ativação normalmente feita via UI (`Settings → License`) ou variável de ambiente na
  subida do container.
- Erros de "license invalid/expired" nos logs geralmente indicam: chave vencida, chave
  associada a outro `TB_SERVICE_ID`/host, ou relógio do host dessincronizado (NTP).

## Upgrade de versão

Fluxo geral (validar passo a passo contra a doc oficial da versão específica):

1. Backup completo do banco (ver seção Backup) e, se possível, snapshot dos volumes.
2. Parar o serviço ThingsBoard (manter banco no ar).
3. Rodar o script de upgrade de schema (`upgrade.sh`/`upgrade.bat` ou equivalente da
   imagem, tipicamente com `--fromVersion=X.Y.Z`).
4. Subir a nova versão da imagem/binário.
5. Validar: rule chains, dashboards e widgets continuam funcionando; checar logs por
   erros de migração.
6. Em microsserviços, atualizar todos os componentes (`tb-core`, `tb-rule-engine`,
   transports) para a mesma versão — não misturar versões entre componentes.

Isso não é cautela hipotética — é um padrão recorrente no issue tracker do
ThingsBoard: mudança de configuração de SSL/credenciais quebrando após upgrade
([thingsboard#5617](https://github.com/thingsboard/thingsboard/issues/5617)), instância
que não conecta mais depois de atualizar
([thingsboard#5239](https://github.com/thingsboard/thingsboard/issues/5239)), e saltos
de versão causando problemas variados
([thingsboard#11630](https://github.com/thingsboard/thingsboard/issues/11630)). Testar
o upgrade num ambiente separado com uma cópia do banco antes de rodar em produção não é
opcional para um upgrade que pula mais de uma versão minor.

## Backup & Restore

- **Postgres**: `pg_dump`/`pg_restore` do banco `thingsboard` (metadados + timeseries se
  `DATABASE_TS_TYPE=sql`).
- **Cassandra**: snapshot via `nodetool snapshot` + cópia dos SSTables.
- **Complementar**: exportar rule chains e dashboards críticos como JSON (via UI/API) —
  mais rápido de restaurar seletivamente do que um restore completo de banco.
- Testar o restore periodicamente em ambiente separado — backup não testado não é backup.

## HA e escalabilidade

- Múltiplas instâncias `tb-core`/`tb-rule-engine` atrás de load balancer (para
  `tb-core`, sticky session ou WebSocket-aware LB para telemetria em tempo real na UI).
- Kafka como fila obrigatório para múltiplas instâncias de rule engine coordenarem
  particionamento de mensagens.
- Redis obrigatório para cache/sessão compartilhados entre instâncias de `tb-core`.
- Transports (`mqtt`, `http`, `coap`) escalam horizontalmente de forma mais simples,
  atrás de LB de rede (TCP para MQTT).

## Troubleshooting

- Logs: dentro do container em `/var/log/thingsboard` (ou `docker logs <container>` se
  não houver volume de log mapeado).
- Erros comuns:
  - Conexão recusada ao banco → checar ordem de subida (banco pronto antes do TB) e
    healthcheck no compose.
  - Fila com lag crescente → checar throughput de `tb-rule-engine`, possível gargalo em
    node externo (REST call, email) dentro de uma rule chain. Perda/atraso de mensagem
    entre gateway↔Kafka é um padrão recorrente relatado por usuários (ex.
    [thingsboard#9783](https://github.com/thingsboard/thingsboard/issues/9783), nunca
    confirmado como bug da plataforma) — antes de suspeitar de perda real, monitorar lag
    de consumer group do Kafka e comportamento de retry/QoS do lado do gateway/device;
    geralmente a causa é configuração de retry insuficiente no cliente, não o Kafka
    "comendo" mensagem.
  - Licença inválida (PE) → ver seção Licenciamento.
  - UI carrega mas dashboards não atualizam em tempo real → checar WebSocket
    (`/api/ws`) não bloqueado por proxy/LB intermediário.
  - `HTTP_BIND_PORT`/`HTTP_BIND_ADDRESS` alterado e o serviço não sobe, log mostra
    `Port X is already in use` → **quase sempre é conflito de porta real, não bug de
    config** ([thingsboard#7539](https://github.com/thingsboard/thingsboard/issues/7539)):
    comum tentar bindar a porta 443 diretamente enquanto um reverse proxy (Traefik/Nginx)
    já está ocupando essa porta no host. Manter o ThingsBoard na porta interna padrão
    (8080) e deixar o reverse proxy expor 443/TLS externamente, em vez de mudar o bind
    interno do TB.

## Referência

Ver [references/docker-compose-example.yml](references/docker-compose-example.yml).
