/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EntityRelationController extends BaseController {

    public static final String TO_TYPE = "toType";
    public static final String FROM_ID = "fromId";
    public static final String FROM_TYPE = "fromType";
    public static final String RELATION_TYPE = "relationType";
    public static final String TO_ID = "toId";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void saveRelation(@RequestBody EntityRelation relation) throws ThingsboardException {
        try {
            checkNotNull(relation);
            checkEntityId(relation.getFrom(), Operation.WRITE);
            checkEntityId(relation.getTo(), Operation.WRITE);
            if (relation.getTypeGroup() == null) {
                relation.setTypeGroup(RelationTypeGroup.COMMON);
            }
            relationService.saveRelation(getTenantId(), relation);

            logEntityAction(relation.getFrom(), null, getCurrentUser().getCustomerId(),
                    ActionType.RELATION_ADD_OR_UPDATE, null, relation);
            logEntityAction(relation.getTo(), null, getCurrentUser().getCustomerId(),
                    ActionType.RELATION_ADD_OR_UPDATE, null, relation);

            sendNotificationMsgToEdgeService(getTenantId(), relation, EdgeEventActionType.RELATION_ADD_OR_UPDATE);
        } catch (Exception e) {
            logEntityAction(relation.getFrom(), null, getCurrentUser().getCustomerId(),
                    ActionType.RELATION_ADD_OR_UPDATE, e, relation);
            logEntityAction(relation.getTo(), null, getCurrentUser().getCustomerId(),
                    ActionType.RELATION_ADD_OR_UPDATE, e, relation);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.DELETE, params = {FROM_ID, FROM_TYPE, RELATION_TYPE, TO_ID, TO_TYPE})
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRelation(@RequestParam(FROM_ID) String strFromId,
                               @RequestParam(FROM_TYPE) String strFromType,
                               @RequestParam(RELATION_TYPE) String strRelationType,
                               @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                               @RequestParam(TO_ID) String strToId, @RequestParam(TO_TYPE) String strToType) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        checkParameter(RELATION_TYPE, strRelationType);
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(fromId, Operation.WRITE);
        checkEntityId(toId, Operation.WRITE);
        RelationTypeGroup relationTypeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        EntityRelation relation = new EntityRelation(fromId, toId, strRelationType, relationTypeGroup);
        try {
            Boolean found = relationService.deleteRelation(getTenantId(), fromId, toId, strRelationType, relationTypeGroup);
            if (!found) {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
            logEntityAction(relation.getFrom(), null, getCurrentUser().getCustomerId(),
                    ActionType.RELATION_DELETED, null, relation);
            logEntityAction(relation.getTo(), null, getCurrentUser().getCustomerId(),
                    ActionType.RELATION_DELETED, null, relation);

            sendNotificationMsgToEdgeService(getTenantId(), relation, EdgeEventActionType.RELATION_DELETED);
        } catch (Exception e) {
            logEntityAction(relation.getFrom(), null, getCurrentUser().getCustomerId(),
                    ActionType.RELATION_DELETED, e, relation);
            logEntityAction(relation.getTo(), null, getCurrentUser().getCustomerId(),
                    ActionType.RELATION_DELETED, e, relation);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.DELETE, params = {"id", "type"})
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRelations(@RequestParam("entityId") String strId,
                                @RequestParam("entityType") String strType) throws ThingsboardException {
        checkParameter("entityId", strId);
        checkParameter("entityType", strType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strType, strId);
        checkEntityId(entityId, Operation.WRITE);
        try {
            relationService.deleteEntityRelations(getTenantId(), entityId);
            logEntityAction(entityId, null, getCurrentUser().getCustomerId(), ActionType.RELATIONS_DELETED, null);
        } catch (Exception e) {
            logEntityAction(entityId, null, getCurrentUser().getCustomerId(), ActionType.RELATIONS_DELETED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE, RELATION_TYPE, TO_ID, TO_TYPE})
    @ResponseBody
    public EntityRelation getRelation(@RequestParam(FROM_ID) String strFromId,
                                      @RequestParam(FROM_TYPE) String strFromType,
                                      @RequestParam(RELATION_TYPE) String strRelationType,
                                      @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                                      @RequestParam(TO_ID) String strToId, @RequestParam(TO_TYPE) String strToType) throws ThingsboardException {
        try {
            checkParameter(FROM_ID, strFromId);
            checkParameter(FROM_TYPE, strFromType);
            checkParameter(RELATION_TYPE, strRelationType);
            checkParameter(TO_ID, strToId);
            checkParameter(TO_TYPE, strToType);
            EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
            EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
            checkEntityId(fromId, Operation.READ);
            checkEntityId(toId, Operation.READ);
            RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
            return checkNotNull(relationService.getRelation(getTenantId(), fromId, toId, strRelationType, typeGroup));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE})
    @ResponseBody
    public List<EntityRelation> findByFrom(@RequestParam(FROM_ID) String strFromId,
                                           @RequestParam(FROM_TYPE) String strFromType,
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByFrom(getTenantId(), entityId, typeGroup)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE})
    @ResponseBody
    public List<EntityRelationInfo> findInfoByFrom(@RequestParam(FROM_ID) String strFromId,
                                                   @RequestParam(FROM_TYPE) String strFromType,
                                                   @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByFrom(getTenantId(), entityId, typeGroup).get()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE, RELATION_TYPE})
    @ResponseBody
    public List<EntityRelation> findByFrom(@RequestParam(FROM_ID) String strFromId,
                                           @RequestParam(FROM_TYPE) String strFromType,
                                           @RequestParam(RELATION_TYPE) String strRelationType,
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        checkParameter(RELATION_TYPE, strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByFromAndType(getTenantId(), entityId, strRelationType, typeGroup)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {TO_ID, TO_TYPE})
    @ResponseBody
    public List<EntityRelation> findByTo(@RequestParam(TO_ID) String strToId,
                                         @RequestParam(TO_TYPE) String strToType,
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByTo(getTenantId(), entityId, typeGroup)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.GET, params = {TO_ID, TO_TYPE})
    @ResponseBody
    public List<EntityRelationInfo> findInfoByTo(@RequestParam(TO_ID) String strToId,
                                                 @RequestParam(TO_TYPE) String strToType,
                                                 @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByTo(getTenantId(), entityId, typeGroup).get()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {TO_ID, TO_TYPE, RELATION_TYPE})
    @ResponseBody
    public List<EntityRelation> findByTo(@RequestParam(TO_ID) String strToId,
                                         @RequestParam(TO_TYPE) String strToType,
                                         @RequestParam(RELATION_TYPE) String strRelationType,
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        checkParameter(RELATION_TYPE, strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByToAndType(getTenantId(), entityId, strRelationType, typeGroup)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityRelation> findByQuery(@RequestBody EntityRelationsQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findByQuery(getTenantId(), query).get()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityRelationInfo> findInfoByQuery(@RequestBody EntityRelationsQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByQuery(getTenantId(), query).get()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private <T extends EntityRelation> List<T> filterRelationsByReadPermission(List<T> relationsByQuery) {
        return relationsByQuery.stream().filter(relationByQuery -> {
            try {
                checkEntityId(relationByQuery.getTo(), Operation.READ);
            } catch (ThingsboardException e) {
                return false;
            }
            try {
                checkEntityId(relationByQuery.getFrom(), Operation.READ);
            } catch (ThingsboardException e) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private RelationTypeGroup parseRelationTypeGroup(String strRelationTypeGroup, RelationTypeGroup defaultValue) {
        RelationTypeGroup result = defaultValue;
        if (strRelationTypeGroup != null && strRelationTypeGroup.trim().length() > 0) {
            try {
                result = RelationTypeGroup.valueOf(strRelationTypeGroup);
            } catch (IllegalArgumentException e) {
            }
        }
        return result;
    }

}
