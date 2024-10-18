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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.util.TDengineTsDao;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * for tdengine
 */
@Component
@TDengineTsDao
@Slf4j
public class BaseTDengine {

    @Autowired
    @Qualifier("TDengineTemplate")
    protected JdbcTemplate jdbcTemplate;

    // <tableName, <columnName, Field>>
    @Autowired
    private CacheManager cacheManager;

    @Value("${tdengine.initStringColumnlength:1024}")
    public int varcharLen = 1024;

    private static final String TDENGINE_TABLE_META = "tdengineTableMeta";

    private static final Cache<String, Object> cache = CacheBuilder.newBuilder().maximumSize(50000).expireAfterAccess(10, TimeUnit.HOURS).concurrencyLevel(16).build();

    public static final List<String> fieldList = Arrays.asList("active", "lastconnecttime", "lastdisconnecttime", "lastactivitytime", "inactivityalarmtime", "t1");

    private String dbName = null;

    private Map<String, Map<String, Field>> stableMap = new HashMap<>();

    @PostConstruct
    public void scanTables() {
        jdbcTemplate.query("select database() as db_name", (RowCallbackHandler) rs -> {
            this.dbName = rs.getString("db_name").trim();
            jdbcTemplate.query(String.format("show `%s`.stables", this.dbName), (RowCallbackHandler) handler -> {
                do {
                    String tableName = handler.getString("stable_name").trim();
                    jdbcTemplate.query(String.format("desc %s.`" + tableName + "`", this.dbName), (RowCallbackHandler) result -> {
                        Map<String, Field> fields = new HashMap<>();
                        do {
                            String name = result.getString("field").trim();
                            String type = result.getString("type");
                            int length = result.getInt("length");
                            String note = result.getString("note");
                            Field field = new Field(type, length, !Strings.isBlank(note));
                            fields.put(name, field);
                        } while (result.next());
                        stableMap.put(tableName, fields);
                    });
                } while (handler.next());
            });
            jdbcTemplate.query(String.format("select table_name from information_schema.ins_tables where db_name = '%s'", this.dbName), (RowCallbackHandler) handler -> {
                do {
                    String tableName = handler.getString("table_name").trim();
                    if (null == get(tableName)) {
                        jdbcTemplate.query("desc `" + tableName + "`", (RowCallbackHandler) result -> {
                            Map<String, Field> fields = new HashMap<>();
                            do {
                                String name = result.getString("field").trim();
                                String type = result.getString("type");
                                int length = result.getInt("length");
                                String note = result.getString("note");
                                Field field = new Field(type, length, !Strings.isBlank(note));
                                fields.put(name, field);
                            } while (result.next());
                            put(tableName, fields);
                        });
                    }
                } while (handler.next());
            });
        });
    }

    private Map<String, Field> getTableMeta(String tableName) {
        if (tableName == null) {
            return Collections.emptyMap();
        }
        Map<String, Field> map = new HashMap<>();
        StringBuilder strTrace = new StringBuilder();
        Stream.of(Thread.currentThread().getStackTrace()).forEach(s -> strTrace.append(s));
        log.info("get meta trace: {}", strTrace.toString());
        jdbcTemplate.query(String.format("select 1 from information_schema.ins_tables where db_name = '%s' and table_name = '%s'", this.dbName.trim(), tableName.trim()), rs -> {
            jdbcTemplate.query("desc `" + tableName.trim() + "`", result -> {
                do {
                    String name = result.getString("field").trim();
                    String note = result.getString("note");
                    Field field = new Field(result.getString("type"), result.getInt("length"), !Strings.isBlank(note));
                    map.put(name, field);
                } while (result.next());
                put(tableName.trim(), map);
            });
        });
        if (map.size() < 1) {
            return Collections.emptyMap();
        }
        return map;
    }

    private Map<String, Field> getSTableMeta(String stableName) {
        if (stableName == null) {
            return Collections.emptyMap();
        }
        Map<String, Field> map = new HashMap<>();
        StringBuilder strTrace = new StringBuilder();
        Stream.of(Thread.currentThread().getStackTrace()).forEach(s -> strTrace.append(s));
        log.info("get meta trace: {}", strTrace.toString());
        jdbcTemplate.query(String.format("select 1 from information_schema.ins_tables where db_name = '%s' and stable_name = '%s'", this.dbName.trim(), stableName.trim()), rs -> {
            jdbcTemplate.query("desc `" + stableName.trim() + "`", result -> {
                do {
                    String name = result.getString("field").trim();
                    String note = result.getString("note");
                    Field field = new Field(result.getString("type"), result.getInt("length"), !Strings.isBlank(note));
                    map.put(name, field);
                } while (result.next());
                stableMap.put(stableName.trim(), map);
            });
        });
        if (map.size() < 1) {
            return Collections.emptyMap();
        }
        return map;
    }

