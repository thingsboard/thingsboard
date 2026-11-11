// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.util.LogCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the periodic device-count warning that reminds an operator running development mode at production scale
 * that it is for non-production use only. The warning sits above the isUnlimited short-circuit in
 * {@code createDeviceAllowed} - a development licence has an unlimited device quota, so a warning placed after
 * that line could never fire - and it must stay off the production licence path entirely, never turn every
 * device creation into an extra device count query, and let exactly one thread through when a burst of
 * concurrent saves races the same unclaimed check window.
 */
public class NonProductionScaleWarningTest {

    private BasicSubscriptionService subscriptionService;
    private AbstractTbLicenseClient tbLicenseClient;
    private DeviceService deviceService;
    private LogCapture logCapture;

    @BeforeEach
    void setUp() {
        subscriptionService = new BasicSubscriptionService();
        tbLicenseClient = mock(AbstractTbLicenseClient.class);
        deviceService = mock(DeviceService.class);
        AssetService assetService = mock(AssetService.class);
        EdgeService edgeService = mock(EdgeService.class);
        LicenseActivationStub.wire(subscriptionService, tbLicenseClient, 2);
        ReflectionTestUtils.setField(subscriptionService, "deviceService", deviceService);
        ReflectionTestUtils.setField(subscriptionService, "assetService", assetService);
        ReflectionTestUtils.setField(subscriptionService, "edgeService", edgeService);
        logCapture = new LogCapture(BasicSubscriptionService.class, Level.WARN);
    }

    @AfterEach
    void tearDown() {
        logCapture.detach();
    }

