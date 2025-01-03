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
package org.thingsboard.server.utils;

import jakarta.annotation.PreDestroy;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RocksDBConfig {

    @Value("${rocksdb.db_path:${java.io.tmpdir}/rocksdb}")
    private String dbPath;
    private RocksDB db;

    static {
        RocksDB.loadLibrary();
    }

    public RocksDB getDb() throws RocksDBException {
        if (db == null) {
            Options options = new Options().setCreateIfMissing(true);
            db = RocksDB.open(options, dbPath);
        }
        return db;
    }

    @PreDestroy
    public void close() {
        if (db != null) {
            db.close();
            db = null;
        }
    }

}
