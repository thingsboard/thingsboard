/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class NoXssValidator implements ConstraintValidator<NoXss, Object> {
    private static final AntiSamy xssChecker = new AntiSamy();
    private static Policy xssPolicy;

    @Override
    public void initialize(NoXss constraintAnnotation) {
        if (xssPolicy == null) {
            try {
                xssPolicy = Policy.getInstance(Resources.getResource("xss-policy.xml"));
            } catch (Exception e) {
                log.error("Failed to set xss policy: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if (!(value instanceof String) || ((String) value).isEmpty() || xssPolicy == null) {
            return true;
        }

        try {
            return xssChecker.scan((String) value, xssPolicy).getNumberOfErrors() == 0;
        } catch (ScanException | PolicyException e) {
            return false;
        }
    }
}
