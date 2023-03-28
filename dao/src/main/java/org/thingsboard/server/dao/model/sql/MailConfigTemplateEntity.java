/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MailConfigTemplateId;
import org.thingsboard.server.common.data.mail.MailConfigTemplate;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.MAIL_CONFIG_TEMPLATE_COLUMN_FAMILY_NAME)
public class MailConfigTemplateEntity extends BaseSqlEntity<MailConfigTemplate> {

    @Column(name = ModelConstants.MAIL_CONFIG_PROVIDER_ID_PROPERTY)
    private String providerId;
    @Column(name = ModelConstants.MAIL_CONFIG_SMTP_PROTOCOL_PROPERTY)
    private String smtpProtocol;
    @Column(name = ModelConstants.MAIL_CONFIG_SMTP_HOST_PROPERTY)
    private String smtpHost;
    @Column(name = ModelConstants.MAIL_CONFIG_SMTP_PORT_PROPERTY)
    private Integer smtpPort;
    @Column(name = ModelConstants.MAIL_CONFIG_SMTP_TIMEOUT_PROPERTY)
    private Integer timeout;
    @Column(name = ModelConstants.MAIL_CONFIG_TLS_ENABLED_PROPERTY)
    private Boolean tlsEnabled;
    @Column(name = ModelConstants.MAIL_CONFIG_TLS_VERSION_PROPERTY)
    private String tlsVersion;
    @Column(name = ModelConstants.MAIL_CONFIG_AUTHORIZATION_URI_PROPERTY)
    private String authorizationUri;
    @Column(name = ModelConstants.MAIL_CONFIG_TOKEN_URI_PROPERTY)
    private String tokenUri;
    @Column(name = ModelConstants.MAIL_CONFIG_SCOPE_PROPERTY)
    private String scope;
    @Column(name = ModelConstants.MAIL_CONFIG_HELP_LINK_PROPERTY)
    private String helpLink;


    public MailConfigTemplateEntity() {
    }

    public MailConfigTemplateEntity(MailConfigTemplate mailConfigTemplate) {
        if (mailConfigTemplate.getId() != null) {
            this.setUuid(mailConfigTemplate.getId().getId());
        }
        this.createdTime = mailConfigTemplate.getCreatedTime();
        this.providerId = mailConfigTemplate.getProviderId();
        this.smtpProtocol = mailConfigTemplate.getSmtpProtocol();
        this.smtpHost = mailConfigTemplate.getSmtpHost();
        this.smtpPort = mailConfigTemplate.getSmtpPort();
        this.timeout = mailConfigTemplate.getTimeout();
        this.tlsEnabled = mailConfigTemplate.getEnableTls();
        this.tlsVersion = mailConfigTemplate.getTlsVersion();
        this.authorizationUri = mailConfigTemplate.getAuthorizationUri();
        this.tokenUri = mailConfigTemplate.getAccessTokenUri();
        this.scope = mailConfigTemplate.getScope().stream().reduce((result, element) -> result + "," + element).orElse("");
        this.helpLink = mailConfigTemplate.getHelpLink();
    }

    @Override
    public MailConfigTemplate toData() {
        MailConfigTemplate mailConfigTemplate = new MailConfigTemplate();
        mailConfigTemplate.setId(new MailConfigTemplateId(id));
        mailConfigTemplate.setCreatedTime(createdTime);
        mailConfigTemplate.setProviderId(providerId);
        mailConfigTemplate.setSmtpProtocol(smtpProtocol);
        mailConfigTemplate.setSmtpHost(smtpHost);
        mailConfigTemplate.setSmtpPort(smtpPort);
        mailConfigTemplate.setTimeout(timeout);
        mailConfigTemplate.setEnableTls(tlsEnabled);
        mailConfigTemplate.setTlsVersion(tlsVersion);
        mailConfigTemplate.setAuthorizationUri(authorizationUri);
        mailConfigTemplate.setAccessTokenUri(tokenUri);
        mailConfigTemplate.setScope(Arrays.asList(scope.split(",")));
        mailConfigTemplate.setHelpLink(helpLink);
        return mailConfigTemplate;
    }
}
