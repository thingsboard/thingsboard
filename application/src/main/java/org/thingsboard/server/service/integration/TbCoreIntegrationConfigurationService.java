// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class TbCoreIntegrationConfigurationService implements IntegrationConfigurationService {

    private final ConverterService converterService;
    private final IntegrationService integrationService;
    private final TbTenantProfileCache tenantProfileCache;
    private final SecretConfigurationService secretConfigurationService;

    @Override
    public List<Integration> getActiveIntegrationList(IntegrationType type, boolean remote) {
        return integrationService.findAllCoreIntegrations(type, remote, true);
    }

    @Override
    public Integration getIntegration(TenantId tenantId, IntegrationId integrationId) {
        Integration integration = integrationService.findIntegrationById(tenantId, integrationId);
        return replaceSecretUsages(tenantId, integration);
    }

    @Override
    public Integration getIntegration(TenantId tenantId, String routingKey) {
        var integrationOpt = integrationService.findIntegrationByRoutingKey(tenantId, routingKey);
        return replaceSecretUsages(tenantId, integrationOpt.orElse(null));
    }

    private Integration replaceSecretUsages(TenantId tenantId, Integration integration) {
        if (integration == null) {
            return null;
        }
        Integration copy = new Integration(integration);
        secretConfigurationService.replaceSecretUsages(tenantId, copy.getConfiguration());
        return copy;
    }

    @Override
    public Converter getConverter(TenantId tenantId, ConverterId converterId) {
        return converterService.findConverterById(tenantId, converterId);
    }

    @Override
    public TenantProfile getTenantProfile(TenantId tenantId) {
        return tenantProfileCache.get(tenantId);
    }

}
