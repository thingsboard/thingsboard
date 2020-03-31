package org.thingsboard.server.service.edge.rpc;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class EdgeEventStorageSettings {
    @Value("${edges.rpc.storage.max_read_records_count}")
    private int maxReadRecordsCount;
    @Value("${edges.rpc.storage.no_read_records_sleep}")
    private long noRecordsSleepInterval;
    @Value("${edges.rpc.storage.sleep_between_batches}")
    private long sleepIntervalBetweenBatches;
}
