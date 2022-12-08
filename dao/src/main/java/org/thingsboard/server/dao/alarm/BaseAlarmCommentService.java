/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.alarm;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.service.DataValidator;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmCommentService extends AbstractEntityService implements AlarmCommentService{

    @Autowired
    private AlarmCommentDao alarmCommentDao;
    @Autowired
    private EntityService entityService;
    @Autowired
    private DataValidator<AlarmComment> alarmCommentDataValidator;
    @Override
    public AlarmCommentOperationResult createOrUpdateAlarmComment(AlarmComment alarmComment) {
        alarmCommentDataValidator.validate(alarmComment, AlarmComment::getTenantId);
        alarmComment.setCustomerId(entityService.fetchEntityCustomerId(alarmComment.getTenantId(), alarmComment.getAlarmId()));
        if (alarmComment.getId() == null) {
            return createAlarmComment(alarmComment);
        } else {
            return updateAlarmComment(alarmComment);
        }
    }

    @Override
    @Transactional
    public AlarmCommentOperationResult deleteAlarmComment(AlarmCommentId alarmCommentId) {
        log.debug("Deleting Alarm Comment with id: {}", alarmCommentId);
        AlarmCommentOperationResult result = new AlarmCommentOperationResult(new AlarmComment(), true);
        alarmCommentDao.deleteAlarmComment(alarmCommentId);
        return result;
    }

    @Override
    public ListenableFuture<PageData<AlarmComment>> findAlarmComments(AlarmId alarmId, PageLink pageLink) {
        PageData<AlarmComment> alarmComments = alarmCommentDao.findAlarmComments(alarmId, pageLink);
        return Futures.immediateFuture(alarmComments);
    }

    @Override
    public ListenableFuture<AlarmComment> findAlarmCommentByIdAsync(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync [{}]", alarmCommentId);
        validateId(alarmCommentId, "Incorrect alarmId " + alarmCommentId);
        return alarmCommentDao.findAlarmCommentByIdAsync(tenantId, alarmCommentId.getId());
    }

    private AlarmCommentOperationResult createAlarmComment(AlarmComment alarmComment) {
        log.debug("New Alarm comment : {}", alarmComment);
        if (alarmComment.getType() == null) {
            alarmComment.setType("OTHER");
        }
        AlarmComment saved = alarmCommentDao.createAlarmComment(alarmComment);
        return new AlarmCommentOperationResult(saved, true, true);
    }

    private AlarmCommentOperationResult updateAlarmComment(AlarmComment newAlarmComment) {
        log.debug("Update Alarm comment : {}", newAlarmComment);

        validateId(newAlarmComment.getId(), "Alarm comment id should be specified!");
        AlarmComment existing = alarmCommentDao.findAlarmCommentById(newAlarmComment.getTenantId(), newAlarmComment.getId().getId());
        if (existing != null) {
            AlarmComment result = alarmCommentDao.save(newAlarmComment.getTenantId(), merge(existing, newAlarmComment));
            return new AlarmCommentOperationResult(result, true, true);
        }
        return null;
    }

    private AlarmComment merge(AlarmComment existing, AlarmComment alarmComment) {
        existing.setCustomerId(alarmComment.getCustomerId());
        existing.setComment(alarmComment.getComment());
        return existing;
    }
}
