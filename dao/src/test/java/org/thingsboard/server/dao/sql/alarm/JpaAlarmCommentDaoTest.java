// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.alarm;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.dao.alarm.AlarmDao;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

@Slf4j
public class JpaAlarmCommentDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private AlarmCommentDao alarmCommentDao;
    @Autowired
    private AlarmDao alarmDao;


    @Test
    public void testFindAlarmCommentsByAlarmId() {
        log.info("Current system time in millis = {}", System.currentTimeMillis());
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID alarmId1 = UUID.randomUUID();
        UUID alarmId2 = UUID.randomUUID();
        UUID commentId1 = UUID.randomUUID();
        UUID commentId2 = UUID.randomUUID();
        UUID commentId3 = UUID.randomUUID();
        saveAlarm(alarmId1, UUID.randomUUID(), UUID.randomUUID(), "TEST_ALARM");
        saveAlarm(alarmId2, UUID.randomUUID(), UUID.randomUUID(), "TEST_ALARM");

        saveAlarmComment(commentId1, alarmId1, userId, AlarmCommentType.OTHER);
        saveAlarmComment(commentId2, alarmId1, userId, AlarmCommentType.OTHER);
        saveAlarmComment(commentId3, alarmId2, userId, AlarmCommentType.OTHER);

        int count = alarmCommentDao.findAlarmComments(TenantId.fromUUID(tenantId), new AlarmId(alarmId1), new PageLink(10, 0)).getData().size();
        assertEquals(2, count);
    }

    private void saveAlarm(UUID id, UUID tenantId, UUID deviceId, String type) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(id));
        alarm.setTenantId(TenantId.fromUUID(tenantId));
        alarm.setOriginator(new DeviceId(deviceId));
        alarm.setType(type);
        alarm.setPropagate(true);
        alarm.setStartTs(System.currentTimeMillis());
        alarm.setEndTs(System.currentTimeMillis());
        alarmDao.save(TenantId.fromUUID(tenantId), alarm);
    }
    private void saveAlarmComment(UUID id, UUID alarmId, UUID userId, AlarmCommentType type) {
        AlarmComment alarmComment = new AlarmComment();
        alarmComment.setId(new AlarmCommentId(id));
        alarmComment.setAlarmId(new AlarmId(alarmId));
        alarmComment.setUserId(new UserId(userId));
        alarmComment.setType(type);
        alarmComment.setComment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)));
        alarmCommentDao.save(TenantId.fromUUID(UUID.randomUUID()), alarmComment);
    }
}
