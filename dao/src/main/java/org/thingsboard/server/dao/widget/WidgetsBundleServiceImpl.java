/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.widget.WidgetsBundleFilter;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.entity.AbstractEntityService.checkConstraintViolation;

@Service("WidgetsBundleDaoService")
@Slf4j
public class WidgetsBundleServiceImpl implements WidgetsBundleService {

    private static final int DEFAULT_WIDGETS_BUNDLE_LIMIT = 300;
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private DataValidator<WidgetsBundle> widgetsBundleValidator;

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @Autowired
    private ImageService imageService;

    @Override
    public WidgetsBundle findWidgetsBundleById(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetsBundleById [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, id -> "Incorrect widgetsBundleId " + id);
        return widgetsBundleDao.findById(tenantId, widgetsBundleId.getId());
    }

    @Override
    public WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle) {
        log.trace("Executing saveWidgetsBundle [{}]", widgetsBundle);
        widgetsBundleValidator.validate(widgetsBundle, WidgetsBundle::getTenantId);
        try {
            imageService.replaceBase64WithImageUrl(widgetsBundle, "bundle");
            WidgetsBundle result = widgetsBundleDao.save(widgetsBundle.getTenantId(), widgetsBundle);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getTenantId())
                    .entityId(result.getId()).created(widgetsBundle.getId() == null).build());
            return result;
        } catch (Exception e) {
            checkConstraintViolation(e,
                    "uq_widgets_bundle_alias", "Widgets Bundle with such alias already exists!",
                    "widgets_bundle_external_id_unq_key", "Widgets Bundle with such external id already exists!");
            throw e;
        }
    }

    @Override
    public void deleteWidgetsBundle(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing deleteWidgetsBundle [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, id -> "Incorrect widgetsBundleId " + id);
        deleteEntity(tenantId, widgetsBundleId, false);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        WidgetsBundle widgetsBundle = findWidgetsBundleById(tenantId, (WidgetsBundleId) id);
        if (widgetsBundle == null) {
            if (force) {
                return;
            } else {
                throw new IncorrectParameterException("Unable to delete non-existent widgets bundle.");
            }
        }
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(id).build());
        widgetsBundleDao.removeById(tenantId, id.getId());
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(TenantId tenantId, String alias) {
        log.trace("Executing findWidgetsBundleByTenantIdAndAlias, tenantId [{}], alias [{}]", tenantId, alias);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateString(alias, a -> "Incorrect alias " + a);
        return widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(tenantId.getId(), alias);
    }

    @Override
    public PageData<WidgetsBundle> findSystemWidgetsBundlesByPageLink(WidgetsBundleFilter widgetsBundleFilter, PageLink pageLink) {
        log.trace("Executing findSystemWidgetsBundles, widgetsBundleFilter [{}], pageLink [{}]", widgetsBundleFilter, pageLink);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findSystemWidgetsBundles(widgetsBundleFilter, pageLink);
    }

    @Override
    public List<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId) {
        log.trace("Executing findSystemWidgetsBundles");
        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_WIDGETS_BUNDLE_LIMIT);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = findSystemWidgetsBundlesByPageLink(WidgetsBundleFilter.fromTenantId(tenantId), pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantWidgetsBundlesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantIdAndPageLink(WidgetsBundleFilter widgetsBundleFilter, PageLink pageLink) {
        TenantId tenantId = widgetsBundleFilter.getTenantId();
        log.trace("Executing findAllTenantWidgetsBundlesByTenantIdAndPageLink, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(widgetsBundleFilter, pageLink);
    }

    @Override
    public PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantIdAndPageLink(WidgetsBundleFilter widgetsBundleFilter, PageLink pageLink) {
        TenantId tenantId = widgetsBundleFilter.getTenantId();
        log.trace("Executing findTenantWidgetsBundlesByTenantIdAndPageLink, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(widgetsBundleFilter, pageLink);
    }

    @Override
    public List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(TenantId tenantId) {
        log.trace("Executing findAllTenantWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_WIDGETS_BUNDLE_LIMIT);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = findAllTenantWidgetsBundlesByTenantIdAndPageLink(WidgetsBundleFilter.fromTenantId(tenantId), pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public void deleteWidgetsBundlesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantWidgetsBundleRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteWidgetsBundlesByTenantId(tenantId);
    }

    @Transactional
    @Override
    public void updateSystemWidgets(Stream<String> bundles, Stream<String> widgets) {
        widgets.forEach(widgetTypeJson -> {
            try {
                updateSystemWidget(JacksonUtil.toJsonNode(widgetTypeJson));
            } catch (Exception e) {
                throw new RuntimeException("Unable to load widget type from json: " + widgetTypeJson, e);
            }
        });

        bundles.forEach(widgetsBundleDescriptorJson -> {
            JsonNode widgetsBundleDescriptor = JacksonUtil.toJsonNode(widgetsBundleDescriptorJson);
            if (widgetsBundleDescriptor == null || !widgetsBundleDescriptor.has("widgetsBundle")) {
                throw new RuntimeException("Invalid widgets bundle json: [" + widgetsBundleDescriptorJson + "]");
            }

            JsonNode widgetsBundleJson = widgetsBundleDescriptor.get("widgetsBundle");
            WidgetsBundle widgetsBundle = JacksonUtil.treeToValue(widgetsBundleJson, WidgetsBundle.class);
            WidgetsBundle existingWidgetsBundle = findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, widgetsBundle.getAlias());
            if (existingWidgetsBundle != null) {
                widgetsBundle.setId(existingWidgetsBundle.getId());
                widgetsBundle.setCreatedTime(existingWidgetsBundle.getCreatedTime());
            }
            widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
            widgetsBundle = saveWidgetsBundle(widgetsBundle);
            log.debug("{} widgets bundle {}", existingWidgetsBundle == null ? "Created" : "Updated", widgetsBundle.getAlias());

            List<String> widgetTypeFqns = new ArrayList<>();
            if (widgetsBundleDescriptor.has("widgetTypes")) {
                JsonNode widgetTypesArrayJson = widgetsBundleDescriptor.get("widgetTypes");
                widgetTypesArrayJson.forEach(widgetTypeJson -> {
                    try {
                        WidgetTypeDetails widgetTypeDetails = updateSystemWidget(widgetTypeJson);
                        widgetTypeFqns.add(widgetTypeDetails.getFqn());
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to load widget type from json: " + widgetsBundleDescriptorJson, e);
                    }
                });
            }
            if (widgetsBundleDescriptor.has("widgetTypeFqns")) {
                JsonNode widgetFqnsArrayJson = widgetsBundleDescriptor.get("widgetTypeFqns");
                widgetFqnsArrayJson.forEach(fqnJson -> widgetTypeFqns.add(fqnJson.asText()));
            }
            widgetTypeService.updateWidgetsBundleWidgetFqns(TenantId.SYS_TENANT_ID, widgetsBundle.getId(), widgetTypeFqns);
        });
    }

    @Override
    public List<WidgetsBundle> findSystemOrTenantWidgetsBundlesByIds(TenantId tenantId, List<WidgetsBundleId> widgetsBundleIds) {
        log.trace("Executing findSystemOrTenantWidgetsBundlesByIds, tenantId [{}], widgetsBundleIds [{}]", tenantId, widgetsBundleIds);
        return widgetsBundleDao.findSystemOrTenantWidgetBundlesByIds(tenantId.getId(), toUUIDs(widgetsBundleIds));
    }

    private WidgetTypeDetails updateSystemWidget(JsonNode widgetTypeJson) {
        WidgetTypeDetails widgetTypeDetails = JacksonUtil.treeToValue(widgetTypeJson, WidgetTypeDetails.class);
        WidgetType existingWidget = widgetTypeService.findWidgetTypeByTenantIdAndFqn(TenantId.SYS_TENANT_ID, widgetTypeDetails.getFqn());
        if (existingWidget != null) {
            widgetTypeDetails.setId(existingWidget.getId());
            widgetTypeDetails.setCreatedTime(existingWidget.getCreatedTime());
        }
        widgetTypeDetails.setTenantId(TenantId.SYS_TENANT_ID);
        widgetTypeDetails = widgetTypeService.saveWidgetType(widgetTypeDetails);
        log.debug("{} widget type {}", existingWidget == null ? "Created" : "Updated", widgetTypeDetails.getFqn());
        return widgetTypeDetails;
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findWidgetsBundleById(tenantId, new WidgetsBundleId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(widgetsBundleDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGETS_BUNDLE;
    }

    private final PaginatedRemover<TenantId, WidgetsBundle> tenantWidgetsBundleRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<WidgetsBundle> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, WidgetsBundle entity) {
            deleteWidgetsBundle(tenantId, new WidgetsBundleId(entity.getUuidId()));
        }

    };

}
