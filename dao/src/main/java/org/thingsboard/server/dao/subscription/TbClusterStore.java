// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Accessor for the single {@code tb_cluster} row: the cluster id, the license portal claim token and the
 * license key the portal granted.
 */
@Component
@Profile("!install")
@RequiredArgsConstructor
@Slf4j
public class TbClusterStore {

    private final JdbcTemplate jdbcTemplate;

    public Optional<UUID> getClusterId() {
        return jdbcTemplate.query("SELECT cluster_id FROM tb_cluster ORDER BY cluster_id LIMIT 1",
                rs -> rs.next() ? Optional.ofNullable(rs.getObject("cluster_id", UUID.class)) : Optional.empty());
    }

    public Optional<String> getLicenseSecret() {
        return getColumn("license_secret");
    }

    public void saveLicenseSecret(String secret) {
        updateColumn("license_secret", secret, "license secret");
    }

    public Optional<String> getLicenseClaimToken() {
        return getColumn("license_claim_token");
    }

    public void saveLicenseClaimToken(String claimToken) {
        updateColumn("license_claim_token", claimToken, "license claim token");
    }

    public void forceClearLicenseClaimToken() {
        try {
            jdbcTemplate.update("UPDATE tb_cluster SET license_claim_token = NULL");
        } catch (Exception e) {
            log.warn("Failed to clear the license claim token", e);
        }
    }

    public void clearLicenseClaimToken(String expectedClaimToken) {
        try {
            jdbcTemplate.update("UPDATE tb_cluster SET license_claim_token = NULL WHERE license_claim_token = ?",
                    expectedClaimToken);
        } catch (Exception e) {
            log.warn("Failed to clear the license claim token", e);
        }
    }

    // The column name is concatenated into the statement: pass only compile-time constants.
    private Optional<String> getColumn(String column) {
        return jdbcTemplate.query("SELECT " + column + " FROM tb_cluster ORDER BY cluster_id LIMIT 1",
                rs -> rs.next() ? Optional.ofNullable(rs.getString(column)) : Optional.empty());
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
