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
package org.thingsboard.server.service.entitiy.alarm;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.housekeeper.HouseKeeperService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class DefaultTbAlarmService extends AbstractTbEntityService implements TbAlarmService {

    @Autowired
    protected TbAlarmCommentService alarmCommentService;

    @Autowired
    private HouseKeeperService housekeeper;

    @Override
    public Alarm save(Alarm alarm, User user) throws ThingsboardException {
        ActionType actionType = alarm.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = alarm.getTenantId();
        try {
            AlarmApiCallResult result;
            if (alarm.getId() == null) {
                result = alarmSubscriptionService.createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm, user.getId()));
            } else {
                result = alarmSubscriptionService.updateAlarm(AlarmUpdateRequest.fromAlarm(alarm, user.getId()));
            }
            if (!result.isSuccessful()) {
                throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
            AlarmInfo resultAlarm = result.getAlarm();
            if (alarm.isAcknowledged() && !resultAlarm.isAcknowledged()) {
                resultAlarm = ack(resultAlarm, alarm.getAckTs(), user);
            }
            if (alarm.isCleared() && !resultAlarm.isCleared()) {
                resultAlarm = clear(resultAlarm, alarm.getClearTs(), user);
            }
            UserId newAssignee = alarm.getAssigneeId();
            UserId curAssignee = resultAlarm.getAssigneeId();
            if (newAssignee != null && !newAssignee.equals(curAssignee)) {
                resultAlarm = assign(resultAlarm, newAssignee, alarm.getAssignTs(), user);
            } else if (newAssignee == null && curAssignee != null) {
                resultAlarm = unassign(alarm, alarm.getAssignTs(), user);
            }
            if (result.isModified()) {
                notificationEntityService.logEntityAction(tenantId, alarm.getOriginator(), resultAlarm,
                        resultAlarm.getCustomerId(), actionType, user);
            }
            return new Alarm(resultAlarm);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ALARM), alarm, actionType, user, e);
            throw e;
        }
    }

    @Override
    public AlarmInfo ack(Alarm alarm, User user) throws ThingsboardException {
        return ack(alarm, System.currentTimeMillis(), user);
    }

    @Override
    public AlarmInfo ack(Alarm alarm, long ackTs, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.acknowledgeAlarm(alarm.getTenantId(), alarm.getId(), getOrDefault(ackTs));
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AlarmInfo alarmInfo = result.getAlarm();
        if (result.isModified()) {
            AlarmComment alarmComment = AlarmComment.builder()
                    .alarmId(alarm.getId())
                    .type(AlarmCommentType.SYSTEM)
                    .comment(JacksonUtil.newObjectNode().put("text", String.format("Alarm was acknowledged by user %s",
                                    (user.getFirstName() == null || user.getLastName() == null) ? user.getName() : user.getFirstName() + " " + user.getLastName()))
                            .put("userId", user.getId().toString())
                            .put("subtype", "ACK"))
                    .build();
            try {
                alarmCommentService.saveAlarmComment(alarm, alarmComment, user);
            } catch (ThingsboardException e) {
                log.error("Failed to save alarm comment", e);
            }
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
                    alarmInfo.getCustomerId(), ActionType.ALARM_ACK, user);
        } else {
            throw new ThingsboardException("Alarm was already acknowledged!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    @Override
    public AlarmInfo clear(Alarm alarm, User user) throws ThingsboardException {
        return clear(alarm, System.currentTimeMillis(), user);
    }

    @Override
    public AlarmInfo clear(Alarm alarm, long clearTs, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.clearAlarm(alarm.getTenantId(), alarm.getId(), getOrDefault(clearTs), null);
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AlarmInfo alarmInfo = result.getAlarm();
        if (result.isCleared()) {
            AlarmComment alarmComment = AlarmComment.builder()
                    .alarmId(alarm.getId())
                    .type(AlarmCommentType.SYSTEM)
                    .comment(JacksonUtil.newObjectNode().put("text", String.format("Alarm was cleared by user %s",
                                    (user.getFirstName() == null || user.getLastName() == null) ? user.getName() : user.getFirstName() + " " + user.getLastName()))
                            .put("userId", user.getId().toString())
                            .put("subtype", "CLEAR"))
                    .build();
            try {
                alarmCommentService.saveAlarmComment(alarm, alarmComment, user);
            } catch (ThingsboardException e) {
                log.error("Failed to save alarm comment", e);
            }
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
                    alarmInfo.getCustomerId(), ActionType.ALARM_CLEAR, user);
        } else {
            throw new ThingsboardException("Alarm was already cleared!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    @Override
    public AlarmInfo assign(Alarm alarm, UserId assigneeId, long assignTs, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.assignAlarm(alarm.getTenantId(), alarm.getId(), assigneeId, getOrDefault(assignTs));
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AlarmInfo alarmInfo = result.getAlarm();
        if (result.isModified()) {
            AlarmAssignee assignee = alarmInfo.getAssignee();
            AlarmComment alarmComment = AlarmComment.builder()
                    .alarmId(alarm.getId())
                    .type(AlarmCommentType.SYSTEM)
                    .comment(JacksonUtil.newObjectNode().put("text", String.format("Alarm was assigned by user %s to user %s",
                                    (user.getFirstName() == null || user.getLastName() == null) ? user.getName() : user.getFirstName() + " " + user.getLastName(),
                                    (assignee.getFirstName() == null || assignee.getLastName() == null) ? assignee.getEmail() : assignee.getFirstName() + " " + assignee.getLastName()))
                            .put("userId", user.getId().toString())
                            .put("assigneeId", assignee.getId().toString())
                            .put("subtype", "ASSIGN"))
                    .build();
            try {
                alarmCommentService.saveAlarmComment(alarm, alarmComment, user);
            } catch (ThingsboardException e) {
                log.error("Failed to save alarm comment", e);
            }
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
                    alarmInfo.getCustomerId(), ActionType.ALARM_ASSIGNED, user);
        } else {
            throw new ThingsboardException("Alarm was already assigned to this user!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    @Override
    public AlarmInfo unassign(Alarm alarm, long unassignTs, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.unassignAlarm(alarm.getTenantId(), alarm.getId(), getOrDefault(unassignTs));
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AlarmInfo alarmInfo = result.getAlarm();
        if (result.isModified()) {
            AlarmComment alarmComment = AlarmComment.builder()
                    .alarmId(alarm.getId())
                    .type(AlarmCommentType.SYSTEM)
                    .comment(JacksonUtil.newObjectNode().put("text", String.format("Alarm was unassigned by user %s",
                                    (user.getFirstName() == null || user.getLastName() == null) ? user.getName() : user.getFirstName() + " " + user.getLastName()))
                            .put("userId", user.getId().toString())
                            .put("subtype", "ASSIGN"))
                    .build();
            try {
                alarmCommentService.saveAlarmComment(alarm, alarmComment, user);
            } catch (ThingsboardException e) {
                log.error("Failed to save alarm comment", e);
            }
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
                    alarmInfo.getCustomerId(), ActionType.ALARM_UNASSIGNED, user);
        } else {
            throw new ThingsboardException("Alarm was already unassigned!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    @Override
    public List<AlarmId> unassignDeletedUserAlarms(TenantId tenantId, User user, long unassignTs) {
        List<AlarmId> totalAlarmIds = new ArrayList<>();
        List<AlarmId> alarmIds;
        do {
            alarmIds = alarmService.findAlarmIdsByAssigneeId(tenantId, user.getId(), 100);
            for (AlarmId alarmId : alarmIds) {
                log.trace("[{}] Unassigning alarm {} userId {}", tenantId, alarmId.getId(), user.getId().getId());
                AlarmApiCallResult result = alarmSubscriptionService.unassignAlarm(user.getTenantId(), alarmId, unassignTs);
                Alarm alarm = result.getAlarm();
                if (!result.isSuccessful()) {
                    continue;
                }
                if (result.isModified()) {
                    try {
                        AlarmComment alarmComment = AlarmComment.builder()
                                .alarmId(alarmId)
                                .type(AlarmCommentType.SYSTEM)
                                .comment(JacksonUtil.newObjectNode()
                                        .put("text", String.format("Alarm was unassigned because user with id %s - was deleted",
                                                (user.getFirstName() == null || user.getLastName() == null) ? user.getName() : user.getFirstName() + " " + user.getLastName()))
                                        .put("userId", user.getId().toString())
                                        .put("subtype", "ASSIGN"))
                                .build();
                        alarmCommentService.saveAlarmComment(alarm, alarmComment, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to save alarm comment", e);
                    }
                    notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), result.getAlarm(),
                            alarm.getCustomerId(), ActionType.ALARM_UNASSIGNED, null);
                }

                totalAlarmIds.addAll(alarmIds);
            }
        }
        while (!alarmIds.isEmpty());
        return totalAlarmIds;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        log.trace("[{}] DeleteEntityEvent handler: {}", event.getTenantId(), event);
        EntityId entityId = event.getEntityId();
        if (EntityType.USER.equals(entityId.getEntityType())) {
            housekeeper.unassignDeletedUserAlarms(event.getTenantId(), (User) event.getEntity(), event.getTs());
        }
    }

    @Override
    public Boolean delete(Alarm alarm, User user) {
        TenantId tenantId = alarm.getTenantId();
        notificationEntityService.logEntityAction(tenantId, alarm.getOriginator(), alarm, alarm.getCustomerId(),
                ActionType.DELETED, user);
        return alarmSubscriptionService.deleteAlarm(tenantId, alarm.getId());
    }

    private static long getOrDefault(long ts) {
        return ts > 0 ? ts : System.currentTimeMillis();
    }
}
