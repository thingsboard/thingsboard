// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.headers")
@Data
public class HttpSecurityHeadersProperties {

    private XContentTypeOptions xContentTypeOptions = new XContentTypeOptions();
    private ReferrerPolicy referrerPolicy = new ReferrerPolicy();
    private XFrameOptions xFrameOptions = new XFrameOptions();
    private ContentSecurityPolicy contentSecurityPolicy = new ContentSecurityPolicy();

    @Data
    public static class XContentTypeOptions {
        private boolean enabled = true;
    }

    @Data
    public static class ReferrerPolicy {
        private boolean enabled = true;
        private String value = "strict-origin-when-cross-origin";
    }

    @Data
    public static class XFrameOptions {
        private boolean enabled = false;
        private String value = "SAMEORIGIN";
    }

    @Data
    public static class ContentSecurityPolicy {
        private boolean enabled = false;
        private String value = "";
        private boolean reportOnly = false;
    }

}
