/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.controller.AbstractWebsocketTest;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@DaoSqlTest
public class AbstractLwM2MIntegrationTest extends AbstractWebsocketTest {

    protected DeviceProfile deviceProfile;
    protected ScheduledExecutorService executor;
    protected TbTestWebSocketClient wsClient;

    @Before
    public void beforeTest() throws Exception {
        executor = Executors.newScheduledThreadPool(10);
        loginTenantAdmin();

        String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "3.xml"};
        for (String resourceName : resources) {
            TbResource lwModel = new TbResource();
            lwModel.setResourceType(ResourceType.LWM2M_MODEL);
            lwModel.setTitle(resourceName);
            lwModel.setFileName(resourceName);
            lwModel.setTenantId(tenantId);
            byte[] bytes = IOUtils.toByteArray(AbstractLwM2MIntegrationTest.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName));
            lwModel.setData(Base64.getEncoder().encodeToString(bytes));
            lwModel = doPostWithTypedResponse("/api/resource", lwModel, new TypeReference<>(){});
            Assert.assertNotNull(lwModel);
        }
        wsClient = buildAndConnectWebSocketClient();
    }

    protected void createDeviceProfile(String transportConfiguration) throws Exception {
        deviceProfile = new DeviceProfile();

        deviceProfile.setName("LwM2M No Security");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription(deviceProfile.getName());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(JacksonUtil.fromString(transportConfiguration, Lwm2mDeviceProfileTransportConfiguration.class));
        deviceProfile.setProfileData(deviceProfileData);

        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
    }

    @After
    public void after() {
        executor.shutdownNow();
        wsClient.close();
    }

}
