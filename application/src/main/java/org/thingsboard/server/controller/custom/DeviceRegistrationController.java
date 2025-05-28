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
@RequiredArgsConstructor
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

        try {

            UUID deviceUUID = deviceRegistrationService.findDeviceByMacAndType(macId, request.getDeviceType());
            Device device = deviceUUID != null ? deviceRegistrationService.findDeviceById(deviceUUID.toString()) : null;
            if (deviceUUID == null || (device != null && !device.getType().equals(request.getDeviceType()))) {
                response.put("status", HttpStatus.BAD_REQUEST.value());
                response.put("message", "No matching device found for the given MAC ID and device type.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String deviceId = deviceUUID.toString();
            Customer customerMailOpt = deviceRegistrationService.findCustomerByEmail(request.getEmail());
            Customer customerDeviceOpt = deviceRegistrationService.findCustomerById(device.getCustomerId().getId().toString());
            String token = deviceRegistrationService.getDeviceAccessToken(deviceId);
            if (customerMailOpt != null) {
                if (device.getCustomerId() != null) {

                    if (customerDeviceOpt != null && customerDeviceOpt.getEmail().equals(request.getEmail())) {
                        response.put("status", HttpStatus.valueOf(202));
                        response.put("message", "Device is already assigned to this customer.");
                        response.put("accessToken", token);
                        return new ResponseEntity<>(response, HttpStatus.valueOf(202));
                    } else if (device.getCustomerId().getId().equals(NULL_UUID) || (customerDeviceOpt != null && customerDeviceOpt.getEmail().equals(request.getEmail()))) {
                        deviceRegistrationService.assignDeviceToCustomer(deviceId, customerMailOpt.getId().toString(), request.getDeviceName(), request.getEmail(), false);
                        response.put("status", HttpStatus.valueOf(201));
                        response.put("message", "Device successfully assigned to existing customer.");
                        response.put("accessToken", token);
                        return new ResponseEntity<>(response, HttpStatus.valueOf(201));
                    }
                }

            } else if (device.getCustomerId().getId().equals(NULL_UUID)) {
                Customer newCustomer = deviceRegistrationService.createCustomer(request.getEmail());
                deviceRegistrationService.assignDeviceToCustomer(deviceId, newCustomer.getId().toString(), request.getDeviceName(), request.getEmail(), true);
                response.put("status", HttpStatus.valueOf(200));
                response.put("message", "New customer created and device assigned successfully.");
                response.put("accessToken", token);
                return new ResponseEntity<>(response, HttpStatus.valueOf(200));
            } else {
                response.put("status", HttpStatus.valueOf(420));
                response.put("message", "Device appears to be already assigned to another customer.");
                return new ResponseEntity<>(response, HttpStatus.valueOf(420));
            }

        } catch (Exception e) {
            log.error("Error during device registration: {}", e.getMessage(), e);
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message", "An error occurred during device registration");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "No matching rules.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
