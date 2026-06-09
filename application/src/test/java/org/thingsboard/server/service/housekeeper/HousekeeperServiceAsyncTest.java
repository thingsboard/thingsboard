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
package org.thingsboard.server.service.housekeeper;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.TaskProcessingFailureTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.housekeeper.processor.LatestTsDeletionTaskProcessor;
import org.thingsboard.server.service.housekeeper.processor.TsHistoryDeletionTaskProcessor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.core.housekeeper.task-reprocessing-delay-ms=2000",
        "queue.core.housekeeper.poll-interval-ms=1000",
        "queue.core.housekeeper.max-reprocessing-attempts=5",
        "queue.core.housekeeper.task-processing-timeout-ms=5000",
        "queue.core.housekeeper.async-processing-enabled=true",
        "queue.core.housekeeper.async-processing-threads=4"
})
public class HousekeeperServiceAsyncTest extends AbstractControllerTest {

    @MockitoSpyBean
    private TsHistoryDeletionTaskProcessor tsHistoryDeletionTaskProcessor;
    @MockitoSpyBean
    private LatestTsDeletionTaskProcessor latestTsDeletionTaskProcessor;
    @MockitoSpyBean
    private HousekeeperReprocessingService housekeeperReprocessingService;
    @MockitoSpyBean
    private NotificationRuleProcessor notificationRuleProcessor;
    @Autowired
    private TimeseriesService timeseriesService;

    private TenantId tenantId;

