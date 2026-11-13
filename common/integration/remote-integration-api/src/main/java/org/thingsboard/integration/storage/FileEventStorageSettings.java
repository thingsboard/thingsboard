// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.storage;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Data
public class FileEventStorageSettings {
    @Value("${storage.data_folder_path}")
    private String dataFolderPath;
    @Value("${storage.max_file_count}")
    private int maxFileCount;
    @Value("${storage.max_records_per_file}")
    private int maxRecordsPerFile;
    @Value("${storage.max_records_between_fsync}")
    private int maxRecordsBetweenFsync;
    @Value("${storage.max_read_records_count}")
    private int maxReadRecordsCount;
    @Value("${storage.no_read_records_sleep}")
    private long noRecordsSleepInterval;
}
