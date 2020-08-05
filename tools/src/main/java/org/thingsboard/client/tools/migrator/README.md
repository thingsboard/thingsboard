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
    
It will generate single jar file with all required dependencies inside `target dir` -> `tools-2.5.3-SNAPSHOT-jar-with-dependencies.jar`.


# Prepare requred files and run Tool:

#### Dump data from the source Postgres Database
*Do not use compression if possible because Tool can only work with uncompressed file

1. Dump `thingsboard` database:

    `pg_dump -h localhost -U postgres -d thingsboard > thingsboard_db.dmp`

3. [Optional] move database dump to the instance where cassandra will be hosted

#### Prepare directory structure for SSTables
Tool use 3 different directories for saving SSTables - `ts_kv_cf`, `ts_kv_latest_cf`, `ts_kv_partitions_cf`

Create 3 empty directories. For example:

    /home/ubunut/migration/ts
    /home/ubunut/migration/ts_latest
    /home/ubunut/migration/ts_partition
    
#### Run tool
*Note: if you run this tool on remote instance - don't forget to execute this command in `screen` to avoid unexpected termination

```
java -jar ./tools-2.5.3-SNAPSHOT-jar-with-dependencies.jar 
        -latestFrom ./source/thingsboard_db.dmp
        -latestOut /home/ubunut/migration/ts_latest 
        -tsFrom ./source/thingsboard_db.dmp
        -tsOut /home/ubunut/migration/ts  
        -partitionsOut /home/ubunut/migration/ts_partition
        -castEnable false
```  

Tool execution time depends on DB size, CPU resources and Disk throughput

## Adding SSTables into Cassandra
* Note that this this part works only for single node Cassandra Cluster. If you have more nodes - it is better to use `sstableloader` tool.

1. [Optional] install Cassandra on the instance
2. [Optional] Using `cqlsh` create `thingsboard` keyspace and requred tables from this file `schema-ts.cql`
3. Stop Cassandra
4. Copy generated SSTable files into cassandra data dir:

```
    sudo find /home/ubunut/migration/ts -name '*.*' -exec mv {} /var/lib/cassandra/data/thingsboard/ts_kv_cf-0e9aaf00ee5511e9a5fa7d6f489ffd13/ \;
    sudo find /home/ubunut/migration/ts_latest -name '*.*' -exec mv {} /var/lib/cassandra/data/thingsboard/ts_kv_latest_cf-161449d0ee5511e9a5fa7d6f489ffd13/ \;
    sudo find /home/ubunut/migration/ts_partition -name '*.*' -exec mv {} /var/lib/cassandra/data/thingsboard/ts_kv_partitions_cf-12e8fa80ee5511e9a5fa7d6f489ffd13/ \;
```   
    
5. Start Cassandra service and trigger compaction

```
    trigger compactions: nodetool compact thingsboard
    check compaction status: nodetool compactionstats
```
    
## Switch Thignsboard into Hybrid Mode

Modify Thingsboard properites file `thingsboard.yml`

    - DATABASE_TS_TYPE = cassandra
    - TS_KV_PARTITIONING = MONTHS    
    
# Final steps
Start Thingsboard and verify migration