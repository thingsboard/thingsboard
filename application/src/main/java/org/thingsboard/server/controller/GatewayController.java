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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationResult;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.gateway.GatewayService;
import org.thingsboard.server.service.security.permission.Operation;

import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class GatewayController extends BaseController {

    private final GatewayService gatewayService;

    @ApiOperation(value = "Check connector configuration (checkConnectorConfiguration)",
            notes = "Check connector configuration for gateway connector" + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/gateway/{deviceId}/configuration/{connectorType}/validate", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<GatewayConnectorValidationResult> checkConnectorConfiguration(@RequestBody String connectorConfiguration,
                                                                                        @Parameter(description = DEVICE_ID_PARAM_DESCRIPTION, required = true)
                                                                                        @PathVariable(DEVICE_ID) String strDeviceId,
                                                                                        @Parameter(description = "A string value representing connector type: [MQTT, MODBUS, OPCUA, BACNET, BLE, CAN,FTP, OCPP, ODBC, REQUEST, REST, SNMP, SOCKET, XMPP]", required = true)
                                                                                        @PathVariable("connectorType") String connectorType) throws ThingsboardException {
        checkParameter("deviceId", strDeviceId);
        checkParameter("connectorType", connectorType);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        checkDeviceId(deviceId, Operation.WRITE_ATTRIBUTES);
        GatewayConnectorValidationResult result = gatewayService.checkConnectorConfiguration(getTenantId(), deviceId, connectorType, connectorConfiguration);
        return result.isValid() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

}
