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
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
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
        Device createdDevice = client.saveDevice(device, null, null, null, null);

        // Create alarm
        Alarm alarm = new Alarm();
        alarm.setType("Temperature Alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setOriginator(createdDevice.getId());

        Alarm createdAlarm = client.saveAlarm(alarm);
        String alarmId = createdAlarm.getId().getId().toString();

        List<AlarmComment> createdComments = new ArrayList<>();

        // Create multiple comments
        for (int i = 0; i < 5; i++) {
            AlarmComment alarmComment = new AlarmComment();
            String message = "Test comment #" + i + " at " + timestamp;
            ObjectNode comment = OBJECT_MAPPER.createObjectNode().put("message", message);
            alarmComment.setComment(comment);

            AlarmComment commentInfo = client.saveAlarmComment(alarmId, alarmComment);

            assertNotNull(commentInfo);
            assertNotNull(commentInfo.getId());
            JsonNode commentValue = commentInfo.getComment();
            assertEquals(message, commentValue.get("message").asText());
            assertNotNull(commentInfo.getCreatedTime());

            createdComments.add(commentInfo);
        }

        // Get all comments for the alarm
        PageDataAlarmCommentInfo allComments = client.getAlarmComments(alarmId, 100, 0, null, null);
        assertEquals("Expected 5 comments", 5, allComments.getData().size());

        // Update a comment
        AlarmComment commentToUpdate = createdComments.get(2);
        JsonNode comment = commentToUpdate.getComment();
        ((ObjectNode) comment).put("message", "New comment");
        commentToUpdate.setComment(comment);

        AlarmComment updatedComment = client.saveAlarmComment(alarmId, commentToUpdate);
        assertEquals("New comment", updatedComment.getComment().get("message").asText());

        // Delete a comment
        UUID commentToDeleteId = createdComments.get(0).getId().getId();

        client.deleteAlarmComment(alarmId, commentToDeleteId.toString());

        // Verify comment was updated to "deleted"
        PageDataAlarmCommentInfo commentsAfterDelete = client.getAlarmComments(alarmId, 100, 0, null, null);
        List<AlarmCommentInfo> data = commentsAfterDelete.getData();
        AlarmCommentInfo deletedComment = data.stream()
                .filter(alarmCommentInfo -> alarmCommentInfo.getId().getId().equals(commentToDeleteId))
                .findFirst()
                .get();
        assertEquals("User " + clientTenantAdmin.getEmail() + " deleted his comment", deletedComment.getComment().get("text").asText());
    }

}
