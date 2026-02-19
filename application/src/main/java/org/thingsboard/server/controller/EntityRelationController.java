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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.entity.relation.TbEntityRelationService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RELATION_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RELATION_TYPE_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class EntityRelationController extends BaseController {

    private final TbEntityRelationService tbEntityRelationService;

    public static final String TO_TYPE = "toType";
    public static final String FROM_ID = "fromId";
    public static final String FROM_TYPE = "fromType";
    public static final String RELATION_TYPE = "relationType";
    public static final String TO_ID = "toId";

    private static final String SECURITY_CHECKS_ENTITIES_DESCRIPTION = "\n\nIf the user has the authority of 'System Administrator', the server checks that 'from' and 'to' entities are owned by the sysadmin. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that 'from' and 'to' entities are owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the 'from' and 'to' entities are assigned to the same customer.";

    private static final String SECURITY_CHECKS_ENTITY_DESCRIPTION = "\n\nIf the user has the authority of 'System Administrator', the server checks that the entity is owned by the sysadmin. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the entity is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the entity is assigned to the same customer.";

    @ApiOperation(value = "Create Relation (saveRelation)",
            notes = "Creates or updates a relation between two entities in the platform. " +
                    "Relations unique key is a combination of from/to entity id and relation type group and relation type. " +
                    SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/relation")
    public void saveRelation(@Parameter(description = "A JSON value representing the relation.", required = true)
                             @RequestBody EntityRelation relation) throws ThingsboardException {
        doSave(relation);
    }

    @ApiOperation(value = "Create Relation (saveRelationV2)",
            notes = "Creates or updates a relation between two entities in the platform. " +
                    "Relations unique key is a combination of from/to entity id and relation type group and relation type. " +
                    SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/v2/relation")
    public EntityRelation saveRelationV2(@Parameter(description = "A JSON value representing the relation.", required = true)
                                         @RequestBody EntityRelation relation) throws ThingsboardException {
        return doSave(relation);
    }

    private EntityRelation doSave(EntityRelation relation) throws ThingsboardException {
        if (relation.getTypeGroup() == null) {
            relation.setTypeGroup(RelationTypeGroup.COMMON);
        }
        ConstraintValidator.validateFields(relation);
        checkCanCreateRelation(relation.getFrom());
        checkCanCreateRelation(relation.getTo());
        return tbEntityRelationService.save(getTenantId(), getCurrentUser().getCustomerId(), relation, getCurrentUser());
    }

    @ApiOperation(value = "Delete Relation (deleteRelation)",
            notes = "Deletes a relation between two entities in the platform. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/relation")
    public void deleteRelation(@Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                               @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                               @Parameter(description = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                               @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                               @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                               @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType) throws ThingsboardException {
        doDelete(strFromId, strFromType, strRelationType, strRelationTypeGroup, strToId, strToType);
    }

    @ApiOperation(value = "Delete Relation (deleteRelationV2)",
            notes = "Deletes a relation between two entities in the platform. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/v2/relation")
    public EntityRelation deleteRelationV2(@Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                           @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                           @Parameter(description = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                                           @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                                           @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                           @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType) throws ThingsboardException {
        return doDelete(strFromId, strFromType, strRelationType, strRelationTypeGroup, strToId, strToType);
    }

    private EntityRelation doDelete(String strFromId, String strFromType, String strRelationType, String strRelationTypeGroup, String strToId, String strToType) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkCanCreateRelation(fromId);
        checkCanCreateRelation(toId);

        RelationTypeGroup relationTypeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        EntityRelation relation = new EntityRelation(fromId, toId, strRelationType, relationTypeGroup);
        return tbEntityRelationService.delete(getTenantId(), getCurrentUser().getCustomerId(), relation, getCurrentUser());
    }

    @ApiOperation(value = "Delete common relations (deleteCommonRelations)",
            notes = "Deletes all the relations ('from' and 'to' direction) for the specified entity and relation type group: 'COMMON'. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/relations")
    public void deleteRelations(@Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam("entityId") String strId,
                                @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam("entityType") String strType) throws ThingsboardException {
        checkParameter("entityId", strId);
        checkParameter("entityType", strType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strType, strId);
        checkEntityId(entityId, Operation.WRITE);
        tbEntityRelationService.deleteCommonRelations(getTenantId(), getCurrentUser().getCustomerId(), entityId, getCurrentUser());
    }

    @ApiOperation(value = "Get Relation (getRelation)",
            notes = "Returns relation object between two specified entities if present. Otherwise throws exception. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relation")
    public EntityRelation getRelation(@Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                      @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                      @Parameter(description = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                                      @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                                      @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                      @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(fromId, Operation.READ);
        checkEntityId(toId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(relationService.getRelation(getTenantId(), fromId, toId, strRelationType, typeGroup));
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations", params = {FROM_ID, FROM_TYPE})
    public List<EntityRelation> findByFrom(@RequestParam(FROM_ID) String strFromId,
                                           @RequestParam(FROM_TYPE) String strFromType,
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findByFrom(getTenantId(), entityId, typeGroup)));
    }

    @ApiOperation(value = "Get List of Relations (findByFromV2)",
            notes = "Returns list of relation objects for the specified entity by the 'from' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations/from/{fromType}/{fromId}")
    public List<EntityRelation> findByFromV2(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @PathVariable(FROM_TYPE) String strFromType,
                                             @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable(FROM_ID) String strFromId,
                                             @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                             @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        return findByFrom(strFromId, strFromType, strRelationTypeGroup);
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations/info", params = {FROM_ID, FROM_TYPE})
    public List<EntityRelationInfo> findInfoByFrom(@Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                                   @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                                   @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                                   @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException, ExecutionException, InterruptedException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByFrom(getTenantId(), entityId, typeGroup).get()));
    }

    @ApiOperation(value = "Get List of Relation Infos (findInfoByFromV2)",
            notes = "Returns list of relation info objects for the specified entity by the 'from' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION + " " + RELATION_INFO_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations/info/from/{fromType}/{fromId}")
    public List<EntityRelationInfo> findInfoByFromV2(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @PathVariable(FROM_TYPE) String strFromType,
                                                     @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable(FROM_ID) String strFromId,
                                                     @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                                     @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException, ExecutionException, InterruptedException {
        return findInfoByFrom(strFromId, strFromType, strRelationTypeGroup);
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations", params = {FROM_ID, FROM_TYPE, RELATION_TYPE})
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
        return checkNotNull(filterRelationsByReadPermission(relationService.findByFromAndType(getTenantId(), entityId, strRelationType, typeGroup)));
    }

    @ApiOperation(value = "Get List of Relations (findByFromAndRelationType)",
            notes = "Returns list of relation objects for the specified entity by the 'from' direction and relation type. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations/from/{fromType}/{fromId}/{relationType}")
    public List<EntityRelation> findByFromAndRelationType(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @PathVariable(FROM_TYPE) String strFromType,
                                             @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable(FROM_ID) String strFromId,
                                             @Parameter(description = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @PathVariable(RELATION_TYPE) String strRelationType,
                                             @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                             @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        return findByFrom(strFromId, strFromType, strRelationType, strRelationTypeGroup);
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations", params = {TO_ID, TO_TYPE})
    public List<EntityRelation> findByTo(@Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                         @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType,
                                         @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findByTo(getTenantId(), entityId, typeGroup)));
    }

    @ApiOperation(value = "Get List of Relations (findByToV2)",
            notes = "Returns list of relation objects for the specified entity by the 'to' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations/to/{toType}/{toId}")
    public List<EntityRelation> findByToV2(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @PathVariable(TO_TYPE) String strToType,
                                           @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable(TO_ID) String strToId,
                                           @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        return findByTo(strToId, strToType, strRelationTypeGroup);
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations/info", params = {TO_ID, TO_TYPE})
    public List<EntityRelationInfo> findInfoByTo(@Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                                 @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType,
                                                 @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                                 @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException, ExecutionException, InterruptedException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByTo(getTenantId(), entityId, typeGroup).get()));
    }

    @ApiOperation(value = "Get List of Relation Infos (findInfoByToV2)",
            notes = "Returns list of relation info objects for the specified entity by the 'to' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION + " " + RELATION_INFO_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations/info/to/{toType}/{toId}")
    public List<EntityRelationInfo> findInfoByToV2(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @PathVariable(TO_TYPE) String strToType,
                                                   @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable(TO_ID) String strToId,
                                                   @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                                   @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException, ExecutionException, InterruptedException {
        return findInfoByTo(strToId, strToType, strRelationTypeGroup);
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations", params = {TO_ID, TO_TYPE, RELATION_TYPE})
    public List<EntityRelation> findByTo(@Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                         @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType,
                                         @Parameter(description = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                                         @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        checkParameter(RELATION_TYPE, strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findByToAndType(getTenantId(), entityId, strRelationType, typeGroup)));
    }

    @ApiOperation(value = "Get List of Relations (findByToAndRelationType)",
            notes = "Returns list of relation objects for the specified entity by the 'to' direction and relation type. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/relations/to/{toType}/{toId}/{relationType}")
    public List<EntityRelation> findByToAndRelationType(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @PathVariable(TO_TYPE) String strToType,
                                           @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable(TO_ID) String strToId,
                                           @Parameter(description = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @PathVariable(RELATION_TYPE) String strRelationType,
                                           @Parameter(description = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        return findByTo(strToId, strToType, strRelationType, strRelationTypeGroup);
    }

    @ApiOperation(value = "Find related entities (findByQuery)",
            notes = "Returns all entities that are related to the specific entity. " +
                    "The entity id, relation type, entity types, depth of the search, and other query parameters defined using complex 'EntityRelationsQuery' object. " +
                    "See 'Model' tab of the Parameters for more info.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/relations")
    public List<EntityRelation> findByQuery(@Parameter(description = "A JSON value representing the entity relations query object.", required = true)
                                            @RequestBody EntityRelationsQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        return checkNotNull(filterRelationsByReadPermission(relationService.findByQuery(getTenantId(), query).get()));
    }

    @ApiOperation(value = "Find related entity infos (findInfoByQuery)",
            notes = "Returns all entity infos that are related to the specific entity. " +
                    "The entity id, relation type, entity types, depth of the search, and other query parameters defined using complex 'EntityRelationsQuery' object. " +
                    "See 'Model' tab of the Parameters for more info. " + RELATION_INFO_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/relations/info")
    public List<EntityRelationInfo> findInfoByQuery(@Parameter(description = "A JSON value representing the entity relations query object.", required = true)
                                                    @RequestBody EntityRelationsQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByQuery(getTenantId(), query).get()));
    }

    private void checkCanCreateRelation(EntityId entityId) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        var isTenantAdminAndRelateToSelf = currentUser.isTenantAdmin() && currentUser.getTenantId().equals(entityId);
        if (!isTenantAdminAndRelateToSelf) {
            checkEntityId(entityId, Operation.WRITE);
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
        }).toList();
    }

    private static RelationTypeGroup parseRelationTypeGroup(String strRelationTypeGroup, RelationTypeGroup defaultValue) {
        if (StringUtils.isBlank(strRelationTypeGroup)) {
            return defaultValue;
        }
        try {
            return RelationTypeGroup.valueOf(strRelationTypeGroup);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

}
