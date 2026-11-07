// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

@CitusDaoSqlTest
public class CitusDeviceServiceTest extends DeviceServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Citus cross-node FK-visibility regression guard for implicit device profiles.
     * <p>
     * Saving a device with a brand-new (never-before-seen) device profile type goes through the
     * {@code @Transactional} {@code DeviceServiceImpl.saveDevice} path, which calls
     * {@code DeviceProfileService.findOrCreateDeviceProfile}. When the named profile is absent it is
     * freshly inserted (device_profile is a Citus reference table) and then the device row
     * (distributed table) is flushed with a device_profile_id FK to it — all in the caller's single
     * transaction.
     * <p>
     * Citus handles this natively: when a distributed-table write (the device) depends on a
     * reference-table row written earlier in the same transaction (the fresh device_profile), the
     * conflicting-parallel-relation-access guard forces the transaction onto a single, sequential
     * connection, so the parent INSERT is visible to the child FK check. No early commit /
     * REQUIRES_NEW is needed (and the {@code citus.all_modifications_commutative=on} flag is only a
     * lock-mode downgrade, not what makes this work). This test pins that guarantee so a future
     * topology or flag change that breaks it fails loudly here.
     * <p>
     * Existing device tests mask this because they use the "default" profile type, which is
     * pre-committed at tenant creation, so findOrCreate always finds and never fresh-inserts.
     */
    @Test
    public void testSaveDeviceWithBrandNewProfileTypeCommitsProfileBeforeFkCheck() {
        String brandNewType = "citus-fresh-profile-" + StringUtils.randomAlphabetic(10);
        Assert.assertNull("Precondition: the profile type must not yet exist",
                deviceProfileService.findDeviceProfileByName(tenantId, brandNewType, false));

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Device with brand-new profile " + StringUtils.randomAlphabetic(10));
        device.setType(brandNewType);

        Device[] saved = new Device[1];
        assertThatCode(() -> saved[0] = deviceService.saveDevice(device))
                .as("Saving a device with a brand-new device profile type must not raise a cross-node FK violation under Citus")
                .doesNotThrowAnyException();

        Assert.assertNotNull(saved[0]);
        Assert.assertNotNull("The implicit device profile must be committed and FK-visible", saved[0].getDeviceProfileId());

        DeviceProfile createdProfile = deviceProfileService.findDeviceProfileByName(tenantId, brandNewType, false);
        Assert.assertNotNull("The fresh device profile must be persisted", createdProfile);
        Assert.assertEquals(createdProfile.getId().getId(), saved[0].getDeviceProfileId().getId());

        deviceService.deleteDevice(tenantId, saved[0].getId());
    }

    /**
     * Guards the assumption behind the shard-aware device scan (CitusShardKeysetPages): PostgreSQL's uuid_hash(id) is the
     * hash Citus places device rows by, so a uuid_hash range bounded to pg_dist_shard's shardminvalue..shardmaxvalue holds
     * exactly that shard's rows. The scan stays correct without it (the ranges cover the whole hash space), but every page
     * would fan out again; this test fails first if a Citus version changed its hashing.
     */
    @Test
    public void testUuidHashMatchesCitusDevicePlacement() {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            devices.add(saveDevice(tenantId, "Placement device " + i));
        }
        List<long[]> shards = jdbcTemplate.query("SELECT shardid, shardminvalue::int AS smin, shardmaxvalue::int AS smax " +
                        "FROM pg_dist_shard WHERE logicalrelid = 'device'::regclass",
                (rs, rowNum) -> new long[]{rs.getLong("shardid"), rs.getInt("smin"), rs.getInt("smax")});
        Assert.assertFalse(shards.isEmpty());
        for (Device device : devices) {
            int hash = jdbcTemplate.queryForObject("SELECT uuid_hash(?::uuid)", Integer.class, device.getUuidId().toString());
            long placedShard = jdbcTemplate.queryForObject("SELECT get_shard_id_for_distribution_column('device', ?::uuid)",
                    Long.class, device.getUuidId().toString());
            long rangeShard = shards.stream().filter(shard -> hash >= shard[1] && hash <= shard[2]).findFirst().orElseThrow()[0];
            Assert.assertEquals("shard of device " + device.getId(), placedShard, rangeShard);
        }
        Long indexes = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE tablename = 'device' AND indexname = 'idx_device_uuid_hash_id'", Long.class);
        Assert.assertEquals(Long.valueOf(1), indexes);
    }

}