    private static final String TELEMETRY_KEY = "asyncTestTelemetry";
    private static final String KV_VALUE = "testValue";

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
        this.tenantId = super.tenantId;
    }

    @After
    public void tearDown() {
        Mockito.reset(tsHistoryDeletionTaskProcessor, latestTsDeletionTaskProcessor, housekeeperReprocessingService, notificationRuleProcessor);
    }

    @Test
    public void whenDeviceIsDeleted_thenAsyncProcessingDeletesTelemetry() throws Exception {
        Device device = createDevice("asyncTest", "asyncTest");

        timeseriesService.save(tenantId, device.getId(),
                new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(TELEMETRY_KEY, KV_VALUE))).get();

        assertThat(getLatestTelemetry(device.getId())).isNotNull();
        assertThat(getTimeseriesHistory(device.getId())).isNotEmpty();

        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getLatestTelemetry(device.getId())).isNull();
            assertThat(getTimeseriesHistory(device.getId())).isEmpty();
        });

        verify(tsHistoryDeletionTaskProcessor).processAsync(any());
        verify(latestTsDeletionTaskProcessor).processAsync(any());
    }

    @Test
    public void whenAsyncProcessingFails_thenTaskIsReprocessed() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);

        // Make processAsync fail twice, then succeed
        doAnswer(invocation -> {
            int count = callCount.incrementAndGet();
            if (count <= 2) {
                return Futures.immediateFailedFuture(new RuntimeException("Simulated async failure #" + count));
            }
            return invocation.callRealMethod();
        }).when(tsHistoryDeletionTaskProcessor).processAsync(any());

        Device device = createDevice("asyncFailTest", "asyncFailTest");

        timeseriesService.save(tenantId, device.getId(),
                new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(TELEMETRY_KEY, KV_VALUE))).get();

        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getTimeseriesHistory(device.getId())).isEmpty();
        });

        verify(housekeeperReprocessingService, times(2)).submitForReprocessing(any(), any());
    }

    @Test
    public void whenMultipleDevicesDeleted_thenAsyncProcessingHandlesAllConcurrently() throws Exception {
        int deviceCount = 5;
        Device[] devices = new Device[deviceCount];

        for (int i = 0; i < deviceCount; i++) {
            devices[i] = createDevice("concurrentTest" + i, "concurrentTest" + i);
            timeseriesService.save(tenantId, devices[i].getId(),
                    new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(TELEMETRY_KEY, KV_VALUE))).get();
        }

        for (Device device : devices) {
            doDelete("/api/device/" + device.getId()).andExpect(status().isOk());
        }

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            for (Device device : devices) {
                assertThat(getTimeseriesHistory(device.getId())).isEmpty();
                assertThat(getLatestTelemetry(device.getId())).isNull();
            }
        });

        verify(tsHistoryDeletionTaskProcessor, times(deviceCount)).processAsync(any());
        verify(latestTsDeletionTaskProcessor, times(deviceCount)).processAsync(any());
    }

    @Test
    public void whenAsyncProcessingTimesOut_thenTaskIsReprocessed() throws Exception {
        SettableFuture<Void> hangingFuture = SettableFuture.create();
        doAnswer(invocation -> hangingFuture)
                .doCallRealMethod()
                .when(tsHistoryDeletionTaskProcessor).processAsync(any());

        Device device = createDevice("timeoutTest", "timeoutTest");

        timeseriesService.save(tenantId, device.getId(),
                new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(TELEMETRY_KEY, KV_VALUE))).get();

        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getTimeseriesHistory(device.getId())).isEmpty();
        });

        verify(housekeeperReprocessingService).submitForReprocessing(any(), any());
    }

    @Test
    public void whenAsyncProcessingKeepsFailing_thenAttemptsExhaustedAndNotificationTriggered() throws Exception {
        RuntimeException error = new RuntimeException("Persistent failure");
        // The first attempt runs on the async path; reprocessed attempts fall back to the sync path by design.
        // Fail both so the task is driven past max-reprocessing-attempts and reaches the terminal notification
        // branch of the shared handleFailure, which is now reachable starting from an async task.
        doReturn(Futures.immediateFailedFuture(error)).when(tsHistoryDeletionTaskProcessor).processAsync(any());
        doThrow(error).when(tsHistoryDeletionTaskProcessor).process(any());

        Device device = createDevice("asyncMaxAttemptsTest", "asyncMaxAttemptsTest");

        timeseriesService.save(tenantId, device.getId(),
                new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(TELEMETRY_KEY, KV_VALUE))).get();

        doDelete("/api/device/" + device.getId()).andExpect(status().isOk());

        // process(...) is shared across all notification triggers, so match the task-processing-failure type only.
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificationRuleProcessor, atLeastOnce()).process(any(TaskProcessingFailureTrigger.class)));

        ArgumentCaptor<NotificationRuleTrigger> triggerCaptor = ArgumentCaptor.forClass(NotificationRuleTrigger.class);
        verify(notificationRuleProcessor, atLeastOnce()).process(triggerCaptor.capture());
        TaskProcessingFailureTrigger trigger = triggerCaptor.getAllValues().stream()
                .filter(t -> t instanceof TaskProcessingFailureTrigger)
                .map(t -> (TaskProcessingFailureTrigger) t)
                .filter(t -> t.getTask().getEntityId().equals(device.getId())
                        && t.getTask().getTaskType() == HousekeeperTaskType.DELETE_TS_HISTORY)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No TaskProcessingFailureTrigger captured for the deleted device"));
        // max-reprocessing-attempts=5: notification fires once the attempt counter reaches the configured maximum.
        assertThat(trigger.getAttempt()).isEqualTo(5);

        // The async path was exercised first, before reprocessing fell back to the sync path.
        verify(tsHistoryDeletionTaskProcessor, atLeastOnce()).processAsync(any());
        verify(tsHistoryDeletionTaskProcessor, atLeastOnce()).process(any());
    }

    private TsKvEntry getLatestTelemetry(org.thingsboard.server.common.data.id.EntityId entityId) throws Exception {
        return timeseriesService.findLatest(tenantId, entityId, TELEMETRY_KEY).get().orElse(null);
    }

    private List<TsKvEntry> getTimeseriesHistory(org.thingsboard.server.common.data.id.EntityId entityId) throws Exception {
        return timeseriesService.findAll(tenantId, entityId, List.of(new BaseReadTsKvQuery(TELEMETRY_KEY, 0, System.currentTimeMillis(), 10, "DESC"))).get();
    }

}
