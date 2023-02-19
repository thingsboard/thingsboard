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
package org.thingsboard.server.common.data.oauth2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.validation.Length;

@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString
@ApiModel
public class OAuth2BasicMapperConfig {
    @Length(fieldName = "emailAttributeKey", max = 31)
    @ApiModelProperty(value = "Email attribute key of OAuth2 principal attributes. " +
            "Must be specified for BASIC mapper type and cannot be specified for GITHUB type")
    private final String emailAttributeKey;
    @Length(fieldName = "firstNameAttributeKey", max = 31)
    @ApiModelProperty(value = "First name attribute key")
    private final String firstNameAttributeKey;
    @Length(fieldName = "lastNameAttributeKey", max = 31)
    @ApiModelProperty(value = "Last name attribute key")
    private final String lastNameAttributeKey;
    @ApiModelProperty(value = "Tenant naming strategy. For DOMAIN type, domain for tenant name will be taken from the email (substring before '@')", required = true)
    private final TenantNameStrategyType tenantNameStrategy;
    @Length(fieldName = "tenantNamePattern")
    @ApiModelProperty(value = "Tenant name pattern for CUSTOM naming strategy. " +
            "OAuth2 attributes in the pattern can be used by enclosing attribute key in '%{' and '}'", example = "%{email}")
    private final String tenantNamePattern;
    @Length(fieldName = "customerNamePattern")
    @ApiModelProperty(value = "Customer name pattern. When creating a user on the first OAuth2 log in, if specified, " +
            "customer name will be used to create or find existing customer in the platform and assign customerId to the user")
    private final String customerNamePattern;
    @Length(fieldName = "defaultDashboardName")
    @ApiModelProperty(value = "Name of the tenant's dashboard to set as default dashboard for newly created user")
    private final String defaultDashboardName;
    @ApiModelProperty(value = "Whether default dashboard should be open in full screen")
    private final boolean alwaysFullScreen;
}
