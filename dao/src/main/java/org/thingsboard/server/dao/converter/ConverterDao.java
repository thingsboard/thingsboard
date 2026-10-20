// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.converter;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The Interface ConverterDao.
 *
 */
public interface ConverterDao extends Dao<Converter>, TenantEntityDao<Converter>, ExportableEntityDao<ConverterId, Converter> {

    /**
     * Find all (core and edge template) converters by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of converter objects
     */
    PageData<Converter> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find core converters by tenantId and page link.
     *
     * @param tenantId        the tenantId
     * @param integrationType
     * @param pageLink        the page link
     * @return the list of converter objects
     */
    PageData<Converter> findCoreConvertersByTenantId(UUID tenantId, IntegrationType integrationType, PageLink pageLink);

    /**
     * Find edge template converters by tenantId and page link.
     *
     * @param tenantId        the tenantId
     * @param integrationType
     * @param pageLink        the page link
     * @return the list of converter objects
     */
    PageData<Converter> findEdgeTemplateConvertersByTenantId(UUID tenantId, IntegrationType integrationType, PageLink pageLink);

    /**
     * Find converter by tenantId and converter name.
     *
     * @param tenantId the tenantId
     * @param name     the converter name
     * @return the optional converter object
     */
    Optional<Converter> findConverterByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find converter by tenantId and converter name.
     *
     * @param tenantId the tenantId
     * @param name     the converter name
     * @param type     the converter type
     * @return the optional converter object
     */
    Optional<Converter> findConverterByTenantIdAndNameAndType(UUID tenantId, String name, ConverterType type);

    /**
     * Find converters by tenantId and converter Ids.
     *
     * @param tenantId the tenantId
     * @param converterIds the converter Ids
     * @return the list of converter objects
     */
    ListenableFuture<List<Converter>> findConvertersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> converterIds);

    /**
     * Find existing converter types grouped by integration type.
     *
     * @param tenantId the tenantId
     * @return map of integration type to set of converter types
     */
    Map<IntegrationType, Set<ConverterType>> findExistingConverterTypes(UUID tenantId);

    boolean existsByTenantIdAndNameAndType(UUID tenantId, String name, ConverterType type, UUID skippedId);

    Long countByJsScriptLang();

    Long countByTbelScriptLang();

    Long countGenericConverters();

    Long countTypedConverters();

    Long countDedicatedConverters();
}
