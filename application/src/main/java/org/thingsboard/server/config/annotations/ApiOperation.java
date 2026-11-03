// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.config.annotations;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Operation
public @interface ApiOperation {

    @AliasFor(annotation = Operation.class, attribute = "summary")
    String value();

    @AliasFor(annotation = Operation.class, attribute = "description")
    String notes() default "";

    boolean hidden() default false;

    RequestBody requestBody() default @RequestBody;

    ApiResponse[] responses() default {};

}