    public Map<String, Field> setTableMetaCache(String entityId, String stableName) {
        stableName = stableName.trim();
        Map<String, Field> stableMeta = stableMap.get(stableName);
        if (null == stableMeta || stableMeta.isEmpty()) {
            stableMeta = getSTableMeta(stableName);
        }
        put(entityId.trim(), stableMeta);
        return stableMeta;
    }

    public Map<String, Field> getTableMetaCache(String entityId) {
        // slow queries caused by the absence of data in memory
        Map<String, Field> tableMeta = get(entityId.trim());
        if (null == tableMeta || tableMeta.isEmpty()) {
            tableMeta = updateTableMetaCache(entityId.trim());
        }
        return tableMeta;
    }

    public Map<String, Field> updateTableMetaCache(String entityId) {
        Map<String, Field> tableMeta = getTableMeta(entityId.trim());
        if (null == tableMeta || tableMeta.isEmpty()) {
            tableMeta = creatEmptyTable(entityId.trim());
        }
        cache.put(entityId.trim(), tableMeta);
        return tableMeta;
    }

    public Map<String, Field> creatEmptyTable(String entityId) {
        String createTableSql = "create table if not exists `" + entityId.trim() + "` (ts timestamp,t1 int, active bool, lastconnecttime bigint, lastdisconnecttime bigint, lastactivitytime bigint)";
        jdbcTemplate.execute(createTableSql);
        Map<String, Field> map = new HashMap<>();
        map.put("active", new Field("BOOL", 1, false));
        map.put("t1", new Field("BIGINT", 8, false));
        map.put("lastconnecttime", new Field("BIGINT", 8, false));
        map.put("lastdisconnecttime", new Field("BIGINT", 8, false));
        map.put("lastactivitytime", new Field("BIGINT", 8, false));
        map.put("ts", new Field("TIMESTAMP", 8, false));
        put(entityId.trim(), map);
        return map;
    }

    public synchronized void createTable(EntityId entityId, TsKvEntry tsKvEntry) {
        String key = tsKvEntry.getKey().trim();
        String tableName = entityId.getId().toString().trim();
        Map<String, Field> fieldMap = get(tableName);
        if (fieldMap == null) {
            String type = getSQLType(tsKvEntry.getDataType());
            String createTableSql = "create table if not exists `" + tableName + "` (ts timestamp, `" + key + "` " + type + ")";
            jdbcTemplate.execute(createTableSql);
            Map<String, Field> map = new HashMap<>();
            Field field = new Field(getType(tsKvEntry.getDataType()), varcharLen, false);
            map.put(key, field);
            map.put("ts", new Field("TIMESTAMP", 8, false));
            put(tableName, map);
        }
    }

    public synchronized void createTable(EntityId entityId, List<TsKvEntry> tsKvEntries) {
        String tableName = entityId.getId().toString().trim();
        Map<String, Field> fieldMap = get(tableName);
        if (fieldMap == null) {
            Map<String, String> fields = tsKvEntries.stream().collect(HashMap::new, (m, v) -> m.put(v.getKey().trim(), getSQLType(v.getDataType())), HashMap::putAll);
            StringBuilder createTableSql = new StringBuilder("create table if not exists `" + tableName + "` (ts timestamp");
            fields.forEach((k, v) -> {
                createTableSql.append(", `");
                createTableSql.append(k);
                createTableSql.append("` ");
                createTableSql.append(v);
            });
            createTableSql.append(")");
            jdbcTemplate.execute(createTableSql.toString());
            Map<String, Field> map = tsKvEntries.stream().collect(HashMap::new, (m, v) -> m.put(v.getKey().trim(), new Field(getType(v.getDataType()), varcharLen, false)), HashMap::putAll);
            map.put("ts", new Field("TIMESTAMP", 8, false));
            put(tableName, map);
        }
    }

