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
package org.thingsboard.server.service.gateway;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.gateway.ConnectorType;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationResult;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationWarningRecord;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.service.gateway.validator.ConnectorConfigurationValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DefaultGatewayService implements GatewayService {

    private final List<ConnectorConfigurationValidator> validatorsList;

    private final Map<ConnectorType, ConnectorConfigurationValidator> validators = new HashMap<>();

    @Autowired
    AttributesService attributesService;

    @PostConstruct
    public void init() {
        validatorsList.forEach(validator -> validators.put(validator.getType(), validator));
    }

    @Override
    public GatewayConnectorValidationResult checkConnectorConfiguration(TenantId tenantId, DeviceId deviceId, String connectorType, Map<String, Object> connectorConfiguration) throws ThingsboardException {
        ConnectorType type = ConnectorType.valueOf(connectorType.toUpperCase());
        String version = null;
        try {
            Optional<AttributeKvEntry> gatewayVersion = attributesService.find(tenantId, deviceId, AttributeScope.CLIENT_SCOPE, "Version").get(10, TimeUnit.SECONDS);
            if (gatewayVersion.isPresent()) {
                version = gatewayVersion.get().getValueAsString();
            }
        } catch (Exception e) {
            throw new ThingsboardException("Failed to get gateway version", e, ThingsboardErrorCode.GENERAL);
        }
        if (!validators.containsKey(type)) {
            return new GatewayConnectorValidationResult(true, new ArrayList<>(),
                    Collections.singletonList(new GatewayConnectorValidationWarningRecord("", "No validator found for the connector type: " + type)));
        }
        return validators.get(type).validate(connectorConfiguration, version);
    }
}