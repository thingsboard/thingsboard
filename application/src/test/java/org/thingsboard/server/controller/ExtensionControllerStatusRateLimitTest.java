// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/extension/{id}/status and GET /api/tenant/extensions reach the orchestrator whenever the
 * status cache cannot answer, and on Kubernetes that is a LIST over the whole shared extensions
 * namespace — so one call costs in proportion to every tenant's pods, not just the caller's, and in
 * private-cloud mode it lands on the API server tb-core itself depends on. Only /logs and /api-docs had
 * a per-user budget before.
 *
 * <p>A class of its own, with a deliberately tiny limit: exhausting the budget is the point of the test,
 * and the tenant admin's bucket is shared with every other case in
 * {@link ExtensionControllerTest}, several of which read the list and the status.</p>
 */
@DaoSqlTest
@TestPropertySource(locations = {"classpath:application-test.properties", "classpath:sql-test.properties"},
        properties = {"extensions.mode=public-cloud", "extensions.status.rate-limit=3:60"})
public class ExtensionControllerStatusRateLimitTest extends AbstractControllerTest {

    @MockBean
    private ExtensionReconciler extensionReconciler;

    @Before
    public void stubReconciler() {
        when(extensionReconciler.describe(any())).thenReturn(DeploymentHealth.missing());
        when(extensionReconciler.listManagedSlugs()).thenReturn(Set.of());
    }

    @Test
    public void statusAndListShareOnePerUserBudget() throws Exception {
        loginTenantAdmin();

        ExtensionContainerSpec spec = new ExtensionContainerSpec();
        spec.setImage("registry.example.com/acme/billing-ext");
        spec.setTag("1.0.0");
        Extension extension = new Extension();
        extension.setName("Status rate limit");
        extension.setContainerSpec(spec);
        Extension created = readResponse(
                doPost("/api/extension", extension).andExpect(status().isAccepted()), Extension.class);

        String statusUrl = "/api/extension/" + created.getUuidId() + "/status";
        for (int i = 0; i < 3; i++) {
            doGet(statusUrl).andExpect(status().isOk());
        }
        doGet(statusUrl).andExpect(status().isTooManyRequests());

        // one budget covers both: the list page enriches every row from the same source
        doGet("/api/tenant/extensions?pageSize=10&page=0").andExpect(status().isTooManyRequests());
    }
}
