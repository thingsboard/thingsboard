// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.integration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.wrapper.ConverterUnwrapper;
import org.thingsboard.integration.api.converter.wrapper.ConverterUnwrapperFactory;
import org.thingsboard.server.common.data.ConvertersInfo;
import org.thingsboard.server.common.data.IntegrationConvertersInfo;
import org.thingsboard.server.common.data.LibraryConvertersInfo;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.converter.ConverterLibraryService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbIntegrationService extends AbstractTbEntityService implements TbIntegrationService {

    private final IntegrationService integrationService;
    private final ConverterLibraryService converterLibraryService;
    private final ConverterService converterService;

    @Override
    public PageData<IntegrationInfo> findTenantIntegrationInfos(TenantId tenantId, PageLink pageLink, boolean isEdgeTemplate) {
        if (isEdgeTemplate) {
            PageData<IntegrationInfo> pageData = integrationService.findTenantIntegrationInfos(tenantId, pageLink, isEdgeTemplate);
            List<IntegrationInfo> integrationInfos = pageData.getData();
            integrationInfos.forEach(integration -> {
                ObjectNode status = JacksonUtil.newObjectNode();
                status.put("success", true);
                integration.setStatus(status);
            });
            return pageData;
        }

        return integrationService.findTenantIntegrationInfosWithStats(tenantId, isEdgeTemplate, pageLink);
    }

    @Override
    public PageData<IntegrationInfo> findIntegrationInfosByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        PageData<IntegrationInfo> pageData = integrationService.findIntegrationInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink);

        pageData.getData().forEach(integration -> {
            ObjectNode status = JacksonUtil.newObjectNode();
            status.put("success", true);
            integration.setStatus(status);
        });

        return pageData;
    }

    @Override
    public Map<IntegrationType, IntegrationConvertersInfo> getIntegrationsConvertersInfo(TenantId tenantId) {
        Map<String, LibraryConvertersInfo> libraryConvertersInfo = converterLibraryService.getConvertersInfo();
        Map<IntegrationType, Set<ConverterType>> existingConverters = converterService.getExistingConverterTypes(tenantId);
        Map<IntegrationType, IntegrationConvertersInfo> result = new HashMap<>();
        for (IntegrationType integrationType : IntegrationType.values()) {
            Set<ConverterType> existing = existingConverters.getOrDefault(integrationType, Collections.emptySet());
            boolean hasUplink = existing.contains(ConverterType.UPLINK);
            boolean hasDownlink = existing.contains(ConverterType.DOWNLINK);

            String directory = integrationType.getDirectory();
            LibraryConvertersInfo libraryInfo = libraryConvertersInfo.getOrDefault(directory, new LibraryConvertersInfo(false, false));
            Set<String> keys = ConverterUnwrapperFactory
                    .getUnwrapper(integrationType)
                    .map(ConverterUnwrapper::getKeys)
                    .orElse(null);
            result.put(integrationType, new IntegrationConvertersInfo(
                    new ConvertersInfo(libraryInfo.uplink(), hasUplink, keys),
                    new ConvertersInfo(libraryInfo.downlink(), hasDownlink, null)
            ));
        }
        return result;
    }

}
