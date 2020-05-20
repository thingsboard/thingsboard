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
package org.thingsboard.server.service.install.migrate;

import com.datastax.oss.driver.api.core.cql.Row;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EPOCH_DIFF;

public class CassandraToSqlEventTsColumn extends CassandraToSqlColumn {

    CassandraToSqlEventTsColumn() {
        super("id", "ts", CassandraToSqlColumnType.BIGINT, null, false);
    }

    @Override
    public String getColumnValue(Row row) {
        UUID id = row.getUuid(getIndex());
        long ts = getTs(id);
        return ts + "";
    }

    private long getTs(UUID uuid) {
        return (uuid.timestamp() - EPOCH_DIFF) / 10000;
    }
}
