/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.system;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Andrew Shvayka
 */
public abstract class BaseHttpDeviceApiTest extends AbstractControllerTest {

    private static final AtomicInteger idSeq = new AtomicInteger(new Random(System.currentTimeMillis()).nextInt());

    protected Device device;
    protected DeviceCredentials deviceCredentials;

    @Before
    public void before() throws Exception {
        loginTenantAdmin();
        device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    @Test
    public void testGetAttributes() throws Exception {
        doGetAsync("/api/v1/" + "WRONG_TOKEN" + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isUnauthorized());
        doGetAsync("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isNotFound());

        Map<String, String> attrMap = new HashMap<>();
        attrMap.put("keyA", "valueA");
        mockMvc.perform(
                asyncDispatch(doPost("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes", attrMap, new String[]{}).andReturn()))
                .andExpect(status().isOk());
        doGetAsync("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isOk());
    }

    protected ResultActions doGetAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest;
        getRequest = get(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

    protected ResultActions doPostAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest = post(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

}
