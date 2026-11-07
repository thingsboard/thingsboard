// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class EdgeEventStorageSettings {
    @Value("${edges.storage.max_read_records_count}")
    private int maxReadRecordsCount;
    @Value("${edges.storage.no_read_records_sleep}")
    private long noRecordsSleepInterval;
    @Value("${edges.storage.sleep_between_batches}")
    private long sleepIntervalBetweenBatches;
    @Value("${edges.storage.misordering_compensation_millis:60000}")
    private long misorderingCompensationMillis;
    @Value("${edges.storage.retry_failed_entity_downlinks:false}")
    private boolean retryFailedEntityDownlinks;
}
