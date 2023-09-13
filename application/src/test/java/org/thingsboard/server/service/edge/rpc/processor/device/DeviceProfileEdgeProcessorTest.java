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
package org.thingsboard.server.service.edge.rpc.processor.device;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;


@SpringBootTest(classes = {DeviceProfileEdgeProcessor.class})
class DeviceProfileEdgeProcessorTest extends AbstractDeviceProcessorTest {

    @SpyBean
    DeviceProfileEdgeProcessor deviceProfileEdgeProcessor;

    @ParameterizedTest
    @MethodSource("provideParameters")
    public void testDeviceProfileDefaultFields_notSendToEdgeOlder3_6_0IfNotAssigned(EdgeVersion edgeVersion, long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                                                                                    long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        updateDeviceProfileDefaultFields(expectedDashboardIdMSB, expectedDashboardIdLSB, expectedRuleChainIdMSB, expectedRuleChainIdLSB);

        edgeEvent.setEntityId(deviceProfileId.getId());

        DownlinkMsg downlinkMsg = deviceProfileEdgeProcessor.convertDeviceProfileEventToDownlink(edgeEvent, edgeId, edgeVersion);

        verify(downlinkMsg, expectedDashboardIdMSB, expectedDashboardIdLSB, expectedRuleChainIdMSB, expectedRuleChainIdLSB);
    }

}