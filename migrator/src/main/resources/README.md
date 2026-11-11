
Docker run command example:

```shell
docker run -v ./migration-data:/tb-migrator/data --env-file tb-migrator.env thingsboard/tb-migrator:latest
```


Env file example for tenant export:
```
MIGRATOR_MODE=TENANT_DATA_EXPORT
EXPORTED_TENANT_ID=<TENANT_ID>
SKIPPED_TABLES=RULE_NODE_DEBUG_EVENT,STATS_EVENT,LC_EVENT

DATABASE_URL=jdbc:postgresql://<POSTGRES_HOST>:5432/thingsboard
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

CASSANDRA_EXPORT_ENABLED=true

CASSANDRA_URL=<CASSANDRA_HOST>:9042
CASSANDRA_LOCAL_DATACENTER=us-east
CASSANDRA_KEYSPACE_NAME=thingsboard
```


Before importing, make sure to create (and then delete) a tenant so that system roles are created.

Env file example for tenant import:
```
MIGRATOR_MODE=TENANT_DATA_IMPORT
SKIPPED_TABLES=RULE_NODE_DEBUG_EVENT,STATS_EVENT,LC_EVENT

POSTGRES_IMPORT_ENABLED=true

DATABASE_URL=jdbc:postgresql://<POSTGRES_HOST>:5432/thingsboard
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

CASSANDRA_IMPORT_ENABLED=true
CASSANDRA_IMPORT_TS_KV_TTL=730

CASSANDRA_URL=cassandra:9042
CASSANDRA_LOCAL_DATACENTER=us-east
CASSANDRA_KEYSPACE_NAME=thingsboard
```