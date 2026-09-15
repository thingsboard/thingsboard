// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.resource.TbResourceDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.UUID;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = ResourceDataValidator.class)
class ResourceDataValidatorTest {

    @MockitoBean
    TbResourceDao resourceDao;
    @MockitoBean
    WidgetTypeDao widgetTypeDao;
    @MockitoBean
    TenantService tenantService;
    @MockitoBean
    TbTenantProfileCache tenantProfileCache;
    @MockitoSpyBean
    ResourceDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @Test
    void testValidateNameInvocation() {
        TbResource resource = new TbResource();
        resource.setTitle("rss");
        resource.setResourceType(ResourceType.PKCS_12);
        resource.setFileName("cert.pem");
        resource.setResourceKey("19_1.0");
        resource.setTenantId(tenantId);

        validator.validateDataImpl(tenantId, resource);
        verify(validator).validateString("Resource title", resource.getTitle());
    }

}
