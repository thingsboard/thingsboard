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
package org.thingsboard.server.service.cf;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.utils.RocksDBConfig;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='monolith'")
public class RocksDBService {

    private final RocksDB db;
    private final WriteOptions writeOptions;

    public RocksDBService(RocksDBConfig config) throws RocksDBException {
        this.db = config.getDb();
        this.writeOptions = new WriteOptions().setSync(true);
    }

    public void put(String key, String value) {
        try {
            db.put(writeOptions, key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
        } catch (RocksDBException e) {
            log.error("Failed to store data to RocksDB", e);
        }
    }

    public void delete(String key) {
        try {
            db.delete(writeOptions, key.getBytes(StandardCharsets.UTF_8));
        } catch (RocksDBException e) {
            log.error("Failed to delete data from RocksDB", e);
        }
    }

    public void deleteAll(List<String> keys) {
        try (WriteBatch batch = new WriteBatch()) {
            for (String key : keys) {
                batch.delete(key.getBytes(StandardCharsets.UTF_8));
            }
            db.write(writeOptions, batch);
        } catch (RocksDBException e) {
            log.error("Failed to delete data from RocksDB", e);
        }
    }

    public String get(String key) {
        try {
            byte[] value = db.get(key.getBytes(StandardCharsets.UTF_8));
            return value != null ? new String(value, StandardCharsets.UTF_8) : null;
        } catch (RocksDBException e) {
            log.error("Failed to retrieve data from RocksDB", e);
            return null;
        }
    }

    public Map<String, String> getAll() {
        Map<String, String> map = new HashMap<>();
        try (RocksIterator iterator = db.newIterator()) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                String key = new String(iterator.key(), StandardCharsets.UTF_8);
                String value = new String(iterator.value(), StandardCharsets.UTF_8);
                map.put(key, value);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve data from RocksDB", e);
        }
        return map;
    }

}
