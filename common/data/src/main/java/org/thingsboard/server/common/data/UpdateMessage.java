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
package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class UpdateMessage {

    @ApiModelProperty(position = 1, value = "'True' if new platform update is available.")
    private final boolean updateAvailable;
    @ApiModelProperty(position = 2, value = "Current ThingsBoard version.")
    private final String currentVersion;
    @ApiModelProperty(position = 3, value = "Latest ThingsBoard version.")
    private final String latestVersion;
    @ApiModelProperty(position = 4, value = "Upgrade instructions URL.")
    private final String upgradeInstructionsUrl;
    @ApiModelProperty(position = 5, value = "Current ThingsBoard version release notes URL.")
    private final String currentVersionReleaseNotesUrl;
    @ApiModelProperty(position = 6, value = "Latest ThingsBoard version release notes URL.")
    private final String latestVersionReleaseNotesUrl;

}
