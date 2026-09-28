// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.thymeleaf.templatemode.TemplateMode.HTML;
import static org.thymeleaf.templatemode.TemplateMode.TEXT;
import static org.thymeleaf.templatemode.TemplateMode.XML;

public class ThymeleafUtil {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern IMAGE_SRC_PATTERN = Pattern.compile("<img[^>]+src=[\"']\\$\\{([^\"'}]+)}[\"']", Pattern.CASE_INSENSITIVE);
    private static final TemplateEngine htmlClassEngine;
    private static final TemplateEngine htmlStringEngine;
    private static final TemplateEngine textStringEngine;
    private static final TemplateEngine svgClassEngine;

    static {
        htmlClassEngine = new TemplateEngine();
        ClassLoaderTemplateResolver classResolver = new ClassLoaderTemplateResolver();
        classResolver.setPrefix("/");
        classResolver.setSuffix(".html");
        classResolver.setTemplateMode(HTML);
        classResolver.setOrder(1);
        classResolver.setCharacterEncoding(UTF_8);
        htmlClassEngine.setTemplateResolver(classResolver);

        htmlStringEngine = new TemplateEngine();
        StringTemplateResolver stringResolver = new StringTemplateResolver();
        stringResolver.setTemplateMode(HTML);
        stringResolver.setResolvablePatterns(Set.of("*"));
        stringResolver.setCacheable(false);
        stringResolver.setOrder(2);
        htmlStringEngine.setTemplateResolver(stringResolver);

        textStringEngine = new TemplateEngine();
        StringTemplateResolver textStringResolver = new StringTemplateResolver();
        textStringResolver.setTemplateMode(TEXT);
        textStringResolver.setResolvablePatterns(Set.of("*"));
        textStringResolver.setCacheable(false);
        textStringResolver.setOrder(3);
        textStringEngine.setTemplateResolver(textStringResolver);

        svgClassEngine = new TemplateEngine();
        ClassLoaderTemplateResolver svgClassResolver = new ClassLoaderTemplateResolver();
        svgClassResolver.setPrefix("/");
        svgClassResolver.setSuffix(".svg");
        svgClassResolver.setTemplateMode(XML);
        svgClassResolver.setOrder(4);
        svgClassResolver.setCharacterEncoding(UTF_8);
        svgClassEngine.setTemplateResolver(svgClassResolver);
    }

    public static String renderFromHtmlTemplate(String templateHtml, Map<String, Object> variables) {
        Context context = new Context(Locale.getDefault(), variables);
        return htmlClassEngine.process(templateHtml, context);
    }

    public static String renderFromTextString(String html, Map<String, Object> variables) {
        populateMissingImagePlaceholders(html, variables);

        Context context = new Context();
        context.setVariables(variables);

        return textStringEngine.process(convertToThymeleafInline(sanitize(html)), context);
    }

    private static void populateMissingImagePlaceholders(String html, Map<String, Object> variables) {
        Matcher matcher = IMAGE_SRC_PATTERN.matcher(html);

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);

            if (value == null || (value instanceof String str && str.isEmpty())) {
                variables.put(key, "noImage");
            }
        }
    }

    public static String renderFromSvgTemplate(String templateSvg, Map<String, Object> variables) {
        Context context = new Context(Locale.getDefault(), variables);
        return svgClassEngine.process(templateSvg, context);
    }

    public static String convertToThymeleafInline(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String var = matcher.group(1).trim();

            String replacement;
            switch (var) {
                case "pageNumber":
                    replacement = "<span class=\"page-number\"></span>";
                    break;
                case "totalPages":
                    replacement = "<span class=\"page-count\"></span>";
                    break;
                default:
                    String safeKey = normalizeVariableName(var);
                    replacement = "[(${" + safeKey + "})]";
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public static String normalizeVariableName(String var) {
        return var.trim().replaceAll("\\s+", "_");
    }

    private static String sanitize(String html) {
        return html.replace("\n", "");
    }
}
