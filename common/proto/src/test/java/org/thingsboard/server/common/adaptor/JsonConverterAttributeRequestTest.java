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
package org.thingsboard.server.common.adaptor;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonConverterAttributeRequestTest {

    private final List<String> names = new ArrayList<>();
    private final AtomicBoolean allInvoked = new AtomicBoolean(false);

    private void parseClientKeys(JsonObject json) {
        JsonConverter.parseAttributeScope(json, "clientKeys", names::addAll, () -> allInvoked.set(true));
    }

    @Test
    public void fieldAbsent_doesNothing() {
        parseClientKeys(new JsonObject());
        assertTrue(names.isEmpty());
        assertFalse(allInvoked.get());
    }

    @Test
    public void fieldNull_doesNothing() {
        JsonObject json = new JsonObject();
        json.add("clientKeys", JsonNull.INSTANCE);
        parseClientKeys(json);
        assertTrue(names.isEmpty());
        assertFalse(allInvoked.get());
    }

    @Test
    public void emptyString_setsAll() {
        JsonObject json = new JsonObject();
        json.addProperty("clientKeys", "");
        parseClientKeys(json);
        assertTrue(allInvoked.get());
        assertTrue(names.isEmpty());
    }

    @Test
    public void blankString_setsAll() {
        JsonObject json = new JsonObject();
        json.addProperty("clientKeys", "   ");
        parseClientKeys(json);
        assertTrue(allInvoked.get());
        assertTrue(names.isEmpty());
    }

    @Test
    public void commaSeparatedString_setsNames() {
        JsonObject json = new JsonObject();
        json.addProperty("clientKeys", "a,b,c");
        parseClientKeys(json);
        assertFalse(allInvoked.get());
        assertEquals(List.of("a", "b", "c"), names);
    }

    @Test
    public void applyClientScope_all_winsOverNames() {
        GetAttributeRequestMsg.Builder b = GetAttributeRequestMsg.newBuilder();
        JsonConverter.applyClientScope(b, true, List.of("ignored"));
        assertThat(b.getAllClientAttributes()).isTrue();
        assertThat(b.getClientAttributeNamesList()).isEmpty();
    }

    @Test
    public void applyClientScope_names_addsNames() {
        GetAttributeRequestMsg.Builder b = GetAttributeRequestMsg.newBuilder();
        JsonConverter.applyClientScope(b, false, List.of("a", "b"));
        assertThat(b.getAllClientAttributes()).isFalse();
        assertThat(b.getClientAttributeNamesList()).containsExactly("a", "b");
    }

    @Test
    public void applyClientScope_nullOrEmpty_setsNothing() {
        GetAttributeRequestMsg.Builder b = GetAttributeRequestMsg.newBuilder();
        JsonConverter.applyClientScope(b, false, null);
        JsonConverter.applyClientScope(b, false, List.of());
        assertThat(b.getAllClientAttributes()).isFalse();
        assertThat(b.getClientAttributeNamesList()).isEmpty();
    }

    @Test
    public void applySharedScope_all_winsOverNames() {
        GetAttributeRequestMsg.Builder b = GetAttributeRequestMsg.newBuilder();
        JsonConverter.applySharedScope(b, true, null);
        assertThat(b.getAllSharedAttributes()).isTrue();
        assertThat(b.getSharedAttributeNamesList()).isEmpty();
    }

    @Test
    public void applySharedScope_names_addsNames() {
        GetAttributeRequestMsg.Builder b = GetAttributeRequestMsg.newBuilder();
        JsonConverter.applySharedScope(b, false, List.of("s1"));
        assertThat(b.getAllSharedAttributes()).isFalse();
        assertThat(b.getSharedAttributeNamesList()).containsExactly("s1");
    }
}
