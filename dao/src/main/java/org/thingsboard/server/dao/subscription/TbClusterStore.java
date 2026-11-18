// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Accessor for the single {@code tb_cluster} row, which is how the nodes of a cluster share activation
 * state without any coordination: whatever one node writes here, every other node reads on its next
 * request.
 * <p>
 * Excluded from the {@code install} profile only; under {@code test} the table exists and the setup service
 * requires this bean. The non-production uptime contract is published here as public constants so the
 * scheduler that ticks the counter and the service that enforces the threshold, both outside this module,
 * cannot drift apart.
 */
@Component
@Profile("!install")
@RequiredArgsConstructor
@Slf4j
public class TbClusterStore {

    /**
     * The interval at which collaborators must call {@link #tickNonProductionUptime}; a compile-time constant
     * because {@link #MAX_DELTA_MS} is derived from it. Kept short because it also bounds what a restart
     * discards: nothing accrues before the first tick of a run, so a node restarted more often than this
     * accrues nothing at all.
     */
    public static final long TICK_INTERVAL_MINUTES = 5;
    /**
     * The {@code maxDeltaMs} collaborators must pass to {@link #tickNonProductionUptime}: twice the tick
     * interval, so a normally spaced call is credited close to its true elapsed time while a longer gap -
     * downtime, or a missed tick - is not counted as uptime.
     */
    public static final long MAX_DELTA_MS = TimeUnit.MINUTES.toMillis(TICK_INTERVAL_MINUTES * 2);
    /**
     * The {@link #getNonProductionUptimeMs()} value at or past which a non-production license client must no
     * longer be granted, and an instance already running one must be locked.
     */
    public static final long NON_PRODUCTION_UPTIME_LIMIT_MS = TimeUnit.DAYS.toMillis(30);

    private final JdbcTemplate jdbcTemplate;

    public Optional<String> getLicenseSecret() {
        return getColumn("license_secret");
    }

    public void saveLicenseSecret(String secret) {
        updateColumn("license_secret", secret, "license secret");
    }

    /** The only writer that ever empties the column; callers latching on it being set must reset that latch alongside. */
    public void clearLicenseSecret() {
        jdbcTemplate.update("UPDATE tb_cluster SET license_secret = NULL");
    }

    public Optional<String> getLicenseClaimToken() {
        return getColumn("license_claim_token");
    }

    public void saveLicenseClaimToken(String claimToken) {
        updateColumn("license_claim_token", claimToken, "license claim token");
    }

    public long getNonProductionUptimeMs() {
        Long uptime = jdbcTemplate.query("SELECT non_production_uptime_ms FROM tb_cluster LIMIT 1",
                rs -> rs.next() ? rs.getLong("non_production_uptime_ms") : 0L);
        return uptime == null ? 0L : uptime;
    }

    /**
     * Accrues the time elapsed since the previous tick; the first tick only establishes the baseline. A gap
     * larger than {@code maxDeltaMs} accrues zero, not {@code maxDeltaMs} - crediting the clamp would count
     * time the node was not running. The accrual is floored at zero, since {@code nowMs} can precede the
     * stored tick when partition ownership fails over to a node with a lagging clock, and a cumulative
     * counter must not move backwards. {@code non_production_last_tick} advances to {@code nowMs}
     * unconditionally - past the cap and backwards alike - or a stale baseline would stall accrual
     * permanently instead of for the one tick that observed the gap.
     */
    public void tickNonProductionUptime(long nowMs, long maxDeltaMs) {
        int updatedRows = jdbcTemplate.update(
                "UPDATE tb_cluster SET non_production_uptime_ms = non_production_uptime_ms " +
                        "+ CASE WHEN ? - COALESCE(non_production_last_tick, ?) > ? THEN 0 " +
                        "ELSE GREATEST(? - COALESCE(non_production_last_tick, ?), 0) END, " +
                        "non_production_last_tick = ?",
                nowMs, nowMs, maxDeltaMs, nowMs, nowMs, nowMs);
        if (updatedRows == 0) {
            log.warn("Failed to accrue non-production uptime: table tb_cluster is empty");
        }
    }

