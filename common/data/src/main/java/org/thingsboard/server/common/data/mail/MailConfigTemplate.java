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
package org.thingsboard.server.common.data.mail;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.MailConfigTemplateId;
import org.thingsboard.server.common.data.validation.Length;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
@ApiModel
public class MailConfigTemplate extends SearchTextBasedWithAdditionalInfo<MailConfigTemplateId> implements HasName {

    @Length(fieldName = "providerId")
    @ApiModelProperty(value = "OAuth2 provider identifier (e.g. its name)", required = true)
    private String providerId;
    @Length(fieldName = "smtpProtocol")
    @ApiModelProperty(value = "SMTP protocol identifier (e.g. SMTP/SMTPS)", required = true)
    private String smtpProtocol;
    @Length(fieldName = "smtpHost")
    @ApiModelProperty(value = "SMTP host identifier (e.g. smtp.gmail.com)", required = true)
    private String smtpHost;
    @Length(fieldName = "smtpPort")
    @ApiModelProperty(value = "SMTP port identifier (e.g. 465)", required = true)
    private Integer smtpPort;
    @Length(fieldName = "timeout")
    @ApiModelProperty(value = "Smtp timeout (e.g. 1000)", required = true)
    private Integer timeout;
    @Length(fieldName = "enableTls")
    @ApiModelProperty(value = "Tls settings (e.g. true/false)", required = true)
    private Boolean enableTls;
    @Length(fieldName = "tlsVersion")
    @ApiModelProperty(value = "Tls version (e.g. TLSv1.2)", required = true)
    private String tlsVersion;
    @Length(fieldName = "authorizationUri")
    @ApiModelProperty(value = "Default authorization URI of the OAuth2 provider")
    private String authorizationUri;
    @Length(fieldName = "accessTokenUri")
    @ApiModelProperty(value = "Default access token URI of the OAuth2 provider")
    private String accessTokenUri;
    @ApiModelProperty(value = "Default OAuth scopes that will be requested from OAuth2 platform")
    private List<String> scope;
    @Length(fieldName = "helpLink")
    @ApiModelProperty(value = "Help link for OAuth2 provider")
    private String helpLink;

    @Override
    public String getName() {
        return providerId;
    }

    @Override
    public String getSearchText() {
        return getName();
    }
}
