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
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.UserId;

@ApiModel
@Data
@AllArgsConstructor
public class UserData {

    @ApiModelProperty(position = 1, value = "User id")
    private UserId id;
    @ApiModelProperty(position = 2, value = "User email", example = "john@gmail.com")
    private String email;
    @ApiModelProperty(position = 3, value = "User first name", example = "John")
    private String firstName;
    @ApiModelProperty(position = 4, value = "User last name", example = "Brown")
    private String lastName;

}
