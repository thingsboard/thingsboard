// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.PlanItem;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity.ASSET;
import static org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity.DEVICE;

/**
 * The startup gate against real devices, real assets and the real count queries, with only the licence
 * portal replaced by {@link FixedPlanLicenseClient}. {@link LicenseCapacityStartupGateTest} pins the same
 * decisions with mocked counts, which cannot tell a correct count query from a wrong one - the boundary is
 * decided by a number this class makes the database produce.
 * <p>
 * Every cap is expressed relative to the count read back from the database rather than to a fixed number,
 * because the suite shares one database and other classes may leave entities behind.
 */
@DaoSqlTest
public class LicenseCapacityStartupGateSqlTest extends AbstractServiceTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    private final List<DeviceId> devices = new ArrayList<>();
    private final List<AssetId> assets = new ArrayList<>();

    @After
    public void tearDown() {
        devices.forEach(deviceId -> deviceService.deleteDevice(tenantId, deviceId));
        assets.forEach(assetId -> assetService.deleteAsset(tenantId, assetId));
        devices.clear();
        assets.clear();
    }

    @Test
    public void aGrantInstanceUnderBothCapsBoots() {
        givenDevices(3);
        givenAssets(3);

        assertThatCode(() -> gate(true, deviceCount() + 1, assetCount() + 1).check())
                .doesNotThrowAnyException();
    }

    @Test
    public void aGrantInstanceOverItsDeviceCapRefusesToBootAndReportsTheCountedDevices() {
        givenDevices(3);
        long deviceCount = deviceCount();

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(() -> gate(true, deviceCount - 1, assetCount() + 1).check())
                // The count in the message is the one the real query produced, so a query that counted the
                // wrong rows fails here rather than passing against a stubbed number.
                .withMessage(LicenseCapacity.capExceededMessage(DEVICE, deviceCount, deviceCount - 1));
    }

    @Test
    public void aGrantInstanceOverItsAssetCapRefusesToBootAndReportsTheCountedAssets() {
        givenAssets(3);
        long assetCount = assetCount();

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(() -> gate(true, deviceCount() + 1, assetCount - 1).check())
                .withMessage(LicenseCapacity.capExceededMessage(ASSET, assetCount, assetCount - 1));
    }

    @Test
    public void aGrantInstanceExactlyAtBothCapsBoots() {
        givenDevices(3);
        givenAssets(3);
        long deviceCount = deviceCount();
        long assetCount = assetCount();

        // Holding exactly the licensed number is a compliant state: the creation guard blocks the next one.
        assertThat(deviceCount).isPositive();
        assertThat(assetCount).isPositive();
        assertThatCode(() -> gate(true, deviceCount, assetCount).check()).doesNotThrowAnyException();
    }

    @Test
    public void aNonGrantLicenseOverBothCapsStillBoots() {
        givenDevices(3);
        givenAssets(3);

        assertThatCode(() -> gate(false, 1, 1).check()).doesNotThrowAnyException();
    }

    /**
     * The real {@link BasicSubscriptionService} rather than a mocked {@link SubscriptionService}: the plan
     * keys, the unlimited sentinel and the quota normalisation are all its work, and a gate wired to a
     * stubbed limit would not exercise any of it.
     */
    private LicenseCapacityStartupGate gate(boolean communityGrant, long deviceLimit, long assetLimit) {
        BasicSubscriptionService subscriptionService = new BasicSubscriptionService();
        LicenseActivationStub.wire(subscriptionService, new FixedPlanLicenseClient(Map.of(
                CommunityGrantPlan.COMMUNITY_GRANT_KEY, new PlanItem(communityGrant),
                PlanDataConstants.MAX_DEVICES_KEY, new PlanItem(deviceLimit),
                PlanDataConstants.MAX_ASSETS_KEY, new PlanItem(assetLimit))), 2);
        return new LicenseCapacityStartupGate(subscriptionService, deviceService, assetService);
    }

    private long deviceCount() {
        return deviceService.countDevices();
    }

    private long assetCount() {
        return assetService.countAssets();
    }

    private void givenDevices(int count) {
        for (int i = 0; i < count; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName(StringUtils.randomAlphabetic(10));
            device.setType("default");
            devices.add(deviceService.saveDevice(device).getId());
        }
    }

    private void givenAssets(int count) {
        for (int i = 0; i < count; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName(StringUtils.randomAlphabetic(10));
            asset.setType("default");
            assets.add(assetService.saveAsset(asset).getId());
        }
    }

}
