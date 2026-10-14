// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.edge.EdgeProfileDao;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = EdgeDataValidator.class)
class EdgeDataValidatorTest {

    @MockitoBean
    EdgeDao edgeDao;
    @MockitoBean
    TenantService tenantService;
    @MockitoBean
    CustomerDao customerDao;
    @MockitoBean
    EdgeProfileDao edgeProfileDao;
    @MockitoSpyBean
    EdgeDataValidator validator;
    @MockitoBean
    SubscriptionService subscriptionService;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    EdgeProfileId edgeProfileId = new EdgeProfileId(UUID.fromString("b6f236f0-5f0a-4c1a-9c7a-9a2f6a0e1a11"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    private Edge validEdge() {
        Edge edge = new Edge();
        edge.setName("Edge 007");
        edge.setType("Silos");
        edge.setSecret("secret");
        edge.setRoutingKey("53c56104-d302-4d6e-97f5-a7a99c7effdc");
        edge.setTenantId(tenantId);
        edge.setEdgeLicenseKey("abcd");
        edge.setCloudEndpoint("https://localhost");
        edge.setEdgeProfileId(edgeProfileId);
        return edge;
    }

    @Test
    void testValidateNameInvocation() {
        EdgeProfile profile = new EdgeProfile(edgeProfileId);
        profile.setTenantId(tenantId);
        willReturn(profile).given(edgeProfileDao).findById(tenantId, edgeProfileId.getId());

        Edge edge = validEdge();

        validator.validateDataImpl(tenantId, edge);
        verify(validator).validateString("Edge name", edge.getName());
        verify(validator).validateString("Edge type", edge.getType());
    }

    @Test
    void testValidateRejectsMissingEdgeProfileId() {
        Edge edge = validEdge();
        edge.setEdgeProfileId(null);

        assertThrows(DataValidationException.class, () -> validator.validateDataImpl(tenantId, edge));
    }

    @Test
    void testValidateRejectsEdgeProfileFromAnotherTenant() {
        TenantId otherTenantId = TenantId.fromUUID(UUID.fromString("1a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d"));
        EdgeProfile profile = new EdgeProfile(edgeProfileId);
        profile.setTenantId(otherTenantId);
        willReturn(profile).given(edgeProfileDao).findById(tenantId, edgeProfileId.getId());

        Edge edge = validEdge();

        assertThrows(DataValidationException.class, () -> validator.validateDataImpl(tenantId, edge));
    }

}
