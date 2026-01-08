/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.rocksdb.Options;
import org.rocksdb.WriteOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.edqs.InMemoryEdqsComponent;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@InMemoryEdqsComponent
public class EdqsRocksDb extends TbRocksDb {

    @Getter
    private boolean isNew;

    public EdqsRocksDb(@Value("${queue.edqs.local.rocksdb_path:${user.home}/.rocksdb/edqs}") String path) {
        super(path, new Options().setCreateIfMissing(true), new WriteOptions());
    }

    @PostConstruct
    @Override
    public void init() {
        isNew = !Files.exists(Path.of(path));
        super.init();
    }

    @PreDestroy
    @Override
    public void close() {
        super.close();
    }

}
