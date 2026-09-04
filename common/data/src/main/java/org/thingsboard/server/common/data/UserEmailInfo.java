// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.UserId;

@Schema
@Data
@AllArgsConstructor
public class UserEmailInfo implements HasId<UserId> {

    @Schema(description = "User id")
    private UserId id;
    @Schema(description = "User email", example = "john@gmail.com")
    private String email;
    @Schema(description = "User first name", example = "John")
    private String firstName;
    @Schema(description = "User last name", example = "Brown")
    private String lastName;

}
