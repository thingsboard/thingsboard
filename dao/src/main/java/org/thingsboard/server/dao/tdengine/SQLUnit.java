/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.tdengine;

import lombok.Data;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;
import java.util.function.Consumer;

/**
 * for tdengine
 */
@Data
public class SQLUnit {

    private String sql;
    private String entity;
    private Consumer<List<TsKvEntry>> consumer;
    private String keySql;
    private String valSql;
    private Long time;
    private List<TsKvEntry> tsKvEntries;

    public SQLUnit(String sql, String entity) {
        this.sql = sql;
        this.entity = entity;
    }

    public SQLUnit(String entity, Long time, String keyStr, String valStr) {
        this.entity = entity;
        this.time = time;
        this.keySql = keyStr;
        this.valSql = valStr;
    }

    public SQLUnit(Consumer<List<TsKvEntry>> consumer, List<TsKvEntry> tsKvEntries, String entity) {
        this.consumer = consumer;
        this.tsKvEntries = tsKvEntries;
        this.entity = entity;
    }
}
