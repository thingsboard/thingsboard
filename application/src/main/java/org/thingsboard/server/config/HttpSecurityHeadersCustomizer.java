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
