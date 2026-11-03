// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.converter;

import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Optional;

public interface ConverterDefaultsService {

    Optional<String> getPayload(IntegrationType integrationType, ConverterType converterType);

    Optional<String> getMetadata(IntegrationType integrationType, ConverterType converterType);

}
