// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmPropagationInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Citus sharding-invariant tests for the {@code alarm} / {@code entity_alarm} group, asserted with raw SQL and
 * {@code EXPLAIN} against the REAL distributed schema produced by {@code DefaultCitusSchemaService.applyDistribution()}.
 * <p>
 * Runs on the same {@code @CitusDaoSqlTest} harness as {@link CitusAlarmServiceTest}: a real coordinator+worker Citus
 * cluster with the full ThingsBoard schema, where {@code alarm} and {@code entity_alarm} are hash-distributed on
 * {@code originator_id} and co-located with the {@code device} anchor group. Data is created through the production
 * service layer (so rows land exactly as production places them), and the autowired {@link JdbcTemplate} points at the
 * cluster coordinator for the invariant assertions.
 * <p>
 * This class deliberately does NOT re-assert what is already covered elsewhere: table classification
 * ({@code CitusTablesTest}), the conversion DDL — widened PKs, single colocationid, composite cascade FK
 * ({@code DefaultCitusSchemaServiceTest}), or general alarm CRUD ({@link CitusAlarmServiceTest}). It pins the runtime
 * sharding CONTRACTS those rely on: same-shard co-location, no-repartition co-located join, single-shard write routing,
 * and the co-located cascade delete of {@code entity_alarm}.
 */
@CitusDaoSqlTest
public class CitusAlarmShardingInvariantsTest extends AbstractServiceTest {

    @Autowired
    AlarmService alarmService;
    @Autowired
    DeviceService deviceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Device createOriginatorDevice(String name) {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device.setTenantId(tenantId);
        return deviceService.saveDevice(device);
    }

