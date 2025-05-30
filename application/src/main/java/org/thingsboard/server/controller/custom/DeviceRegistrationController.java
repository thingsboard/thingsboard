/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.controller.custom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.custom.DeviceRegistrationService;
import org.thingsboard.server.service.custom.DeviceRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class DeviceRegistrationController extends BaseController {

    private static final UUID NULL_UUID = UUID.fromString("13814000-1dd2-11b2-8080-808080808080");

    @Autowired
    private DeviceRegistrationService deviceRegistrationService;

    @PostMapping("/device/registerDevice")
    public ResponseEntity<Map<String, Object>> registerDevice(
            @RequestHeader("Mac_id") String macId,
            @RequestBody DeviceRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();
        String deviceId = null;

        try {
            UUID deviceUUID = deviceRegistrationService.findDeviceByMacAndType(macId, request.getDeviceType());
            Device device = (deviceUUID != null) ? deviceRegistrationService.findDeviceById(deviceUUID.toString()) : null;

            if (device == null || !request.getDeviceType().equals(device.getType())) {
                return buildResponse(400, "No matching device found for the given MAC ID and device type.");
            }

            Customer requestCustomer = deviceRegistrationService.findCustomerByEmail(request.getEmail());
            Customer currentCustomer = deviceRegistrationService.findCustomerById(device.getCustomerId().getId().toString());
            String token = deviceRegistrationService.getDeviceAccessToken(deviceUUID.toString());
            deviceId = deviceUUID.toString();
            if (requestCustomer != null) {
                boolean isAssigned = device.getCustomerId() != null && currentCustomer != null;
                boolean isSameCustomer = isAssigned && currentCustomer.getEmail().equals(request.getEmail());

                if (isSameCustomer) {
                    return buildResponse(202, "Device is already assigned to this customer.", token);
                } else if (device.getCustomerId().getId().equals(NULL_UUID)) {
                    deviceRegistrationService.assignDeviceToCustomer(deviceId, requestCustomer.getId().toString(), request.getDeviceName(), request.getEmail(), false);
                    return buildResponse(201, "Device assigned to existing customer.", token);
                } else {
                    return buildResponse(420, "Device is already assigned to another customer.");
                }
            } else if (device.getCustomerId().getId().equals(NULL_UUID)) {
                Customer newCustomer = deviceRegistrationService.createCustomer(request.getEmail());
                deviceRegistrationService.assignDeviceToCustomer(deviceId, newCustomer.getId().toString(), request.getDeviceName(), request.getEmail(), true);
                return buildResponse(200, "New customer created and device assigned.", token);
            } else {
                return buildResponse(420, "Device is already assigned to another customer.");
            }

        } catch (Exception e) {
            log.error("Error during device registration: {}", e.getMessage(), e);
            if (deviceId != null) {
                deviceRegistrationService.unassignCustomerFromDevice(deviceId);
            }
            return buildResponse(500, "Internal error: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> buildResponse(int statusCode, String message) {
        return buildResponse(statusCode, message, null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(int statusCode, String message, String token) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", statusCode);
        body.put("message", message);
        if (token != null) {
            body.put("accessToken", token);
        }
        return ResponseEntity.status(statusCode).body(body);
    }

}
