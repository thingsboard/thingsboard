// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.DomainId;

@Data
@EqualsAndHashCode(callSuper = true)
public class WebSelfRegistrationParams extends AbstractSelfRegistrationParams {

    @Schema(description = "Domain name for self registration URL. Typically this matches the domain name from the Login White Labeling page.")
    @NotNull
    private DomainId domainId;

    @Override
    public SelfRegistrationType getType() {
        return SelfRegistrationType.WEB;
    }
}
