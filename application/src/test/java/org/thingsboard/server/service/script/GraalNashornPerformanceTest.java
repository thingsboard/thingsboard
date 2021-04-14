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
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.usagestats.TbApiUsageClient;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class GraalNashornPerformanceTest {
    private final int NUMBER_OF_INVOCATIONS = 100000;

    private static final String FUNCTION = "var relations = [];\n" +
            "\n" +
            "switch (msg.type) {\n" +
            "    case 'Low Battery':\n" +
            "        if (metadata.lowBattEmailEnabled === 'true'){\n" +
            "            relations.push('Low Battery Email');\n" +
            "        }\n" +
            "        if (metadata.lowBattSmsEnabled === 'true'){\n" +
            "            relations.push('Low Battery SMS');\n" +
            "        }\n" +
            "        break;\n" +
            "    case 'Low Temperature':\n" +
            "        if (metadata.lowTempEmailEnabled === 'true'){\n" +
            "            relations.push('Low Temperature Email');\n" +
            "        }\n" +
            "        if (metadata.lowTempSmsEnabled === 'true'){\n" +
            "            relations.push('Low Temperature SMS');\n" +
            "        }\n" +
            "        break;\n" +
            "    case 'Device Inactive':\n" +
            "        if (metadata.inactivityEmailEnabled === 'true'){\n" +
            "            relations.push('Device Inactive Email');\n" +
            "        }\n" +
            "        if (metadata.inactivitySmsEnabled === 'true'){\n" +
            "            relations.push('Device Inactive SMS');\n" +
            "        }\n" +
            "        break;        \n" +
            "    case 'Daily Consumption Threshold Exceeded':\n" +
            "        if (metadata.dailyConsumptionEmailEnabled === 'true'){\n" +
            "            relations.push('Daily Consumption Email');\n" +
            "        }\n" +
            "        if (metadata.dailyConsumptionSmsEnabled === 'true'){\n" +
            "            relations.push('Daily Consumption SMS');\n" +
            "        }\n" +
            "        break; \n" +
            "    case 'Weekly Consumption Threshold Exceeded':\n" +
            "        if (metadata.weeklyConsumptionEmailEnabled === 'true'){\n" +
            "            relations.push('Weekly Consumption Email');\n" +
            "        }\n" +
            "        if (metadata.weeklyConsumptionSmsEnabled === 'true'){\n" +
            "            relations.push('Weekly Consumption SMS');\n" +
            "        }\n" +
            "        break;\n" +
            "    case 'Leakage Detected':\n" +
            "        if (metadata.leakageEmailEnabled === 'true'){\n" +
            "            relations.push('Leakage Detected Email');\n" +
            "        }\n" +
            "        if (metadata.leakageSmsEnabled === 'true'){\n" +
            "            relations.push('Leakage Detected SMS');\n" +
            "        }\n" +
            "        break;        \n" +
            "    default:\n" +
            "        relations.push('Other');\n" +
            "}\n" +
            "\n" +
            "return relations;";

    private static final String FUNCTION2 = "var result = [];\n" +
            "switch (metadata.deviceType) {\n" +
            "    case 'FSL_Telemetry': \n" +
            "        result = ['Asset'];\n" +
            "        break;\n" +
            "    default:\n" +
            "        result = ['Asset'];\n" +
            "}\n" +
            "return result;";

    private final String data = "{\"type\": \"Weekly Consumption Threshold Exceeded\", \"msg\": {\"temp\": %s, \"humidity\": %s}}";
    private final String metadata = "{\"weeklyConsumptionSmsEnabled\": \"true\", \"deviceType\": \"FSL_Speed\"}";

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
        var executor = Executors.newSingleThreadExecutor();
        try {
            var executorService = new JsExecutorService();
            executorService.init();
            GraalJsInvokeService graalJsInvokeService = new GraalJsInvokeService(tbApiUsageStateService, tbApiUsageClient, executorService);
            graalJsInvokeService.initSandbox(executor);
            ReflectionTestUtils.setField(graalJsInvokeService, "useJsSandbox", true);

            printStatistics(testExecutor(graalJsInvokeService), "Graal Executor");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void nashornPerformanceTest() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try {
            var executorService = new JsExecutorService();
            executorService.init();
            NashornJsInvokeService nashornJsInvokeService = new NashornJsInvokeService(tbApiUsageStateService, tbApiUsageClient, executorService);
            nashornJsInvokeService.initSandbox(executor);
            ReflectionTestUtils.setField(nashornJsInvokeService, "useJsSandbox", true);
            printStatistics(testExecutor(nashornJsInvokeService), "Nashorn Executor");
        } finally {
            executor.shutdownNow();
        }
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
        List<Long> allIterationsResultTime = new ArrayList<>(NUMBER_OF_INVOCATIONS);
        UUID scriptId = executor.eval(tenantId, JsScriptType.RULE_NODE_SCRIPT, FUNCTION2).get();
        int temperature = 42;
        int humidity = 73;
        System.out.println(executor.invokeFunction(tenantId, scriptId, String.format(data, temperature++, humidity++), metadata, "POST_TELEMETRY_REQUEST").get());
        while (counter < NUMBER_OF_INVOCATIONS) {
            startTime = System.nanoTime();
            executor.invokeFunction(tenantId, scriptId, String.format(data, temperature++, humidity++), metadata, "POST_TELEMETRY_REQUEST").get();
            allIterationsResultTime.add(System.nanoTime() - startTime);
            counter++;
        }
        return allIterationsResultTime;
    }

}
