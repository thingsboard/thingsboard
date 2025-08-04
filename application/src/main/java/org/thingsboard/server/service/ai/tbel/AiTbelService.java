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
package org.thingsboard.server.service.ai.tbel;

import com.fasterxml.jackson.databind.JsonNode;

public interface AiTbelService {
    /**
     * Analyze TBEL script via AI and return structured diff result.
     *
     * @return JsonNode containing:
     *  - updated msg with comments if changed,
     *  - or "unchanged",
     *  - or error info.
     */
    JsonNode verifyScript(String script, String scriptType, String[] argNames,
                          String msgType, String msgJson, String metadataJson);
}
