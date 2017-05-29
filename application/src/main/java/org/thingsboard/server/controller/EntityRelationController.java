/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.dao.relation.EntityRelationsQuery;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.List;


@RestController
@RequestMapping("/api")
public class EntityRelationController extends BaseController {

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void saveRelation(@RequestBody EntityRelation relation) throws ThingsboardException {
        try {
            checkNotNull(relation);
            checkEntityId(relation.getFrom());
            checkEntityId(relation.getTo());
            relationService.saveRelation(relation).get();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.DELETE, params = {"fromId", "fromType", "relationType", "toId", "toType"})
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRelation(@RequestParam("fromId") String strFromId,
                               @RequestParam("fromType") String strFromType, @RequestParam("relationType") String strRelationType,
                               @RequestParam("toId") String strToId, @RequestParam("toType") String strToType) throws ThingsboardException {
        checkParameter("fromId", strFromId);
        checkParameter("fromType", strFromType);
        checkParameter("relationType", strRelationType);
        checkParameter("toId", strToId);
        checkParameter("toType", strToType);
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(fromId);
        checkEntityId(toId);
        try {
            Boolean found = relationService.deleteRelation(fromId, toId, strRelationType).get();
            if (!found) {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.DELETE, params = {"id", "type"})
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRelations(@RequestParam("entityId") String strId,
                                @RequestParam("entityType") String strType) throws ThingsboardException {
        checkParameter("entityId", strId);
        checkParameter("entityType", strType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strType, strId);
        checkEntityId(entityId);
        try {
            relationService.deleteEntityRelations(entityId).get();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.GET, params = {"fromId", "fromType", "relationType", "toId", "toType"})
    @ResponseStatus(value = HttpStatus.OK)
    public void checkRelation(@RequestParam("fromId") String strFromId,
                              @RequestParam("fromType") String strFromType, @RequestParam("relationType") String strRelationType,
                              @RequestParam("toId") String strToId, @RequestParam("toType") String strToType) throws ThingsboardException {
        try {
            checkParameter("fromId", strFromId);
            checkParameter("fromType", strFromType);
            checkParameter("relationType", strRelationType);
            checkParameter("toId", strToId);
            checkParameter("toType", strToType);
            EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
            EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
            checkEntityId(fromId);
            checkEntityId(toId);
            Boolean found = relationService.checkRelation(fromId, toId, strRelationType).get();
            if (!found) {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {"fromId", "fromType"})
    @ResponseBody
    public List<EntityRelation> findByFrom(@RequestParam("fromId") String strFromId, @RequestParam("fromType") String strFromType) throws ThingsboardException {
        checkParameter("fromId", strFromId);
        checkParameter("fromType", strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId);
        try {
            return checkNotNull(relationService.findByFrom(entityId).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.GET, params = {"fromId", "fromType"})
    @ResponseBody
    public List<EntityRelationInfo> findInfoByFrom(@RequestParam("fromId") String strFromId, @RequestParam("fromType") String strFromType) throws ThingsboardException {
        checkParameter("fromId", strFromId);
        checkParameter("fromType", strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId);
        try {
            return checkNotNull(relationService.findInfoByFrom(entityId).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {"fromId", "fromType", "relationType"})
    @ResponseBody
    public List<EntityRelation> findByFrom(@RequestParam("fromId") String strFromId, @RequestParam("fromType") String strFromType
            , @RequestParam("relationType") String strRelationType) throws ThingsboardException {
        checkParameter("fromId", strFromId);
        checkParameter("fromType", strFromType);
        checkParameter("relationType", strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId);
        try {
            return checkNotNull(relationService.findByFromAndType(entityId, strRelationType).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {"toId", "toType"})
    @ResponseBody
    public List<EntityRelation> findByTo(@RequestParam("toId") String strToId, @RequestParam("toType") String strToType) throws ThingsboardException {
        checkParameter("toId", strToId);
        checkParameter("toType", strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId);
        try {
            return checkNotNull(relationService.findByTo(entityId).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {"toId", "toType", "relationType"})
    @ResponseBody
    public List<EntityRelation> findByTo(@RequestParam("toId") String strToId, @RequestParam("toType") String strToType
            , @RequestParam("relationType") String strRelationType) throws ThingsboardException {
        checkParameter("toId", strToId);
        checkParameter("toType", strToType);
        checkParameter("relationType", strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId);
        try {
            return checkNotNull(relationService.findByToAndType(entityId, strRelationType).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityRelation> findByQuery(@RequestBody EntityRelationsQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId());
        try {
            return checkNotNull(relationService.findByQuery(query).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
