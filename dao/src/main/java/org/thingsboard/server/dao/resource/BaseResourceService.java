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
package org.thingsboard.server.dao.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.resourceInfo.ResourceInfoCacheKey;
import org.thingsboard.server.cache.resourceInfo.ResourceInfoEvictEvent;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceDataInfo;
import org.thingsboard.server.common.data.TbResourceDeleteResult;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.ResourceContainerDao;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.service.validator.ResourceDataValidator;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.common.data.StringUtils.isNotEmpty;
import static org.thingsboard.server.dao.device.DeviceServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("TbResourceDaoService")
@Slf4j
@RequiredArgsConstructor
@Primary
public class BaseResourceService extends AbstractCachedEntityService<ResourceInfoCacheKey, TbResourceInfo, ResourceInfoEvictEvent> implements ResourceService {

    protected static final String INCORRECT_RESOURCE_ID = "Incorrect resourceId ";
    protected static final int MAX_ENTITIES_TO_FIND = 10;

    protected final TbResourceDao resourceDao;
    protected final TbResourceInfoDao resourceInfoDao;
    protected final ResourceDataValidator resourceValidator;
    protected final WidgetTypeDao widgetTypeDao;
    protected final DashboardInfoDao dashboardInfoDao;
    protected final RuleChainDao ruleChainDao;
    private final Map<EntityType, ResourceContainerDao<?>> resourceLinkContainerDaoMap = new HashMap<>();
    private final Map<EntityType, ResourceContainerDao<?>> generalResourceContainerDaoMap = new HashMap<>();

    @PostConstruct
    public void init() {
        resourceLinkContainerDaoMap.put(EntityType.WIDGET_TYPE, widgetTypeDao);
        resourceLinkContainerDaoMap.put(EntityType.DASHBOARD, dashboardInfoDao);
        generalResourceContainerDaoMap.put(EntityType.RULE_CHAIN, ruleChainDao);
    }

    @Autowired
    @Lazy
    private ImageService imageService;

    private static final Map<String, String> DASHBOARD_RESOURCES_MAPPING = Map.of(
            "widgets.*.config.actions.*.*.customResources.*.url", ""
    );
    private static final Map<String, String> WIDGET_RESOURCES_MAPPING = Map.of(
            "resources.*.url", ""
    );
    private static final Map<String, String> WIDGET_DEFAULT_CONFIG_RESOURCES_MAPPING = Map.of(
            "actions.*.*.customResources.*.url", ""
    );

    @Override
    public TbResource saveResource(TbResource resource, boolean doValidate) {
        log.trace("Executing saveResource [{}]", resource);
        if (resource.getTenantId() == null) {
            resource.setTenantId(TenantId.SYS_TENANT_ID);
        }
        if (resource.getId() == null) {
            resource.setResourceKey(getUniqueKey(resource.getTenantId(), resource.getResourceType(), StringUtils.defaultIfEmpty(resource.getResourceKey(), resource.getFileName())));
        }
        if (doValidate) {
            resourceValidator.validate(resource, TbResourceInfo::getTenantId);
        }
        if (resource.getData() != null) {
            resource.setEtag(calculateEtag(resource.getData()));
        }
        return doSaveResource(resource);
    }

    @Override
    public TbResource saveResource(TbResource resource) {
        return saveResource(resource, true);
    }

