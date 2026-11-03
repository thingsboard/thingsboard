// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpSecurityHeadersCustomizer {

    private final HttpSecurityHeadersProperties properties;

    public void customize(HeadersConfigurer<?> headers) {
        if (properties.getXContentTypeOptions().isEnabled()) {
            headers.contentTypeOptions(config -> {});
        }

        if (properties.getReferrerPolicy().isEnabled()) {
            headers.addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", properties.getReferrerPolicy().getValue()));
        }

        if (properties.getXFrameOptions().isEnabled()) {
            String value = properties.getXFrameOptions().getValue();
            if ("DENY".equalsIgnoreCase(value)) {
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
            } else {
                if (!"SAMEORIGIN".equalsIgnoreCase(value)) {
                    log.warn("Unrecognized X-Frame-Options value '{}', falling back to SAMEORIGIN. Valid values: DENY, SAMEORIGIN", value);
                }
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
            }
        }

        if (properties.getContentSecurityPolicy().isEnabled() && StringUtils.hasText(properties.getContentSecurityPolicy().getValue())) {
            headers.contentSecurityPolicy(csp -> {
                csp.policyDirectives(properties.getContentSecurityPolicy().getValue());
                if (properties.getContentSecurityPolicy().isReportOnly()) {
                    csp.reportOnly();
                }
            });
        }

    }

}
