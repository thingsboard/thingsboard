/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.client.tools.migrator;

import org.apache.cassandra.io.sstable.CQLSSTableWriter;

import java.io.File;

public class WriterBuilder {

    private static final String tsSchema = "CREATE TABLE thingsboard.ts_kv_cf (\n" +
            "    entity_type text, // (DEVICE, CUSTOMER, TENANT)\n" +
            "    entity_id timeuuid,\n" +
            "    key text,\n" +
            "    partition bigint,\n" +
            "    ts bigint,\n" +
            "    bool_v boolean,\n" +
            "    str_v text,\n" +
            "    long_v bigint,\n" +
            "    dbl_v double,\n" +
            "    PRIMARY KEY (( entity_type, entity_id, key, partition ), ts)\n" +
            ");";

    private static final String latestSchema = "CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_latest_cf (\n" +
            "    entity_type text, // (DEVICE, CUSTOMER, TENANT)\n" +
            "    entity_id timeuuid,\n" +
            "    key text,\n" +
            "    ts bigint,\n" +
            "    bool_v boolean,\n" +
            "    str_v text,\n" +
            "    long_v bigint,\n" +
            "    dbl_v double,\n" +
            "    PRIMARY KEY (( entity_type, entity_id ), key)\n" +
            ") WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };";

    private static final String partitionSchema = "CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_partitions_cf (\n" +
            "    entity_type text, // (DEVICE, CUSTOMER, TENANT)\n" +
            "    entity_id timeuuid,\n" +
            "    key text,\n" +
            "    partition bigint,\n" +
            "    PRIMARY KEY (( entity_type, entity_id, key ), partition)\n" +
            ") WITH CLUSTERING ORDER BY ( partition ASC )\n" +
            "  AND compaction = { 'class' :  'LeveledCompactionStrategy'  };";

    public static CQLSSTableWriter getTsWriter(File dir) {
        return CQLSSTableWriter.builder()
                .inDirectory(dir)
                .forTable(tsSchema)
                .using("INSERT INTO thingsboard.ts_kv_cf (entity_type, entity_id, key, partition, ts, bool_v, str_v, long_v, dbl_v) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .build();
    }

    public static CQLSSTableWriter getLatestWriter(File dir) {
        return CQLSSTableWriter.builder()
                .inDirectory(dir)
                .forTable(latestSchema)
                .using("INSERT INTO thingsboard.ts_kv_latest_cf (entity_type, entity_id, key, ts, bool_v, str_v, long_v, dbl_v) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
                .build();
    }

    public static CQLSSTableWriter getPartitionWriter(File dir) {
        return CQLSSTableWriter.builder()
                .inDirectory(dir)
                .forTable(partitionSchema)
                .using("INSERT INTO thingsboard.ts_kv_partitions_cf (entity_type, entity_id, key, partition) " +
                        "VALUES (?, ?, ?, ?)")
                .build();
    }
}
