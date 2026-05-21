/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.dashboard.filter;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DashboardStatefulRoot.Validator.class)
public @interface DashboardStatefulRoot {

    String message() default "invalid root entity configuration";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<DashboardStatefulRoot, DashboardStatefulRootFilter> {

        @Override
        public boolean isValid(DashboardStatefulRootFilter f, ConstraintValidatorContext ctx) {
            if (f == null) {
                return true;
            }
            if (f.getRootEntity() == null && !f.isRootStateEntity()) {
                ctx.disableDefaultConstraintViolation();
                ctx.buildConstraintViolationWithTemplate("must not be null when 'rootStateEntity' is false")
                        .addPropertyNode("rootEntity")
                        .addConstraintViolation();
                return false;
            }
            return true;
        }

    }

}
