// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DomainOauth2Client {

    private DomainId domainId;
    private OAuth2ClientId oAuth2ClientId;

}
