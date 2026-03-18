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
