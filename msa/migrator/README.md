Can be run with 
```shell
docker run --env-file migrator.env thingsboard/tb-migrator:latest
```

Example of `migrator.env` for tenant export: 
```
MIGRATOR_MODE=TENANT_DATA_EXPORT
WORKING_DIRECTORY=/tmp/tb-migration/data  # will be created automatically

POSTGRES_EXPORT_ENABLED=true  # to export all tenant's entities
EXPORTED_TENANT_ID=<TENANT_ID>  # id of the tenant to export
DATABASE_URL=jdbc:postgresql://<POSTGRES_HOST>:5432/thingsboard  
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
SKIPPED_TABLES=RULE_NODE_DEBUG_EVENT,STATS_EVENT,LC_EVENT  # Postgres tables to skip export for

CASSANDRA_EXPORT_ENABLED=true  # to export all tenant's timeseries history
CASSANDRA_URL=127.0.0.1:9042  # contact points
CASSANDRA_LOCAL_DATACENTER=datacenter1

PARALLELISM_LEVEL=4
```

Example of `migrator.env` for tenant import:
```
MIGRATOR_MODE=TENANT_DATA_IMPORT
WORKING_DIRECTORY=/tmp/tb-migration/data  # directory with all the previously exported tenant's data

POSTGRES_IMPORT_ENABLED=true  # to import all tenant's entities
DATABASE_URL=jdbc:postgresql://<POSTGRES_HOST>:5432/thingsboard  
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
SKIPPED_TABLES=RULE_NODE_DEBUG_EVENT,STATS_EVENT,LC_EVENT  # Postgres tables to skip import for

CASSANDRA_IMPORT_ENABLED=true  # to import all tenant's timeseries history
CASSANDRA_URL=127.0.0.1:9042  # contact points
CASSANDRA_LOCAL_DATACENTER=datacenter1
PARALLELISM_LEVEL=4
```
Before importing, create a temporary tenant and then delete it, so that system roles are created.
