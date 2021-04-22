/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.init;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DefaultSyncEdgeService implements SyncEdgeService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final int DEFAULT_LIMIT = 100;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    private TbClusterService tbClusterService;

    @Override
    public void sync(TenantId tenantId, Edge edge) {
        log.trace("[{}][{}] Staring edge sync process", tenantId, edge.getId());
        try {
            syncWidgetsBundles(tenantId, edge);
            // TODO: voba - implement this functionality
            // syncAdminSettings(edge);
            syncDeviceProfiles(tenantId, edge);
            syncRuleChains(tenantId, edge);
            syncUsers(tenantId, edge);
            syncAssets(tenantId, edge);
            syncEntityViews(tenantId, edge);
            syncDashboards(tenantId, edge);
            syncWidgetsTypes(tenantId, edge);
            syncDevices(tenantId, edge);
        } catch (Exception e) {
            log.error("[{}][{}] Exception during sync process", tenantId, edge.getId(), e);
        }
    }

    private void syncRuleChains(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncRuleChains [{}]", tenantId, edge.getName());
        try {
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<RuleChain> pageData;
            do {
                pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] rule chains(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (RuleChain ruleChain : pageData.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN, EdgeEventActionType.ADDED, ruleChain.getId(), null);
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge rule chain(s) on sync!", e);
        }
    }

    private void syncDevices(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncDevices [{}]", tenantId, edge.getName());
        try {
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<Device> pageData;
            do {
                pageData = deviceService.findDevicesByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] device(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (Device device : pageData.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.ADDED, device.getId(), null);
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge device(s) on sync!", e);
        }
    }

    private void syncDeviceProfiles(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncDeviceProfiles [{}]", tenantId, edge.getName());
        try {
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<DeviceProfile> pageData;
            do {
                pageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] user(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (DeviceProfile deviceProfile : pageData.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE_PROFILE, EdgeEventActionType.ADDED, deviceProfile.getId(), null);
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading device profile(s) on sync!", e);
        }
    }

    private void syncAssets(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncAssets [{}]", tenantId, edge.getName());
        try {
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<Asset> pageData;
            do {
                pageData = assetService.findAssetsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] asset(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (Asset asset : pageData.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET, EdgeEventActionType.ADDED, asset.getId(), null);
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge asset(s) on sync!", e);
        }
    }

    private void syncEntityViews(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncEntityViews [{}]", tenantId, edge.getName());
        try {
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<EntityView> pageData;
            do {
                pageData = entityViewService.findEntityViewsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] entity view(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (EntityView entityView : pageData.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW, EdgeEventActionType.ADDED, entityView.getId(), null);
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge entity view(s) on sync!", e);
        }
    }

    private void syncDashboards(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncDashboards [{}]", tenantId, edge.getName());
        try {
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<DashboardInfo> pageData;
            do {
                pageData = dashboardService.findDashboardsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] dashboard(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (DashboardInfo dashboardInfo : pageData.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DASHBOARD, EdgeEventActionType.ADDED, dashboardInfo.getId(), null);
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge dashboard(s) on sync!", e);
        }
    }

    private void syncUsers(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncUsers [{}]", tenantId, edge.getName());
        try {
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<User> pageData;
            do {
                pageData = userService.findTenantAdmins(tenantId, pageLink);
                pushUsersToEdge(tenantId, pageData, edge);
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            } while (pageData.hasNext());
            syncCustomerUsers(tenantId, edge);
        } catch (Exception e) {
            log.error("Exception during loading edge user(s) on sync!", e);
        }
    }

    private void syncCustomerUsers(TenantId tenantId, Edge edge) {
        if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, edge.getCustomerId(), null);
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<User> pageData;
            do {
                pageData = userService.findCustomerUsers(tenantId, edge.getCustomerId(), pageLink);
                pushUsersToEdge(tenantId, pageData, edge);
                if (pageData != null && pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            } while (pageData != null && pageData.hasNext());
        }
    }

    private void pushUsersToEdge(TenantId tenantId, PageData<User> pageData, Edge edge) {
        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
            log.trace("[{}] [{}] user(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
            for (User user : pageData.getData()) {
                saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER, EdgeEventActionType.ADDED, user.getId(), null);
            }
        }
    }

    private void syncWidgetsBundles(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncWidgetsBundles [{}]", tenantId, edge.getName());
        List<WidgetsBundle> widgetsBundlesToPush = new ArrayList<>();
        widgetsBundlesToPush.addAll(widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId));
        widgetsBundlesToPush.addAll(widgetsBundleService.findSystemWidgetsBundles(tenantId));
        try {
            for (WidgetsBundle widgetsBundle : widgetsBundlesToPush) {
                saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.WIDGETS_BUNDLE, EdgeEventActionType.ADDED, widgetsBundle.getId(), null);
            }
        } catch (Exception e) {
            log.error("Exception during loading widgets bundle(s) on sync!", e);
        }
    }

    private void syncWidgetsTypes(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncWidgetsTypes [{}]", tenantId, edge.getName());
        List<WidgetsBundle> widgetsBundlesToPush = new ArrayList<>();
        widgetsBundlesToPush.addAll(widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId));
        widgetsBundlesToPush.addAll(widgetsBundleService.findSystemWidgetsBundles(tenantId));
        try {
            for (WidgetsBundle widgetsBundle : widgetsBundlesToPush) {
                List<WidgetType> widgetTypesToPush =
                        widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(widgetsBundle.getTenantId(), widgetsBundle.getAlias());
                for (WidgetType widgetType : widgetTypesToPush) {
                    saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.WIDGET_TYPE, EdgeEventActionType.ADDED, widgetType.getId(), null);
                }
            }
        } catch (Exception e) {
            log.error("Exception during loading widgets type(s) on sync!", e);
        }
    }

    private void syncAdminSettings(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncAdminSettings [{}]", tenantId, edge.getName());
        try {
            AdminSettings systemMailSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(systemMailSettings));
            AdminSettings tenantMailSettings = convertToTenantAdminSettings(systemMailSettings.getKey(), (ObjectNode) systemMailSettings.getJsonValue());
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(tenantMailSettings));
            AdminSettings systemMailTemplates = loadMailTemplates();
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(systemMailTemplates));
            AdminSettings tenantMailTemplates = convertToTenantAdminSettings(systemMailTemplates.getKey(), (ObjectNode) systemMailTemplates.getJsonValue());
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(tenantMailTemplates));
        } catch (Exception e) {
            log.error("Can't load admin settings", e);
        }
    }

    private AdminSettings loadMailTemplates() throws Exception {
        Map<String, Object> mailTemplates = new HashMap<>();
        Pattern startPattern = Pattern.compile("<div class=\"content\".*?>");
        Pattern endPattern = Pattern.compile("<div class=\"footer\".*?>");
        File[] files = new DefaultResourceLoader().getResource("classpath:/templates/").getFile().listFiles();
        for (File file : files) {
            Map<String, String> mailTemplate = new HashMap<>();
            String name = validateName(file.getName());
            String stringTemplate = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            Matcher start = startPattern.matcher(stringTemplate);
            Matcher end = endPattern.matcher(stringTemplate);
            if (start.find() && end.find()) {
                String body = StringUtils.substringBetween(stringTemplate, start.group(), end.group()).replaceAll("\t", "");
                String subject = StringUtils.substringBetween(body, "<h2>", "</h2>");
                mailTemplate.put("subject", subject);
                mailTemplate.put("body", body);
                mailTemplates.put(name, mailTemplate);
            } else {
                log.error("Can't load mail template from file {}", file.getName());
            }
        }
        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setId(new AdminSettingsId(Uuids.timeBased()));
        adminSettings.setKey("mailTemplates");
        adminSettings.setJsonValue(mapper.convertValue(mailTemplates, JsonNode.class));
        return adminSettings;
    }

    private String validateName(String name) throws Exception {
        StringBuilder nameBuilder = new StringBuilder();
        name = name.replace(".vm", "");
        String[] nameParts = name.split("\\.");
        if (nameParts.length >= 1) {
            nameBuilder.append(nameParts[0]);
            for (int i = 1; i < nameParts.length; i++) {
                String word = WordUtils.capitalize(nameParts[i]);
                nameBuilder.append(word);
            }
            return nameBuilder.toString();
        } else {
            throw new Exception("Error during filename validation");
        }
    }

    private AdminSettings convertToTenantAdminSettings(String key, ObjectNode jsonValue) {
        AdminSettings tenantMailSettings = new AdminSettings();
        jsonValue.put("useSystemMailSettings", true);
        tenantMailSettings.setJsonValue(jsonValue);
        tenantMailSettings.setKey(key);
        return tenantMailSettings;
    }

    @Override
    public ListenableFuture<Void> processRuleChainMetadataRequestMsg(TenantId tenantId, Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg) {
        log.trace("[{}] processRuleChainMetadataRequestMsg [{}][{}]", tenantId, edge.getName(), ruleChainMetadataRequestMsg);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() != 0 && ruleChainMetadataRequestMsg.getRuleChainIdLSB() != 0) {
            RuleChainId ruleChainId =
                    new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
            ListenableFuture<EdgeEvent> future = saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN_METADATA, EdgeEventActionType.ADDED, ruleChainId, null);
            Futures.addCallback(future, new FutureCallback<EdgeEvent>() {
                @Override
                public void onSuccess(@Nullable EdgeEvent result) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't save edge event [{}]", ruleChainMetadataRequestMsg, t);
                    futureToSet.setException(t);
                }
            }, dbCallbackExecutorService);
        }
        return futureToSet;
    }

    @Override
    public ListenableFuture<Void> processAttributesRequestMsg(TenantId tenantId, Edge edge, AttributesRequestMsg attributesRequestMsg) {
        log.trace("[{}] processAttributesRequestMsg [{}][{}]", tenantId, edge.getName(), attributesRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(attributesRequestMsg.getEntityType()),
                new UUID(attributesRequestMsg.getEntityIdMSB(), attributesRequestMsg.getEntityIdLSB()));
        final EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(entityId.getEntityType());
        if (type != null) {
            SettableFuture<Void> futureToSet = SettableFuture.create();
            String scope = attributesRequestMsg.getScope();
            ListenableFuture<List<AttributeKvEntry>> ssAttrFuture = attributesService.findAll(tenantId, entityId, scope);
            Futures.addCallback(ssAttrFuture, new FutureCallback<List<AttributeKvEntry>>() {
                @Override
                public void onSuccess(@Nullable List<AttributeKvEntry> ssAttributes) {
                    if (ssAttributes != null && !ssAttributes.isEmpty()) {
                        try {
                            Map<String, Object> entityData = new HashMap<>();
                            ObjectNode attributes = mapper.createObjectNode();
                            for (AttributeKvEntry attr : ssAttributes) {
                                if (attr.getDataType() == DataType.BOOLEAN && attr.getBooleanValue().isPresent()) {
                                    attributes.put(attr.getKey(), attr.getBooleanValue().get());
                                } else if (attr.getDataType() == DataType.DOUBLE && attr.getDoubleValue().isPresent()) {
                                    attributes.put(attr.getKey(), attr.getDoubleValue().get());
                                } else if (attr.getDataType() == DataType.LONG && attr.getLongValue().isPresent()) {
                                    attributes.put(attr.getKey(), attr.getLongValue().get());
                                } else {
                                    attributes.put(attr.getKey(), attr.getValueAsString());
                                }
                            }
                            entityData.put("kv", attributes);
                            entityData.put("scope", scope);
                            JsonNode body = mapper.valueToTree(entityData);
                            log.debug("Sending attributes data msg, entityId [{}], attributes [{}]", entityId, body);
                            saveEdgeEvent(tenantId,
                                    edge.getId(),
                                    type,
                                    EdgeEventActionType.ATTRIBUTES_UPDATED,
                                    entityId,
                                    body);
                        } catch (Exception e) {
                            log.error("[{}] Failed to send attribute updates to the edge", edge.getName(), e);
                            throw new RuntimeException("[" + edge.getName() + "] Failed to send attribute updates to the edge", e);
                        }
                    } else {
                        log.trace("[{}][{}] No attributes found for entity {} [{}]", tenantId,
                                edge.getName(),
                                entityId.getEntityType(),
                                entityId.getId());
                    }
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't save attributes [{}]", attributesRequestMsg, t);
                    futureToSet.setException(t);
                }
            }, dbCallbackExecutorService);
            return futureToSet;
        } else {
            log.warn("[{}] Type doesn't supported {}", tenantId, entityId.getEntityType());
            return Futures.immediateFuture(null);
        }
    }

    @Override
    public ListenableFuture<Void> processRelationRequestMsg(TenantId tenantId, Edge edge, RelationRequestMsg relationRequestMsg) {
        log.trace("[{}] processRelationRequestMsg [{}][{}]", tenantId, edge.getName(), relationRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(relationRequestMsg.getEntityType()),
                new UUID(relationRequestMsg.getEntityIdMSB(), relationRequestMsg.getEntityIdLSB()));

        List<ListenableFuture<List<EntityRelation>>> futures = new ArrayList<>();
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.FROM));
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.TO));
        ListenableFuture<List<List<EntityRelation>>> relationsListFuture = Futures.allAsList(futures);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        Futures.addCallback(relationsListFuture, new FutureCallback<List<List<EntityRelation>>>() {
            @Override
            public void onSuccess(@Nullable List<List<EntityRelation>> relationsList) {
                try {
                    if (relationsList != null && !relationsList.isEmpty()) {
                        for (List<EntityRelation> entityRelations : relationsList) {
                            log.trace("[{}] [{}] [{}] relation(s) are going to be pushed to edge.", edge.getId(), entityId, entityRelations.size());
                            for (EntityRelation relation : entityRelations) {
                                try {
                                    if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                                            !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
                                        saveEdgeEvent(tenantId,
                                                edge.getId(),
                                                EdgeEventType.RELATION,
                                                EdgeEventActionType.ADDED,
                                                null,
                                                mapper.valueToTree(relation));
                                    }
                                } catch (Exception e) {
                                    log.error("Exception during loading relation [{}] to edge on sync!", relation, e);
                                    futureToSet.setException(e);
                                    return;
                                }
                            }
                        }
                    }
                    futureToSet.set(null);
                } catch (Exception e) {
                    log.error("Exception during loading relation(s) to edge on sync!", e);
                    futureToSet.setException(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't find relation by query. Entity id [{}]", tenantId, entityId, t);
                futureToSet.setException(t);
            }
        }, dbCallbackExecutorService);
        return futureToSet;
    }

    private ListenableFuture<List<EntityRelation>> findRelationByQuery(TenantId tenantId, Edge edge, EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, -1, false));
        return relationService.findByQuery(tenantId, query);
    }

    @Override
    public ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        log.trace("[{}] processDeviceCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), deviceCredentialsRequestMsg);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            ListenableFuture<EdgeEvent> future = saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_UPDATED, deviceId, null);
            Futures.addCallback(future, new FutureCallback<EdgeEvent>() {
                @Override
                public void onSuccess(@Nullable EdgeEvent result) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't save edge event [{}]", deviceCredentialsRequestMsg, t);
                    futureToSet.setException(t);
                }
            }, dbCallbackExecutorService);
        }
        return futureToSet;
    }

    @Override
    public ListenableFuture<Void> processUserCredentialsRequestMsg(TenantId tenantId, Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg) {
        log.trace("[{}] processUserCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), userCredentialsRequestMsg);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        if (userCredentialsRequestMsg.getUserIdMSB() != 0 && userCredentialsRequestMsg.getUserIdLSB() != 0) {
            UserId userId = new UserId(new UUID(userCredentialsRequestMsg.getUserIdMSB(), userCredentialsRequestMsg.getUserIdLSB()));
            ListenableFuture<EdgeEvent> future = saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER, EdgeEventActionType.CREDENTIALS_UPDATED, userId, null);
            Futures.addCallback(future, new FutureCallback<EdgeEvent>() {
                @Override
                public void onSuccess(@Nullable EdgeEvent result) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't save edge event [{}]", userCredentialsRequestMsg, t);
                    futureToSet.setException(t);
                }
            }, dbCallbackExecutorService);
        }
        return futureToSet;
    }

    private ListenableFuture<EdgeEvent> saveEdgeEvent(TenantId tenantId,
                                                      EdgeId edgeId,
                                                      EdgeEventType type,
                                                      EdgeEventActionType action,
                                                      EntityId entityId,
                                                      JsonNode body) {
        log.trace("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], type [{}], action[{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setType(type);
        edgeEvent.setAction(action);
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        edgeEvent.setBody(body);
        ListenableFuture<EdgeEvent> future = edgeEventService.saveAsync(edgeEvent);
        Futures.addCallback(future, new FutureCallback<EdgeEvent>() {
            @Override
            public void onSuccess(@Nullable EdgeEvent result) {
                tbClusterService.onEdgeEventUpdate(tenantId, edgeId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Can't save edge event [{}] for edge [{}]", tenantId.getId(), edgeEvent, edgeId.getId(), t);
            }
        }, dbCallbackExecutorService);
        return future;
    }
}
