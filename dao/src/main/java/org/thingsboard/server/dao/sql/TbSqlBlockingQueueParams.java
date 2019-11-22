package org.thingsboard.server.dao.sql;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
public class TbSqlBlockingQueueParams {

    private final String logName;
    private final int batchSize;
    private final long maxDelay;
    private final long statsPrintIntervalMs;
}
