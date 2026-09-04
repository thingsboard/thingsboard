// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.limit.RateLimitUtil;
import org.thingsboard.server.common.data.validation.RateLimit;

@Slf4j
public class RateLimitValidator implements ConstraintValidator<RateLimit, String>  {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return value == null || RateLimitUtil.isValid(value);
    }

}
