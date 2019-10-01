/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class PsqlPartition {

    private static final String BOOL_V = "bool_v";
    private static final String STR_V = "str_v";
    private static final String LONG_V = "long_v";
    private static final String DBL_V = "dbl_v";

    private static final String ON_BOOL_VALUE_UPDATE_SET_NULLS = "str_v = null, long_v = null, dbl_v = null";
    private static final String ON_STR_VALUE_UPDATE_SET_NULLS = "bool_v = null, long_v = null, dbl_v = null";
    private static final String ON_LONG_VALUE_UPDATE_SET_NULLS = "str_v = null, bool_v = null, dbl_v = null";
    private static final String ON_DBL_VALUE_UPDATE_SET_NULLS = "str_v = null, long_v = null, bool_v = null";

    private long start;
    private long end;
    private String partionDate;
    private String insertOrUpdateBoolStatement;
    private String insertOrUpdateStrStatement;
    private String insertOrUpdateLongStatement;
    private String insertOrUpdateDblStatement;

    public PsqlPartition(long start, long end, String pattern) {
        this.start = start;
        this.end = end;
        this.partionDate = new SimpleDateFormat(pattern).format(new Date(start));
        this.insertOrUpdateBoolStatement = getInsertOrUpdateString(BOOL_V, partionDate, ON_BOOL_VALUE_UPDATE_SET_NULLS);
        this.insertOrUpdateStrStatement = getInsertOrUpdateString(STR_V, partionDate, ON_STR_VALUE_UPDATE_SET_NULLS);
        this.insertOrUpdateLongStatement = getInsertOrUpdateString(LONG_V, partionDate, ON_LONG_VALUE_UPDATE_SET_NULLS);
        this.insertOrUpdateDblStatement = getInsertOrUpdateString(DBL_V, partionDate, ON_DBL_VALUE_UPDATE_SET_NULLS);
    }

    private String getInsertOrUpdateString(String value, String partitionDate, String nullValues) {
        return "INSERT INTO ts_kv_" + partitionDate + " (entity_id, key, ts, " + value + ") VALUES (:entity_id, :key, :ts, :" + value + ") ON CONFLICT (entity_id, key, ts) DO UPDATE SET " + value + " = :" + value + ", ts = :ts," + nullValues;
    }


}