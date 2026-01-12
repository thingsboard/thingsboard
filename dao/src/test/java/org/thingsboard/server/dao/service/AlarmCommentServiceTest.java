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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.user.UserService;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.alarm.AlarmCommentType.OTHER;

@DaoSqlTest
public class AlarmCommentServiceTest extends AbstractServiceTest {

    @Autowired
    AlarmService alarmService;
    @Autowired
    AlarmCommentService alarmCommentService;
    @Autowired
    UserService userService;

    public static final String TEST_ALARM = "TEST_ALARM";
    private Alarm alarm;
    private User user;

    @Before
    public void before() {
        alarm = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(new AssetId(Uuids.timeBased()))
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(System.currentTimeMillis()).build()).getAlarm();

        user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail("tenant@thingsboard.org");
        user.setFirstName("John");
        user.setLastName("Brown");
        user = userService.saveUser(TenantId.SYS_TENANT_ID, user);
    }

    @After
    public void after() {
        alarmService.delAlarm(tenantId, alarm.getId());
    }

    @Test
    public void testCreateAndFetchAlarmComment() throws ExecutionException, InterruptedException {
        AlarmComment alarmComment = AlarmComment.builder().alarmId(alarm.getId())
                .userId(user.getId())
                .type(OTHER)
                .comment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)))
                .build();

        AlarmComment createdComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);

        Assert.assertNotNull(createdComment);
        Assert.assertNotNull(createdComment.getId());

        Assert.assertEquals(alarm.getId(), createdComment.getAlarmId());
        Assert.assertEquals(user.getId(), createdComment.getUserId());
        Assert.assertEquals(OTHER, createdComment.getType());
        Assert.assertTrue(createdComment.getCreatedTime() > 0);

        AlarmComment fetched = alarmCommentService.findAlarmCommentByIdAsync(tenantId, createdComment.getId()).get();
        Assert.assertEquals(createdComment, fetched);

        PageData<AlarmCommentInfo> alarmComments = alarmCommentService.findAlarmComments(tenantId, alarm.getId(), new PageLink(10, 0));
        Assert.assertNotNull(alarmComments.getData());
        Assert.assertEquals(1, alarmComments.getData().size());
        Assert.assertEquals(createdComment, new AlarmComment(alarmComments.getData().get(0)));
    }

    @Test
    public void testUpdateAlarmComment() throws ExecutionException, InterruptedException {
        UserId userId = new UserId(UUID.randomUUID());
        AlarmComment alarmComment = AlarmComment.builder().alarmId(alarm.getId())
                .userId(userId)
                .type(OTHER)
                .comment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)))
                .build();

        AlarmComment createdComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);

        Assert.assertNotNull(createdComment);
        Assert.assertNotNull(createdComment.getId());

        //update comment
        String newComment = "new comment";
        createdComment.setComment(JacksonUtil.newObjectNode().put("text", newComment));
        AlarmComment updatedComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, createdComment);

        Assert.assertEquals(alarm.getId(), updatedComment.getAlarmId());
        Assert.assertEquals(userId, updatedComment.getUserId());
        Assert.assertEquals(OTHER, updatedComment.getType());
        Assert.assertTrue(updatedComment.getCreatedTime() > 0);
        Assert.assertEquals(newComment, updatedComment.getComment().get("text").asText());
        Assert.assertEquals("true", updatedComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedComment.getComment().get("editedOn").asText());

        AlarmComment fetched = alarmCommentService.findAlarmCommentByIdAsync(tenantId, createdComment.getId()).get();
        Assert.assertEquals(updatedComment, fetched);

        PageData<AlarmCommentInfo> alarmComments = alarmCommentService.findAlarmComments(tenantId, alarm.getId(), new PageLink(10, 0));
        Assert.assertNotNull(alarmComments.getData());
        Assert.assertEquals(1, alarmComments.getData().size());
        Assert.assertEquals(updatedComment, new AlarmComment(alarmComments.getData().get(0)));
    }

    @Test
    public void testSaveAlarmComment() throws ExecutionException, InterruptedException {
        UserId userId = new UserId(UUID.randomUUID());
        AlarmComment alarmComment = AlarmComment.builder().alarmId(alarm.getId())
                .userId(userId)
                .type(OTHER)
                .comment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)))
                .build();

        AlarmComment createdComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);

        createdComment.setType(AlarmCommentType.SYSTEM);
        createdComment.setUserId(null);
        alarmCommentService.saveAlarmComment(tenantId, createdComment);

        AlarmComment fetched = alarmCommentService.findAlarmCommentByIdAsync(tenantId, createdComment.getId()).get();
        Assert.assertNull(fetched.getUserId());
        Assert.assertEquals(AlarmCommentType.SYSTEM, fetched.getType());
    }
}
