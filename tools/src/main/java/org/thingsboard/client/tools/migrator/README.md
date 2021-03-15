# Description:
This tool used for migrating ThingsBoard into hybrid mode from Postgres.
   
Performance of this tool depends on disk type and instance type (mostly on CPU resources).
But in general here are few benchmarks:
1. Creating Dump of the postgres ts_kv table -> 100GB = 90 minutes
2. If postgres table has size 100GB then dump file will be about 30GB 
3. Generation SSTables from dump -> 100GB = 3 hours
4. 100GB Dump file will be converted into SSTable with size about 18GB

# Tool build Instruction:
Switch to `tools` module in Command Line and execute 

    mvn clean compile assembly:single
    
It will generate single jar file with all required dependencies inside `target dir` -> `tools-2.4.1-SNAPSHOT-jar-with-dependencies.jar`.


# Prepare requred files and run Tool:

#### Dump data from the source Postgres Database
*Do not use compression if possible because Tool can only work with uncompressed file

1. Dump related tables that need to correct save telemetry
   
   `pg_dump -h localhost -U postgres -d thingsboard -t tenant -t customer -t user -t dashboard -t asset -t device -t alarm -t rule_chain -t rule_node -t entity_view -t widgets_bundle -t widget_type -t tenant_profile -t device_profile -t api_usage_state -t tb_user > related_entities.dmp`
   
2. Dump `ts_kv` and child:
   
   `pg_dump -h localhost -U postgres -d thingsboard --load-via-partition-root --data-only -t ts_kv* > ts_kv_all.dmp`

3. [Optional] Move table dumps to the instance where cassandra will be hosted

#### Prepare directory structure for SSTables
Tool use 3 different directories for saving SSTables - `ts_kv_cf`, `ts_kv_latest_cf`, `ts_kv_partitions_cf`

Create 3 empty directories. For example:

    /home/user/migration/ts
    /home/user/migration/ts_latest
    /home/user/migration/ts_partition
    
#### Run tool

**If you want to migrate just `ts_kv` without `ts_kv_latest` or vice versa don't use arguments (paths) for output files*

**Note: if you run this tool on remote instance - don't forget to execute this command in `screen` to avoid unexpected termination*

```
java -jar ./tools-2.4.1-SNAPSHOT-jar-with-dependencies.jar 
        -telemetryFrom /home/user/dump/ts_kv_all.dmp 
        -relatedEntities /home/user/dump/relatedEntities.dmp 
        -latestOut /home/ubunut/migration/ts_latest 
        -tsOut /home/ubunut/migration/ts 
        -partitionsOut /home/ubunut/migration/ts_partition 
        -castEnable false 
```  
*Use your paths for program arguments*

Tool execution time depends on DB size, CPU resources and Disk throughput

## Adding SSTables into Cassandra
* Note that this this part works only for single node Cassandra Cluster. If you have more nodes - it is better to use `sstableloader` tool.

1. [Optional] install Cassandra on the instance
2. [Optional] Using `cqlsh` create `thingsboard` keyspace and requred tables from this file `schema-ts.cql`
3. Stop Cassandra
4. Look at `/var/lib/cassandra/data/thingsboard` and check for names of data folders
5. Copy generated SSTable files into cassandra data dir using next command:

```
    sudo find /home/user/migration/ts -name '*.*' -exec mv {} /var/lib/cassandra/data/thingsboard/ts_kv_cf-0e9aaf00ee5511e9a5fa7d6f489ffd13/ \;
    sudo find /home/user/migration/ts_latest -name '*.*' -exec mv {} /var/lib/cassandra/data/thingsboard/ts_kv_latest_cf-161449d0ee5511e9a5fa7d6f489ffd13/ \;
    sudo find /home/user/migration/ts_partition -name '*.*' -exec mv {} /var/lib/cassandra/data/thingsboard/ts_kv_partitions_cf-12e8fa80ee5511e9a5fa7d6f489ffd13/ \;
```   
  *Pay attention! Data folders have similar name  `ts_kv_cf-0e9aaf00ee5511e9a5fa7d6f489ffd13`, but you have to use own*  
6. Start Cassandra service and trigger compaction

    Trigger compactions: `nodetool compact thingsboard`
   
    Check compaction status: `nodetool compactionstats`

    
## Switch Thignsboard into Hybrid Mode

Modify Thingsboard properites file `thingsboard.yml`

    - DATABASE_TS_TYPE = cassandra
    - TS_KV_PARTITIONING = MONTHS    
    
# Final steps
Start Thingsboard and verify migration