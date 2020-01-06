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
package org.thingsboard.server.dao.sqlts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.regex.Pattern;

@Repository
public abstract class AbstractInsertRepository {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));
    private static final String EMPTY_STR = "";

    protected static final String BOOL_V = "bool_v";
    protected static final String STR_V = "str_v";
    protected static final String LONG_V = "long_v";
    protected static final String DBL_V = "dbl_v";

    protected static final String TS_KV_LATEST_TABLE = "ts_kv_latest";
    protected static final String TS_KV_TABLE = "ts_kv";

    protected static final String HSQL_ON_BOOL_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_TABLE, BOOL_V);
    protected static final String HSQL_ON_STR_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_TABLE, STR_V);
    protected static final String HSQL_ON_LONG_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_TABLE, LONG_V);
    protected static final String HSQL_ON_DBL_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_TABLE, DBL_V);

    protected static final String HSQL_LATEST_ON_BOOL_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_LATEST_TABLE, BOOL_V);
    protected static final String HSQL_LATEST_ON_STR_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_LATEST_TABLE, STR_V);
    protected static final String HSQL_LATEST_ON_LONG_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_LATEST_TABLE, LONG_V);
    protected static final String HSQL_LATEST_ON_DBL_VALUE_UPDATE_SET_NULLS = getHsqlNullValues(TS_KV_LATEST_TABLE, DBL_V);

    protected static final String PSQL_ON_BOOL_VALUE_UPDATE_SET_NULLS = "str_v = null, long_v = null, dbl_v = null";
    protected static final String PSQL_ON_STR_VALUE_UPDATE_SET_NULLS = "bool_v = null, long_v = null, dbl_v = null";
    protected static final String PSQL_ON_LONG_VALUE_UPDATE_SET_NULLS = "str_v = null, bool_v = null, dbl_v = null";
    protected static final String PSQL_ON_DBL_VALUE_UPDATE_SET_NULLS = "str_v = null, long_v = null, bool_v = null";

    @Value("${sql.remove_null_chars}")
    private boolean removeNullChars;

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    protected static String getInsertOrUpdateStringHsql(String tableName, String constraint, String value, String nullValues) {
        return "MERGE INTO " + tableName + " USING(VALUES :entity_type, :entity_id, :key, :ts, :" + value + ") A (entity_type, entity_id, key, ts, " + value + ") ON " + constraint + " WHEN MATCHED THEN UPDATE SET " + tableName + "." + value + " = A." + value + ", " + tableName + ".ts = A.ts," + nullValues + "WHEN NOT MATCHED THEN INSERT (entity_type, entity_id, key, ts, " + value + ") VALUES (A.entity_type, A.entity_id, A.key, A.ts, A." + value + ")";
    }

    protected static String getInsertOrUpdateStringPsql(String tableName, String constraint, String value, String nullValues) {
        return "INSERT INTO " + tableName + " (entity_type, entity_id, key, ts, " + value + ") VALUES (:entity_type, :entity_id, :key, :ts, :" + value + ") ON CONFLICT " + constraint + " DO UPDATE SET " + value + " = :" + value + ", ts = :ts," + nullValues;
    }

    private static String getHsqlNullValues(String tableName, String notNullValue) {
        switch (notNullValue) {
            case BOOL_V:
                return " " + tableName + ".str_v = null, " + tableName + ".long_v = null, " + tableName + ".dbl_v = null ";
            case STR_V:
                return " " + tableName + ".bool_v = null, " + tableName + ".long_v = null, " + tableName + ".dbl_v = null ";
            case LONG_V:
                return " " + tableName + ".str_v = null, " + tableName + ".bool_v = null, " + tableName + ".dbl_v = null ";
            case DBL_V:
                return " " + tableName + ".str_v = null, " + tableName + ".long_v = null, " + tableName + ".bool_v = null ";
            default:
                throw new RuntimeException("Unsupported insert value: [" + notNullValue + "]");
        }
    }

    protected String replaceNullChars(String strValue) {
        if (removeNullChars && strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }
}