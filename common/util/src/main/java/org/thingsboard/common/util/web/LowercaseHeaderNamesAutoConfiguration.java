// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.common.util.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class LowercaseHeaderNamesAutoConfiguration {

    @Bean
    public FilterRegistrationBean<LowercaseHeaderNamesFilter> lowercaseHeaderNamesFilter() {
        FilterRegistrationBean<LowercaseHeaderNamesFilter> registration = new FilterRegistrationBean<>(new LowercaseHeaderNamesFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

}
