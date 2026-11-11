// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.monitoring.config.integration;

import lombok.Data;
import org.thingsboard.monitoring.data.notification.ShortNameProvider;

@Data
public class IntegrationInfo implements ShortNameProvider {

    private final IntegrationType integrationType;
    private final String baseUrl;

    @Override
    public String getShortName() {
        return "i" + integrationType.getName();
    }

    @Override
    public String toString() {
        return String.format("*%s integration* (%s)", integrationType.getName(), baseUrl);
    }

}