    protected TbResource doSaveResource(TbResource resource) {
        TenantId tenantId = resource.getTenantId();
        try {
            TbResource saved;
            if (resource.getData() != null) {
                saved = resourceDao.save(tenantId, resource);
            } else {
                TbResourceInfo resourceInfo = saveResourceInfo(resource);
                saved = new TbResource(resourceInfo);
            }
            publishEvictEvent(new ResourceInfoEvictEvent(tenantId, resource.getId()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId()).entityId(saved.getId())
                    .entity(saved).created(resource.getId() == null).build());
            return saved;
        } catch (Exception t) {
            publishEvictEvent(new ResourceInfoEvictEvent(tenantId, resource.getId()));
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("resource_unq_key")) {
                throw new DataValidationException("Resource with such key already exists!");
            } else {
                throw t;
            }
        }
    }

    private TbResourceInfo saveResourceInfo(TbResource resource) {
        return resourceInfoDao.save(resource.getTenantId(), new TbResourceInfo(resource));
    }

    protected String getUniqueKey(TenantId tenantId, ResourceType resourceType, String filename) {
        if (!resourceInfoDao.existsByTenantIdAndResourceTypeAndResourceKey(tenantId, resourceType, filename)) {
            return filename;
        }

        String basename = StringUtils.substringBeforeLast(filename, ".");
        String extension = StringUtils.substringAfterLast(filename, ".");

        Set<String> existing = resourceInfoDao.findKeysByTenantIdAndResourceTypeAndResourceKeyPrefix(
                tenantId, resourceType, basename
        );
        String resourceKey = filename;
        int idx = 1;
        while (existing.contains(resourceKey)) {
            resourceKey = basename + "_(" + idx + ")" + (!extension.isEmpty() ? "." + extension : "");
            idx++;
        }
        log.debug("[{}] Generated unique key {} for {} {}", tenantId, resourceKey, resourceType, filename);
        return resourceKey;
    }

    @Override
    public TbResource findResourceByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        log.trace("Executing findResourceByTenantIdAndKey [{}] [{}] [{}]", tenantId, resourceType, resourceKey);
        return resourceDao.findResourceByTenantIdAndKey(tenantId, resourceType, resourceKey);
    }

    @Override
    public TbResource findResourceById(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, id -> INCORRECT_RESOURCE_ID + id);
        return resourceDao.findById(tenantId, resourceId.getId());
    }

    @Override
    public byte[] getResourceData(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing getResourceData [{}] [{}]", tenantId, resourceId);
        return resourceDao.getResourceData(tenantId, resourceId);
    }

    @Override
    public TbResourceDataInfo getResourceDataInfo(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing getResourceDataInfo [{}] [{}]", tenantId, resourceId);
        return resourceDao.getResourceDataInfo(tenantId, resourceId);
    }

    @Override
    public ResourceExportData exportResource(TbResourceInfo resourceInfo) {
        byte[] data = getResourceData(resourceInfo.getTenantId(), resourceInfo.getId());
        return ResourceExportData.builder()
                .link(resourceInfo.getLink())
                .mediaType(resourceInfo.getResourceType().getMediaType())
                .fileName(resourceInfo.getFileName())
                .title(resourceInfo.getTitle())
                .type(resourceInfo.getResourceType())
                .subType(resourceInfo.getResourceSubType())
                .resourceKey(resourceInfo.getResourceKey())
                .data(Base64.getEncoder().encodeToString(data))
                .build();
    }

    @Override
    public List<ResourceExportData> exportResources(TenantId tenantId, Collection<TbResourceInfo> resources) {
        return resources.stream()
                .sorted(Comparator.comparing(TbResourceInfo::getResourceType).thenComparing(TbResourceInfo::getResourceKey))
                .map(resourceInfo -> {
                    if (resourceInfo.getResourceType() == ResourceType.IMAGE) {
                        ResourceExportData imageExportData = imageService.exportImage(resourceInfo);
                        imageExportData.setResourceKey(null); // so that the image is not updated by resource key on import
                        return imageExportData;
                    } else {
                        return exportResource(resourceInfo);
                    }
                })
                .toList();
    }

    @Override
    public void importResources(TenantId tenantId, List<ResourceExportData> resources) {
        for (ResourceExportData resourceData : resources) {
            if (resourceData.getNewLink() != null) {
                continue; // already imported
            }

            TbResource resource;
            if (resourceData.getType() == ResourceType.IMAGE) {
                resource = imageService.toImage(tenantId, resourceData, true);
                if (resource.getData() != null) {
                    imageService.saveImage(resource);
                }
            } else {
                resource = toResource(tenantId, resourceData);
                if (resource.getData() != null) {
                    saveResource(resource);
                }
            }
            resourceData.setNewLink(resource.getLink());
        }
    }

    @Override
    public TbResource toResource(TenantId tenantId, ResourceExportData exportData) {
        if (exportData.getType() == ResourceType.IMAGE || exportData.getSubType() == ResourceSubType.IMAGE
                || exportData.getSubType() == ResourceSubType.SCADA_SYMBOL) {
            throw new IllegalArgumentException("Image import not supported");
        }

        byte[] data = Base64.getDecoder().decode(exportData.getData());
        String etag = calculateEtag(data);

        TbResourceInfo existingResource;
        boolean update = false;
        if (!tenantId.isSysTenantId()) {
            existingResource = findSystemOrTenantResourceByEtag(tenantId, exportData.getType(), etag);
        } else {
            existingResource = findResourceInfoByTenantIdAndKey(tenantId, exportData.getType(), exportData.getResourceKey());
            update = true; // we overwrite system resource instead of creating new
        }
        if (existingResource != null) {
            TbResource resource = new TbResource(existingResource);
            if (update && !etag.equals(resource.getEtag())) {
                resource.setData(data);
                resource.setTitle(exportData.getTitle());
                log.debug("[{}] Updating existing resource {}", tenantId, existingResource.getLink());
            } else {
                log.debug("[{}] Using existing resource {}", tenantId, existingResource.getLink());
            }
            return resource;
        }

        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setFileName(exportData.getFileName());
        if (isNotEmpty(exportData.getTitle())) {
            resource.setTitle(exportData.getTitle());
        } else {
            resource.setTitle(exportData.getFileName());
        }
        resource.setResourceSubType(exportData.getSubType());
        resource.setResourceType(exportData.getType());
        resource.setResourceKey(exportData.getResourceKey());
        resource.setData(data);
        log.debug("[{}] Creating resource {}", tenantId, resource.getResourceKey());
        return resource;
    }

    @Override
    public TbResourceInfo findResourceInfoById(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceInfoById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, id -> INCORRECT_RESOURCE_ID + id);

        return cache.getAndPutInTransaction(new ResourceInfoCacheKey(resourceId),
                () -> resourceInfoDao.findById(tenantId, resourceId.getId()), true);
    }

    @Override
    public TbResourceInfo findResourceInfoByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        log.trace("Executing findResourceInfoByTenantIdAndKey [{}] [{}] [{}]", tenantId, resourceType, resourceKey);
        return resourceInfoDao.findByTenantIdAndKey(tenantId, resourceType, resourceKey);
    }

    @Override
    public ListenableFuture<TbResourceInfo> findResourceInfoByIdAsync(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceInfoById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, id -> INCORRECT_RESOURCE_ID + id);
        return resourceInfoDao.findByIdAsync(tenantId, resourceId.getId());
    }

    @Override
    public TbResourceDeleteResult deleteResource(TenantId tenantId, TbResourceId resourceId, boolean force) {
        log.trace("Executing deleteResource [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, id -> INCORRECT_RESOURCE_ID + id);
        TbResourceInfo resource = findResourceInfoById(tenantId, resourceId);
        boolean success = true;
        var result = TbResourceDeleteResult.builder();

        if (resource == null) {
            if (!force) {
                success = false;
            }
            return result.success(success).build();
        }

        if (!force) {
            Map<String, List<EntityInfo>> references = findResourceReferences(tenantId, resource);
            if (!references.isEmpty()) {
                success = false;
                result.references(references);
            }
        }
        if (success) {
            resourceDao.removeById(tenantId, resourceId.getId());
            publishEvictEvent(new ResourceInfoEvictEvent(tenantId, resourceId));
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entity(resource).entityId(resourceId).build());
        }

        return result.success(success).build();
    }

    private Map<String, List<EntityInfo>> findResourceReferences(TenantId tenantId, TbResourceInfo resource) {
        Map<String, List<EntityInfo>> references = new HashMap<>();

        if (resource.getResourceType() == ResourceType.JS_MODULE) {
            var ref = resource.getLink();
            findReferences(tenantId, references, ref, resourceLinkContainerDaoMap);
        }

        if (resource.getResourceType() == ResourceType.GENERAL) {
            var ref = resource.getId().getId().toString();
            findReferences(tenantId, references, ref, generalResourceContainerDaoMap);
        }

        return references;
    }

    private void findReferences(TenantId tenantId, Map<String, List<EntityInfo>> references, String ref, Map<EntityType, ResourceContainerDao<?>> resourceLinkContainerDaoMap) {
        resourceLinkContainerDaoMap.forEach((entityType, dao) -> {
            List<EntityInfo> entities = tenantId.isSysTenantId()
                    ? dao.findByResource(ref, MAX_ENTITIES_TO_FIND)
                    : dao.findByTenantIdAndResource(tenantId, ref, MAX_ENTITIES_TO_FIND);
            if (!entities.isEmpty()) {
                references.put(entityType.name(), entities);
            }
        });
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteResource(tenantId, (TbResourceId) id, force);
    }

    @Override
    public PageData<TbResourceInfo> findAllTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink) {
        TenantId tenantId = filter.getTenantId();
        log.trace("Executing findAllTenantResourcesByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return resourceInfoDao.findAllTenantResourcesByTenantId(filter, pageLink);
    }

    @Override
    public PageData<TbResourceInfo> findTenantResourcesByTenantId(TbResourceInfoFilter filter, PageLink pageLink) {
        TenantId tenantId = filter.getTenantId();
        log.trace("Executing findTenantResourcesByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return resourceInfoDao.findTenantResourcesByTenantId(filter, pageLink);
    }

    @Override
    public List<TbResource> findTenantResourcesByResourceTypeAndObjectIds(TenantId tenantId, ResourceType resourceType, String[] objectIds) {
        log.trace("Executing findTenantResourcesByResourceTypeAndObjectIds [{}][{}][{}]", tenantId, resourceType, objectIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return resourceDao.findResourcesByTenantIdAndResourceType(tenantId, resourceType, null, objectIds, null);
    }

    @Override
    public PageData<TbResource> findAllTenantResources(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAllTenantResources [{}][{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return resourceDao.findAllByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<TbResource> findTenantResourcesByResourceTypeAndPageLink(TenantId tenantId, ResourceType resourceType, PageLink pageLink) {
        log.trace("Executing findTenantResourcesByResourceTypeAndPageLink [{}][{}][{}]", tenantId, resourceType, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return resourceDao.findResourcesByTenantIdAndResourceType(tenantId, resourceType, null, pageLink);
    }

    @Override
    public void deleteResourcesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteResourcesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantResourcesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteResourcesByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findResourceInfoById(tenantId, new TbResourceId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(findResourceInfoByIdAsync(tenantId, new TbResourceId(entityId.getId())))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }

    @Override
    public long sumDataSizeByTenantId(TenantId tenantId) {
        return resourceDao.sumDataSizeByTenantId(tenantId);
    }

    @Override
    public boolean updateResourcesUsage(TenantId tenantId, Dashboard dashboard) {
        if (dashboard.getConfiguration() == null) {
            return false;
        }
        Map<String, String> links = getResourcesLinks(dashboard.getResources());
        return updateResourcesUsage(tenantId, List.of(dashboard.getConfiguration()), List.of(DASHBOARD_RESOURCES_MAPPING), links);
    }

    @Override
    public boolean updateResourcesUsage(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
        Map<String, String> links = getResourcesLinks(widgetTypeDetails.getResources());
        List<JsonNode> jsonNodes = new ArrayList<>(2);
        List<Map<String, String>> mappings = new ArrayList<>(2);

        if (widgetTypeDetails.getDescriptor() != null) {
            jsonNodes.add(widgetTypeDetails.getDescriptor());
            mappings.add(WIDGET_RESOURCES_MAPPING);
        }

        JsonNode defaultConfig = widgetTypeDetails.getDefaultConfig();
        if (defaultConfig != null) {
            jsonNodes.add(defaultConfig);
            mappings.add(WIDGET_DEFAULT_CONFIG_RESOURCES_MAPPING);
        }

        boolean updated = updateResourcesUsage(tenantId, jsonNodes, mappings, links);
        if (defaultConfig != null) {
            widgetTypeDetails.setDefaultConfig(defaultConfig);
        }
        return updated;
    }

    protected Map<String, String> getResourcesLinks(List<ResourceExportData> resources) {
        Map<String, String> links;
        if (CollectionUtils.isNotEmpty(resources)) {
            links = new HashMap<>();
            resources.forEach(resource -> {
                if (resource.getNewLink() != null) {
                    links.put(resource.getLink(), resource.getNewLink());
                }
            });
        } else {
            links = Collections.emptyMap();
        }
        return links;
    }

    private boolean updateResourcesUsage(TenantId tenantId, List<JsonNode> jsonNodes, List<Map<String, String>> mappings, Map<String, String> links) {
        log.trace("[{}] updateResourcesUsage (new links: {}) for {}", tenantId, links, jsonNodes);
        return processResources(jsonNodes, mappings, value -> {
            String link = getResourceLink(value);
            if (link != null) {
                String newLink = links.get(link);
                if (newLink == null || newLink.equals(link)) {
                    return value; // leaving link as is
                } else {
                    return DataConstants.TB_RESOURCE_PREFIX + newLink;
                }
            } else { // probably importing an old dashboard json where resources are referenced by ids
                TbResourceId resourceId;
                try {
                    resourceId = new TbResourceId(UUID.fromString(value));
                } catch (IllegalArgumentException e) {
                    return value;
                }
                TbResourceInfo resourceInfo = findResourceInfoById(tenantId, resourceId);
                if (resourceInfo != null) {
                    return DataConstants.TB_RESOURCE_PREFIX + resourceInfo.getLink();
                } else {
                    log.warn("[{}] Couldn't find resource referenced as '{}'", tenantId, value);
                    return "";
                }
            }
        });
    }

    @Override
    public Collection<TbResourceInfo> getUsedResources(TenantId tenantId, Dashboard dashboard) {
        return getUsedResources(tenantId, List.of(dashboard.getConfiguration()), List.of(DASHBOARD_RESOURCES_MAPPING)).values();
    }

    @Override
    public Collection<TbResourceInfo> getUsedResources(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
        List<JsonNode> jsonNodes = new ArrayList<>(2);
        List<Map<String, String>> mappings = new ArrayList<>(2);

        jsonNodes.add(widgetTypeDetails.getDescriptor());
        mappings.add(WIDGET_RESOURCES_MAPPING);

        JsonNode defaultConfig = widgetTypeDetails.getDefaultConfig();
        if (defaultConfig != null) {
            jsonNodes.add(defaultConfig);
            mappings.add(WIDGET_DEFAULT_CONFIG_RESOURCES_MAPPING);
        }

        return getUsedResources(tenantId, jsonNodes, mappings).values();
    }

    private Map<TbResourceId, TbResourceInfo> getUsedResources(TenantId tenantId, List<JsonNode> jsonNodes, List<Map<String, String>> mappings) {
        Map<TbResourceId, TbResourceInfo> resources = new HashMap<>();
        log.trace("[{}] getUsedResources for {}", tenantId, jsonNodes);
        processResources(jsonNodes, mappings, value -> {
            String link = getResourceLink(value);
            if (link == null) {
                return value;
            }

            ResourceType resourceType;
            String resourceKey;
            TenantId resourceTenantId;
            try {
                String[] parts = StringUtils.removeStart(link, "/api/resource/").split("/");
                resourceType = ResourceType.valueOf(parts[0].toUpperCase());
                String scope = parts[1];
                resourceKey = parts[2];
                resourceTenantId = scope.equals("system") ? TenantId.SYS_TENANT_ID : tenantId;
            } catch (Exception e) {
                log.warn("[{}] Invalid resource link '{}'", tenantId, value);
                return value;
            }

            TbResourceInfo resourceInfo = findResourceInfoByTenantIdAndKey(resourceTenantId, resourceType, resourceKey);
            if (resourceInfo != null) {
                resources.putIfAbsent(resourceInfo.getId(), resourceInfo);
            } else {
                log.warn("[{}] Unknown resource referenced with '{}'", tenantId, value);
            }
            return value;
        });
        return resources;
    }

    private String getResourceLink(String value) {
        if (StringUtils.startsWith(value, DataConstants.TB_RESOURCE_PREFIX + "/api/resource/")) {
            return StringUtils.removeStart(value, DataConstants.TB_RESOURCE_PREFIX);
        } else {
            return null;
        }
    }

    private boolean processResources(List<JsonNode> jsonNodes, List<Map<String, String>> mappings, UnaryOperator<String> processor) {
        AtomicBoolean updated = new AtomicBoolean(false);

        for (int i = 0; i < jsonNodes.size(); i++) {
            JsonNode jsonNode = jsonNodes.get(i);
            // processing by mappings first
            if (i <= mappings.size() - 1) {
                JacksonUtil.replaceByMapping(jsonNode, mappings.get(i), Collections.emptyMap(), (name, urlNode) -> {
                    String value = null;
                    if (urlNode.isTextual()) { // link is in the right place
                        value = urlNode.asText();
                    } else {
                        JsonNode id = urlNode.get("id"); // old structure is used
                        if (id != null && id.isTextual()) {
                            value = id.asText();
                        }
                    }

                    if (StringUtils.isNotBlank(value)) {
                        value = processor.apply(value);
                    } else {
                        value = "";
                    }

                    JsonNode newValue = new TextNode(value);
                    if (!newValue.toString().equals(urlNode.toString())) {
                        updated.set(true);
                        log.trace("Replaced by mapping '{}' ({}) with '{}'", value, name, newValue);
                    }
                    return newValue;
                });
            }


            // processing all
            JacksonUtil.replaceAll(jsonNode, "", (name, value) -> {
                if (!StringUtils.startsWith(value, DataConstants.TB_RESOURCE_PREFIX + "/api/resource/")) {
                    return value;
                }

                String newValue = processor.apply(value);
                if (StringUtils.equals(value, newValue)) {
                    return value;
                } else {
                    updated.set(true);
                    log.trace("Replaced '{}' ({}) with '{}'", value, name, newValue);
                    return newValue;
                }
            });
        }

        return updated.get();
    }

    @Override
    public TbResource createOrUpdateSystemResource(ResourceType resourceType, ResourceSubType resourceSubType, String resourceKey, byte[] data) {
        if (resourceType == ResourceType.DASHBOARD) {
            Dashboard dashboard = JacksonUtil.fromBytes(data, Dashboard.class);
            dashboard.setTenantId(TenantId.SYS_TENANT_ID);
            if (CollectionUtils.isNotEmpty(dashboard.getResources())) {
                importResources(dashboard.getTenantId(), dashboard.getResources());
            }
            imageService.updateImagesUsage(dashboard);
            updateResourcesUsage(dashboard.getTenantId(), dashboard);

            data = JacksonUtil.writeValueAsBytes(dashboard);
        }

        TbResource resource = findResourceByTenantIdAndKey(TenantId.SYS_TENANT_ID, resourceType, resourceKey);
        if (resource == null) {
            resource = new TbResource();
            resource.setTenantId(TenantId.SYS_TENANT_ID);
            resource.setResourceType(resourceType);
            resource.setResourceSubType(resourceSubType);
            resource.setResourceKey(resourceKey);
            resource.setFileName(resourceKey);
            resource.setTitle(resourceKey);
        }
        resource.setData(data);
        log.info("{} system resource {}", (resource.getId() == null ? "Creating" : "Updating"), resourceKey);
        return saveResource(resource);
    }

    @Override
    public List<TbResourceInfo> findSystemOrTenantResourcesByIds(TenantId tenantId, List<TbResourceId> resourceIds) {
        log.trace("Executing findSystemOrTenantResourcesByIds, tenantId [{}], resourceIds [{}]", tenantId, resourceIds);
        return resourceInfoDao.findSystemOrTenantResourcesByIds(tenantId, resourceIds);
    }

    @Override
    public String calculateEtag(byte[] data) {
        return Hashing.sha256().hashBytes(data).toString();
    }

    @Override
    public TbResourceInfo findSystemOrTenantResourceByEtag(TenantId tenantId, ResourceType resourceType, String etag) {
        if (StringUtils.isEmpty(etag)) {
            return null;
        }
        log.trace("Executing findSystemOrTenantResourceByEtag [{}] [{}] [{}]", tenantId, resourceType, etag);
        return resourceInfoDao.findSystemOrTenantResourceByEtag(tenantId, resourceType, etag);
    }

    protected String encode(String data) {
        return encode(data.getBytes(StandardCharsets.UTF_8));
    }

    protected String encode(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        return Base64.getEncoder().encodeToString(data);
    }

    protected String decode(String value) {
        if (value == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private final PaginatedRemover<TenantId, TbResourceId> tenantResourcesRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<TbResourceId> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return resourceDao.findIdsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, TbResourceId resourceId) {
            deleteResource(tenantId, resourceId, true);
        }
    };

    @TransactionalEventListener(classes = ResourceInfoEvictEvent.class)
    @Override
    public void handleEvictEvent(ResourceInfoEvictEvent event) {
        if (event.getResourceId() != null) {
            cache.evict(new ResourceInfoCacheKey(event.getResourceId()));
        }
    }

}
