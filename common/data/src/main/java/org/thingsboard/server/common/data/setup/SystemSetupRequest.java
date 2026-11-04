// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.setup;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemSetupRequest {

    @NotBlank
    private String email;
    @NotBlank
    private String password;
    private boolean loadDemo;

}
