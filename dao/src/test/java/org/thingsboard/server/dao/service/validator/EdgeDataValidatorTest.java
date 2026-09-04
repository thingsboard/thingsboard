// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.UUID;

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
    @MockitoSpyBean
    EdgeDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @Test
    void testValidateNameInvocation() {
        Edge edge = new Edge();
        edge.setName("Edge 007");
        edge.setType("Silos");
        edge.setSecret("secret");
        edge.setRoutingKey("53c56104-d302-4d6e-97f5-a7a99c7effdc");
        edge.setTenantId(tenantId);

        validator.validateDataImpl(tenantId, edge);
        verify(validator).validateString("Edge name", edge.getName());
        verify(validator).validateString("Edge type", edge.getType());
    }

}
