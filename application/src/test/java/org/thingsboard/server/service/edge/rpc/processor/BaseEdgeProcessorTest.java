/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor;

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;

import java.util.UUID;
import java.util.stream.Stream;

@Import(EdgeProcessorBeansConfiguration.class)
public abstract class BaseEdgeProcessorTest {

    @MockBean
    protected DataDecodingEncodingService dataDecodingEncodingService;

    protected EdgeId edgeId;
    protected TenantId tenantId;
    protected EdgeEvent edgeEvent;

    protected DashboardId getDashboardId(long expectedDashboardIdMSB, long expectedDashboardIdLSB) {
        DashboardId dashboardId;
        if (expectedDashboardIdMSB != 0 && expectedDashboardIdLSB != 0) {
            dashboardId = new DashboardId(new UUID(expectedDashboardIdMSB, expectedDashboardIdLSB));
        } else {
            dashboardId = new DashboardId(UUID.randomUUID());
        }
        return dashboardId;
    }

    protected RuleChainId getRuleChainId(long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        RuleChainId ruleChainId;
        if (expectedRuleChainIdMSB != 0 && expectedRuleChainIdLSB != 0) {
            ruleChainId = new RuleChainId(new UUID(expectedRuleChainIdMSB, expectedRuleChainIdLSB));
        } else {
            ruleChainId = new RuleChainId(UUID.randomUUID());
        }
        return ruleChainId;
    }

    protected static Stream<Arguments> provideParameters() {
        UUID dashoboardUUID = UUID.randomUUID();
        UUID ruleChaindUUID = UUID.randomUUID();
        return Stream.of(
                Arguments.of(EdgeVersion.V_3_3_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_3_3, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_4_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_6_0,
                        dashoboardUUID.getMostSignificantBits(),
                        dashoboardUUID.getLeastSignificantBits(),
                        ruleChaindUUID.getMostSignificantBits(),
                        ruleChaindUUID.getLeastSignificantBits())
        );
    }
}
