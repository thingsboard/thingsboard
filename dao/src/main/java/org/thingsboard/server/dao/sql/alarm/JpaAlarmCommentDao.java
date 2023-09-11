/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.dao.model.sql.AlarmCommentEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_COLUMN_FAMILY_NAME;

@Slf4j
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaAlarmCommentDao extends JpaAbstractDao<AlarmCommentEntity, AlarmComment> implements AlarmCommentDao {
    private final SqlPartitioningRepository partitioningRepository;
    @Value("${sql.alarm_comments.partition_size:168}")
    private int partitionSizeInHours;

    @Autowired
    private AlarmCommentRepository alarmCommentRepository;

    @Override
    public AlarmComment createAlarmComment(TenantId tenantId, AlarmComment alarmComment){
        log.trace("Saving entity {}", alarmComment);
        partitioningRepository.createPartitionIfNotExists(ALARM_COMMENT_COLUMN_FAMILY_NAME, alarmComment.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
        AlarmCommentEntity saved = alarmCommentRepository.save(new AlarmCommentEntity(alarmComment));
        return DaoUtil.getData(saved);
    }

    @Override
    public void deleteAlarmComment(TenantId tenantId, AlarmCommentId alarmCommentId){
        log.trace("Try to delete entity alarm comment by id using [{}]", alarmCommentId);
        alarmCommentRepository.deleteById(alarmCommentId.getId());
    }

    @Override
    public PageData<AlarmCommentInfo> findAlarmComments(TenantId tenantId, AlarmId id, PageLink pageLink){
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
    protected Class<AlarmCommentEntity> getEntityClass() {
        return AlarmCommentEntity.class;
    }

    @Override
    protected JpaRepository<AlarmCommentEntity, UUID> getRepository() {
        return alarmCommentRepository;
    }
}
