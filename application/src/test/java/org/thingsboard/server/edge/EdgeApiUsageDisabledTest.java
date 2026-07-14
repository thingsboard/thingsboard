/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.edge;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.edge.exception.EdgeFeatureDisabledException;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.service.edge.rpc.session.EdgeSessionsHolder;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.interval=2",
        "usage.stats.report.urgent_interval=1"
})
public class EdgeApiUsageDisabledTest extends AbstractEdgeTest {

    private static final int MAX_EDGE_EVENTS = 1;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private TbApiUsageReportClient apiUsageReportClient;

    @Autowired
    private EdgeSessionsHolder edgeSessionsHolder;

    @After
    public void restoreEdgeLimit() {
        try {
            loginSysAdmin();
            updateDefaultTenantProfileConfig(cfg -> cfg.setMaxEdgeEvents(0));
        } catch (Exception ignored) {}
    }

    @Test
    public void testLiveSessionForceClosedWhenEdgeStateDisabled() throws Exception {
        await().atMost(10, TimeUnit.SECONDS).until(() -> sessionConnected(tenantId));

        loginSysAdmin();
        updateDefaultTenantProfileConfig(cfg -> cfg.setMaxEdgeEvents(MAX_EDGE_EVENTS));
        loginTenantAdmin();

        for (int i = 0; i < MAX_EDGE_EVENTS + 5; i++) {
            apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.EDGE_EVENT_COUNT);
        }

        await().atMost(15, TimeUnit.SECONDS).until(() -> apiUsageStateService.findTenantApiUsageState(tenantId).getEdgeState() == ApiUsageStateValue.DISABLED);

        await().atMost(10, TimeUnit.SECONDS).until(() -> !sessionConnected(tenantId));
    }

    @Test
    public void testReconnectRejectedWhenEdgeStateDisabled() throws Exception {
        await().atMost(10, TimeUnit.SECONDS).until(() -> sessionConnected(tenantId));

        loginSysAdmin();
        updateDefaultTenantProfileConfig(cfg -> cfg.setMaxEdgeEvents(MAX_EDGE_EVENTS));
        loginTenantAdmin();

        for (int i = 0; i < MAX_EDGE_EVENTS + 5; i++) {
            apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.EDGE_EVENT_COUNT);
        }

        await().atMost(15, TimeUnit.SECONDS).until(() -> !sessionConnected(tenantId));

        EdgeImitator rejectedImitator = new EdgeImitator(EDGE_HOST, EDGE_PORT, edge.getRoutingKey(), edge.getSecret());
        rejectedImitator.connect();

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Exception closeException = rejectedImitator.getCloseException();
                    Assert.assertNotNull("Imitator must receive a connection-rejected callback", closeException);
                    Assert.assertTrue("Expected EdgeFeatureDisabledException, got: " + closeException, closeException instanceof EdgeFeatureDisabledException);
                });
        Assert.assertFalse("Edge session must not be admitted when edge feature is disabled",
                sessionConnected(tenantId));

        try {
            rejectedImitator.disconnect();
        } catch (Exception ignored) {}
    }

    @Test
    public void testReconnectAdmittedAfterEdgeStateReEnabled() throws Exception {
        await().atMost(10, TimeUnit.SECONDS).until(() -> sessionConnected(tenantId));

        loginSysAdmin();
        updateDefaultTenantProfileConfig(cfg -> cfg.setMaxEdgeEvents(MAX_EDGE_EVENTS));
        loginTenantAdmin();

        for (int i = 0; i < MAX_EDGE_EVENTS + 5; i++) {
            apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.EDGE_EVENT_COUNT);
        }

        await().atMost(15, TimeUnit.SECONDS)
                .until(() -> apiUsageStateService.findTenantApiUsageState(tenantId).getEdgeState() == ApiUsageStateValue.DISABLED);
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> !sessionConnected(tenantId));

        loginSysAdmin();
        updateDefaultTenantProfileConfig(cfg -> cfg.setMaxEdgeEvents(0));
        loginTenantAdmin();

        await().atMost(15, TimeUnit.SECONDS)
                .until(() -> apiUsageStateService.findTenantApiUsageState(tenantId).getEdgeState() == ApiUsageStateValue.ENABLED);

        EdgeImitator reconnectImitator = new EdgeImitator(EDGE_HOST, EDGE_PORT, edge.getRoutingKey(), edge.getSecret());
        reconnectImitator.connect();

        try {
            await().atMost(15, TimeUnit.SECONDS)
                    .until(() -> sessionConnected(tenantId));
            Assert.assertNull("Imitator must not receive a close callback after re-enable",
                    reconnectImitator.getCloseException());
        } finally {
            try {
                reconnectImitator.disconnect();
            } catch (Exception ignored) {}
        }
    }

    private boolean sessionConnected(TenantId tenantId) {
        return edgeSessionsHolder.getByTenantId(tenantId).stream().anyMatch(s -> s.getState().isConnected());
    }

}
