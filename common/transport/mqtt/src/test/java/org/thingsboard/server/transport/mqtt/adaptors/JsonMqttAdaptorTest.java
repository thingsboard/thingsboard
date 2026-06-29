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
package org.thingsboard.server.transport.mqtt.adaptors;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonMqttAdaptorTest {

    private final List<String> names = new ArrayList<>();
    private final AtomicBoolean allInvoked = new AtomicBoolean(false);

    private void parseClientKeys(JsonObject json) {
        JsonMqttAdaptor.parseAttributeScope(json, "clientKeys", names::addAll, () -> allInvoked.set(true));
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
    public void emptyArray_setsAll() {
        JsonObject json = new JsonObject();
        json.add("clientKeys", new JsonArray());
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
    public void jsonArray_setsNames() {
        JsonObject json = new JsonObject();
        JsonArray arr = new JsonArray();
        arr.add("a");
        arr.add("b");
        json.add("clientKeys", arr);
        parseClientKeys(json);
        assertFalse(allInvoked.get());
        assertEquals(List.of("a", "b"), names);
    }
}
