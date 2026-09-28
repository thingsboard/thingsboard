// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.GetCurrentLoginWhiteLabelParamsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCurrentWhiteLabelParamsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDomainArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveLoginWhiteLabelParamsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveWhiteLabelParamsArgs;
import org.thingsboard.client.model.Domain;
import org.thingsboard.client.model.LoginWhiteLabelingParams;
import org.thingsboard.client.model.WhiteLabelingParams;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class WhiteLabelingApiClientTest extends AbstractApiClientTest {

    @Test
    public void testTenantWhiteLabelingLifecycle() throws Exception {
        client.login(TENANT_ADMIN_USERNAME, TEST_PASSWORD);

        Boolean whiteLabelingAllowed = client.isWhiteLabelingAllowed();
        assertTrue(whiteLabelingAllowed);

        Boolean customerWhiteLabelingAllowed = client.isCustomerWhiteLabelingAllowed();
        assertTrue(customerWhiteLabelingAllowed);

        WhiteLabelingParams currentParams = client.getCurrentWhiteLabelParams(GetCurrentWhiteLabelParamsArgs.builder()

                .build());
        assertNotNull(currentParams);
        currentParams.setAppTitle("Custom title");

        WhiteLabelingParams saved = client.saveWhiteLabelParams(SaveWhiteLabelParamsArgs.builder()
                .whiteLabelingParams(currentParams)
                .build());
        assertNotNull(saved);
        assertEquals("Custom title", saved.getAppTitle());

        LoginWhiteLabelingParams currentLoginParams = client.getCurrentLoginWhiteLabelParams(GetCurrentLoginWhiteLabelParamsArgs.builder()

                .build());
        assertNotNull(currentLoginParams);

        Domain domain = new Domain();
        domain.setName("test.com");
        domain = client.saveDomain(SaveDomainArgs.builder()
                .domain(domain)
                .build());

        currentLoginParams.setDomainId(domain.getId());
        currentLoginParams.setAppTitle("Custom login title");
        LoginWhiteLabelingParams savedLoginParams = client.saveLoginWhiteLabelParams(SaveLoginWhiteLabelParamsArgs.builder()
                .loginWhiteLabelingParams(currentLoginParams)
                .build());
        assertEquals("Custom login title", savedLoginParams.getAppTitle());
    }

}
