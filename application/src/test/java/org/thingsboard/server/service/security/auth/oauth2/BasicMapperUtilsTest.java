/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth.oauth2;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class BasicMapperUtilsTest {
    private final String userName = "Test user";
    private final String token = "12345";
    private final String json = "{ \"user\": { \"name\": \"" + userName + "\" }, \"token\": \"" + token + "\" }";

    @Test
    public void testSimpleKey() {
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<>() {};
        HashMap<String, Object> attributes = JacksonUtil.fromString(json, typeRef);

        assertEquals(token, BasicMapperUtils.getStringAttributeByKey(attributes, "token"));
    }

    @Test
    public void testJsonPathKey() {
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<>() {};
        HashMap<String, Object> attributes = JacksonUtil.fromString(json, typeRef);

        assertEquals(token, BasicMapperUtils.getStringAttributeByKey(attributes, "$.token"));
        assertEquals(userName, BasicMapperUtils.getStringAttributeByKey(attributes, "$.user.name"));
    }

}