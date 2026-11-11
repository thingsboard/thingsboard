// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity.ASSET;
import static org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity.DEVICE;

public class LicenseCapacityStartupGateTest {

    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final DeviceService deviceService = mock(DeviceService.class);
    private final AssetService assetService = mock(AssetService.class);

    private LicenseCapacityStartupGate gate() {
        return new LicenseCapacityStartupGate(subscriptionService, deviceService, assetService);
    }

    @Test
    public void aGrantInstanceOverItsDeviceCapRefusesToBoot() {
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(1000L);
        when(deviceService.countDevices()).thenReturn(1200L);

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(() -> gate().check())
                // Pinned against the shared source itself, so this gate and the upgrade pre-flight cannot
                // drift apart and a swapped argument order shows up here. The wording itself is pinned by
                // theCapMessageNamesBothNumbersAndEveryWayOut below.
                .withMessage(LicenseCapacity.capExceededMessage(DEVICE, 1200L, 1000L));
    }

    @Test
    public void aGrantInstanceOverItsAssetCapRefusesToBoot() {
        // The asset quota equals the device quota on these plans, so it is enforced at boot just as strictly.
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(1000L);
        when(deviceService.countDevices()).thenReturn(10L);
        when(subscriptionService.getLicensedAssetLimit()).thenReturn(1000L);
        when(assetService.countAssets()).thenReturn(1200L);

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(() -> gate().check())
                .withMessage(LicenseCapacity.capExceededMessage(ASSET, 1200L, 1000L));
    }

    @Test
    public void theCapMessageNamesBothNumbersAndEveryWayOut() {
        // The assertions above compare the gate's message with the output of the method that produced it, so
        // they would stay green against an empty string. This is where the wording is pinned: an operator who
        // did not configure the licence has to be able to act on it without any further context.
        assertThat(LicenseCapacity.capExceededMessage(DEVICE, 1200L, 1000L))
                // Each number in its own slot, so a swapped argument order lands here too.
                .contains("Device count exceeds")
                .contains("1200 devices are present")
                .contains("the license allows 1000")
                .contains("Reduce the number of devices to 1000 or fewer")
                .contains("buy an add-on")
                .contains("use a different license key")
                // Spelled out rather than taken from the constant: this is the address the operator has to be
                // able to type, and a rename of the constant's value must not pass silently.
                .contains("license.thingsboard.io")
                // The activation path shares this sentence and runs on every plan and edition.
                .doesNotContain("Community Grant")
                .doesNotContain("Professional Edition");

        assertThat(LicenseCapacity.capExceededMessage(ASSET, 1200L, 1000L))
                .contains("Asset count exceeds")
                .contains("1200 assets are present")
                .contains("the license allows 1000")
                .contains("Reduce the number of assets to 1000 or fewer")
                .contains("buy an add-on")
                .contains("use a different license key")
                .contains("license.thingsboard.io");
    }

    @Test
    public void aGrantInstanceExactlyAtItsCapsBoots() {
        // The creation guard blocks the (limit + 1)-th entity with actual >= limit, so holding exactly
        // `limit` of them is a legal, fully compliant state and must not be refused at boot.
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(1000L);
        when(deviceService.countDevices()).thenReturn(1000L);
        when(subscriptionService.getLicensedAssetLimit()).thenReturn(1000L);
        when(assetService.countAssets()).thenReturn(1000L);

        assertThatCode(() -> gate().check()).doesNotThrowAnyException();
    }

    @Test
    public void aNonGrantLicenseOverItsCapsStillBoots() {
        // The regression guard for the whole design: entity counts have never been enforced at startup, so an
        // instance over-provisioned on 4.3 must still start on 4.4. Nothing is even counted.
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(false);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(1000L);
        when(subscriptionService.getLicensedAssetLimit()).thenReturn(1000L);

        assertThatCode(() -> gate().check()).doesNotThrowAnyException();
        verify(deviceService, never()).countDevices();
        verify(assetService, never()).countAssets();
    }

    @Test
    public void anUnlimitedDeviceQuotaIsNeverGated() {
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(0L);

        assertThatCode(() -> gate().check()).doesNotThrowAnyException();
        verify(deviceService, never()).countDevices();
    }

    @Test
    public void anUnlimitedAssetQuotaIsNeverGated() {
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedAssetLimit()).thenReturn(0L);

        assertThatCode(() -> gate().check()).doesNotThrowAnyException();
        verify(assetService, never()).countAssets();
    }

    @Test
    public void aNegativeLimitIsNeverGated() {
        // The guard is limit <= 0, not == 0. If it narrowed to == 0 the count would be read - an unstubbed
        // mock answers 0L - and 0 > -1 would refuse the boot, so both assertions below catch that narrowing.
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(-1L);
        when(subscriptionService.getLicensedAssetLimit()).thenReturn(-1L);

        assertThatCode(() -> gate().check()).doesNotThrowAnyException();
        verify(deviceService, never()).countDevices();
        verify(assetService, never()).countAssets();
    }

    @Test
    public void anInstanceWithNoLicenseIsNotGated() {
        // isCommunityGrantLicense is false with no client, so the instance settles into the ordinary
        // unactivated state instead of failing to boot at all.
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(false);

        assertThatCode(() -> gate().check()).doesNotThrowAnyException();
        verify(deviceService, never()).countDevices();
        verify(assetService, never()).countAssets();
        verify(subscriptionService, never()).getLicensedDeviceLimit();
        verify(subscriptionService, never()).getLicensedAssetLimit();
    }

    @Test
    public void aCountThatCannotBeReadDoesNotRefuseTheBoot() {
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(1000L);
        when(deviceService.countDevices()).thenReturn(null);
        when(subscriptionService.getLicensedAssetLimit()).thenReturn(1000L);
        when(assetService.countAssets()).thenReturn(null);

        assertThatCode(() -> gate().check()).doesNotThrowAnyException();
    }

    @Test
    public void aCountQueryThatThrowsDoesNotRefuseTheBoot() {
        // The try/catch around each count is the fail-open path a future "cleanup" is most likely to strip:
        // without it a transient database hiccup would refuse to start an instance nobody has a verdict on.
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(1000L);
        when(deviceService.countDevices()).thenThrow(new RuntimeException("database unavailable"));
        when(subscriptionService.getLicensedAssetLimit()).thenReturn(1000L);
        when(assetService.countAssets()).thenThrow(new RuntimeException("database unavailable"));

        assertThatCode(() -> gate().check()).doesNotThrowAnyException();
    }

    @Test
    public void anUnreadableDeviceCountStillLeavesTheAssetVerdictIntact() {
        // Fail-open is per entity kind: one count that cannot be read must not silently disable the other.
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(1000L);
        when(deviceService.countDevices()).thenThrow(new RuntimeException("database unavailable"));
        when(subscriptionService.getLicensedAssetLimit()).thenReturn(1000L);
        when(assetService.countAssets()).thenReturn(1200L);

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(() -> gate().check())
                .withMessage(LicenseCapacity.capExceededMessage(ASSET, 1200L, 1000L));
    }

    @Test
    public void theDeviceViolationIsReportedFirst() {
        when(subscriptionService.isCommunityGrantLicense()).thenReturn(true);
        when(subscriptionService.getLicensedDeviceLimit()).thenReturn(1000L);
        when(deviceService.countDevices()).thenReturn(1200L);
        when(subscriptionService.getLicensedAssetLimit()).thenReturn(1000L);
        when(assetService.countAssets()).thenReturn(1500L);

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(() -> gate().check())
                .withMessage(LicenseCapacity.capExceededMessage(DEVICE, 1200L, 1000L));
    }

}
