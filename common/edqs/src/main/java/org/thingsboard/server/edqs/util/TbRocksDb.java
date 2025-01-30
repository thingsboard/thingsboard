/**
 * Copyright © 2016-2024 ThingsBoard, Inc.
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
package org.thingsboard.server.edqs.util;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class TbRocksDb {

    protected final String path;
    private final Options options;

    private RocksDB db;

    static {
        RocksDB.loadLibrary();
    }

    @SneakyThrows
    public void init() {
        db = RocksDB.open(options, path);
    }

    public void put(String key, byte[] value) throws RocksDBException {
        db.put(key.getBytes(StandardCharsets.UTF_8), value);
    }

    public void forEach(BiConsumer<String, byte[]> processor) {
        try (RocksIterator iterator = db.newIterator()) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                String key = new String(iterator.key(), StandardCharsets.UTF_8);
                processor.accept(key, iterator.value());
            }
        }
    }

    public void delete(String key) throws RocksDBException {
        db.delete(key.getBytes(StandardCharsets.UTF_8));
    }

    public void close() {
        if (db != null) {
            db.close();
        }
    }

}
