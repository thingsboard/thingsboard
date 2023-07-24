/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PROTOCOL;
import static org.thingsboard.server.controller.ControllerConstants.PROTOCOL_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTT_SSL_PEM_FILE_NAME;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DeviceConnectivityController extends BaseController {

    private final SystemSecurityService systemSecurityService;

    @ApiOperation(value = "Get commands to publish device telemetry (getDevicePublishTelemetryCommands)",
            notes = "Fetch the list of commands to publish device telemetry based on device profile " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the device is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the device is assigned to the same customer. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK",
                    examples = @io.swagger.annotations.Example(
                            value = {
                                    @io.swagger.annotations.ExampleProperty(
                                            mediaType="application/json",
                                            value="{\"http\":\"curl -v -X POST http://localhost:8080/api/v1/0ySs4FTOn5WU15XLmal8/telemetry --header Content-Type:application/json --data {temperature:25}\"," +
                                                    "\"mqtt\":\"mosquitto_pub -d -q 1 -h localhost -t v1/devices/me/telemetry -i myClient1 -u myUsername1 -P myPassword -m {temperature:25}\"," +
                                                    "\"coap\":\"coap-client -m POST coap://localhost:5683/api/v1/0ySs4FTOn5WU15XLmal8/telemetry -t json -e {temperature:25}\"}")}))})
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device-connectivity/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getDevicePublishTelemetryCommands(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                        @PathVariable(DEVICE_ID) String strDeviceId, HttpServletRequest request) throws ThingsboardException, URISyntaxException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.READ_CREDENTIALS);

        String baseUrl = systemSecurityService.getBaseUrl(getTenantId(), getCurrentUser().getCustomerId(), request);
        return deviceConnectivityService.findDevicePublishTelemetryCommands(baseUrl, device);
    }

    @ApiOperation(value = "Download mqtt ssl certificate using file path defined in device.connectivity properties (downloadMqttServerCertificate)", notes = "Download Mqtt server certificate." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @RequestMapping(value = "/device-connectivity/{protocol}/certificate/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> downloadMqttServerCertificate(@ApiParam(value = PROTOCOL_PARAM_DESCRIPTION)
                                                                                                  @PathVariable(PROTOCOL) String protocol) throws ThingsboardException, IOException {
        String certificate = checkSslServerPemFile(protocol);

        ByteArrayResource cert = new ByteArrayResource(certificate.getBytes());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + MQTT_SSL_PEM_FILE_NAME)
                .header("x-filename", MQTT_SSL_PEM_FILE_NAME)
                .contentLength(cert.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(cert);
    }

}
