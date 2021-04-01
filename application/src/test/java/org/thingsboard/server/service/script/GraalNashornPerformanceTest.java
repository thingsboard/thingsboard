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
package org.thingsboard.server.service.script;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.usagestats.TbApiUsageClient;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.UUID;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class GraalNashornPerformanceTest {

    NashornJsInvokeService nashornJsInvokeService;

    private final String FUNCTION =
            "var data = {devaName: 'TBD01', msg: {temp: 42, humidity: 77}};\n" +
            "var deviceName = data.devName;\n" +
            "var deviceType = 'thermostat';\n" +
            "\n" +
            "var result = {\n" +
            "   deviceName: deviceName,\n" +
            "   deviceType: deviceType,\n" +
            "   telemetry: {\n" +
            "       temperature: data.msg.temp,\n" +
            "       humidity: data.msg.humidity\n" +
            "   }, \n" +
            "   metadata: generateMetadata()\n" +
            "};\n" +
            "\n" +
            "function generateMetadata() {\n" +
            "   var metadata = {reason: 'created to test function'," +
                    "data: {msgNumber: 0, nextMsg: false}};\n" +
            "   return metadata;\n" +
            "}\n" +
            "\n" +
            "return result;";

    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    @Mock
    TbApiUsageStateService tbApiUsageStateService;
    @Mock
    TbApiUsageClient tbApiUsageClient;
    @Mock
    ApiUsageState apiUsageState;

    @Test
    public void graalPerformanceTest() {
        var executorService = new JsExecutorService();
        executorService.init();
        GraalJsInvokeService graalJsInvokeService = new GraalJsInvokeService(tbApiUsageStateService, tbApiUsageClient, executorService);
        graalJsInvokeService.initEngine();

        Mockito.when(tbApiUsageStateService.getApiUsageState(tenantId)).thenReturn(apiUsageState);
        Mockito.when(apiUsageState.isJsExecEnabled()).thenReturn(true);

        try {
            long startTime = System.currentTimeMillis();
            UUID scriptId = graalJsInvokeService.eval(tenantId, JsScriptType.RULE_NODE_SCRIPT, FUNCTION).get();
            String result = (String) graalJsInvokeService.invokeFunction(tenantId, scriptId, "{}", "{}", "POST_TELEMETRY_REQUEST").get();
            long endTime = System.currentTimeMillis() - startTime;

            System.out.println("GraalJsInvokeService performance test.\nExecution time: " + endTime + "(ms)\nResult: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void nashornPerformanceTest() {
        var executorService = new JsExecutorService();
        executorService.init();
        nashornJsInvokeService = new NashornJsInvokeService(tbApiUsageStateService, tbApiUsageClient, executorService);
        nashornJsInvokeService.initEngine();

        Mockito.when(tbApiUsageStateService.getApiUsageState(tenantId)).thenReturn(apiUsageState);
        Mockito.when(apiUsageState.isJsExecEnabled()).thenReturn(true);

        try {
            long startTime = System.currentTimeMillis();
            UUID scriptId = nashornJsInvokeService.eval(tenantId, JsScriptType.RULE_NODE_SCRIPT, FUNCTION).get();
            String result = (String) nashornJsInvokeService.invokeFunction(tenantId, scriptId, "{}", "{}", "POST_TELEMETRY_REQUEST").get();
            long endTime = System.currentTimeMillis() - startTime;

            System.out.println("NashornJsInvokeService performance test.\nExecution time: " + endTime + "(ms)\nResult: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
