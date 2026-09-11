// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.install;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.thingsboard.server.dao.audit.AuditLogLevelFilter;
import org.thingsboard.server.dao.audit.AuditLogLevelProperties;

import java.util.HashMap;

@Configuration
@Profile("install")
public class ThingsboardInstallConfiguration {

    @Bean
    public AuditLogLevelFilter emptyAuditLogLevelFilter() {
        var props = new AuditLogLevelProperties();
        props.setMask(new HashMap<>());
        return new AuditLogLevelFilter(props);
    }
}
