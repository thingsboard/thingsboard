// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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