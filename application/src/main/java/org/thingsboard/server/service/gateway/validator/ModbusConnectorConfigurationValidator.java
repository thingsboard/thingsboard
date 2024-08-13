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
package org.thingsboard.server.service.gateway.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.gateway.ConnectorType;
import org.thingsboard.server.common.data.gateway.connector.modbus.LatestModbusConnectorConfiguration;
import org.thingsboard.server.common.data.gateway.connector.modbus.Version352OrBelowModbusConnectorConfiguration;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationResult;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.regex.Pattern;

@Component
@TbCoreComponent
public class ModbusConnectorConfigurationValidator extends ConnectorConfigurationValidator {

    private static final Pattern latestVersionPattern = Pattern.compile("^3\\.[5-9]\\.[2-9].*$|^[4-9]\\..*$|^[1-9][0-9]+\\..*");

    @Override
    public GatewayConnectorValidationResult validate(String connectorConfiguration, String version) {
        if (version == null || version.matches(latestVersionPattern.pattern())) {
            return validateLatestConfiguration(connectorConfiguration);
        } else {
            return validateOldConfiguration(connectorConfiguration);
        }
    }

    private GatewayConnectorValidationResult validateLatestConfiguration(String connectorConfiguration) {
        return validateConfiguration(connectorConfiguration, LatestModbusConnectorConfiguration.class);
    }

    private GatewayConnectorValidationResult validateOldConfiguration(String connectorConfiguration) {
        return validateConfiguration(connectorConfiguration, Version352OrBelowModbusConnectorConfiguration.class);
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.MODBUS;
    }
}
