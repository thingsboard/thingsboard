// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.converter;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ConverterService extends EntityDaoService {

    Converter saveConverter(Converter converter);

    Converter findConverterById(TenantId tenantId, ConverterId converterId);

    Optional<Converter> findConverterByName(TenantId tenantId, String converterName);

    ListenableFuture<Optional<Converter>> findConverterByNameAsync(TenantId tenantId, String converterName);

    ListenableFuture<Converter> findConverterByIdAsync(TenantId tenantId, ConverterId converterId);

    ListenableFuture<List<Converter>> findConvertersByIdsAsync(TenantId tenantId, List<ConverterId> converterIds);

    PageData<Converter> findTenantConverters(TenantId tenantId, IntegrationType integrationType, PageLink pageLink);

    PageData<Converter> findTenantEdgeTemplateConverters(TenantId tenantId, IntegrationType integrationType, PageLink pageLink);

    void deleteConverter(TenantId tenantId, ConverterId converterId);

    void deleteConvertersByTenantId(TenantId tenantId);

    Map<IntegrationType, Set<ConverterType>> getExistingConverterTypes(TenantId tenantId);
}
