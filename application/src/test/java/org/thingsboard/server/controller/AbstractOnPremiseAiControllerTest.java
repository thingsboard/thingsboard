// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.client.TbAiHeaders;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.service.install.ProjectInfo;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@TestPropertySource(properties = {
        "ai.jwt.signing_key=",
        "TB_CORE_BASE_URL=" + AbstractOnPremiseAiControllerTest.ORIGIN
})
public abstract class AbstractOnPremiseAiControllerTest extends AbstractAiControllerTest {

    protected static final String AI_TOKEN = "on-premise-ai-token";
    protected static final String ORIGIN = "https://tb.example.org";

    @MockitoSpyBean
    protected SubscriptionService subscriptionService;
    @Autowired
    protected ProjectInfo projectInfo;

    protected void givenSubscriptionProvidesAiToken() {
        doReturn(AI_TOKEN).when(subscriptionService).getAiToken();
    }

    @Override
    protected void assertTokenProviderBelongsToTenantAdmin(TbAiClient.TokenProvider tokenProvider) {
        assertThat(tokenProvider.getToken()).isEqualTo(AI_TOKEN);
        assertThat(tokenProvider.getAdditionalInfo()).containsExactlyInAnyOrderEntriesOf(Map.of(
                TbAiHeaders.USER_ID, tenantAdminUserId.getId().toString(),
                TbAiHeaders.TENANT_ID, tenantId.getId().toString(),
                TbAiHeaders.TB_VERSION, projectInfo.getProjectVersion(),
                HttpHeaders.ORIGIN, ORIGIN
        ));
    }

}
