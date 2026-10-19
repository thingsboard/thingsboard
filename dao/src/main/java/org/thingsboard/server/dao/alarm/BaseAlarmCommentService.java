// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmCommentService extends AbstractEntityService implements AlarmCommentService {

    @Autowired
    private AlarmCommentDao alarmCommentDao;

    @Autowired
    private DataValidator<AlarmComment> alarmCommentDataValidator;

    @Override
    public AlarmComment createOrUpdateAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        AlarmComment oldAlarmComment = alarmCommentDataValidator.validate(alarmComment, c -> tenantId);
        boolean isCreated = alarmComment.getId() == null;
        AlarmComment result;
        if (isCreated) {
            result = createAlarmComment(tenantId, alarmComment);
        } else {
            result = updateAlarmComment(tenantId, alarmComment, oldAlarmComment);
        }
        if (result != null) {
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(result)
                    .entityId(result.getAlarmId()).created(isCreated).build());
        }
        return result;
    }

    @Override
    public AlarmComment saveAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        log.debug("Saving Alarm Comment: {}", alarmComment);
        alarmCommentDataValidator.validate(alarmComment, c -> tenantId);
        AlarmComment result = alarmCommentDao.save(tenantId, alarmComment);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entity(result)
                .entityId(result.getAlarmId()).build());
        return result;
    }

    @Override
    public PageData<AlarmCommentInfo> findAlarmComments(TenantId tenantId, AlarmId alarmId, PageLink pageLink) {
        log.trace("Executing findAlarmComments by alarmId [{}]", alarmId);
        return alarmCommentDao.findAlarmComments(tenantId, alarmId, pageLink);
    }

    @Override
    public ListenableFuture<AlarmComment> findAlarmCommentByIdAsync(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync by alarmCommentId [{}]", alarmCommentId);
        validateId(alarmCommentId, id -> "Incorrect alarmCommentId " + id);
        return alarmCommentDao.findAlarmCommentByIdAsync(tenantId, alarmCommentId.getId());
    }

    @Override
    public AlarmComment findAlarmCommentById(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync by alarmCommentId [{}]", alarmCommentId);
        validateId(alarmCommentId, id -> "Incorrect alarmCommentId " + id);
        return alarmCommentDao.findById(tenantId, alarmCommentId.getId());
    }

    private AlarmComment createAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        log.debug("New Alarm comment : {}", alarmComment);
        if (alarmComment.getType() == null) {
            alarmComment.setType(AlarmCommentType.OTHER);
        }
        return alarmCommentDao.save(tenantId, alarmComment);
    }

    private AlarmComment updateAlarmComment(TenantId tenantId, AlarmComment newAlarmComment, AlarmComment oldAlarmComment) {
        log.debug("Update Alarm comment : {}", newAlarmComment);

        if (oldAlarmComment != null) {
            if (newAlarmComment.getComment() != null) {
                JsonNode comment = newAlarmComment.getComment();
                ((ObjectNode) comment).put("edited", "true");
                ((ObjectNode) comment).put("editedOn", System.currentTimeMillis());
                oldAlarmComment.setComment(comment);
            }
            return alarmCommentDao.save(tenantId, oldAlarmComment);
        }
        return null;
    }
}
