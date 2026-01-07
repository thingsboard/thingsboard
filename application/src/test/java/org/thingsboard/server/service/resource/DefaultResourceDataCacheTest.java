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
package org.thingsboard.server.service.resource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.GeneralFileDescriptor;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceDataInfo;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.resource.TbResourceDataCache;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DaoSqlTest
public class DefaultResourceDataCacheTest extends AbstractControllerTest {

    @MockitoSpyBean
    private ResourceService resourceService;
    @Autowired
    private TbResourceService tbResourceService;
    @MockitoSpyBean
    private TbResourceDataCache resourceDataCache;

    @Test
    public void testGetCachedResourceData() throws Exception {
        loginTenantAdmin();

        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setTitle("File for AI request");
        resource.setResourceType(ResourceType.GENERAL);
        resource.setFileName("myTestJson.json");
        GeneralFileDescriptor descriptor = new GeneralFileDescriptor("application/json");
        resource.setDescriptorValue(descriptor);
        byte[] data = "This is a test prompt for AI request.".getBytes();
        resource.setData(data);
        TbResourceInfo savedResource = tbResourceService.save(resource);
        verify(resourceDataCache, timeout(2000).times(1)).evictResourceData(tenantId, savedResource.getId());

        TbResourceDataInfo cachedData = resourceDataCache.getResourceDataInfoAsync(tenantId, savedResource.getId()).get();
        assertThat(cachedData.getData()).isEqualTo(data);
        assertThat(JacksonUtil.treeToValue(cachedData.getDescriptor(), GeneralFileDescriptor.class)).isEqualTo(descriptor);
        verify(resourceService).getResourceDataInfo(tenantId, savedResource.getId());

        // retrieve resource data second time
        clearInvocations(resourceService);
        TbResourceDataInfo cachedData2 = resourceDataCache.getResourceDataInfoAsync(tenantId, savedResource.getId()).get();
        assertThat(cachedData2.getData()).isEqualTo(data);
        verifyNoMoreInteractions(resourceService);

        // delete resource, check cache
        TbResource resourceById = resourceService.findResourceById(tenantId, savedResource.getId());
        tbResourceService.delete(resourceById, true, null);
        verify(resourceDataCache, timeout(2000).times(2)).evictResourceData(tenantId, savedResource.getId());
        TbResourceDataInfo cachedDataAfterDeletion = resourceDataCache.getResourceDataInfoAsync(tenantId, savedResource.getId()).get();
        assertThat(cachedDataAfterDeletion).isEqualTo(null);
    }

}
