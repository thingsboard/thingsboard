// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.integration;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.IntegrationFields;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.dao.model.sql.IntegrationEntity;
import org.thingsboard.server.dao.sql.HasSecretsEntityDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@SqlDao
public class JpaIntegrationDao extends JpaAbstractDao<IntegrationEntity, Integration> implements IntegrationDao, HasSecretsEntityDao {

    @Autowired
    private IntegrationRepository integrationRepository;

    @Override
    public PageData<Integration> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                integrationRepository.findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Integration> findCoreIntegrationsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                integrationRepository.findByTenantIdAndIsEdgeTemplate(
                        tenantId,
                        pageLink.getTextSearch(),
                        false,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Integration> findEdgeTemplateIntegrationsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                integrationRepository.findByTenantIdAndIsEdgeTemplate(
                        tenantId,
                        pageLink.getTextSearch(),
                        true,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<Integration> findByRoutingKey(UUID tenantId, String routingKey) {
        Integration integration = DaoUtil.getData(integrationRepository.findByRoutingKey(routingKey));
        return Optional.ofNullable(integration);
    }

    @Override
    public List<Integration> findByConverterId(UUID tenantId, UUID converterId) {
        return DaoUtil.convertDataList(integrationRepository.findByConverterId(tenantId, converterId));
    }

    @Override
    public ListenableFuture<List<Integration>> findIntegrationsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> integrationIds) {
        return service.submit(() -> DaoUtil.convertDataList(integrationRepository.findIntegrationsByTenantIdAndIdIn(tenantId, integrationIds)));
    }

    @Override
    public List<Integration> findTenantIntegrationsByName(UUID tenantId, String name) {
        return DaoUtil.convertDataList(integrationRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public Integration findByTenantIdAndName(UUID tenantId, String name) {
        return findTenantIntegrationsByName(tenantId, name).stream().findFirst().orElse(null);
    }

    @Override
    public PageData<Integration> findIntegrationsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find integrations by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(integrationRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Long countCoreIntegrations() {
        return integrationRepository.countByEdgeTemplateFalse();
    }

    @Override
    public Map<String, Long> countIntegrationsPerType() {
        return integrationRepository.countIntegrationsPerType().stream().collect(Collectors.toMap(p -> p.getFirst().name(), TbPair::getSecond));
    }

    @Override
    public List<Integration> findAllCoreIntegrations(IntegrationType integrationType, boolean remote, boolean enabled) {
        return integrationRepository.findCoreIntegrations(integrationType, remote, enabled);
    }

    @Override
    protected Class<IntegrationEntity> getEntityClass() {
        return IntegrationEntity.class;
    }

    @Override
    protected JpaRepository<IntegrationEntity, UUID> getRepository() {
        return integrationRepository;
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return integrationRepository.countByTenantIdAndEdgeTemplateFalse(tenantId.getId());
    }

    @Override
    public Integration findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(integrationRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public IntegrationId getExternalIdByInternal(IntegrationId internalId) {
        return Optional.ofNullable(integrationRepository.getExternalIdById(internalId.getId()))
                .map(IntegrationId::new).orElse(null);
    }

    @Override
    public List<EntityInfo> findByTenantIdAndSecretPlaceholder(TenantId tenantId, String placeholder) {
        return integrationRepository.findByTenantIdAndSecretPlaceholder(tenantId.getId(), placeholder);
    }

    @Override
    public PageData<Integration> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<IntegrationFields> findNextBatch(UUID id, int batchSize) {
        return integrationRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }

}