    /**
     * An alarm and its propagated entity_alarm rows must land on the SAME Citus shard as the originator device,
     * because all three distribute on originator_id and are co-located. Co-located tables have independent shardid
     * series, so the oracle is that the owning shards cover the same hash range and live on the same worker node
     * (see {@link #shardPlacementKey}), cross-checked against a shared colocationid in pg_dist_partition.
     */
    @Test
    public void alarmAndEntityAlarmCoLocateWithOriginatorDevice() {
        Device device = createOriginatorDevice("ShardCoLocationDevice");
        UUID originatorId = device.getId().getId();

        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(device.getId())
                .type("CoLocationAlarm")
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagateToTenant(true).build())
                .startTs(System.currentTimeMillis()).build());
        Alarm created = result.getAlarm();
        Assert.assertNotNull(created);

        // Sanity: the alarm row and at least one entity_alarm row for this originator actually exist.
        Long alarmRows = jdbcTemplate.queryForObject(
                "select count(*) from alarm where originator_id = ?::uuid and id = ?::uuid",
                Long.class, originatorId.toString(), created.getId().getId().toString());
        assertThat(alarmRows).as("alarm row must exist for the originator").isEqualTo(1L);
        Long entityAlarmRows = jdbcTemplate.queryForObject(
                "select count(*) from entity_alarm where originator_id = ?::uuid and alarm_id = ?::uuid",
                Long.class, originatorId.toString(), created.getId().getId().toString());
        assertThat(entityAlarmRows).as("entity_alarm propagation rows must exist for the originator").isGreaterThanOrEqualTo(1L);

        String devicePlacement = CitusTestSupport.shardPlacementKey(jdbcTemplate, "device", originatorId);
        String alarmPlacement = CitusTestSupport.shardPlacementKey(jdbcTemplate, "alarm", originatorId);
        String entityAlarmPlacement = CitusTestSupport.shardPlacementKey(jdbcTemplate, "entity_alarm", originatorId);

        assertThat(alarmPlacement)
                .as("alarm must land in the same hash range / worker node as its originator device (co-location on originator_id)")
                .isEqualTo(devicePlacement);
        assertThat(entityAlarmPlacement)
                .as("entity_alarm must land in the same hash range / worker node as its originator device (co-location on originator_id)")
                .isEqualTo(devicePlacement);

        // Cross-check the placement co-location with the catalog-level co-location group: device, alarm and
        // entity_alarm must all share one colocationid (i.e. they are in the same Citus co-location group).
        Long distinctColocationIds = jdbcTemplate.queryForObject(
                "select count(distinct colocationid) from pg_dist_partition " +
                        "where logicalrelid in ('device'::regclass, 'alarm'::regclass, 'entity_alarm'::regclass)",
                Long.class);
        assertThat(distinctColocationIds)
                .as("device, alarm and entity_alarm must belong to a single Citus co-location group")
                .isEqualTo(1L);
    }

    /**
     * The findAlarms-shaped join (alarm a inner join entity_alarm ea on a.id = ea.alarm_id and
     * a.originator_id = ea.originator_id) must be a co-located join: because both sides distribute on originator_id
     * and the join includes originator_id equality, Citus can push the whole join down to each shard. The plan must
     * therefore contain NO repartition and NO broadcast markers. Asserting the ABSENCE of those markers (rather than
     * exact plan equality) keeps this robust across Citus 12.x plan-text variations.
     */
    @Test
    public void coLocatedAlarmEntityAlarmJoinDoesNotRepartitionOrBroadcast() {
        Device device = createOriginatorDevice("CoLocatedJoinDevice");
        alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(device.getId())
                .type("JoinAlarm")
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagateToTenant(true).build())
                .startTs(System.currentTimeMillis()).build());

        String join = "select a.id, ea.entity_id from alarm a " +
                "inner join entity_alarm ea on a.id = ea.alarm_id and a.originator_id = ea.originator_id " +
                "where a.tenant_id = '" + tenantId.getId() + "'::uuid";
        String plan = CitusTestSupport.explainText(jdbcTemplate, join).toLowerCase();

        // Positive guard first: the plan must be a distributed Citus plan at all — a plain-Postgres plan (tables
        // silently left local) contains neither of the markers below, so the absence assertions alone would pass
        // vacuously on exactly the regression they guard against.
        assertThat(plan)
                .as("the join must produce a distributed Citus plan. Plan:\n%s", plan)
                .contains("task count:");

        assertThat(plan)
                .as("a co-located alarm <-> entity_alarm join must not repartition. Plan:\n%s", plan)
                .doesNotContain("repartition");
        assertThat(plan)
                .as("a co-located alarm <-> entity_alarm join must not broadcast. Plan:\n%s", plan)
                .doesNotContain("broadcast");
    }

    /**
     * The create_or_update_active_alarm latest-active lookup
     * (select ... from alarm a where a.originator_id = ? and a.type = ? and a.cleared = false ... for update)
     * filters on the distribution column originator_id, so Citus must route it to a SINGLE shard (Task Count: 1).
     * This is what makes the create-or-update alarm path a single-shard write rather than a distributed transaction.
     */
    @Test
    public void latestActiveLookupRoutesToSingleShard() {
        Device device = createOriginatorDevice("SingleShardWriteDevice");
        UUID originatorId = device.getId().getId();

        String lookup = "select * from alarm a " +
                "where a.originator_id = '" + originatorId + "'::uuid and a.type = 'AnyType' and a.cleared = false " +
                "order by a.start_ts desc for update";

        assertThat(CitusTestSupport.taskCount(jdbcTemplate, lookup))
                .as("the latest-active alarm lookup (filtered on originator_id) must route to a single shard")
                .isEqualTo(1);
    }

    /**
     * The mutation functions' result fetch and per-row UPDATE are constrained by (originator_id, id), not by id alone.
     * Adding the originator_id (distribution column) predicate is what keeps these statements single-shard
     * (Task Count: 1) under Citus instead of scattering across every shard. The alarm_info read goes through the view
     * (alarm a LEFT JOIN ...), so filtering on a.originator_id also prunes the co-located joins to one shard.
     */
    @Test
    public void resultFetchAndUpdateByOriginatorAndIdRouteToSingleShard() {
        Device device = createOriginatorDevice("SingleShardResultDevice");
        UUID originatorId = device.getId().getId();

        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(device.getId())
                .type("SingleShardResultAlarm")
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagateToTenant(true).build())
                .startTs(System.currentTimeMillis()).build());
        Alarm created = result.getAlarm();
        Assert.assertNotNull(created);
        UUID alarmId = created.getId().getId();

        String resultFetch = "select * from alarm_info a " +
                "where a.originator_id = '" + originatorId + "'::uuid and a.id = '" + alarmId + "'::uuid";
        assertThat(CitusTestSupport.taskCount(jdbcTemplate, resultFetch))
                .as("the alarm_info result fetch keyed by (originator_id, id) must route to a single shard")
                .isEqualTo(1);

        String update = "update alarm a set severity = a.severity " +
                "where a.originator_id = '" + originatorId + "'::uuid and a.id = '" + alarmId + "'::uuid";
        assertThat(CitusTestSupport.taskCount(jdbcTemplate, update))
                .as("the per-row alarm UPDATE keyed by (originator_id, id) must route to a single shard")
                .isEqualTo(1);
    }

    /**
     * Deleting an alarm must remove its entity_alarm propagation rows via the co-located ON DELETE CASCADE FK
     * (originator_id, alarm_id) -> alarm(originator_id, id). Verified by counting entity_alarm rows directly before
     * and after delAlarm.
     */
    @Test
    public void deletingAlarmCascadesEntityAlarmRows() {
        Device device = createOriginatorDevice("CascadeDevice");
        UUID originatorId = device.getId().getId();

        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(device.getId())
                .type("CascadeAlarm")
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagateToTenant(true).build())
                .startTs(System.currentTimeMillis()).build());
        Alarm created = result.getAlarm();
        Assert.assertNotNull(created);

        Long before = jdbcTemplate.queryForObject(
                "select count(*) from entity_alarm where originator_id = ?::uuid and alarm_id = ?::uuid",
                Long.class, originatorId.toString(), created.getId().getId().toString());
        assertThat(before).as("entity_alarm propagation rows must exist before delete").isGreaterThanOrEqualTo(1L);

        Assert.assertTrue("Alarm was not deleted when expected",
                alarmService.delAlarm(tenantId, created.getOriginator(), created.getId()).isSuccessful());

        Long after = jdbcTemplate.queryForObject(
                "select count(*) from entity_alarm where alarm_id = ?::uuid",
                Long.class, created.getId().getId().toString());
        assertThat(after)
                .as("entity_alarm rows must be removed by the co-located ON DELETE CASCADE FK when the alarm is deleted")
                .isEqualTo(0L);
    }
}
