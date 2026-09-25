// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteAlarmCommentArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAlarmCommentsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveAlarmArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveAlarmCommentArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceArgs;
import org.thingsboard.client.model.Alarm;
import org.thingsboard.client.model.AlarmComment;
import org.thingsboard.client.model.AlarmCommentInfo;
import org.thingsboard.client.model.AlarmSeverity;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.PageDataAlarmCommentInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class AlarmCommentApiClientTest extends AbstractApiClientTest {

    @Test
    public void testAlarmComments() throws Exception {
        long timestamp = System.currentTimeMillis();

        // Create device for alarm
        Device device = new Device();
        device.setName("Device_For_Comments_" + timestamp);
        device.setType("default");
        Device createdDevice = client.saveDevice(SaveDeviceArgs.builder()
                .device(device)
                .build());

        // Create alarm
        Alarm alarm = new Alarm();
        alarm.setType("Temperature Alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setOriginator(createdDevice.getId());

        Alarm createdAlarm = client.saveAlarm(SaveAlarmArgs.builder()
                .alarm(alarm)
                .build());
        String alarmId = createdAlarm.getId().getId().toString();

        List<AlarmComment> createdComments = new ArrayList<>();

        // Create multiple comments
        for (int i = 0; i < 5; i++) {
            AlarmComment alarmComment = new AlarmComment();
            String message = "Test comment #" + i + " at " + timestamp;
            ObjectNode comment = OBJECT_MAPPER.createObjectNode().put("message", message);
            alarmComment.setComment(comment);

            AlarmComment commentInfo = client.saveAlarmComment(SaveAlarmCommentArgs.builder()
                    .alarmId(alarmId)
                    .alarmComment(alarmComment)
                    .build());

            assertNotNull(commentInfo);
            assertNotNull(commentInfo.getId());
            JsonNode commentValue = commentInfo.getComment();
            assertEquals(message, commentValue.get("message").asText());
            assertNotNull(commentInfo.getCreatedTime());

            createdComments.add(commentInfo);
        }

        // Get all comments for the alarm
        PageDataAlarmCommentInfo allComments = client.getAlarmComments(GetAlarmCommentsArgs.builder()
                .alarmId(alarmId)
                .pageSize(100)
                .page(0)
                .build());
        assertEquals("Expected 5 comments", 5, allComments.getData().size());

        // Update a comment
        AlarmComment commentToUpdate = createdComments.get(2);
        JsonNode comment = commentToUpdate.getComment();
        ((ObjectNode) comment).put("message", "New comment");
        commentToUpdate.setComment(comment);

        AlarmComment updatedComment = client.saveAlarmComment(SaveAlarmCommentArgs.builder()
                .alarmId(alarmId)
                .alarmComment(commentToUpdate)
                .build());
        assertEquals("New comment", updatedComment.getComment().get("message").asText());

        // Delete a comment
        UUID commentToDeleteId = createdComments.get(0).getId().getId();

        client.deleteAlarmComment(DeleteAlarmCommentArgs.builder()
                .alarmId(alarmId)
                .commentId(commentToDeleteId.toString())
                .build());

        // Verify comment was updated to "deleted"
        PageDataAlarmCommentInfo commentsAfterDelete = client.getAlarmComments(GetAlarmCommentsArgs.builder()
                .alarmId(alarmId)
                .pageSize(100)
                .page(0)
                .build());
        List<AlarmCommentInfo> data = commentsAfterDelete.getData();
        AlarmCommentInfo deletedComment = data.stream()
                .filter(alarmCommentInfo -> alarmCommentInfo.getId().getId().equals(commentToDeleteId))
                .findFirst()
                .get();
        assertEquals("Comment was deleted by user " + clientTenantAdmin.getEmail(), deletedComment.getComment().get("text").asText());
    }

}
