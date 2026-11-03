// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema
public class DomainInfo extends Domain {

    @Schema(description = "List of available oauth2 clients")
    private List<OAuth2ClientInfo> oauth2ClientInfos;

    public DomainInfo(Domain domain, List<OAuth2ClientInfo> oauth2ClientInfos) {
        super(domain);
        this.oauth2ClientInfos = oauth2ClientInfos;
    }

    public DomainInfo() {
        super();
    }

    public DomainInfo(DomainId domainId) {
        super(domainId);
    }
}
