# Description:
Tool used for migrating ThingsBoard into hybrid mode from Postgres.   
Performance of this tool depends on disk type and instance type (mostly on CPU resources).
But in general here are few benchmarks:
1. Creating Dump of the postgres ts_kv table -> 100GB = 90 minutes
2. If postgres table has size 100GB then dump file will be about 30GB 
3. Generation SSTables from dump -> 100GB = 3 hours
4. 100GB Dump file will be converted into SSTable with size about 18GB

# How to build Tool:
Switch to `tools` project in Command Line and execute 
`mvn clean compile assembly:single`
It will generate single jar with dependencies.

# Instruction:

1. Dump Telemetry table from the source Postgres table. Do not use compression if possible because Tool can only work with uncompressed file
dump ts_kv table -> `pg_dump -h localhost -U postgres -d thingsboard -t ts_kv > ts_kv.dmp`

2. Dump Latest Telemetry table from the source Postgres table. Do not use compression if possible because Tool can only work with uncompressed file
dump ts_kv_latest -> `pg_dump -h localhost -U postgres -d thingsboard -t ts_kv_latest > ts_kv_latest.dmp`

3. [Optional] - move dumped files to the machine where cassandra will be hosted

4. Prepare directory structure:
Tool will use 3 different directories for saving SSTables - ts_kv_cf, ts_kv_latest_cf, ts_kv_partitions_cf
Create 3 empty directories. For example:
    /home/ubunut/migration/ts
    /home/ubunut/migration/ts_latest
    /home/ubunut/migration/ts_partition
    
5. Run tool:
*Note: if you run this tool on remote instance - don't forget to execute this command in screen to avoid unexpected termination
java -jar ./tools-2.4.1-SNAPSHOT-jar-with-dependencies.jar 
        -latestFrom ./source/ts_kv_latest.dmp 
        -latestOut /home/ubunut/migration/ts_latest 
        -tsFrom ./source/ts_kv.dmp
        -tsOut /home/ubunut/migration/ts  
        -partitionsOut /home/ubunut/migration/ts_partition
        -castEnable false  


## After tool finished
1. install Cassandra on the instance
2. Using `cqlsh` create `thingsboard` keyspace and requred tables from this file `schema-ts.cql`
3. Stop Cassandra
4. Copy generated SSTable files into cassandra data dir:
    sudo find /home/ubunut/migration/ts -name '*.*' -exec mv {} /var/lib/cassandra/data/thingsboard/ts_kv_cf-0e9aaf00ee5511e9a5fa7d6f489ffd13/ \;
    sudo find /home/ubunut/migration/ts_latest -name '*.*' -exec mv {} /var/lib/cassandra/data/thingsboard/ts_kv_latest_cf-161449d0ee5511e9a5fa7d6f489ffd13/ \;
    sudo find /home/ubunut/migration/ts_partition -name '*.*' -exec mv {} /var/lib/cassandra/data/thingsboard/ts_kv_partitions_cf-12e8fa80ee5511e9a5fa7d6f489ffd13/ \;
    
5. Start Cassandra service and trigger compaction
    trigger compactions: nodetool compact thingsboard
    check compaction status: nodetool compactionstats
    
6. Switch Thignsboard to hybrid mode:
Modify Thingsboard properites file `thingsboard.yml`
    - DATABASE_TS_TYPE = cassandra
    - TS_KV_PARTITIONING = MONTHS
    - [optional] - connection properties for cassandra
    
7. Start Thingsboard