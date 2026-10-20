// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.alarm.AlarmService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the alarm-type-names registration cache end-to-end through the alarm service API. Named
 * {@code *ServiceSqlTest} so {@code RedisSqlTestSuite} reruns it against Redis, covering
 * {@code AlarmTypeNamesRedisCache}: the {@code registerAlarmType} fast path and the {@code HashSet<String>} JSON
 * round-trip through {@code TbTypedJsonRedisSerializer}, plus the eviction on type deletion.
 */
@DaoSqlTest
public class AlarmTypesServiceSqlTest extends AbstractServiceTest {

    @Autowired
    private AlarmService alarmService;

    @Test
    public void testAlarmTypeRegistrationCacheLifecycle() {
        // Unique type name (and the per-test tenant as the cache key) keeps this bleed-safe on a shared Redis.
        String type = "test-alarm-type-" + UUID.randomUUID();
        long ts = System.currentTimeMillis();

        // First alarm of the type registers it (cache miss -> alarm_types insert + cache population).
        AlarmInfo first = createAlarm(type, ts);
        assertThat(findAlarmTypes()).containsExactly(type);

        // Second alarm of the same type hits the cached name set (fast path, no re-insert).
        AlarmInfo second = createAlarm(type, ts);
        assertThat(findAlarmTypes()).containsExactly(type);

        // Deleting every alarm of the type removes it from alarm_types and must evict the cached name set.
        assertThat(alarmService.delAlarm(tenantId, first.getOriginator(), first.getId()).isSuccessful()).isTrue();
        assertThat(alarmService.delAlarm(tenantId, second.getOriginator(), second.getId()).isSuccessful()).isTrue();
        assertThat(findAlarmTypes()).isEmpty();

        // Recreating the type must re-register it; a stale cached set would skip the alarm_types re-insert.
        AlarmInfo recreated = createAlarm(type, ts);
        assertThat(findAlarmTypes()).containsExactly(type);
        assertThat(alarmService.delAlarm(tenantId, recreated.getOriginator(), recreated.getId()).isSuccessful()).isTrue();
    }

    private AlarmInfo createAlarm(String type, long ts) {
        return alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(new AssetId(Uuids.timeBased()))
                .type(type)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(ts).build()).getAlarm();
    }

    private List<String> findAlarmTypes() {
        return alarmService.findAlarmTypesByTenantId(tenantId, new PageLink(100)).getData().stream()
                .map(EntitySubtype::getType)
                .toList();
    }

}
