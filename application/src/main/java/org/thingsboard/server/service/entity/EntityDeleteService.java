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
package org.thingsboard.server.service.entity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;

public interface EntityDeleteService {

    void  deleteEntity(TenantId tenantId, EntityId entityId)  throws ThingsboardException;

    Boolean  deleteAlarm( AlarmId alarmId)  throws ThingsboardException;

    DeferredResult<ResponseEntity> deleteReClaimDevice(String deviceName)  throws ThingsboardException;

    void  deleteRelation(EntityId fromId, EntityId toId, String strRelationType, String strRelationTypeGroup,
                         EntityRelation relation, RelationTypeGroup relationTypeGroup)  throws ThingsboardException;

    void  deleteRelations(EntityId entityId)  throws ThingsboardException;

    RuleChain deleteUnsetAutoAssignToEdgeRuleChain(RuleChainId ruleChainId)  throws ThingsboardException;

    RuleChain deleteUnassignRuleChain(String strRuleChainId, String strEdgeId)  throws ThingsboardException;
}
