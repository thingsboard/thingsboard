// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ttl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.blob.BlobEntityDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnExpression("${sql.ttl.blob_entities.enabled:true} && ${sql.ttl.blob_entities.ttl:0} > 0")
public class BlobEntitiesCleanUpService extends AbstractCleanUpService {

    @Autowired
    private BlobEntityDao blobEntityDao;
    @Autowired
    private SqlPartitioningRepository partitioningRepository;

    @Value("${sql.ttl.blob_entities.ttl:0}")
    private long ttlInSec;
    @Value("${sql.blob_entities.partition_size:168}")
    private int partitionSizeInHours;

    public BlobEntitiesCleanUpService(PartitionService partitionService) {
        super(partitionService);
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.blob_entities.checking_interval_ms})}",
            fixedDelayString = "${sql.ttl.blob_entities.checking_interval_ms}")
    public void cleanUp() {
        if (ttlInSec <= 0) return;

        long blobEntitiesExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        if (isSystemTenantPartitionMine()) {
            blobEntityDao.cleanUpBlobEntities(blobEntitiesExpTime);
        } else {
            partitioningRepository.cleanupPartitionsCache(ModelConstants.BLOB_ENTITY_TABLE_NAME, blobEntitiesExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

}
