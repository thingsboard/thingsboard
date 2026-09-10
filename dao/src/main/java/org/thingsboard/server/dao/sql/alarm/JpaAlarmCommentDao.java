// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.dao.model.sql.AlarmCommentEntity;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_TABLE_NAME;

@Slf4j
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaAlarmCommentDao extends JpaPartitionedAbstractDao<AlarmCommentEntity, AlarmComment> implements AlarmCommentDao, TenantEntityDao<AlarmComment> {
    private final SqlPartitioningRepository partitioningRepository;
    @Value("${sql.alarm_comments.partition_size:168}")
    private int partitionSizeInHours;

    @Autowired
    private AlarmCommentRepository alarmCommentRepository;

    @Override
    public PageData<AlarmCommentInfo> findAlarmComments(TenantId tenantId, AlarmId id, PageLink pageLink) {
        log.trace("Try to find alarm comments by alarm id using [{}]", id);
        return DaoUtil.toPageData(
                alarmCommentRepository.findAllByAlarmId(id.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public AlarmComment findAlarmCommentById(TenantId tenantId, UUID key) {
        log.trace("Try to find alarm comment by id using [{}]", key);
        return DaoUtil.getData(alarmCommentRepository.findById(key));
    }

    @Override
    public ListenableFuture<AlarmComment> findAlarmCommentByIdAsync(TenantId tenantId, UUID key) {
        log.trace("Try to find alarm comment by id using [{}]", key);
        return findByIdAsync(tenantId, key);
    }

    @Override
    public void createPartition(AlarmCommentEntity entity) {
        partitioningRepository.createPartitionIfNotExists(ALARM_COMMENT_TABLE_NAME, entity.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    @Override
    public PageData<AlarmComment> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(alarmCommentRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    protected Class<AlarmCommentEntity> getEntityClass() {
        return AlarmCommentEntity.class;
    }

    @Override
    protected JpaRepository<AlarmCommentEntity, UUID> getRepository() {
        return alarmCommentRepository;
    }

}
