// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.converter.ConverterLookupService;
import org.thingsboard.server.service.integration.IntegrationConfigurationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TbIntegrationExecutorConverterLookupService implements ConverterLookupService {

    private final IntegrationConfigurationService configurationService;

    @Override
    public Converter findConverterById(TenantId tenantId, ConverterId converterId) {
        try {
            return configurationService.getConverter(tenantId, converterId);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to fetch the converter due to {}", tenantId, converterId, e);
            throw new RuntimeException("Failed to fetch the converter", e);
        }
    }
}
