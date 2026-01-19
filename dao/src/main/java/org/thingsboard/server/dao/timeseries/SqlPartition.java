/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.timeseries;

import lombok.Data;

@Data
public class SqlPartition {

    public static final String TS_KV = "ts_kv";

    private long start;
    private long end;
    private String partitionDate;
    private String query;

    public SqlPartition(String table, long start, long end, String partitionDate) {
        this.start = start;
        this.end = end;
        this.partitionDate = partitionDate;
        this.query = createStatement(table, start, end, partitionDate);
    }

    private String createStatement(String table, long start, long end, String partitionDate) {
        return "CREATE TABLE IF NOT EXISTS " + table + "_" + partitionDate + " PARTITION OF " + table + " FOR VALUES FROM (" + start + ") TO (" + end + ")";
    }
}