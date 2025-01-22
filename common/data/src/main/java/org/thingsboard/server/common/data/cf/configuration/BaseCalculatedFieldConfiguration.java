/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration;

import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldLinkConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public abstract class BaseCalculatedFieldConfiguration implements CalculatedFieldConfiguration {

    protected Map<String, Argument> arguments;
    protected String expression;
    protected Output output;

    @Override
    public List<EntityId> getReferencedEntities() {
        return arguments.values().stream()
                .map(Argument::getRefEntityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public CalculatedFieldLinkConfiguration getReferencedEntityConfig(EntityId entityId) {
        CalculatedFieldLinkConfiguration linkConfiguration = new CalculatedFieldLinkConfiguration();

        arguments.entrySet().stream()
                .filter(entry -> entry.getValue().getRefEntityId() != null && entry.getValue().getRefEntityId().equals(entityId))
                .forEach(entry -> {
                    ReferencedEntityKey refEntityKey = entry.getValue().getRefEntityKey();
                    String argumentName = entry.getKey();

                    switch (refEntityKey.getType()) {
                        case ATTRIBUTE -> {
                            switch (refEntityKey.getScope()) {
                                case CLIENT_SCOPE ->
                                        linkConfiguration.getClientAttributes().put(refEntityKey.getKey(), argumentName);
                                case SERVER_SCOPE ->
                                        linkConfiguration.getServerAttributes().put(refEntityKey.getKey(), argumentName);
                                case SHARED_SCOPE ->
                                        linkConfiguration.getSharedAttributes().put(refEntityKey.getKey(), argumentName);
                            }
                        }
                        case TS_LATEST, TS_ROLLING ->
                                linkConfiguration.getTimeSeries().put(refEntityKey.getKey(), argumentName);
                    }
                });

        return linkConfiguration;
    }

    @Override
    public List<CalculatedFieldLink> buildCalculatedFieldLinks(TenantId tenantId, EntityId cfEntityId, CalculatedFieldId calculatedFieldId) {
        return getReferencedEntities().stream()
                .filter(referencedEntity -> !referencedEntity.equals(cfEntityId))
                .map(referencedEntityId -> buildCalculatedFieldLink(tenantId, referencedEntityId, calculatedFieldId))
                .collect(Collectors.toList());
    }

    @Override
    public CalculatedFieldLink buildCalculatedFieldLink(TenantId tenantId, EntityId referencedEntityId, CalculatedFieldId calculatedFieldId) {
        CalculatedFieldLink link = new CalculatedFieldLink();
        link.setTenantId(tenantId);
        link.setEntityId(referencedEntityId);
        link.setCalculatedFieldId(calculatedFieldId);
        link.setConfiguration(getReferencedEntityConfig(referencedEntityId));
        return link;
    }

}
