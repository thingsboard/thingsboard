// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class LwM2MServerSecurityConfigDefault extends LwM2MServerSecurityConfig {
    @Schema(description = "Host for 'Security' mode (DTLS)", example = "0.0.0.0", accessMode = Schema.AccessMode.READ_ONLY)
    protected String securityHost;
    @Schema(description = "Port for 'Security' mode (DTLS): Lwm2m Server or Bootstrap Server", example = "5686 or 5688", accessMode = Schema.AccessMode.READ_ONLY)
    protected Integer securityPort;
}
