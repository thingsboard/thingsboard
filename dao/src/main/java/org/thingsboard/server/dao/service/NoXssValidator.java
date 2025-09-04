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
