/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class DefaultTbAlarmService extends AbstractTbEntityService implements TbAlarmService {

    @Autowired
    protected TbAlarmCommentService alarmCommentService;

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
                logEntityActionService.logEntityAction(tenantId, alarm.getOriginator(), resultAlarm,
                        resultAlarm.getCustomerId(), actionType, user);
            }
            return new Alarm(resultAlarm);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ALARM), alarm, actionType, user, e);
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
            String systemComment = String.format("Alarm was acknowledged by user %s", user.getTitle());
            addSystemAlarmComment(alarmInfo, user, "ACK", systemComment);
            logEntityActionService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
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
            String systemComment = String.format("Alarm was cleared by user %s", user.getTitle());
            addSystemAlarmComment(alarmInfo, user, "CLEAR", systemComment);
            logEntityActionService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
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
            String systemComment = String.format("Alarm was assigned by user %s to user %s", user.getTitle(), assignee.getTitle());
            addSystemAlarmComment(alarmInfo, user, "ASSIGN", systemComment, assignee.getId());
            logEntityActionService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
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
            String systemComment = String.format("Alarm was unassigned by user %s", user.getTitle());
            addSystemAlarmComment(alarmInfo, user, "ASSIGN", systemComment);
            logEntityActionService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
                    alarmInfo.getCustomerId(), ActionType.ALARM_UNASSIGNED, user);
        } else {
            throw new ThingsboardException("Alarm was already unassigned!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    @Override
    public void unassignDeletedUserAlarms(TenantId tenantId, UserId userId, String userTitle, List<UUID> alarms, long unassignTs) {
        for (UUID alarmId : alarms) {
            log.trace("[{}] Unassigning alarm {} from user {}", tenantId, alarmId, userId);
            AlarmApiCallResult result = alarmSubscriptionService.unassignAlarm(tenantId, new AlarmId(alarmId), unassignTs);
            if (!result.isSuccessful()) {
                log.error("[{}] Cannot unassign alarm {} from user {}", tenantId, alarmId, userId);
                continue;
            }
            if (result.isModified()) {
                String comment = String.format("Alarm was unassigned because user %s - was deleted", userTitle);
                addSystemAlarmComment(result.getAlarm(), null, "ASSIGN", comment);
                logEntityActionService.logEntityAction(result.getAlarm().getTenantId(), result.getAlarm().getOriginator(), result.getAlarm(), result.getAlarm().getCustomerId(), ActionType.ALARM_UNASSIGNED, null);
            }
        }
    }

    @Override
    public boolean delete(Alarm alarm, User user) {
        var tenantId = alarm.getTenantId();
        var alarmId = alarm.getId();
        var alarmOriginator = alarm.getOriginator();

        boolean deleted;
        try {
            deleted = alarmSubscriptionService.deleteAlarm(tenantId, alarmId);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(alarmOriginator.getEntityType()), ActionType.ALARM_DELETE, user, e, alarmId);
            throw e;
        }

        if (deleted) {
            logEntityActionService.logEntityAction(tenantId, alarmOriginator, alarm, alarm.getCustomerId(), ActionType.ALARM_DELETE, user, alarmId);
        }

        return deleted;
    }

    private static long getOrDefault(long ts) {
        return ts > 0 ? ts : System.currentTimeMillis();
    }

    private void addSystemAlarmComment(Alarm alarm, User user, String subType, String commentText) {
        addSystemAlarmComment(alarm, user, subType, commentText, null);
    }

    private void addSystemAlarmComment(Alarm alarm, User user, String subType, String commentText, UserId assigneeId) {
        ObjectNode commentNode = JacksonUtil.newObjectNode();
        commentNode.put("text", commentText)
                .put("subtype", subType);
        if (user != null) {
            commentNode.put("userId", user.getId().getId().toString());
        }
        if (assigneeId != null) {
            commentNode.put("assigneeId", assigneeId.getId().toString());
        }
        AlarmComment alarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(commentNode)
                .build();
        try {
            alarmCommentService.saveAlarmComment(alarm, alarmComment, user);
        } catch (ThingsboardException e) {
            log.error("Failed to save alarm comment", e);
        }
    }

}