    private void whenDevelopmentModeWithDeviceCount(long count) {
        when(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.DEVELOPMENT_KEY)).thenReturn(true);
        when(tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY)).thenReturn(-1L);
        when(deviceService.countDevices()).thenReturn(count);
    }

    private void whenProductionModeWithDeviceLimit(long limit) {
        when(tbLicenseClient.getPlanBooleanValue(PlanDataConstants.DEVELOPMENT_KEY)).thenReturn(false);
        when(tbLicenseClient.getPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY)).thenReturn(limit);
        when(deviceService.countDevices()).thenReturn(1L);
    }

    @Test
    public void aWarningIsLoggedPastTheThresholdEvenThoughTheQuotaIsUnlimited() {
        // The whole point: a development licence has maxdevices = -1, so createDeviceAllowed returns at the
        // isUnlimited short-circuit. A warning placed after it can never fire.
        long deviceCount = BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD + 1;
        whenDevelopmentModeWithDeviceCount(deviceCount);

        assertThatCode(() -> subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID))
                .doesNotThrowAnyException();
        // Pins the exact wording and the device-count interpolation, not just a substring.
        String expectedMessage = deviceCount + " devices provisioned. " + DataConstants.NON_PRODUCTION_NOTICE +
                ". This instance has unlimited device, asset and edge quotas, so this is not a limit warning. " +
                "If the deployment is production, obtain a free production key at https://license.thingsboard.io";
        assertThat(logCapture.getMessages()).contains(expectedMessage);
    }

    @Test
    public void theWarningDoesNotReadAsACapThisInstanceIsOver() {
        // The defect this wording replaced: a live device count on its own reads as a limit the instance has
        // already exceeded. It has to state the instance's own quotas as unlimited and say so in as many
        // words, and it has to point at the licence portal for what a free production key actually covers
        // rather than restate plan terms that change far more often than this class does.
        long deviceCount = BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD + 1;
        whenDevelopmentModeWithDeviceCount(deviceCount);

        subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);

        assertThat(logCapture.getMessages())
                .filteredOn(message -> message.contains("non-production use only"))
                .singleElement(as(STRING))
                .contains("unlimited device, asset and edge quotas")
                .contains("not a limit warning")
                .contains("obtain a free production key at https://license.thingsboard.io");
    }

    @Test
    public void noWarningBelowTheThreshold() {
        whenDevelopmentModeWithDeviceCount(BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD - 1);

        subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);

        assertThat(logCapture.getMessages()).noneMatch(message -> message.contains("non-production use only"));
    }

    @Test
    public void aBelowThresholdCallStillConsumesTheWindow() {
        // The window is spent on the attempt, not on the outcome. If it were only spent on a logged warning, a
        // below-threshold check followed immediately by an over-threshold one would log on the second call;
        // it must not, because the first check already claimed the window.
        whenDevelopmentModeWithDeviceCount(BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD - 1);
        subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);

        when(deviceService.countDevices()).thenReturn(BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD + 1);
        subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);

        assertThat(logCapture.getMessages()).noneMatch(message -> message.contains("non-production use only"));
        verify(deviceService, times(1)).countDevices();
    }

    @Test
    public void aSecondWarningFiresOnceTheWindowExpires() {
        whenDevelopmentModeWithDeviceCount(BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD + 1);
        subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);

        // Simulate the window elapsing rather than sleeping the test: push the recorded check back past the
        // interval.
        AtomicLong lastScaleWarnTs = (AtomicLong) ReflectionTestUtils.getField(subscriptionService, "lastScaleWarnTs");
        lastScaleWarnTs.set(System.currentTimeMillis() - BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_INTERVAL_MS - 1);

        subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);

        assertThat(logCapture.getMessages())
                .filteredOn(message -> message.contains("non-production use only"))
                .hasSize(2);
        verify(deviceService, times(2)).countDevices();
    }

    @Test
    public void aWindowRecordedInTheFutureIsTreatedAsExpiredRatherThanAsOpen() {
        // The separate `now >= seenAt` conjunct of the throttle guard, which the expired-window test above
        // cannot reach: it falsifies the elapsed-interval conjunct instead. A clock that steps backward -
        // an NTP correction, a virtual machine resumed from a snapshot - leaves a window recorded in the
        // future, and now - seenAt is then negative, which is below the interval. Without this conjunct the
        // guard would read that as a window still open and suppress the warning until wall-clock time caught
        // up with the stale value, which can be arbitrarily long.
        whenDevelopmentModeWithDeviceCount(BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD + 1);
        subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);

        AtomicLong lastScaleWarnTs = (AtomicLong) ReflectionTestUtils.getField(subscriptionService, "lastScaleWarnTs");
        lastScaleWarnTs.set(System.currentTimeMillis() + BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_INTERVAL_MS);

        subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);

        assertThat(logCapture.getMessages())
                .filteredOn(message -> message.contains("non-production use only"))
                .hasSize(2);
        verify(deviceService, times(2)).countDevices();
    }

    @Test
    public void aDeviceCountThatCannotBeReadNeverFailsADeviceSave() {
        // The warning is a diagnostic sitting on the device-creation path: a database hiccup while evaluating
        // it must not turn into a failed device save. Removing the try/catch around the warning body fails
        // here and nowhere else.
        whenDevelopmentModeWithDeviceCount(BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD + 1);
        when(deviceService.countDevices()).thenThrow(new DataAccessResourceFailureException("the database is unreachable"));

        assertThatCode(() -> subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    public void aProductionInstanceNeverCountsDevicesForTheWarning() {
        // A licensed instance must reach its normal device-count check exactly once per save; the warning must
        // not add a second one just because it also needs a device count on the development path.
        whenProductionModeWithDeviceLimit(1000L);

        subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);

        verify(deviceService, times(1)).countDevices();
    }

    @Test
    public void repeatedSavesDoNotRecountOnEveryDevice() {
        // Development mode issues no queries per save today. The warning must not turn every device creation
        // into a SELECT count(*).
        whenDevelopmentModeWithDeviceCount(BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD + 1);

        for (int i = 0; i < 50; i++) {
            subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);
        }

        verify(deviceService, times(1)).countDevices();
    }

    @Test
    public void concurrentSavesRacingTheSameWindowStillCountAndLogOnlyOnce() throws InterruptedException {
        // The scenario the throttle exists for and a single-threaded test cannot catch: many threads reading
        // the same unclaimed window before any of them writes it back, e.g. a bulk device import at process
        // start with lastScaleWarnTs still at its initial 0. Only one of them may reach the count and the log
        // line; the rest must return without touching either.
        whenDevelopmentModeWithDeviceCount(BasicSubscriptionService.NON_PRODUCTION_SCALE_WARN_THRESHOLD + 1);
        int threadCount = 50;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                subscriptionService.createDeviceAllowed(TenantId.SYS_TENANT_ID);
            });
            threads.add(thread);
            thread.start();
        }
        ready.await();
        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        verify(deviceService, times(1)).countDevices();
        assertThat(logCapture.getMessages())
                .filteredOn(message -> message.contains("non-production use only"))
                .hasSize(1);
    }

}
