/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isBlank;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_INFO_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_VIEW_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.MODEL_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.EdgeController.EDGE_ID;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class EntityViewController extends BaseController {

    public static final String ENTITY_VIEW_ID = "entityViewId";

    @Autowired
    private TimeseriesService tsService;

    @ApiOperation(value = "Get entity view (getEntityViewById)",
            notes = "Fetch the EntityView object based on the provided entity view id. "
                    + ENTITY_VIEW_DESCRIPTION + MODEL_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/{entityViewId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityView getEntityViewById(
            @ApiParam(value = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            return checkEntityViewId(new EntityViewId(toUUID(strEntityViewId)), Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Entity View info (getEntityViewInfoById)",
            notes = "Fetch the Entity View info object based on the provided Entity View Id. "
                    + ENTITY_VIEW_INFO_DESCRIPTION + MODEL_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/info/{entityViewId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityViewInfo getEntityViewInfoById(
            @ApiParam(value = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            return checkEntityViewInfoId(entityViewId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save or update entity view (saveEntityView)",
            notes = ENTITY_VIEW_DESCRIPTION + MODEL_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView", method = RequestMethod.POST)
    @ResponseBody
    public EntityView saveEntityView(
            @ApiParam(value = "A JSON object representing the entity view.")
            @RequestBody EntityView entityView) throws ThingsboardException {
        try {
            entityView.setTenantId(getCurrentUser().getTenantId());

            List<ListenableFuture<?>> futures = new ArrayList<>();

            if (entityView.getId() == null) {
                accessControlService
                        .checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.CREATE, null, entityView);
            } else {
                EntityView existingEntityView = checkEntityViewId(entityView.getId(), Operation.WRITE);
                if (existingEntityView.getKeys() != null) {
                    if (existingEntityView.getKeys().getAttributes() != null) {
                        futures.add(deleteAttributesFromEntityView(existingEntityView, DataConstants.CLIENT_SCOPE, existingEntityView.getKeys().getAttributes().getCs(), getCurrentUser()));
                        futures.add(deleteAttributesFromEntityView(existingEntityView, DataConstants.SERVER_SCOPE, existingEntityView.getKeys().getAttributes().getCs(), getCurrentUser()));
                        futures.add(deleteAttributesFromEntityView(existingEntityView, DataConstants.SHARED_SCOPE, existingEntityView.getKeys().getAttributes().getCs(), getCurrentUser()));
                    }
                }
                List<String> tsKeys = existingEntityView.getKeys() != null && existingEntityView.getKeys().getTimeseries() != null ?
                        existingEntityView.getKeys().getTimeseries() : Collections.emptyList();
                futures.add(deleteLatestFromEntityView(existingEntityView, tsKeys, getCurrentUser()));
            }

            EntityView savedEntityView = checkNotNull(entityViewService.saveEntityView(entityView));
            if (savedEntityView.getKeys() != null) {
                if (savedEntityView.getKeys().getAttributes() != null) {
                    futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.CLIENT_SCOPE, savedEntityView.getKeys().getAttributes().getCs(), getCurrentUser()));
                    futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SERVER_SCOPE, savedEntityView.getKeys().getAttributes().getSs(), getCurrentUser()));
                    futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SHARED_SCOPE, savedEntityView.getKeys().getAttributes().getSh(), getCurrentUser()));
                }
                futures.add(copyLatestFromEntityToEntityView(savedEntityView, getCurrentUser()));
            }
            for (ListenableFuture<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Failed to copy attributes to entity view", e);
                }
            }

            logEntityAction(savedEntityView.getId(), savedEntityView, null,
                    entityView.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            if (entityView.getId() != null) {
                sendEntityNotificationMsg(savedEntityView.getTenantId(), savedEntityView.getId(), EdgeEventActionType.UPDATED);
            }

            return savedEntityView;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_VIEW), entityView, null,
                    entityView.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    private ListenableFuture<Void> deleteLatestFromEntityView(EntityView entityView, List<String> keys, SecurityUser user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteLatest(entityView.getTenantId(), entityId, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logTimeseriesDeleted(user, entityId, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(user, entityId, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            tsSubService.deleteAllLatest(entityView.getTenantId(), entityId, new FutureCallback<Collection<String>>() {
                @Override
                public void onSuccess(@Nullable Collection<String> keys) {
                    try {
                        logTimeseriesDeleted(user, entityId, new ArrayList<>(keys), null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(user, entityId, Collections.emptyList(), t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        }
        return resultFuture;
    }

    private ListenableFuture<Void> deleteAttributesFromEntityView(EntityView entityView, String scope, List<String> keys, SecurityUser user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteAndNotify(entityView.getTenantId(), entityId, scope, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logAttributesDeleted(user, entityId, scope, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logAttributesDeleted(user, entityId, scope, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            resultFuture.set(null);
        }
        return resultFuture;
    }

    private ListenableFuture<List<Void>> copyLatestFromEntityToEntityView(EntityView entityView, SecurityUser user) {
        EntityViewId entityId = entityView.getId();
        List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                entityView.getKeys().getTimeseries() : Collections.emptyList();
        long startTs = entityView.getStartTimeMs();
        long endTs = entityView.getEndTimeMs() == 0 ? Long.MAX_VALUE : entityView.getEndTimeMs();
        ListenableFuture<List<String>> keysFuture;
        if (keys.isEmpty()) {
            keysFuture = Futures.transform(tsService.findAllLatest(user.getTenantId(),
                    entityView.getEntityId()), latest -> latest.stream().map(TsKvEntry::getKey).collect(Collectors.toList()), MoreExecutors.directExecutor());
        } else {
            keysFuture = Futures.immediateFuture(keys);
        }
        ListenableFuture<List<TsKvEntry>> latestFuture = Futures.transformAsync(keysFuture, fetchKeys -> {
            List<ReadTsKvQuery> queries = fetchKeys.stream().filter(key -> !isBlank(key)).map(key -> new BaseReadTsKvQuery(key, startTs, endTs, 1, "DESC")).collect(Collectors.toList());
            if (!queries.isEmpty()) {
                return tsService.findAll(user.getTenantId(), entityView.getEntityId(), queries);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
        return Futures.transform(latestFuture, latestValues -> {
            if (latestValues != null && !latestValues.isEmpty()) {
                tsSubService.saveLatestAndNotify(entityView.getTenantId(), entityId, latestValues, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                    }

                    @Override
                    public void onFailure(Throwable t) {
                    }
                });
            }
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<List<Void>> copyAttributesFromEntityToEntityView(EntityView entityView, String scope, Collection<String> keys, SecurityUser user) throws ThingsboardException {
        EntityViewId entityId = entityView.getId();
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<AttributeKvEntry>> getAttrFuture = attributesService.find(getTenantId(), entityView.getEntityId(), scope, keys);
            return Futures.transform(getAttrFuture, attributeKvEntries -> {
                List<AttributeKvEntry> attributes;
                if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
                    attributes =
                            attributeKvEntries.stream()
                                    .filter(attributeKvEntry -> {
                                        long startTime = entityView.getStartTimeMs();
                                        long endTime = entityView.getEndTimeMs();
                                        long lastUpdateTs = attributeKvEntry.getLastUpdateTs();
                                        return startTime == 0 && endTime == 0 ||
                                                (endTime == 0 && startTime < lastUpdateTs) ||
                                                (startTime == 0 && endTime > lastUpdateTs)
                                                ? true : startTime < lastUpdateTs && endTime > lastUpdateTs;
                                    }).collect(Collectors.toList());
                    tsSubService.saveAndNotify(entityView.getTenantId(), entityId, scope, attributes, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            try {
                                logAttributesUpdated(user, entityId, scope, attributes, null);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            try {
                                logAttributesUpdated(user, entityId, scope, attributes, t);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }
                    });
                }
                return null;
            }, MoreExecutors.directExecutor());
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private void logAttributesUpdated(SecurityUser user, EntityId entityId, String scope, List<AttributeKvEntry> attributes, Throwable e) throws ThingsboardException {
        logEntityAction(user, entityId, null, null, ActionType.ATTRIBUTES_UPDATED, toException(e),
                scope, attributes);
    }

    private void logAttributesDeleted(SecurityUser user, EntityId entityId, String scope, List<String> keys, Throwable e) throws ThingsboardException {
        logEntityAction(user, entityId, null, null, ActionType.ATTRIBUTES_DELETED, toException(e),
                scope, keys);
    }

    private void logTimeseriesDeleted(SecurityUser user, EntityId entityId, List<String> keys, Throwable e) throws ThingsboardException {
        logEntityAction(user, entityId, null, null, ActionType.TIMESERIES_DELETED, toException(e),
                keys);
    }

    @ApiOperation(value = "Delete entity view (deleteEntityView)",
            notes = "Delete the EntityView object based on the provided entity view id. "
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityView/{entityViewId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEntityView(
            @ApiParam(value = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            EntityView entityView = checkEntityViewId(entityViewId, Operation.DELETE);

            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), entityViewId);

            entityViewService.deleteEntityView(getTenantId(), entityViewId);
            logEntityAction(entityViewId, entityView, entityView.getCustomerId(),
                    ActionType.DELETED, null, strEntityViewId);

            sendDeleteNotificationMsg(getTenantId(), entityViewId, relatedEdgeIds);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_VIEW),
                    null,
                    null,
                    ActionType.DELETED, e, strEntityViewId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Entity View by name (getTenantEntityView)",
            notes = "Fetch the Entity View object based on the tenant id and entity view name. " + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/entityViews", params = {"entityViewName"}, method = RequestMethod.GET)
    @ResponseBody
    public EntityView getTenantEntityView(
            @ApiParam(value = "Entity View name")
            @RequestParam String entityViewName) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Assign Entity View to customer (assignEntityViewToCustomer)",
            notes = "Creates assignment of the Entity View to customer. Customer will be able to query Entity View afterwards." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/entityView/{entityViewId}", method = RequestMethod.POST)
    @ResponseBody
    public EntityView assignEntityViewToCustomer(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId, Operation.READ);

            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            checkEntityViewId(entityViewId, Operation.ASSIGN_TO_CUSTOMER);

            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToCustomer(getTenantId(), entityViewId, customerId));
            logEntityAction(entityViewId, savedEntityView,
                    savedEntityView.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strEntityViewId, strCustomerId, customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedEntityView.getTenantId(), savedEntityView.getId(),
                    customerId, EdgeEventActionType.ASSIGNED_TO_CUSTOMER);

            return savedEntityView;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_VIEW), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strEntityViewId, strCustomerId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Unassign Entity View from customer (unassignEntityViewFromCustomer)",
            notes = "Clears assignment of the Entity View to customer. Customer will not be able to query Entity View afterwards." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/entityView/{entityViewId}", method = RequestMethod.DELETE)
    @ResponseBody
    public EntityView unassignEntityViewFromCustomer(
            @ApiParam(value = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            EntityView entityView = checkEntityViewId(entityViewId, Operation.UNASSIGN_FROM_CUSTOMER);
            if (entityView.getCustomerId() == null || entityView.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Entity View isn't assigned to any customer!");
            }
            Customer customer = checkCustomerId(entityView.getCustomerId(), Operation.READ);
            EntityView savedEntityView = checkNotNull(entityViewService.unassignEntityViewFromCustomer(getTenantId(), entityViewId));
            logEntityAction(entityViewId, entityView,
                    entityView.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strEntityViewId, customer.getId().toString(), customer.getName());

            sendEntityAssignToCustomerNotificationMsg(savedEntityView.getTenantId(), savedEntityView.getId(),
                    customer.getId(), EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER);

            return savedEntityView;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_VIEW), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strEntityViewId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Entity Views (getCustomerEntityViews)",
            notes = "Returns a page of Entity View objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getCustomerEntityViews(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @ApiParam(value = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ENTITY_VIEW_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerIdAndType(tenantId, customerId, pageLink, type));
            } else {
                return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Entity View info (getCustomerEntityViewInfos)",
            notes = "Returns a page of Entity View info objects assigned to customer. " + ENTITY_VIEW_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/entityViewInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityViewInfo> getCustomerEntityViewInfos(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @ApiParam(value = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ENTITY_VIEW_INFO_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Entity Views (getTenantEntityViews)",
            notes = "Returns a page of entity views owned by tenant. " + ENTITY_VIEW_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getTenantEntityViews(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @ApiParam(value = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ENTITY_VIEW_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);

            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewByTenantIdAndType(tenantId, pageLink, type));
            } else {
                return checkNotNull(entityViewService.findEntityViewByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Entity Views (getTenantEntityViews)",
            notes = "Returns a page of entity views info owned by tenant. " + ENTITY_VIEW_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/entityViewInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityViewInfo> getTenantEntityViewInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @ApiParam(value = ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ENTITY_VIEW_INFO_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find related entity views (findByQuery)",
            notes = "Returns all entity views that are related to the specific entity. " +
                    "The entity id, relation type, entity view types, depth of the search, and other query parameters defined using complex 'EntityViewSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityViews", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityView> findByQuery(
            @ApiParam(value = "The entity view search query JSON")
            @RequestBody EntityViewSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEntityViewTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<EntityView> entityViews = checkNotNull(entityViewService.findEntityViewsByQuery(getTenantId(), query).get());
            entityViews = entityViews.stream().filter(entityView -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ, entityView.getId(), entityView);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return entityViews;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Entity View Types (getEntityViewTypes)",
            notes = "Returns a set of unique entity view types based on entity views that are either owned by the tenant or assigned to the customer which user is performing the request."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getEntityViewTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> entityViewTypes = entityViewService.findEntityViewTypesByTenantId(tenantId);
            return checkNotNull(entityViewTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Make entity view publicly available (assignEntityViewToPublicCustomer)",
            notes = "Entity View will be available for non-authorized (not logged-in) users. " +
                    "This is useful to create dashboards that you plan to share/embed on a publicly available website. " +
                    "However, users that are logged-in and belong to different tenant will not be able to access the entity view." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/entityView/{entityViewId}", method = RequestMethod.POST)
    @ResponseBody
    public EntityView assignEntityViewToPublicCustomer(
            @ApiParam(value = ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            EntityView entityView = checkEntityViewId(entityViewId, Operation.ASSIGN_TO_CUSTOMER);
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(entityView.getTenantId());
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToCustomer(getCurrentUser().getTenantId(), entityViewId, publicCustomer.getId()));

            logEntityAction(entityViewId, savedEntityView,
                    savedEntityView.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strEntityViewId, publicCustomer.getId().toString(), publicCustomer.getName());

            return savedEntityView;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_VIEW), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strEntityViewId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Assign entity view to edge (assignEntityViewToEdge)",
            notes = "Creates assignment of an existing entity view to an instance of The Edge. " +
                    EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment entity view " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once entity view will be delivered to edge service, it's going to be available for usage on remote edge instance.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/entityView/{entityViewId}", method = RequestMethod.POST)
    @ResponseBody
    public EntityView assignEntityViewToEdge(@PathVariable(EDGE_ID) String strEdgeId,
                                             @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.READ);

            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            checkEntityViewId(entityViewId, Operation.READ);

            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToEdge(getTenantId(), entityViewId, edgeId));
            logEntityAction(entityViewId, savedEntityView,
                    savedEntityView.getCustomerId(),
                    ActionType.ASSIGNED_TO_EDGE, null, strEntityViewId, strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, savedEntityView.getId(), EdgeEventActionType.ASSIGNED_TO_EDGE);

            return savedEntityView;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_VIEW), null,
                    null,
                    ActionType.ASSIGNED_TO_EDGE, e, strEntityViewId, strEdgeId);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Unassign entity view from edge (unassignEntityViewFromEdge)",
            notes = "Clears assignment of the entity view to the edge. " +
                    EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove entity view " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove entity view locally.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/entityView/{entityViewId}", method = RequestMethod.DELETE)
    @ResponseBody
    public EntityView unassignEntityViewFromEdge(@PathVariable(EDGE_ID) String strEdgeId,
                                                 @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.READ);

            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            EntityView entityView = checkEntityViewId(entityViewId, Operation.READ);

            EntityView savedEntityView = checkNotNull(entityViewService.unassignEntityViewFromEdge(getTenantId(), entityViewId, edgeId));
            logEntityAction(entityViewId, entityView,
                    entityView.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_EDGE, null, strEntityViewId, strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, savedEntityView.getId(), EdgeEventActionType.UNASSIGNED_FROM_EDGE);

            return savedEntityView;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_VIEW), null,
                    null,
                    ActionType.UNASSIGNED_FROM_EDGE, e, strEntityViewId, strEdgeId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getEdgeEntityViews(
            @PathVariable(EDGE_ID) String strEdgeId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            checkEdgeId(edgeId, Operation.READ);
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            PageData<EntityView> nonFilteredResult;
            if (type != null && type.trim().length() > 0) {
                nonFilteredResult = entityViewService.findEntityViewsByTenantIdAndEdgeIdAndType(tenantId, edgeId, type, pageLink);
            } else {
                nonFilteredResult = entityViewService.findEntityViewsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            }
            List<EntityView> filteredEntityViews = nonFilteredResult.getData().stream().filter(entityView -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ, entityView.getId(), entityView);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            PageData<EntityView> filteredResult = new PageData<>(filteredEntityViews,
                    nonFilteredResult.getTotalPages(),
                    nonFilteredResult.getTotalElements(),
                    nonFilteredResult.hasNext());
            return checkNotNull(filteredResult);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
