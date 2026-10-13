// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.nullToEmpty;
import static org.thingsboard.server.common.data.StringUtils.removeStart;

public class TemplateUtils {

    private static final Pattern TEMPLATE_PARAM_PATTERN = Pattern.compile("\\$\\{(.+?)(:[a-zA-Z]+)?}");

    private static final Map<String, UnaryOperator<String>> FUNCTIONS = Map.of(
            "upperCase", String::toUpperCase,
            "lowerCase", String::toLowerCase,
            "capitalize", StringUtils::capitalize
    );

    private TemplateUtils() {}

    public static String processTemplate(String template, Map<String, String> context, Map<String, UnaryOperator<String>> customFunctions) {
        return TEMPLATE_PARAM_PATTERN.matcher(template).replaceAll(matchResult -> {
            String key = matchResult.group(1);
            String functionName = removeStart(matchResult.group(2), ":");
            if (!context.containsKey(key)) {
                if (functionName == null || customFunctions == null || !customFunctions.containsKey(functionName)) {
                    return "\\" + matchResult.group();
                }
            }

            String value = nullToEmpty(context.get(key));
            if (functionName != null) {
                UnaryOperator<String> function = FUNCTIONS.get(functionName);
                if (function != null) {
                    value = function.apply(value);
                } else if (customFunctions != null) {
                    function = customFunctions.get(functionName);
                    if (function != null) {
                        value = function.apply(key);
                        value = processTemplate(value, context, null);
                    }
                }
            }
            return Matcher.quoteReplacement(value);
        });
    }

}
