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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.tbel.AiTbelService;

import java.util.Map;

@Validated
@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/ai/tbel")
public class AiTbelController {

    private final AiTbelService aiTbelService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/chat")
    public JsonNode testScript(
            @Parameter(description = "Script language: JS or TBEL")
            @RequestParam(required = false) ScriptLanguage scriptLang,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Test TBEL request. See API call description above.")
            @RequestBody JsonNode inputParams) {

        if (scriptLang != null && !ScriptLanguage.TBEL.equals(scriptLang)) {
            return JacksonUtil.newObjectNode()
                    .put("status", "error")
                    .put("error", "Only TBEL script language is supported.");
        }

        String script = inputParams.get("script").asText();
        String scriptType = inputParams.get("scriptType").asText();
        String[] argNames = JacksonUtil.treeToValue(inputParams.get("argNames"), String[].class);
        String msgJson = inputParams.get("msg").asText();
        String msgType = inputParams.get("msgType").asText();

        JsonNode metadataNode = inputParams.get("metadata");
        Map<String, String> metadata = JacksonUtil.convertValue(metadataNode, new TypeReference<>() {});
        String metadataJson = JacksonUtil.toString(metadata);

        return aiTbelService.verifyScript(script, scriptType, argNames, msgType, msgJson, metadataJson);
    }
}
