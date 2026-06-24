/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.adaptor;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonConverterGatewayResponseTest {

    private TsKvProto kv(String key, long v) {
        return TsKvProto.newBuilder().setTs(1L).setKv(
                KeyValueProto.newBuilder().setKey(key).setType(KeyValueType.LONG_V).setLongV(v).build()).build();
    }

    @Test
    public void separateScopes_keepsScopeLabels_keyNames_andOverlap() {
        GetAttributeResponseMsg msg = GetAttributeResponseMsg.newBuilder()
                .setRequestId(1)
                .setSeparateScopesResponse(true)
                .addClientAttributeList(kv("test", 27))
                .addSharedAttributeList(kv("test", 99))
                .build();

        JsonObject out = JsonConverter.getJsonObjectForGateway("DeviceA", msg);

        assertThat(out.get("id").getAsInt()).isEqualTo(1);
        assertThat(out.get("device").getAsString()).isEqualTo("DeviceA");
        assertThat(out.getAsJsonObject("client").get("test").getAsLong()).isEqualTo(27);
        assertThat(out.getAsJsonObject("shared").get("test").getAsLong()).isEqualTo(99);
        assertThat(out.has("value")).isFalse();
        assertThat(out.has("values")).isFalse();
    }

    @Test
    public void separateScopes_omitsEmptyScope() {
        GetAttributeResponseMsg msg = GetAttributeResponseMsg.newBuilder()
                .setRequestId(2).setSeparateScopesResponse(true)
                .addSharedAttributeList(kv("s", 1)).build();
        JsonObject out = JsonConverter.getJsonObjectForGateway("DeviceA", msg);
        assertThat(out.has("client")).isFalse();
        assertThat(out.getAsJsonObject("shared").get("s").getAsLong()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void legacy_singleValue_unchanged() {
        GetAttributeResponseMsg msg = GetAttributeResponseMsg.newBuilder()
                .setRequestId(3).setSeparateScopesResponse(false)
                .setIsMultipleAttributesRequest(false)
                .addSharedAttributeList(kv("s", 5)).build();
        JsonObject out = JsonConverter.getJsonObjectForGateway("DeviceA", msg);
        assertThat(out.get("value").getAsLong()).isEqualTo(5);
        assertThat(out.has("shared")).isFalse();
    }
}
