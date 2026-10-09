// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Traces carry message payloads, so both controllers are guarded by the RULE_CHAIN resource:
 * READ for every getter, WRITE for saving the tenant's coverage setting.
 */
@DaoSqlTest
public class TraceMonitoringPermissionTest extends AbstractControllerTest {

    private static final String RANGE = "startTime=0&endTime=1000";
    private static final String TS_RANGE = "startTs=0&endTs=1000";
    private static final List<String> GET_URLS = List.of(
            "/api/traces/coverage",
            "/api/traces/trace-1",
            "/api/traces/trace-1/spans",
            "/api/traces/trace-1/spans/tree",
            "/api/traces/trace-1/spans/span-1/attributes",
            "/api/traces/trace-1/spans/span-1/events",
            "/api/traces/trace-1/spans/span-1/links",
            "/api/traces/filters",
            "/api/traces?" + RANGE,
            "/api/traces/paths?" + RANGE,
            "/api/traces/paths/filters",
            "/api/traces/paths/path-1?" + RANGE,
            "/api/ruleEngineMonitoring/filters",
            "/api/ruleEngineMonitoring/nodeStats/timeseries?" + TS_RANGE,
            "/api/ruleEngineMonitoring/nodeStats/table?" + TS_RANGE,
            "/api/ruleEngineMonitoring/queueStats/timeseries?" + TS_RANGE,
            "/api/ruleEngineMonitoring/queueStats/table?" + TS_RANGE,
            "/api/ruleEngineMonitoring/queueLagStats/timeseries?" + TS_RANGE,
            "/api/ruleEngineMonitoring/queueLagStats/table?" + TS_RANGE,
            "/api/ruleEngineMonitoring/queueLagStats/last",
            "/api/ruleEngineMonitoring/stats/table?" + TS_RANGE,
            "/api/ruleEngineMonitoring/stats/table/compare?" + TS_RANGE + "&compareStartTs=0&compareEndTs=1000");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void createTraceSchema() throws Exception {
        // The suite DB is built without schema-trace.sql.
        jdbcTemplate.execute(Resources.toString(Resources.getResource("sql/schema-trace.sql"), StandardCharsets.UTF_8));
    }

    @After
    public void dropTraceSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS span, span_stats_leader, trace_summary, rule_node_exec_stats, " +
                "rule_node_queue_stats, rule_node_queue_lag_stats, trace_group");
    }

    @Test
    public void givenDeviceReadOnlyUser_whenCallTraceAndMonitoringApi_thenForbidden() throws Exception {
        loginUserWithRole("{\"DEVICE\":[\"READ\"]}");

        for (String url : GET_URLS) {
            doGet(url).andExpect(status().isForbidden());
        }
        doPost("/api/traces/coverage", coverageSetting()).andExpect(status().isForbidden());
    }

    @Test
    public void givenRuleChainReadOnlyUser_whenCallTraceAndMonitoringApi_thenReadAllowedAndSaveForbidden() throws Exception {
        loginUserWithRole("{\"RULE_CHAIN\":[\"READ\"]}");

        assertReadAllowed();
        doPost("/api/traces/coverage", coverageSetting()).andExpect(status().isForbidden());
    }

    @Test
    public void givenTenantAdmin_whenCallTraceAndMonitoringApi_thenAllowed() throws Exception {
        loginTenantAdmin();

        assertReadAllowed();
        doPost("/api/traces/coverage", coverageSetting()).andExpect(status().isOk());
    }

    private void assertReadAllowed() throws Exception {
        for (String url : GET_URLS) {
            // Unknown trace/path ids may legitimately answer 404; only the permission outcome matters here.
            doGet(url).andExpect(result -> assertNotEquals(url, 403, result.getResponse().getStatus()));
        }
        doGet("/api/traces/coverage").andExpect(status().isOk());
    }

    private void loginUserWithRole(String permissionsJson) throws Exception {
        loginTenantAdmin();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setOwnerId(tenantId);
        userGroup.setName("Trace users " + suffix);
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        String email = "trace-user-" + suffix + "@thingsboard.org";
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        createUser(user, "password", userGroup.getId());

        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName("Trace role " + suffix);
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode(permissionsJson));
        role = doPost("/api/role", role, Role.class);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(role.getId());
        groupPermission.setUserGroupId(userGroup.getId());
        doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        loginUser(email, "password");
    }

    private TraceCoverageSetting coverageSetting() {
        return TraceCoverageSetting.defaultSetting(tenantId.getId());
    }

}
