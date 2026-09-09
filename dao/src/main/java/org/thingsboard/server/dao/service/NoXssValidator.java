// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
public class NoXssValidator implements ConstraintValidator<NoXss, Object> {

    private static final Pattern JS_TEMPLATE_PATTERN = Pattern.compile("\\{\\{.*}}", Pattern.DOTALL);

    private static final AntiSamy xssChecker = new AntiSamy();
    private static final Policy xssPolicy;

    static {
        xssPolicy = Optional.ofNullable(NoXssValidator.class.getClassLoader().getResourceAsStream("xss-policy.xml"))
                .map(inputStream -> {
                    try {
                        return Policy.getInstance(inputStream);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("XSS policy file not found"));
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        String stringValue;
        if (value instanceof CharSequence || value instanceof JsonNode) {
            stringValue = value.toString();
        } else {
            return true;
        }
        return isValid(stringValue);
    }

    public static boolean isValid(String stringValue) {
        if (stringValue.isEmpty()) {
            return true;
        }
        if (JS_TEMPLATE_PATTERN.matcher(stringValue).find()) {
            return false;
        }
        try {
            return xssChecker.scan(stringValue, xssPolicy).getNumberOfErrors() == 0;
        } catch (ScanException | PolicyException e) {
            return false;
        }
    }

}
