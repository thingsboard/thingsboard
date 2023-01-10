/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.Valid;

@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString
public class OAuth2MapperConfig {
    @ApiModelProperty(value = "Whether user should be created if not yet present on the platform after successful authentication")
    private boolean allowUserCreation;
    @ApiModelProperty(value = "Whether user credentials should be activated when user is created after successful authentication")
    private boolean activateUser;
    @ApiModelProperty(value = "Type of OAuth2 mapper. Depending on this param, different mapper config fields must be specified", required = true)
    private MapperType type;
    @Valid
    @ApiModelProperty(value = "Mapper config for BASIC and GITHUB mapper types")
    private OAuth2BasicMapperConfig basic;
    @Valid
    @ApiModelProperty(value = "Mapper config for CUSTOM mapper type")
    private OAuth2CustomMapperConfig custom;
}
