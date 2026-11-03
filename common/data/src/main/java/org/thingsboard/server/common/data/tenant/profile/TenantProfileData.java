// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.tenant.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Schema
@Data
public class TenantProfileData implements Serializable {

    private static final long serialVersionUID = -3642550257035920976L;

    @Valid
    @Schema(description = "Complex JSON object that contains profile settings: max devices, max assets, rate limits, etc.")
    private TenantProfileConfiguration configuration;

    @Schema(description = "JSON array of queue configuration per tenant profile")
    private List<TenantProfileQueueConfiguration> queueConfiguration;

}
