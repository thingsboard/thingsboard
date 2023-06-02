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
package org.thingsboard.server.common.data.settings;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApiModel
@Data
@AllArgsConstructor
public class UserDashboardsInfo implements Serializable {

    private static final long serialVersionUID = 2628320657987010348L;
    public static final UserDashboardsInfo EMPTY = new UserDashboardsInfo(Collections.emptyList(), Collections.emptyList());

    @ApiModelProperty(position = 1, value = "List of last visited dashboards.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private List<LastVisitedDashboardInfo> last;

    @ApiModelProperty(position = 2, value = "List of starred dashboards.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private List<StarredDashboardInfo> starred;

    public UserDashboardsInfo() {
        this(new ArrayList<>(), new ArrayList<>());
    }
}
