// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