    /** Guards the {@code @Nullable}-declared query result, which is unreachable in practice. */
    public Optional<Long> getNonProductionConfirmedTs() {
        Optional<Long> confirmedTs = jdbcTemplate.query("SELECT non_production_confirmed_ts FROM tb_cluster LIMIT 1", rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            long value = rs.getLong("non_production_confirmed_ts");
            return rs.wasNull() ? Optional.<Long>empty() : Optional.of(value);
        });
        return confirmedTs == null ? Optional.empty() : confirmedTs;
    }

    /**
     * @return whether the row was actually updated, so a caller can surface a confirmation that was attempted
     * but never stored rather than reporting success.
     */
    public boolean saveNonProductionConfirmedTs(long confirmedTs) {
        int updatedRows = jdbcTemplate.update("UPDATE tb_cluster SET non_production_confirmed_ts = ?", confirmedTs);
        if (updatedRows == 0) {
            log.warn("Failed to store the non-production confirmation: table tb_cluster is empty");
            return false;
        }
        return true;
    }

    /**
     * Drops whatever claim is stored, whichever token that is. Only for callers to which any pending claim is
     * moot - an activation that already put a license in place; everyone else wants
     * {@link #clearLicenseClaimToken(String)}. Never throws: best-effort hygiene on paths whose outcome does
     * not depend on it.
     */
    public void forceClearLicenseClaimToken() {
        try {
            jdbcTemplate.update("UPDATE tb_cluster SET license_claim_token = NULL");
        } catch (Exception e) {
            log.warn("Failed to clear the license claim token", e);
        }
    }

    /**
     * Compare-and-clear: retires a claim token only while it is still the stored one. A claim runs against
     * the portal outside any lock a claim request takes, so a claim that comes back terminal may find a token
     * a fresh request stored while it was out, and clearing unconditionally would destroy a claim that is
     * about to succeed. Never throws, for the same reason as {@link #forceClearLicenseClaimToken()}.
     */
    public void clearLicenseClaimToken(String expectedClaimToken) {
        try {
            jdbcTemplate.update("UPDATE tb_cluster SET license_claim_token = NULL WHERE license_claim_token = ?",
                    expectedClaimToken);
        } catch (Exception e) {
            log.warn("Failed to clear the license claim token", e);
        }
    }

    // The column name is concatenated into the statement: pass only compile-time constants.

    /**
     * An empty value and an absent row are different answers and must not be collapsed: callers read the
     * former as a value the cluster deliberately cleared, and would act on a stale replica, a restored dump or
     * an unfinished install as though the cluster had cleared it. Refuses the absent row on the same invariant
     * {@link #updateColumn} does, so a caller's error handling - not its success path - decides what happens.
     */
    private Optional<String> getColumn(String column) {
        return jdbcTemplate.query("SELECT " + column + " FROM tb_cluster LIMIT 1", rs -> {
            if (!rs.next()) {
                log.error("Failed to read the {}: table tb_cluster is empty. The row is created with the schema " +
                        "and is never removed, so an empty table means this deployment's installation did not " +
                        "complete.", column);
                throw new IllegalStateException("Failed to read the " + column + ": table tb_cluster is empty");
            }
            return Optional.ofNullable(rs.getString(column));
        });
    }

    private void updateColumn(String column, String value, String description) {
        int updatedRows = jdbcTemplate.update("UPDATE tb_cluster SET " + column + " = ?", value);
        if (updatedRows == 0) {
            log.error("Failed to store the {}: table tb_cluster is empty; the install or upgrade that creates " +
                    "its row has not completed", description);
            throw new IllegalStateException("Failed to store the " + description + ": table tb_cluster is empty");
        }
    }
}
