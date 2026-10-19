// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.common.util.SsrfProtectionValidator;

import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "service.type", havingValue = "tb-report")
public class ReportServiceConfiguration {

    @Value("${reports.ssrf_protection_enabled:false}")
    private boolean ssrfProtectionEnabled;

    @Value("${reports.ssrf_additional_blocked_hosts:}")
    private List<String> ssrfAdditionalBlockedHosts;

    @Value("${reports.ssrf_allowed_hosts:}")
    private List<String> ssrfAllowedHosts;

    @PostConstruct
    public void init() {
        SsrfProtectionValidator.setEnabled(ssrfProtectionEnabled);
        SsrfProtectionValidator.setAdditionalBlockedHosts(ssrfAdditionalBlockedHosts);
        SsrfProtectionValidator.setAllowedHosts(ssrfAllowedHosts);
        if (!ssrfProtectionEnabled) {
            log.warn("SSRF protection for report service is DISABLED. This allows report generation to access " +
                    "internal/private network addresses including cloud metadata endpoints. It is strongly recommended to " +
                    "enable SSRF protection by setting SSRF_PROTECTION_ENABLED=true. If your reports need to access " +
                    "resources on local networks, use SSRF_ALLOWED_HOSTS to whitelist specific addresses or ranges.");
        }
    }

}
