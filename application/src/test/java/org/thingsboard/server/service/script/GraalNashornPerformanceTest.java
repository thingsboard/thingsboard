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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.usagestats.TbApiUsageClient;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class GraalNashornPerformanceTest {
    private final int NUMBER_OF_INVOCATIONS = 1500;
    private final String FUNCTION =
            "var data = msg;\n" +
            "var metadata = metadata;\n" +
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
            "   metadata: metadata\n" +
            "};\n" +
            "\n" +
            "return result;";

    private final String data = "{\"devName\": \"TBD01\", \"msg\": {\"temp\": 11, \"humidity\": 77}}";
    private final String metadata = "{\"data\": 40}";

    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    @Mock
    TbApiUsageStateService tbApiUsageStateService;
    @Mock
    TbApiUsageClient tbApiUsageClient;
    @Mock
    ApiUsageState apiUsageState;

    @Before
    public void init() {
        Mockito.when(tbApiUsageStateService.getApiUsageState(tenantId)).thenReturn(apiUsageState);
        Mockito.when(apiUsageState.isJsExecEnabled()).thenReturn(true);
    }

    @Test
    public void graalPerformanceTest() throws Exception {
        var executorService = new JsExecutorService();
        executorService.init();
        GraalJsInvokeService graalJsInvokeService = new GraalJsInvokeService(tbApiUsageStateService, tbApiUsageClient, executorService);
        graalJsInvokeService.initEngine();

        printStatistics(testExecutor(graalJsInvokeService), "Graal Executor");
    }

    @Test
    public void nashornPerformanceTest() throws Exception {
        var executorService = new JsExecutorService();
        executorService.init();
        NashornJsInvokeService nashornJsInvokeService = new NashornJsInvokeService(tbApiUsageStateService, tbApiUsageClient, executorService);
        nashornJsInvokeService.initEngine();

        printStatistics(testExecutor(nashornJsInvokeService), "Nashorn Executor");
    }

    private void printStatistics(List<Long> allIterationsResultTime, String executorName) {
        var statistics = allIterationsResultTime.stream().mapToLong(x -> x).summaryStatistics();
        System.out.println(executorName + "\nNumber of executions: " + statistics.getCount() +
                "\nTotal time: " + statistics.getSum() +
                "(ns)\nMinimal time: " + statistics.getMin() +
                "(ns)\nMaximum time: " + statistics.getMax() +
                "(ns)\nAverage time: " + statistics.getAverage() + "(ns)");
    }

    private List<Long> testExecutor(AbstractLocalJsInvokeService executor) throws Exception {
        int counter = 0;
        long startTime;
        long endTime;
        List<Long> allIterationsResultTime = new ArrayList<>(NUMBER_OF_INVOCATIONS);
        UUID scriptId = executor.eval(tenantId, JsScriptType.RULE_NODE_SCRIPT, FUNCTION).get();
        while (counter < NUMBER_OF_INVOCATIONS) {
            startTime = System.nanoTime();
            executor.invokeFunction(tenantId, scriptId, data, metadata, "POST_TELEMETRY_REQUEST").get();
            endTime = System.nanoTime() - startTime;
            allIterationsResultTime.add(endTime);
            counter++;
        }

        return allIterationsResultTime;
    }

}
