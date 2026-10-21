// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.edge.EdgeProfileService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Drives {@link V4_4_1_0Migration#applyAfterCommit()} against a real Postgres instance with a tenant left in the
 * 4.4.0 state: edges without a profile and an auto-assign rule chain kept as a tenant relation.
 */
@DaoSqlTest
public class V4_4_1_0MigrationIntegrationTest extends AbstractControllerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private V4_4_1_0Migration migration;
    @Autowired
    private EdgeProfileService edgeProfileService;

    @Test
    public void edgesWithoutProfileGetTheDefaultProfileWithTheAutoAssignRuleChain() throws Exception {
        loginTenantAdmin();
        RuleChain autoAssign = saveEdgeRuleChain("Auto assigned before 4.4.1");
        Edge edge = doPost("/api/edge", constructEdge("Edge before 4.4.1", null), Edge.class);

        jdbcTemplate.update("UPDATE edge SET edge_profile_id = NULL, type = 'default' WHERE tenant_id = ?", tenantId.getId());
        edgeProfileService.deleteEdgeProfilesByTenantId(tenantId);
        jdbcTemplate.update("INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info, version) " +
                        "VALUES (?, 'TENANT', ?, 'RULE_CHAIN', 'EDGE_AUTO_ASSIGN_RULE_CHAIN', 'Contains', NULL, 1)",
                tenantId.getId(), autoAssign.getId().getId());

        migration.applyAfterCommit();

        UUID defaultProfileId = jdbcTemplate.queryForObject("SELECT id FROM edge_profile WHERE tenant_id = ? AND is_default = true",
                UUID.class, tenantId.getId());
        assertNotNull(defaultProfileId);
        assertEquals(defaultProfileId, profileOf(edge));
        assertTrue(profileDataOf(defaultProfileId).contains(autoAssign.getId().getId().toString()));
        assertEquals(Integer.valueOf(0), autoAssignRelations());
    }

    private UUID profileOf(Edge edge) {
        return jdbcTemplate.queryForObject("SELECT edge_profile_id FROM edge WHERE id = ?", UUID.class, edge.getId().getId());
    }

    private String profileDataOf(UUID edgeProfileId) {
        return jdbcTemplate.queryForObject("SELECT profile_data::text FROM edge_profile WHERE id = ?", String.class, edgeProfileId);
    }

    private Integer autoAssignRelations() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM relation WHERE from_id = ? AND relation_type_group = 'EDGE_AUTO_ASSIGN_RULE_CHAIN'",
                Integer.class, tenantId.getId());
    }

    private RuleChain saveEdgeRuleChain(String name) throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.EDGE);
        return doPost("/api/ruleChain", ruleChain, RuleChain.class);
    }

}
