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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.user.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmCommentService extends AbstractEntityService implements AlarmCommentService{

    @Autowired
    private AlarmCommentDao alarmCommentDao;
    @Autowired
    private UserService userService;
    @Autowired
    private EntityService entityService;
    @Autowired
    private DataValidator<AlarmComment> alarmCommentDataValidator;
    @Override
    public AlarmCommentOperationResult createOrUpdateAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        alarmCommentDataValidator.validate(alarmComment, tenantId);
        if (alarmComment.getId() == null) {
            return createAlarmComment(tenantId, alarmComment);
        } else {
            return updateAlarmComment(tenantId, alarmComment);
        }
    }

    @Override
    @Transactional
    public AlarmCommentOperationResult deleteAlarmComment(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.debug("Deleting Alarm Comment with id: {}", alarmCommentId);
        AlarmCommentOperationResult result = new AlarmCommentOperationResult(new AlarmComment(), true);
        alarmCommentDao.deleteAlarmComment(tenantId, alarmCommentId);
        return result;
    }

    @Override
    public ListenableFuture<PageData<AlarmCommentInfo>> findAlarmComments(TenantId tenantId, AlarmId alarmId, PageLink pageLink) {
        PageData<AlarmCommentInfo> alarmComments = alarmCommentDao.findAlarmComments(tenantId, alarmId, pageLink);
        return fetchAlarmCommentUserNames(tenantId,alarmComments);
    }

    private ListenableFuture<PageData<AlarmCommentInfo>> fetchAlarmCommentUserNames(TenantId tenantId, PageData<AlarmCommentInfo> alarmComments) {
        List<ListenableFuture<AlarmCommentInfo>> alarmCommentFutures = new ArrayList<>(alarmComments.getData().size());
        for (AlarmCommentInfo alarmCommentInfo : alarmComments.getData()) {
            alarmCommentFutures.add(Futures.transform(
                    userService.findUserByIdAsync(tenantId, alarmCommentInfo.getUserId()), user -> {
                        alarmCommentInfo.setFirstName(user.getFirstName());
                        alarmCommentInfo.setLastName(user.getLastName());
                        return alarmCommentInfo;
                    }, MoreExecutors.directExecutor()
            ));
        }
        return Futures.transform(Futures.successfulAsList(alarmCommentFutures),
                alarmCommentInfos -> new PageData<>(alarmCommentInfos, alarmComments.getTotalPages(), alarmComments.getTotalElements(),
                        alarmComments.hasNext()), MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<AlarmComment> findAlarmCommentByIdAsync(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync [{}]", alarmCommentId);
        validateId(alarmCommentId, "Incorrect alarmId " + alarmCommentId);
        return alarmCommentDao.findAlarmCommentByIdAsync(tenantId, alarmCommentId.getId());
    }

    private AlarmCommentOperationResult createAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        log.debug("New Alarm comment : {}", alarmComment);
        if (alarmComment.getType() == null) {
            alarmComment.setType("OTHER");
        }
        if (alarmComment.getId() == null) {
            UUID uuid = Uuids.timeBased();
            alarmComment.setId(new AlarmCommentId(uuid));
            alarmComment.setCreatedTime(Uuids.unixTimestamp(uuid));
        }
        AlarmComment saved = alarmCommentDao.createAlarmComment(tenantId, alarmComment);
        return new AlarmCommentOperationResult(saved, true, true);
    }

    private AlarmCommentOperationResult updateAlarmComment(TenantId tenantId, AlarmComment newAlarmComment) {
        log.debug("Update Alarm comment : {}", newAlarmComment);

        AlarmComment existing = alarmCommentDao.findAlarmCommentById(tenantId, newAlarmComment.getId().getId());
        if (existing != null) {
            if (newAlarmComment.getComment() != null) {
                JsonNode comment = newAlarmComment.getComment();
                UUID uuid = Uuids.timeBased();
                ((ObjectNode) comment).put("edited", "true");
                ((ObjectNode) comment).put("editedOn", Uuids.unixTimestamp(uuid));
                existing.setComment(comment);
            }
            AlarmComment result = alarmCommentDao.save(tenantId, existing);
            return new AlarmCommentOperationResult(result, true, true);
        }
        return null;
    }
}
