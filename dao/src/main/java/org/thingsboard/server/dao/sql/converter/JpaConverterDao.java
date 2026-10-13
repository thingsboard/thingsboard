// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.converter;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.edqs.fields.ConverterFields;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.model.sql.ConverterEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@SqlDao
public class JpaConverterDao extends JpaAbstractDao<ConverterEntity, Converter> implements ConverterDao {

    @Autowired
    private ConverterRepository converterRepository;

    @Override
    public PageData<Converter> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                converterRepository.findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Converter> findCoreConvertersByTenantId(UUID tenantId, IntegrationType integrationType, PageLink pageLink) {
        return DaoUtil.toPageData(
                converterRepository.findByTenantIdAndIsEdgeTemplate(
                        tenantId,
                        pageLink.getTextSearch(),
                        false,
                        integrationType,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Converter> findEdgeTemplateConvertersByTenantId(UUID tenantId, IntegrationType integrationType, PageLink pageLink) {
        return DaoUtil.toPageData(
                converterRepository.findByTenantIdAndIsEdgeTemplate(
                        tenantId,
                        pageLink.getTextSearch(),
                        true,
                        integrationType,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<Converter> findConverterByTenantIdAndName(UUID tenantId, String name) {
        Converter converter = DaoUtil.getData(converterRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(converter);
    }

    @Override
    public boolean existsByTenantIdAndNameAndType(UUID tenantId, String name, ConverterType type, UUID skippedId) {
        return converterRepository.existsByTenantIdAndNameAndTypeAndIdNot(tenantId, name,  type, skippedId);
    }

    @Override
    public Long countByJsScriptLang() {
        return converterRepository.countByScriptLang(ScriptLanguage.JS.name());
    }

    @Override
    public Long countByTbelScriptLang() {
        return converterRepository.countByScriptLang(ScriptLanguage.TBEL.name());
    }

    @Override
    public Long countGenericConverters() {
        return converterRepository.countAllByIntegrationTypeIsNull();
    }

    @Override
    public Long countTypedConverters() {
        return converterRepository.countAllByIntegrationTypeIsNotNull();
    }

    @Override
    public Long countDedicatedConverters() {
        return converterRepository.countAllByConverterVersionAndIntegrationTypeIsNotNull(2);
    }

    @Override
    public Optional<Converter> findConverterByTenantIdAndNameAndType(UUID tenantId, String name, ConverterType type) {
        Converter converter = DaoUtil.getData(converterRepository.findByTenantIdAndNameAndType(tenantId, name, type));
        return Optional.ofNullable(converter);
    }

    @Override
    public ListenableFuture<List<Converter>> findConvertersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> converterIds) {
        return service.submit(() -> DaoUtil.convertDataList(converterRepository.findConvertersByTenantIdAndIdIn(tenantId, converterIds)));
    }

    @Override
    public Map<IntegrationType, Set<ConverterType>> findExistingConverterTypes(UUID tenantId) {
        List<Object[]> rows = converterRepository.findExistingConverterTypes(tenantId);

        Set<ConverterType> genericConverters = rows.stream()
                .filter(row -> row[0] == null && row[1] != null)
                .map(row -> (ConverterType) row[1]).collect(Collectors.toUnmodifiableSet());

        Map<IntegrationType, Set<ConverterType>> typedConverters = rows.stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.groupingBy(
                        row -> (IntegrationType) row[0],
                        Collectors.mapping(row -> (ConverterType) row[1], Collectors.toSet())
                ));

        return Arrays.stream(IntegrationType.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        type -> typedConverters.containsKey(type)
                                ? Stream.concat(typedConverters.get(type).stream(), genericConverters.stream()).collect(Collectors.toSet())
                                : genericConverters
                ));
    }

    @Override
    protected Class<ConverterEntity> getEntityClass() {
        return ConverterEntity.class;
    }

    @Override
    protected JpaRepository<ConverterEntity, UUID> getRepository() {
        return converterRepository;
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return converterRepository.countByTenantIdAndEdgeTemplateFalse(tenantId.getId());
    }

    @Override
    public Converter findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(converterRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Converter findByTenantIdAndName(UUID tenantId, String name) {
        return findConverterByTenantIdAndName(tenantId, name).orElse(null);
    }

    @Override
    public ConverterId getExternalIdByInternal(ConverterId internalId) {
        return Optional.ofNullable(converterRepository.getExternalIdById(internalId.getId()))
                .map(ConverterId::new).orElse(null);
    }

    @Override
    public PageData<Converter> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<ConverterFields> findNextBatch(UUID id, int batchSize) {
        return converterRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CONVERTER;
    }

}
