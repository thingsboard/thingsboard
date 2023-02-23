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
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.alarm.AlarmApiCallResult;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbAlarmService extends AbstractTbEntityService implements TbAlarmService {

    @Override
    public Alarm save(Alarm alarm, User user) throws ThingsboardException {
        ActionType actionType = alarm.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = alarm.getTenantId();
        try {
            AlarmApiCallResult result;
            if (alarm.getId() == null) {
                result = alarmSubscriptionService.createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm));
            } else {
                result = alarmSubscriptionService.updateAlarm(AlarmUpdateRequest.fromAlarm(alarm));
            }
            actionType = result.isCreated() ? ActionType.ADDED : ActionType.UPDATED;
            if (result.isModified()) {
                notificationEntityService.notifyCreateOrUpdateAlarm(result.getAlarm(), actionType, user);
            }
            return new Alarm(result.getAlarm());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ALARM), alarm, actionType, user, e);
            throw e;
        }
    }

    @Override
    public AlarmInfo ack(Alarm alarm, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.acknowledgeAlarm(alarm.getTenantId(), alarm.getId(), System.currentTimeMillis());
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        if (result.isModified()) {
            AlarmComment alarmComment = AlarmComment.builder()
                    .alarmId(alarm.getId())
                    .type(AlarmCommentType.SYSTEM)
                    .comment(JacksonUtil.newObjectNode().put("text", String.format("Alarm was acknowledged by user %s",
                            (user.getFirstName() == null || user.getLastName() == null) ? user.getName() : user.getFirstName() + " " + user.getLastName()))
                            .put("userId", user.getId().toString())
                            .put("subtype", "ACK"))
                    .build();
            alarmCommentService.createOrUpdateAlarmComment(alarm.getTenantId(), alarmComment);
            notificationEntityService.notifyCreateOrUpdateAlarm(result.getAlarm(), ActionType.ALARM_ACK, user);
        } else {
            throw new ThingsboardException("Alarm was already acknowledged!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return result.getAlarm();
    }

    @Override
    public AlarmInfo clear(Alarm alarm, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.clearAlarm(alarm.getTenantId(), alarm.getId(), System.currentTimeMillis(), null);
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        if (result.isCleared()) {
            AlarmComment alarmComment = AlarmComment.builder()
                    .alarmId(alarm.getId())
                    .type(AlarmCommentType.SYSTEM)
                    .comment(JacksonUtil.newObjectNode().put("text", String.format("Alarm was cleared by user %s",
                            (user.getFirstName() == null || user.getLastName() == null) ? user.getName() : user.getFirstName() + " " + user.getLastName()))
                            .put("userId", user.getId().toString())
                            .put("subtype", "CLEAR"))
                    .build();
            alarmCommentService.createOrUpdateAlarmComment(alarm.getTenantId(), alarmComment);
            notificationEntityService.notifyCreateOrUpdateAlarm(result.getAlarm(), ActionType.ALARM_CLEAR, user);
        } else {
            throw new ThingsboardException("Alarm was already cleared!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return result.getAlarm();
    }

    @Override
    public AlarmInfo assign(Alarm alarm, User user, UserId assigneeId) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.assignAlarm(alarm.getTenantId(), alarm.getId(), assigneeId, System.currentTimeMillis());
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
            alarmCommentService.createOrUpdateAlarmComment(alarm.getTenantId(), alarmComment);
            notificationEntityService.notifyCreateOrUpdateAlarm(result.getAlarm(), ActionType.ALARM_ASSIGN, user);
        } else {
            throw new ThingsboardException("Alarm was already assigned to this user!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    @Override
    public AlarmInfo unassign(Alarm alarm, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.unassignAlarm(alarm.getTenantId(), alarm.getId(), System.currentTimeMillis());
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
            alarmCommentService.createOrUpdateAlarmComment(alarm.getTenantId(), alarmComment);
            notificationEntityService.notifyCreateOrUpdateAlarm(result.getAlarm(), ActionType.ALARM_UNASSIGN, user);
        } else {
            throw new ThingsboardException("Alarm was already unassigned!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    @Override
    public Boolean delete(Alarm alarm, User user) {
        TenantId tenantId = alarm.getTenantId();
        List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, alarm.getOriginator());
        notificationEntityService.notifyDeleteAlarm(tenantId, alarm, alarm.getOriginator(), alarm.getCustomerId(),
                relatedEdgeIds, user, JacksonUtil.toString(alarm));
        return alarmSubscriptionService.deleteAlarm(tenantId, alarm.getId());
    }
}