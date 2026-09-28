// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.subscription.SubscriptionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class NonProductionHeaderTest extends AbstractControllerTest {

    @MockitoSpyBean
    private SubscriptionService subscriptionService;

    @Test
    public void theHeaderIsSetWhenTheInstanceIsNonProduction() throws Exception {
        loginTenantAdmin();
        when(subscriptionService.isDevelopment(any())).thenReturn(true);

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(header().string("X-ThingsBoard-Non-Production", "true"));
    }

    @Test
    public void theHeaderSurvivesAnUnauthenticatedRequest() throws Exception {
        // No login here: the filter is registered ahead of every authentication filter (and ahead of
        // systemSetupFilter) precisely so a request that never gets past authentication still carries
        // the signal, rather than the header silently depending on how far the request got.
        when(subscriptionService.isDevelopment(any())).thenReturn(true);

        doGet("/api/auth/user")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-ThingsBoard-Non-Production", "true"));
    }

    @Test
    public void theHeaderIsAbsentOnAProductionInstance() throws Exception {
        // Set only when true. Absence must never be read as proof of production, since every build older
        // than this change also omits it.
        when(subscriptionService.isDevelopment(any())).thenReturn(false);

        doGet("/api/auth/user")
                .andExpect(header().doesNotExist("X-ThingsBoard-Non-Production"))
                .andExpect(status().isUnauthorized());
    }

}
