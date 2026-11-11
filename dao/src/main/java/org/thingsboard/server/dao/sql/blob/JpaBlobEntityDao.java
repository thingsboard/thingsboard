// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.blob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.edqs.fields.BlobEntityFields;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.blob.BlobEntityDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.BlobEntityEntity;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@SqlDao
@RequiredArgsConstructor
@Slf4j
public class JpaBlobEntityDao extends JpaPartitionedAbstractDao<BlobEntityEntity, BlobEntity> implements BlobEntityDao {

    private final BlobEntityRepository blobEntityRepository;
    private final SqlPartitioningRepository partitioningRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${sql.blob_entities.partition_size:168}")
    private int partitionSizeInHours;
    @Value("${sql.ttl.blob_entities.enabled:false}")
    private boolean ttlEnabled;
    @Value("${sql.ttl.blob_entities.ttl:0}")
    private int ttlInSec;

    private static final String TABLE_NAME = ModelConstants.BLOB_ENTITY_TABLE_NAME;

    @Override
    protected Class<BlobEntityEntity> getEntityClass() {
        return BlobEntityEntity.class;
    }

    @Override
    protected JpaRepository<BlobEntityEntity, UUID> getRepository() {
        return blobEntityRepository;
    }

    @Override
    public void cleanUpBlobEntities(long expTime) {
        partitioningRepository.dropPartitionsBefore(TABLE_NAME, expTime, getPartitionSizeInMs());
    }

    @Override
    public void migrateBlobEntities() {
        long startTime = ttlEnabled && (long) ttlInSec > 0 ?
                System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec) : 1480982400000L;

        long currentTime = System.currentTimeMillis();
        var partitionStepInMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        long numberOfPartitions = (currentTime - startTime) / partitionStepInMs;

        if (numberOfPartitions > 1000) {
            String error = "Please adjust your " + TABLE_NAME + " partitioning configuration. Configuration with partition size " +
                    "of " + partitionSizeInHours + " hours and corresponding TTL will use " + numberOfPartitions + " " +
                    "(> 1000) partitions which is not recommended!";
            log.error(error);
            throw new RuntimeException(error);
        }

        while (startTime < currentTime) {
            var endTime = startTime + partitionStepInMs;
            log.info("Migrating blob entities for time period: {} - {}", startTime, endTime);
            jdbcTemplate.update("CALL migrate_blob_entities(?, ?, ?)", startTime, endTime, partitionStepInMs);
            startTime = endTime;
        }

        jdbcTemplate.execute("DROP TABLE IF EXISTS old_blob_entity");
        log.info("Dropped old_blob_entity table");
        log.info("Blob entities migration finished");
    }

    @Override
    public void createPartition(BlobEntityEntity entity) {
        partitioningRepository.createPartitionIfNotExists(TABLE_NAME, entity.getCreatedTime(), getPartitionSizeInMs());
    }

    private long getPartitionSizeInMs() {
        return TimeUnit.HOURS.toMillis(partitionSizeInHours);
    }

    @Override
    public PageData<BlobEntity> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(blobEntityRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<BlobEntityFields> findNextBatch(UUID id, int batchSize) {
        return blobEntityRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.BLOB_ENTITY;
    }

}
