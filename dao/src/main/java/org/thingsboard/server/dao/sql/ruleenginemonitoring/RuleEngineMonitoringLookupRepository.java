// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ruleenginemonitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleEngineMonitoringOption;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleEngineMonitoringQueueOption;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleEngineMonitoringRuleNodeOption;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Queue / rule chain / rule node lookups for the monitoring filters. These are entity tables, so they always live
 * in the main database, also when the stats tables are on the dedicated events datasource.
 */
@Repository
@SqlDao
@RequiredArgsConstructor
public class RuleEngineMonitoringLookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<QueueShortInfo> findAllQueuesShortInfo() {
        return jdbcTemplate.query(
                "SELECT id, tenant_id, name, partitions, consumer_per_partition FROM queue",
                (rs, rowNum) -> new QueueShortInfo(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("tenant_id"),
                        rs.getString("name"),
                        rs.getInt("partitions"),
                        rs.getBoolean("consumer_per_partition")
                ));
    }

    public List<RuleEngineMonitoringQueueOption> findFilterQueues(UUID tenantId) {
        return jdbcTemplate.query("""
                        SELECT id, name, tenant_id
                        FROM queue
                        WHERE tenant_id IN (?, ?)
                        ORDER BY name, id
                        """,
                (rs, rowNum) -> new RuleEngineMonitoringQueueOption(
                        (UUID) rs.getObject("id"),
                        rs.getString("name"),
                        (UUID) rs.getObject("tenant_id")
                ),
                TenantId.SYS_TENANT_ID.getId(), tenantId);
    }

    public List<RuleEngineMonitoringOption> findFilterRuleChains(UUID tenantId) {
        return jdbcTemplate.query("""
                        SELECT id, name
                        FROM rule_chain
                        WHERE tenant_id = ?
                          AND type = 'CORE'
                        ORDER BY name, id
                        """,
                (rs, rowNum) -> new RuleEngineMonitoringOption(
                        (UUID) rs.getObject("id"),
                        rs.getString("name")
                ),
                tenantId);
    }

    public List<RuleEngineMonitoringRuleNodeOption> findFilterRuleNodes(UUID tenantId) {
        return jdbcTemplate.query("""
                        SELECT rn.id,
                               rn.name,
                               rc.id AS rule_chain_id,
                               rc.name AS rule_chain_name
                        FROM rule_chain rc
                        JOIN rule_node rn ON rn.rule_chain_id = rc.id
                        WHERE rc.tenant_id = ?
                          AND rc.type = 'CORE'
                        ORDER BY rn.name, rn.id
                        """,
                (rs, rowNum) -> new RuleEngineMonitoringRuleNodeOption(
                        (UUID) rs.getObject("id"),
                        rs.getString("name"),
                        (UUID) rs.getObject("rule_chain_id"),
                        rs.getString("rule_chain_name")
                ),
                tenantId);
    }

    public Map<UUID, UUID> findRuleChainIdsByRuleNodeIds(Collection<UUID> ruleNodeIds) {
        if (ruleNodeIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> result = new HashMap<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement("SELECT id, rule_chain_id FROM rule_node WHERE id = ANY(?)");
            ps.setArray(1, con.createArrayOf("uuid", ruleNodeIds.toArray(new UUID[0])));
            return ps;
        }, rs -> {
            UUID chainId = (UUID) rs.getObject("rule_chain_id");
            if (chainId != null) {
                result.put((UUID) rs.getObject("id"), chainId);
            }
        });
        return result;
    }

}
