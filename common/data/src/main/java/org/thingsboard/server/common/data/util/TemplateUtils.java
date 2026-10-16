// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

    public static String processTemplate(String template, Map<String, String> context) {
        return TEMPLATE_PARAM_PATTERN.matcher(template).replaceAll(matchResult -> {
            String key = matchResult.group(1);
            if (!context.containsKey(key)) {
                return "\\" + matchResult.group();
            }
            String value = nullToEmpty(context.get(key));
            String function = removeStart(matchResult.group(2), ":");
            if (function != null) {
                if (FUNCTIONS.containsKey(function)) {
                    value = FUNCTIONS.get(function).apply(value);
                }
            }
            return Matcher.quoteReplacement(value);
        });
    }

}
