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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.model.sqlts.dictionary.TsKvDictionary;
import org.thingsboard.server.dao.model.sqlts.dictionary.TsKvDictionaryCompositeKey;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.dictionary.TsKvDictionaryRepository;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.thingsboard.server.dao.util.NoSqlTsDao;
import org.thingsboard.server.dao.util.SqlTsLatestDao;
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.bigintColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.booleanColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.doubleColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.idColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.jsonColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.stringColumn;

@Service
@Profile("install")
@NoSqlTsDao
@SqlTsLatestDao
@Slf4j
public class CassandraTsLatestToSqlMigrateService implements TsLatestMigrateService {

    private static final int MAX_KEY_LENGTH = 255;
    private static final int MAX_STR_V_LENGTH = 10000000;

    @Autowired
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

    @Autowired
    private InsertLatestTsRepository insertLatestTsRepository;

    @Autowired
    protected CassandraCluster cluster;

    @Autowired
    protected TsKvDictionaryRepository dictionaryRepository;

    @Value("${spring.datasource.url}")
    protected String dbUrl;

    @Value("${spring.datasource.username}")
    protected String dbUserName;

    @Value("${spring.datasource.password}")
    protected String dbPassword;

    private final ConcurrentMap<String, Integer> tsKvDictionaryMap = new ConcurrentHashMap<>();

    protected static final ReentrantLock tsCreationLock = new ReentrantLock();

    @Override
    public void migrate() throws Exception {
        log.info("Performing migration of latest timeseries data from cassandra to SQL database ...");
        entityDatabaseSchemaService.createDatabaseSchema(false);
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.setAutoCommit(false);
            for (CassandraToSqlTable table : tables) {
                table.migrateToSql(cluster.getSession(), conn);
            }
        } catch (Exception e) {
            log.error("Unexpected error during ThingsBoard entities data migration!", e);
            throw e;
        }
    }

    private List<CassandraToSqlTable> tables = Arrays.asList(
            new CassandraToSqlTable("ts_kv_latest_cf", "ts_kv_latest",
                    idColumn("entity_id"),
                    stringColumn("key"),
                    bigintColumn("ts"),
                    booleanColumn("bool_v"),
                    stringColumn("str_v"),
                    bigintColumn("long_v"),
                    doubleColumn("dbl_v"),
                    jsonColumn("json_v")) {

                @Override
                protected void batchInsert(List<CassandraToSqlColumnData[]> batchData, Connection conn) {
                    insertLatestTsRepository
                            .saveOrUpdate(batchData.stream().map(data -> getTsKvLatestEntity(data)).collect(Collectors.toList()));
                }

                @Override
                protected CassandraToSqlColumnData[] validateColumnData(CassandraToSqlColumnData[] data) {
                    return data;
                }
            });

    private TsKvLatestEntity getTsKvLatestEntity(CassandraToSqlColumnData[] data) {
        TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityId(UUIDConverter.fromString(data[0].getValue()));
        latestEntity.setKey(getOrSaveKeyId(data[1].getValue()));
        latestEntity.setTs(Long.parseLong(data[2].getValue()));

        String strV = data[4].getValue();
        if (strV != null) {
            if (strV.length() > MAX_STR_V_LENGTH) {
                log.warn("[ts_kv_latest] Value size [{}] exceeds maximum size [{}] of column [str_v] and will be truncated!",
                        strV.length(), MAX_STR_V_LENGTH);
                log.warn("Affected data:\n{}", strV);
                strV = strV.substring(0, MAX_STR_V_LENGTH);
            }
            latestEntity.setStrValue(strV);
        } else {
            Long longV = null;
            try {
                longV = Long.parseLong(data[5].getValue());
            } catch (Exception e) {
            }
            if (longV != null) {
                latestEntity.setLongValue(longV);
            } else {
                Double doubleV = null;
                try {
                    doubleV = Double.parseDouble(data[6].getValue());
                } catch (Exception e) {
                }
                if (doubleV != null) {
                    latestEntity.setDoubleValue(doubleV);
                } else {

                    String jsonV = data[7].getValue();
                    if (StringUtils.isNoneEmpty(jsonV)) {
                        latestEntity.setJsonValue(jsonV);
                    } else {
                        Boolean boolV = null;
                        try {
                            boolV = Boolean.parseBoolean(data[3].getValue());
                        } catch (Exception e) {
                        }
                        if (boolV != null) {
                            latestEntity.setBooleanValue(boolV);
                        } else {
                            log.warn("All values in key-value row are nullable ");
                        }
                    }
                }
            }
        }
        return latestEntity;
    }

    protected Integer getOrSaveKeyId(String strKey) {
        if (strKey.length() > MAX_KEY_LENGTH) {
            log.warn("[ts_kv_latest] Value size [{}] exceeds maximum size [{}] of column [key] and will be truncated!",
                    strKey.length(), MAX_KEY_LENGTH);
            log.warn("Affected data:\n{}", strKey);
            strKey = strKey.substring(0, MAX_KEY_LENGTH);
        }

        Integer keyId = tsKvDictionaryMap.get(strKey);
        if (keyId == null) {
            Optional<TsKvDictionary> tsKvDictionaryOptional;
            tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
            if (!tsKvDictionaryOptional.isPresent()) {
                tsCreationLock.lock();
                try {
                    tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
                    if (!tsKvDictionaryOptional.isPresent()) {
                        TsKvDictionary tsKvDictionary = new TsKvDictionary();
                        tsKvDictionary.setKey(strKey);
                        try {
                            TsKvDictionary saved = dictionaryRepository.save(tsKvDictionary);
                            tsKvDictionaryMap.put(saved.getKey(), saved.getKeyId());
                            keyId = saved.getKeyId();
                        } catch (ConstraintViolationException e) {
                            tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
                            TsKvDictionary dictionary = tsKvDictionaryOptional.orElseThrow(() -> new RuntimeException("Failed to get TsKvDictionary entity from DB!"));
                            tsKvDictionaryMap.put(dictionary.getKey(), dictionary.getKeyId());
                            keyId = dictionary.getKeyId();
                        }
                    } else {
                        keyId = tsKvDictionaryOptional.get().getKeyId();
                    }
                } finally {
                    tsCreationLock.unlock();
                }
            } else {
                keyId = tsKvDictionaryOptional.get().getKeyId();
                tsKvDictionaryMap.put(strKey, keyId);
            }
        }
        return keyId;
    }
}
