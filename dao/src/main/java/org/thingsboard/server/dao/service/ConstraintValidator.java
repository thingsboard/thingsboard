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
package org.thingsboard.server.dao.service;

import com.google.common.collect.Iterators;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.internal.cfg.context.DefaultConstraintMapping;
import org.hibernate.validator.internal.engine.ConfigurationImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoNullChar;
import org.thingsboard.server.common.data.validation.NoXss;
import org.thingsboard.server.common.data.validation.RateLimit;
import org.thingsboard.server.common.data.validation.ValidJsonSchema;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ConstraintValidator {

    private static Validator fieldsValidator;

    static {
        initializeValidators();
    }

    public static void validateFields(Object data) {
        validateFields(data, "Validation error: ");
    }

    public static void validateFields(Object data, String errorPrefix) {
        Set<ConstraintViolation<Object>> constraintsViolations = fieldsValidator.validate(data);
        if (!constraintsViolations.isEmpty()) {
            throw new DataValidationException(errorPrefix + getErrorMessage(constraintsViolations));
        }
    }

    public static String getErrorMessage(Collection<ConstraintViolation<Object>> constraintsViolations) {
        return constraintsViolations.stream()
                .map(ConstraintValidator::getErrorMessage)
                .distinct().sorted().collect(Collectors.joining(", "));
    }

    public static String getErrorMessage(ConstraintViolation<Object> constraintViolation) {
        ConstraintDescriptor<?> constraintDescriptor = constraintViolation.getConstraintDescriptor();
        String property = (String) constraintDescriptor.getAttributes().get("fieldName");
        if (StringUtils.isEmpty(property) && !(constraintDescriptor.getAnnotation() instanceof AssertTrue)) {
            property = Iterators.getLast(constraintViolation.getPropertyPath().iterator()).toString();
        }

        String error = "";
        if (StringUtils.isNotEmpty(property)) {
            error += property + " ";
        }
        error += constraintViolation.getMessage();
        return error;
    }

    private static void initializeValidators() {
        HibernateValidatorConfiguration validatorConfiguration = Validation.byProvider(HibernateValidator.class).configure();

        ConstraintMapping constraintMapping = getCustomConstraintMapping();
        validatorConfiguration.addMapping(constraintMapping);

        try (var validatorFactory = validatorConfiguration.buildValidatorFactory()) {
            fieldsValidator = validatorFactory.getValidator();
        }
    }

    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.setConfigurationInitializer(configuration -> {
            ((ConfigurationImpl) configuration).addMapping(getCustomConstraintMapping());
        });
        return localValidatorFactoryBean;
    }

    private static ConstraintMapping getCustomConstraintMapping() {
        ConstraintMapping constraintMapping = new DefaultConstraintMapping(null);
        constraintMapping.constraintDefinition(NoXss.class).validatedBy(NoXssValidator.class);
        constraintMapping.constraintDefinition(Length.class).validatedBy(StringLengthValidator.class);
        constraintMapping.constraintDefinition(RateLimit.class).validatedBy(RateLimitValidator.class);
        constraintMapping.constraintDefinition(NoNullChar.class).validatedBy(NoNullCharValidator.class);
        constraintMapping.constraintDefinition(ValidJsonSchema.class).validatedBy(JsonSchemaValidator.class);
        return constraintMapping;
    }

}
