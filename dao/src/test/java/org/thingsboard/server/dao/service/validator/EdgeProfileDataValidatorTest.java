// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeProfileDao;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = EdgeProfileDataValidator.class)
class EdgeProfileDataValidatorTest {

    @MockitoBean
    EdgeProfileDao edgeProfileDao;
    @MockitoBean
    TenantService tenantService;
    @MockitoBean
    CustomerDao customerDao;
    @MockitoBean
    RuleChainService ruleChainService;
    @MockitoBean
    EntityGroupService entityGroupService;
    @MockitoBean
    IntegrationService integrationService;
    @MockitoBean
    SchedulerEventService schedulerEventService;
    @Autowired
    EdgeProfileDataValidator validator;

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    IntegrationId integrationId = new IntegrationId(UUID.fromString("b6f236f0-5f0a-4c1a-9c7a-9a2f6a0e1a11"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    private EdgeProfile profileWithIntegration() {
        EdgeProfile profile = new EdgeProfile();
        profile.setTenantId(tenantId);
        profile.setName("With integration");
        profile.getProfileData().getIntegrationIds().add(integrationId);
        return profile;
    }

    private Integration integration(boolean edgeTemplate) {
        Integration integration = new Integration(integrationId);
        integration.setTenantId(tenantId);
        integration.setName("Integration");
        integration.setEdgeTemplate(edgeTemplate);
        return integration;
    }

    @Test
    void rejectsIntegrationThatIsNotAnEdgeTemplate() {
        willReturn(integration(false)).given(integrationService).findIntegrationById(tenantId, integrationId);

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, profileWithIntegration()))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("edge template integrations");
    }

    @Test
    void acceptsEdgeTemplateIntegration() {
        willReturn(integration(true)).given(integrationService).findIntegrationById(tenantId, integrationId);

        assertThatCode(() -> validator.validateDataImpl(tenantId, profileWithIntegration())).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingIntegration() {
        willReturn(null).given(integrationService).findIntegrationById(tenantId, integrationId);

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, profileWithIntegration()))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("non-existent integration");
    }
}
