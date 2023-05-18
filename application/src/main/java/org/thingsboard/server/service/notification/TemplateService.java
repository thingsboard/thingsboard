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
package org.thingsboard.server.service.notification;

import com.google.common.base.Strings;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.commons.lang3.StringUtils.removeStart;

@Service
public class TemplateService {

    private static final Pattern TEMPLATE_PARAM_PATTERN = Pattern.compile("\\$\\{(.+?)(:[a-zA-Z]+)?}");
    private static final Pattern TEMPLATE_SCRIPT_PATTERN = Pattern.compile("#\\{(.+?)}");

    private static final Map<String, UnaryOperator<String>> SHORT_FUNCTIONS = Map.of(
            "upperCase", String::toUpperCase,
            "lowerCase", String::toLowerCase,
            "capitalize", StringUtils::capitalize
    );

    @Autowired(required = false)
    private TbelInvokeService tbelInvokeService;

    public String processTemplate(TenantId tenantId, String template, Map<String, String> context, Set<String> ignoredParams) {
        String result = TEMPLATE_PARAM_PATTERN.matcher(template).replaceAll(matchResult -> {
            String key = matchResult.group(1);
            if (!context.containsKey(key) || ignoredParams.contains(key)) {
                return "\\" + matchResult.group();
            }
            String value = nullToEmpty(context.get(key));
            String function = removeStart(matchResult.group(2), ":");
            if (function != null) {
                if (SHORT_FUNCTIONS.containsKey(function)) {
                    value = SHORT_FUNCTIONS.get(function).apply(value);
                }
            }
            return value;
        });

        if (tbelInvokeService != null) {
            result = TEMPLATE_SCRIPT_PATTERN.matcher(result).replaceAll(matchResult -> {
                  String script = matchResult.group(1);
                if (!ignoredParams.isEmpty() && ignoredParams.stream().anyMatch(script::contains)) {
                    return matchResult.group();
                }
                return processScript(tenantId, script, context);
            });
        }

        return result;
    }

    @SneakyThrows
    private String processScript(TenantId tenantId, String script, Map<String, String> context) {
        String scriptBody = "return " + script + ";";
        String[] argsNames = context.keySet().toArray(String[]::new);
        Object[] args = context.values().stream().map(Strings::nullToEmpty).toArray();
        try {
            UUID scriptId = tbelInvokeService.eval(tenantId, ScriptType.TEMPLATE_SCRIPT, scriptBody, argsNames).get();
            Object result = tbelInvokeService.invokeScript(tenantId, null, scriptId, args).get();
            return String.valueOf(result);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

}
