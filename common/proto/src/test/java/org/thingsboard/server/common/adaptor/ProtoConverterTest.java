/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.adaptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.transport.TransportProtos;

public class ProtoConverterTest {

    @Test
    public void testEmptyStringMetricIsFilteredOut() throws Exception {

        TransportProtos.KeyValueProto validKv = TransportProtos.KeyValueProto.newBuilder()
                .setKey("temperature")
                .setType(TransportProtos.KeyValueType.DOUBLE_V)
                .setDoubleV(25.5)
                .build();

        TransportProtos.KeyValueProto invalidKv = TransportProtos.KeyValueProto.newBuilder()
                .setKey("status")
                .setType(TransportProtos.KeyValueType.STRING_V)
                .setStringV("")
                .build();

        TransportProtos.PostAttributeMsg msg = TransportProtos.PostAttributeMsg.newBuilder()
                .addKv(validKv)
                .addKv(invalidKv)
                .build();

        TransportProtos.PostAttributeMsg resultMsg = ProtoConverter.validatePostAttributeMsg(msg);

        Assertions.assertEquals(1, resultMsg.getKvCount(), "Only the valid metric should remain");
        Assertions.assertEquals("temperature", resultMsg.getKv(0).getKey(), "The surviving metric must be the valid one");
    }
}