    public synchronized void alterTable(EntityId entityId, TsKvEntry tsKvEntry) {
        String key = tsKvEntry.getKey().trim();
        String tableName = entityId.getId().toString().trim();
        Map<String, Field> map = get(tableName);
        if (!map.containsKey(key)) {
            String type = getSQLType(tsKvEntry.getDataType());
            String addColSql = "alter table `" + tableName + "` add column `" + key + "` " + type;
            try {
                jdbcTemplate.execute(addColSql);
                // 更新内存与redis
                Field field = new Field(getType(tsKvEntry.getDataType()), varcharLen, false);
                map.put(key, field);
                put(tableName, map);
                log.debug("add column success(table={}, column={}, current={}): ", tableName, key, map.keySet());
            } catch (Exception e) {
                log.error("add column error(table={}, column={}, current={}): ", tableName, key, map.keySet(), e);
                // 查库并更新内存与redis
                updateTableMetaCache(tableName);
            }
        }
    }

    public synchronized void resizeBinaryColumn(EntityId entityId, TsKvEntry tsKvEntry, int len) {
        String key = tsKvEntry.getKey().trim();
        String tableName = entityId.getId().toString().trim();
        Map<String, Field> map = get(tableName);
        if (len > map.get(key).getLength()) {
            Field field = map.get(key);
            String addColSql = "alter table `" + tableName + "` modify column `" + key + "` binary(" + len + ")";
            try {
                jdbcTemplate.execute(addColSql);
                // 更新内存与redis
                field.setLength(len);
                put(tableName, map);
                log.debug("resize column length success(table={}, column={}, length={}): ", tableName, key, len);
            } catch (Exception e) {
                log.debug("resize column length error(table={}, column={}, length={}): ", tableName, key, len, e);
                // 查库并更新内存与redis
                updateTableMetaCache(tableName);
            }
        }
    }

    private String getSQLType(DataType type) {
        if (type == DataType.STRING || type == DataType.JSON) {
            return " varchar(" + varcharLen + ")";
        } else if (type == DataType.DOUBLE || type == DataType.LONG) {
            // sql exception: invalid bigint data
            // 设备最初的数据是 0，后面是 double 类型导致错误，此处统一按 double 处理
            return " double";
        }
        return " bool";
    }

    private String getType(DataType type) {
        if (type == DataType.STRING || type == DataType.JSON) {
            return "VARCHAR";
        } else if (type == DataType.DOUBLE || type == DataType.LONG) {
            // sql exception: invalid bigint data
            // 设备最初的数据是 0，后面是 double 类型导致错误，此处统一按 double 处理
            return "DOUBLE";
        }
        return "BOOL";
    }

    @Data
    @AllArgsConstructor
    public static class Field implements Serializable {

        // VARCHAR BIGINT...
        private String type;
        private int length;
        private boolean tag;
    }

    private Map<String, Field> get(String key) {
        key = key.trim();
        // 取内存
        Map<String, Field> fieldMap = (Map<String, Field>) cache.getIfPresent(key);
        // 取redis
        if (fieldMap == null || fieldMap.size() == 0) {
            fieldMap = cacheManager.getCache(TDENGINE_TABLE_META).get(key, Map.class);
            if (fieldMap != null && fieldMap.size() > 0) {
                try {
                    // 写内存
                    cache.put(key, fieldMap);
                } catch (Exception e) {
                    log.error("write to cache error: {}", e.getMessage(), e);
                }
            }
        }
        // 查数据库
        if (fieldMap == null || fieldMap.size() == 0) {
            fieldMap = getTableMeta(key);
            if (fieldMap != null && fieldMap.size() > 0) {
                try {
                    // 写redis
                    cacheManager.getCache(TDENGINE_TABLE_META).putIfAbsent(key, fieldMap);
                } catch (Exception e) {
                    log.error("write to redis error: {}", e.getMessage(), e);
                }
                try {
                    // 写内存
                    cache.put(key, fieldMap);
                } catch (Exception e) {
                    log.error("write to cache error: {}", e.getMessage(), e);
                }
            }
        }
        return fieldMap;
    }

    private void put(String key, Map<String, Field> value) {
        key = key.trim();
        try {
            // 写redis
            cacheManager.getCache(TDENGINE_TABLE_META).putIfAbsent(key, value);
        } catch (Exception e) {
            log.error("write to redis error: {}", e.getMessage(), e);
        }
        try {
            // 写内存
            cache.put(key, value);
        } catch (Exception e) {
            log.error("write to cache error: {}", e.getMessage(), e);
        }
    }
}
