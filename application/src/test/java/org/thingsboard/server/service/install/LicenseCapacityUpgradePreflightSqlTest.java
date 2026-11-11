// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.shared.PlanData;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.license.shared.PlanItem;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.subscription.CommunityGrantPlan;
import org.thingsboard.server.dao.subscription.EntityCapExceededException;
import org.thingsboard.server.dao.subscription.LicenseCapacity;
import org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The pre-flight's five hand-written statements - the two {@code tb_cluster} reads, its write, the bounded
 * count and the exact count - against the real schema and real rows. {@link LicenseCapacityUpgradePreflightTest}
 * pins every branch with a mocked {@code JdbcTemplate}, which answers whatever it is told and would keep
 * passing against SQL no database accepts.
 * <p>
 * Only the licence is stubbed, through the same package-visible {@code buildLicenseClient} seam that unit
 * test uses: reading a plan needs a real offline licence key, which no test can hold. The offline seam is the
 * one used here on purpose - the portal preview the online path falls back to would be a network call.
 */
@DaoSqlTest
public class LicenseCapacityUpgradePreflightSqlTest extends AbstractControllerTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    private final List<DeviceId> devices = new ArrayList<>();
    private final List<AssetId> assets = new ArrayList<>();

    @After
    public void deleteCreatedEntities() {
        devices.forEach(deviceId -> deviceService.deleteDevice(tenantId, deviceId));
        assets.forEach(assetId -> assetService.deleteAsset(tenantId, assetId));
        devices.clear();
        assets.clear();
    }

    @After
    public void clearStoredLicenseSecret() {
        // tb_cluster is the single shared row, and every passing check here writes the supplied key into it.
        jdbcTemplate.update("UPDATE tb_cluster SET license_secret = NULL");
    }

    @Test
    public void anUpgradeOfAGrantInstanceOverItsDeviceCapIsAbortedWithTheCountedDevices() throws Exception {
        givenDevices(3);
        long deviceCount = deviceCount();

        LicenseCapacityUpgradePreflight preflight = preflight(true, deviceCount - 1, assetCount() + 1);

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(preflight::check)
                // Both statements are exercised here: the bounded one decides the refusal, and the exact one
                // produces the number in the message.
                .withMessage(LicenseCapacity.capExceededMessage(CappedEntity.DEVICE, deviceCount, deviceCount - 1));
    }

    @Test
    public void anUpgradeOfAGrantInstanceExactlyAtItsDeviceCapProceeds() throws Exception {
        givenDevices(3);
        long deviceCount = deviceCount();
        assertThat(deviceCount).isPositive();

        LicenseCapacityUpgradePreflight preflight = preflight(true, deviceCount, assetCount() + 1);

        assertThatCode(preflight::check).doesNotThrowAnyException();
        // Proves the check really ran: without the licence secret or the cluster id it would return before
        // any counting and pass just as quietly.
        verify(preflight).buildLicenseClient(anyString(), any(UUID.class));
    }

    @Test
    public void anUpgradeOfAGrantInstanceOverItsAssetCapIsAbortedWithTheCountedAssets() throws Exception {
        givenAssets(3);
        long assetCount = assetCount();

        LicenseCapacityUpgradePreflight preflight = preflight(true, deviceCount() + 1, assetCount - 1);

        assertThatExceptionOfType(EntityCapExceededException.class)
                .isThrownBy(preflight::check)
                .withMessage(LicenseCapacity.capExceededMessage(CappedEntity.ASSET, assetCount, assetCount - 1));
    }

    @Test
    public void anUpgradeOfANonGrantInstanceOverBothCapsProceeds() throws Exception {
        givenDevices(3);
        givenAssets(3);

        LicenseCapacityUpgradePreflight preflight = preflight(false, 1, 1);

        assertThatCode(preflight::check).doesNotThrowAnyException();
        verify(preflight).buildLicenseClient(anyString(), any(UUID.class));
    }

    @Test
    public void aSuppliedKeyLandsInTheColumnOnceTheCheckPasses() throws Exception {
        givenDevices(3);
        jdbcTemplate.update("UPDATE tb_cluster SET license_secret = ?", "a-previous-license-secret");

        LicenseCapacityUpgradePreflight preflight = preflight(true, deviceCount(), assetCount() + 1);

        assertThatCode(preflight::check).doesNotThrowAnyException();
        assertThat(storedLicenseSecret()).isEqualTo("a-license-secret");
    }

    @Test
    public void aRefusedUpgradeLeavesTheColumnAsItWas() throws Exception {
        givenDevices(3);
        jdbcTemplate.update("UPDATE tb_cluster SET license_secret = ?", "a-previous-license-secret");

        LicenseCapacityUpgradePreflight preflight = preflight(true, deviceCount() - 1, assetCount() + 1);

        assertThatExceptionOfType(EntityCapExceededException.class).isThrownBy(preflight::check);
        assertThat(storedLicenseSecret()).isEqualTo("a-previous-license-secret");
    }

    /** The secret is supplied through the property, which is the one the check runs on whenever it is set. */
    private LicenseCapacityUpgradePreflight preflight(boolean communityGrant, long deviceLimit, long assetLimit)
            throws Exception {
        PlanData planData = new PlanData();
        planData.put(CommunityGrantPlan.COMMUNITY_GRANT_KEY, new PlanItem(communityGrant));
        planData.put(PlanDataConstants.MAX_DEVICES_KEY, new PlanItem(deviceLimit));
        planData.put(PlanDataConstants.MAX_ASSETS_KEY, new PlanItem(assetLimit));
        AbstractTbLicenseClient client = mock(AbstractTbLicenseClient.class);
        when(client.getPlanData()).thenReturn(planData);

        LicenseCapacityUpgradePreflight preflight = spy(new LicenseCapacityUpgradePreflight(jdbcTemplate));
        ReflectionTestUtils.setField(preflight, "licenseSecret", "a-license-secret");
        doReturn(client).when(preflight).buildLicenseClient(anyString(), any(UUID.class));
        return preflight;
    }

    private String storedLicenseSecret() {
        return jdbcTemplate.queryForObject("SELECT license_secret FROM tb_cluster LIMIT 1", String.class);
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
