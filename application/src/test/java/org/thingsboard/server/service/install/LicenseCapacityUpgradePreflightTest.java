// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.shared.PlanData;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.PlanItem;
import org.thingsboard.license.shared.SubscriptionPreviewResponse;
import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;
import org.thingsboard.server.dao.subscription.CommunityGrantPlan;
import org.thingsboard.server.dao.subscription.EntityCapExceededException;
import org.thingsboard.server.dao.subscription.LicenseCapacity;
import org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity;
import org.thingsboard.server.dao.util.LogCapture;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LicenseCapacityUpgradePreflightTest {

    private static final UUID CLUSTER_ID = UUID.fromString("cbb0ea36-3d5f-4b0e-9f5a-4a3d3b8f5f21");

    private static final String SUPPLIED_SECRET = "a-supplied-license-secret";

    private static final String STORED_SECRET = "a-stored-license-secret";

    private static final String INSTANCE_DATA_FILE = "instance-license.data";

    private static final String LICENSE_SERVER_PROPERTY = "tb.license.server";

    // Refused on connect rather than timed out, so the test spends no part of the client's 5s connect budget.
    private static final String UNREACHABLE_PORTAL = "http://127.0.0.1:1";

    @Test
    public void anUpgradeOfAGrantInstanceOverItsDeviceCapIsAborted() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1200L);

        AbstractTbLicenseClient client = grantClient(true, 1000L);
        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret", client);

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(preflight::check)
                .withMessageContaining("1200")
                .withMessageContaining("1000");
        // The refusal path must still shut the client's background scheduler down - that is why the
        // production code stops it in a finally rather than at the end of the happy path.
        verify(client).stop();
    }

    @Test
    public void anUpgradeOfAGrantInstanceUnderItsDeviceCapProceeds() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1000L);

        AbstractTbLicenseClient client = grantClient(true, 1000L);
        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret", client);

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(client).stop();
    }

    @Test
    public void anUpgradeOfAGrantInstanceOverItsAssetCapIsAborted() throws Exception {
        // The asset quota equals the device quota on these plans, so it is enforced just as strictly.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(contains("device"), eq(Long.class))).thenReturn(10L);
        when(jdbcTemplate.queryForObject(contains("asset"), eq(Long.class))).thenReturn(1200L);

        AbstractTbLicenseClient client = grantClient(true, 1000L, 1000L);
        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret", client);

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(preflight::check)
                .withMessage(LicenseCapacity.capExceededMessage(CappedEntity.ASSET, 1200L, 1000L));
        verify(client).stop();
    }

    @Test
    public void anUpgradeOfAGrantInstanceExactlyAtItsAssetCapProceeds() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1000L);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret",
                grantClient(true, 1000L, 1000L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
    }

    @Test
    public void anUnlimitedAssetQuotaIsNotACap() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(contains("device"), eq(Long.class))).thenReturn(10L);

        // A negative value is the unlimited sentinel, and a zero is what an absent key reads as; the assets
        // are not counted at all in either case.
        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret",
                grantClient(true, 1000L, -1L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(jdbcTemplate, never()).queryForObject(contains("asset"), eq(Long.class));
    }

    @Test
    public void anAssetCountThatCannotBeReadIsNotACap() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(contains("device"), eq(Long.class))).thenReturn(10L);
        when(jdbcTemplate.queryForObject(contains("asset"), eq(Long.class))).thenReturn(null);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret",
                grantClient(true, 1000L, 1L));

        LogCapture logCapture = new LogCapture(LicenseCapacityUpgradePreflight.class, Level.INFO);
        try {
            assertThatCode(preflight::check).doesNotThrowAnyException();
            // Not throwing is not enough to pin the null branch: without it the comparison unboxes null and
            // the broad catch swallows the NPE, which also does not throw. The broad catch announces itself,
            // so a silent run is what proves the null was handled deliberately.
            assertThat(logCapture.getMessages())
                    .noneMatch(message -> message.startsWith("Skipping the pre-upgrade license capacity check:"));
        } finally {
            logCapture.detach();
        }
    }

    @Test
    public void anUpgradeOfANonGrantInstanceIsNeverAborted() throws Exception {
        // The same regression guard the startup gate carries: a non-grant licence over its quota must upgrade.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret",
                grantClient(false, 1000L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    public void anUpgradeRunWithoutTheLicenseSecretIsNotBlocked() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        // Answering the cluster identity is what makes this bite on the secret guard: without the stub, the
        // check would stop at the missing cluster id and the test would pass with the secret guard deleted.
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "", null);

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(preflight, never()).buildLicenseClient(anyString(), any(UUID.class));
    }

    @Test
    public void aLicenseThatCannotBeResolvedDoesNotBlockTheUpgrade() throws Exception {
        // A secret neither path can read a plan for - a malformed key, a licence issued for another cluster,
        // a portal answering without plan data - is not a verdict about the device count, and the startup
        // gate will re-check anyway.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);

        LicenseCapacityUpgradePreflight preflight = preflightReturningNull(jdbcTemplate, "a-license-secret");

        assertThatCode(preflight::check).doesNotThrowAnyException();
    }

    @Test
    public void aLicenseClientThatFailsToBuildDoesNotBlockTheUpgrade() throws Exception {
        // Anything unexpected while the licence is being read surfaces as an exception out of
        // buildLicenseClient. It must be swallowed - but the broad catch that swallows it sits *after* the
        // EntityCapExceededException catch, which the over-cap test above pins.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);

        LicenseCapacityUpgradePreflight preflight = preflightThrowing(jdbcTemplate, "a-license-secret",
                new IllegalStateException("license server is unreachable"));

        assertThatCode(preflight::check).doesNotThrowAnyException();
    }

    @Test
    public void anInstanceWithoutAClusterIdentityIsNotBlocked() throws Exception {
        // On the CE-to-PE path the cluster identity may not be readable yet; that is not a verdict either.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(null);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret",
                grantClient(true, 1L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(preflight, never()).buildLicenseClient(anyString(), any(UUID.class));
    }

    @Test
    public void anUnlimitedDeviceQuotaIsNotACap() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);

        // A negative value is the unlimited sentinel, and a zero is what an absent key reads as; neither is
        // a cap the upgrade can be refused on.
        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret",
                grantClient(true, -1L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    public void aDeviceCountThatCannotBeReadIsNotACap() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret",
                grantClient(true, 1L));

        LogCapture logCapture = new LogCapture(LicenseCapacityUpgradePreflight.class, Level.INFO);
        try {
            assertThatCode(preflight::check).doesNotThrowAnyException();
            // Not throwing is not enough to pin the null branch: without it the comparison unboxes null and
            // the broad catch swallows the NPE, which also does not throw. What separates the two is that the
            // broad catch announces itself, so a silent run is what proves the null was handled deliberately.
            assertThat(logCapture.getMessages())
                    .noneMatch(message -> message.startsWith("Skipping the pre-upgrade license capacity check:"));
        } finally {
            logCapture.detach();
        }
    }

    @Test
    public void anOverCapEdgeCountNeverBlocksTheUpgrade() throws Exception {
        // Edges stay a soft, creation-time cap for every licence. The pre-flight must not grow an edge
        // branch: it counts devices and nothing else.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);

        // The edge quota of 1 is what makes this an over-cap edge fixture, and it must stay even though
        // production never reads it: without it an absent key reads as 0, which an added edge branch shaped
        // like the device branch would read as "no cap" and return on - so the verify below would pass
        // against exactly the change this test exists to forbid.
        PlanData planData = planData(true, 1000L, 0L);
        planData.put(PlanDataConstants.MAX_EDGES_KEY, new PlanItem(1L));

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret",
                clientWith(planData));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(jdbcTemplate, never()).queryForObject(contains("edge"), eq(Long.class));
    }

    @Test
    public void aSecretThatIsNotAnOfflineLicenseResolvesToNoClientRatherThanAnException() throws Exception {
        // The one part of the class every other test stubs out. Its contract is that anything which is not an
        // offline licence - an online subscription secret, a malformed key, a licence for another cluster -
        // answers null, rather than leaving the broad catch in check() to cover for it.
        LicenseCapacityUpgradePreflight preflight = new LicenseCapacityUpgradePreflight(mock(JdbcTemplate.class));

        assertThat(preflight.buildLicenseClient("not-a-license", CLUSTER_ID)).isNull();
    }

    @Test
    public void aSecretThatIsNotAnOfflineLicenseDoesNotBlockTheUpgrade() throws Exception {
        // The same path as above, but through the real buildLicenseClient rather than a stub, so a licence
        // failure escaping it as an exception would show up here as a refused upgrade. Only the portal call
        // it falls through to is stubbed, with the rejection such a secret earns there.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);

        LicenseCapacityUpgradePreflight preflight = spy(new LicenseCapacityUpgradePreflight(jdbcTemplate));
        ReflectionTestUtils.setField(preflight, "licenseSecret", "not-a-license");
        doThrow(new LicenseException("Invalid license secret!", LicenseErrorCode.INVALID_LICENSE_SECRET))
                .when(preflight).previewPlanData(anyString(), any(UUID.class));

        assertThatCode(preflight::check).doesNotThrowAnyException();
    }

    @Test
    public void anOnlineSecretIsCheckedThroughThePortalPreview() throws Exception {
        // The gap this closes: an online community-grant deployment used to skip the check altogether and
        // meet its over-cap after the upgrade, at the startup gate.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1200L);

        LicenseCapacityUpgradePreflight preflight = preflightPreviewing(jdbcTemplate, planData(true, 1000L, 0L));

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(preflight::check)
                .withMessage(LicenseCapacity.capExceededMessage(CappedEntity.DEVICE, 1200L, 1000L));
        verify(preflight).previewPlanData(anyString(), any(UUID.class));
    }

    @Test
    public void anOnlineSecretUnderItsDeviceCapProceeds() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);

        LicenseCapacityUpgradePreflight preflight = preflightPreviewing(jdbcTemplate, planData(true, 1000L, 1000L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(preflight).previewPlanData(anyString(), any(UUID.class));
    }

    @Test
    public void anOfflineLicenseIsNeverPreviewedAtThePortal() throws Exception {
        // The portal is the fallback, not the source: an offline licence carries its own plan, and asking
        // about it would add a network round trip to every offline upgrade for nothing.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "a-license-secret",
                grantClient(true, 1000L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(preflight, never()).previewPlanData(anyString(), any(UUID.class));
    }

    @Test
    public void aPortalThatCannotBeReachedDoesNotBlockTheUpgrade() throws Exception {
        // The whole point of reading the licence online: a deployment that upgrades without internet access,
        // or while the portal is down, still upgrades. Only a positively established over-cap refuses.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);

        LicenseCapacityUpgradePreflight preflight = preflightWithAFailingPreview(jdbcTemplate,
                new LicenseException("Unable to connect to the license server!", LicenseErrorCode.CONNECTION_ERROR));

        LogCapture logCapture = new LogCapture(LicenseCapacityUpgradePreflight.class, Level.INFO);
        try {
            assertThatCode(preflight::check).doesNotThrowAnyException();
            assertThat(logCapture.getMessages())
                    .anyMatch(message -> message.startsWith("Skipping the pre-upgrade license capacity check:"));
        } finally {
            logCapture.detach();
        }
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    public void aPortalCallFailingOutsideTheLicenseContractDoesNotBlockTheUpgrade() throws Exception {
        // A LicenseException is the answer the client promises; anything else - a broken proxy, a parse
        // failure, an unexpected null - reaches the broad catch, and must be just as harmless.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);

        LicenseCapacityUpgradePreflight preflight = preflightWithAFailingPreview(jdbcTemplate,
                new IllegalStateException("the portal answered something else entirely"));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    public void anOverCapRefusalSurvivesAFailureToReadTheExactCount() throws Exception {
        // The bounded count already decided the refusal; the exact count only fills in the sentence. A
        // database that stops answering between the two must not turn a refusal into a skipped check.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(contains("bounded"), eq(Long.class))).thenReturn(1001L);
        when(jdbcTemplate.queryForObject(eq("SELECT count(*) FROM device"), eq(Long.class)))
                .thenThrow(new DataAccessResourceFailureException("the connection was reset"));

        LicenseCapacityUpgradePreflight preflight = preflightPreviewing(jdbcTemplate, planData(true, 1000L, 0L));

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(preflight::check)
                .withMessage(LicenseCapacity.capExceededMessage(CappedEntity.DEVICE, 1000L));
    }

    @Test
    public void theCheckLeavesTheInstanceDataFileAlone() throws Exception {
        // This is why the online plan is read through the describe-only preview and not through an online
        // client: that client deletes the instance data file as soon as the secret differs from the one
        // recorded in it, before it contacts the portal at all. Nothing here is stubbed except the portal's
        // address, so the real buildLicenseClient and the real previewPlanData both run against the planted
        // file - the preview against an address that refuses the connection at once, which is also what
        // proves an unreachable portal lets the upgrade through.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);

        LicenseCapacityUpgradePreflight preflight = new LicenseCapacityUpgradePreflight(jdbcTemplate);
        ReflectionTestUtils.setField(preflight, "licenseSecret", "an-online-secret");

        Path instanceDataFile = Path.of(INSTANCE_DATA_FILE);
        // Never plant over a real instance data file: a test for "this class must not endanger that file"
        // must not be the thing that endangers it. A workspace where a node has been run from here simply
        // skips this one.
        assumeTrue(Files.notExists(instanceDataFile), "an instance data file is already present here");
        String previousPortal = System.getProperty(LICENSE_SERVER_PROPERTY);
        System.setProperty(LICENSE_SERVER_PROPERTY, UNREACHABLE_PORTAL);
        try {
            byte[] plantedContent = "planted instance data".getBytes(StandardCharsets.UTF_8);
            Files.write(instanceDataFile, plantedContent);

            LogCapture logCapture = new LogCapture(LicenseCapacityUpgradePreflight.class, Level.INFO);
            try {
                assertThatCode(preflight::check).doesNotThrowAnyException();
                // The portal really was called and really failed: every earlier exit - no secret, no cluster
                // id, no plan - announces itself with a different sentence, and only this one proves the file
                // survived a run that went all the way through previewPlanData.
                assertThat(logCapture.getMessages())
                        .anyMatch(message -> message.startsWith("Skipping the pre-upgrade license capacity check:"));
            } finally {
                logCapture.detach();
            }

            assertThat(instanceDataFile).exists();
            assertThat(Files.readAllBytes(instanceDataFile)).isEqualTo(plantedContent);
        } finally {
            if (previousPortal != null) {
                System.setProperty(LICENSE_SERVER_PROPERTY, previousPortal);
            } else {
                System.clearProperty(LICENSE_SERVER_PROPERTY);
            }
            Files.deleteIfExists(instanceDataFile);
        }
    }

    @Test
    public void aPreviewCallThatBlocksPastTheBudgetLeavesTheCheckSkipped() throws Exception {
        // The client's own timeouts are inactivity timeouts and would never fire here; only the wall-clock
        // budget around the call can end this.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        // Set so that reaching verifyCap would refuse the upgrade - proving what let it through was the
        // skip, not a coincidental under-cap count.
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1200L);

        LicenseCapacityUpgradePreflight preflight = preflightPreviewingThroughThePortal(jdbcTemplate);
        ReflectionTestUtils.setField(preflight, "previewBudgetMillis", 50L);
        doAnswer(invocation -> blockThenAnswer(2000L, planData(true, 1000L, 0L)))
                .when(preflight).requestSubscriptionPreview(anyString(), any(UUID.class));

        LogCapture logCapture = new LogCapture(LicenseCapacityUpgradePreflight.class, Level.INFO);
        try {
            assertThatCode(preflight::check).doesNotThrowAnyException();
            assertThat(logCapture.getMessages())
                    .anyMatch(message -> message.startsWith("Skipping the pre-upgrade license capacity check:"));
        } finally {
            logCapture.detach();
        }
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    public void theBlockedPreviewWorkerDoesNotLeak() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);

        LicenseCapacityUpgradePreflight preflight = preflightPreviewingThroughThePortal(jdbcTemplate);
        ReflectionTestUtils.setField(preflight, "previewBudgetMillis", 50L);
        long blockMillis = 2000L;
        doAnswer(invocation -> blockThenAnswer(blockMillis, planData(true, 1000L, 0L)))
                .when(preflight).requestSubscriptionPreview(anyString(), any(UUID.class));

        long startNanos = System.nanoTime();
        assertThatCode(preflight::check).doesNotThrowAnyException();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        // Nowhere near the full block: the budget, not the blocked worker, decided when this returned.
        assertThat(elapsedMillis).isLessThan(blockMillis / 2);

        // The worker outlives the call - a leak unless it is a daemon thread that cannot keep the JVM alive.
        Thread previewThread = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().equals("license-capacity-preflight-preview"))
                .findFirst()
                .orElse(null);
        assertThat(previewThread).isNotNull();
        assertThat(previewThread.isDaemon()).isTrue();
    }

    @Test
    public void theFastPreviewPathStillReturnsThePlanAndEnforcesTheCap() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1200L);

        LicenseCapacityUpgradePreflight preflight = preflightPreviewingThroughThePortal(jdbcTemplate);
        doReturn(new SubscriptionPreviewResponse(planData(true, 1000L, 0L), null))
                .when(preflight).requestSubscriptionPreview(anyString(), any(UUID.class));

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(preflight::check)
                .withMessage(LicenseCapacity.capExceededMessage(CappedEntity.DEVICE, 1200L, 1000L));
    }

    @Test
    public void theSuppliedKeyIsTheOneTheCheckRunsOn() throws Exception {
        // The inverse of the runtime rule, and only here: at upgrade the key the operator supplies is the one
        // the instance is about to run on, so it is the one measured against the counts.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn(STORED_SECRET);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, SUPPLIED_SECRET, grantClient(true, 1000L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(preflight).buildLicenseClient(eq(SUPPLIED_SECRET), any(UUID.class));
    }

    @Test
    public void theStoredKeyIsTheOneTheCheckRunsOnWhenNoneIsSupplied() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn(STORED_SECRET);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "", grantClient(true, 1000L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(preflight).buildLicenseClient(eq(STORED_SECRET), any(UUID.class));
        verify(jdbcTemplate, never()).update(anyString(), anyString());
    }

    @Test
    public void theSuppliedKeyReplacesTheStoredOneOnceTheCheckPasses() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn(STORED_SECRET);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, SUPPLIED_SECRET, grantClient(true, 1000L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(jdbcTemplate).update(contains("license_secret"), eq(SUPPLIED_SECRET));
    }

    @Test
    public void aCheckThatCouldNotBeMadeStillReplacesTheStoredKey() throws Exception {
        // Only a positively established over-cap withholds the replacement; a plan that is not a grant - like
        // every other skip - leaves the upgrade free to proceed, and on the supplied key.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn(STORED_SECRET);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, SUPPLIED_SECRET, grantClient(false, 1000L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(jdbcTemplate).update(contains("license_secret"), eq(SUPPLIED_SECRET));
    }

    @Test
    public void aRefusedUpgradeLeavesTheStoredKeyInPlace() throws Exception {
        // So that running the upgrade again without the variable behaves exactly as it did before.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1200L);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn(STORED_SECRET);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, SUPPLIED_SECRET, grantClient(true, 1000L));

        assertThatExceptionOfType(EntityCapExceededException.class).isThrownBy(preflight::check);
        verify(jdbcTemplate, never()).update(anyString(), anyString());
    }

    @Test
    public void aSuppliedKeyThatIsAlreadyTheStoredOneIsNotWrittenAgain() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn(SUPPLIED_SECRET);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, SUPPLIED_SECRET, grantClient(true, 1000L));

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(jdbcTemplate, never()).update(anyString(), anyString());
    }

    @Test
    public void aRefusalNamesTheVariableThatSuppliesADifferentKey() throws Exception {
        // The shared sentence cannot carry this - it is also read in the activation UI - so the operator only
        // learns the other way out if the pre-flight logs it alongside.
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class))).thenReturn(CLUSTER_ID);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1200L);

        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, SUPPLIED_SECRET, grantClient(true, 1000L));

        LogCapture logCapture = new LogCapture(LicenseCapacityUpgradePreflight.class, Level.ERROR);
        try {
            assertThatExceptionOfType(EntityCapExceededException.class).isThrownBy(preflight::check);
            assertThat(logCapture.getMessages()).anyMatch(message -> message.contains("TB_LICENSE_SECRET"));
        } finally {
            logCapture.detach();
        }
    }

    /**
     * Blocks the calling thread for {@code blockMillis}, swallowing any interruption instead of reacting to
     * it - standing in for a socket read that does not respond to {@code cancel(true)} - and only then answers.
     */
    private static SubscriptionPreviewResponse blockThenAnswer(long blockMillis, PlanData planData) {
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(blockMillis);
        while (System.nanoTime() < deadlineNanos) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
                // The point of this loop: a real stalled socket read would not react to this either.
            }
        }
        return new SubscriptionPreviewResponse(planData, null);
    }

    private AbstractTbLicenseClient grantClient(boolean grant, long maxDevices) {
        return grantClient(grant, maxDevices, 0L);
    }

    private AbstractTbLicenseClient grantClient(boolean grant, long maxDevices, long maxAssets) {
        return clientWith(planData(grant, maxDevices, maxAssets));
    }

    private AbstractTbLicenseClient clientWith(PlanData planData) {
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanData()).thenReturn(planData);
        return client;
    }

    private static PlanData planData(boolean grant, long maxDevices, long maxAssets) {
        PlanData planData = new PlanData();
        planData.put(CommunityGrantPlan.COMMUNITY_GRANT_KEY, new PlanItem(grant));
        planData.put(PlanDataConstants.MAX_DEVICES_KEY, new PlanItem(maxDevices));
        planData.put(PlanDataConstants.MAX_ASSETS_KEY, new PlanItem(maxAssets));
        return planData;
    }

    private LicenseCapacityUpgradePreflight preflightWith(JdbcTemplate jdbcTemplate, String secret,
                                                         AbstractTbLicenseClient client) throws Exception {
        LicenseCapacityUpgradePreflight preflight = spy(new LicenseCapacityUpgradePreflight(jdbcTemplate));
        ReflectionTestUtils.setField(preflight, "licenseSecret", secret);
        doReturn(client).when(preflight).buildLicenseClient(anyString(), any(UUID.class));
        // No test may reach the real portal by accident: every case that wants the online path stubs this
        // again with the answer it expects from the portal.
        doReturn(null).when(preflight).previewPlanData(anyString(), any(UUID.class));
        return preflight;
    }

    /**
     * A preflight that will reach the real {@link LicenseCapacityUpgradePreflight#previewPlanData} and
     * {@link LicenseCapacityUpgradePreflight#requestSubscriptionPreview} - unlike {@link #preflightWith}, which
     * stubs {@code previewPlanData} itself - so the wall-clock budget wrapping the portal call can be
     * exercised. Only the portal round trip, {@code requestSubscriptionPreview}, is left for each test to stub.
     */
    private LicenseCapacityUpgradePreflight preflightPreviewingThroughThePortal(JdbcTemplate jdbcTemplate) throws Exception {
        LicenseCapacityUpgradePreflight preflight = spy(new LicenseCapacityUpgradePreflight(jdbcTemplate));
        ReflectionTestUtils.setField(preflight, "licenseSecret", "an-online-secret");
        doReturn(null).when(preflight).buildLicenseClient(anyString(), any(UUID.class));
        return preflight;
    }

    private LicenseCapacityUpgradePreflight preflightPreviewing(JdbcTemplate jdbcTemplate, PlanData planData)
            throws Exception {
        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "an-online-secret", null);
        doReturn(planData).when(preflight).previewPlanData(anyString(), any(UUID.class));
        return preflight;
    }

    private LicenseCapacityUpgradePreflight preflightWithAFailingPreview(JdbcTemplate jdbcTemplate, Throwable failure)
            throws Exception {
        LicenseCapacityUpgradePreflight preflight = preflightWith(jdbcTemplate, "an-online-secret", null);
        doThrow(failure).when(preflight).previewPlanData(anyString(), any(UUID.class));
        return preflight;
    }

    private LicenseCapacityUpgradePreflight preflightReturningNull(JdbcTemplate jdbcTemplate, String secret) throws Exception {
        return preflightWith(jdbcTemplate, secret, null);
    }

    private LicenseCapacityUpgradePreflight preflightThrowing(JdbcTemplate jdbcTemplate, String secret,
                                                             Throwable failure) throws Exception {
        LicenseCapacityUpgradePreflight preflight = spy(new LicenseCapacityUpgradePreflight(jdbcTemplate));
        ReflectionTestUtils.setField(preflight, "licenseSecret", secret);
        doThrow(failure).when(preflight).buildLicenseClient(anyString(), any(UUID.class));
        return preflight;
    }
}
