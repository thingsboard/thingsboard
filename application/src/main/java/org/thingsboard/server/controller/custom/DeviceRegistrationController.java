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
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.custom.DeviceRegistrationService;
import org.thingsboard.server.service.custom.DeviceRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DeviceRegistrationController extends BaseController {

    @Autowired
    private DeviceRegistrationService deviceRegistrationService;

    @PostMapping("/device/registerDevice")
    public ResponseEntity<Map<String, Object>> registerDevice(
            @RequestHeader("Mac_id") String macId,
            @RequestBody DeviceRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            Optional<Map<String, Object>> existingDeviceOpt = deviceRegistrationService.findDeviceByMac(macId);
            if (existingDeviceOpt.isEmpty()) {
                response.put("status", HttpStatus.BAD_REQUEST.value());
                response.put("message", "Device not recognized");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            Map<String, Object> existingDevice = existingDeviceOpt.get();
            String deviceId = (String) ((Map<String, Object>) existingDevice.get("id")).get("id");

            Optional<Map<String, Object>> customerOpt = deviceRegistrationService.findCustomerByEmail(request.getEmail());

            if (customerOpt.isPresent()) {
                Map<String, Object> customer = customerOpt.get();

                Map<String, Object> customerIdMap = (Map<String, Object>) existingDevice.get("customerId");
                if (customerIdMap != null && !"NULL_UUID".equals(customerIdMap.get("id"))) {
                    response.put("status", HttpStatus.CONFLICT.value());
                    response.put("message", "Device is already registered with a customer");
                    return new ResponseEntity<>(response, HttpStatus.valueOf(420));
                }

                deviceRegistrationService.assignDeviceToCustomer(deviceId, (String) customer.get("id"), request.getDeviceName(), request.getEmail());
                String token = deviceRegistrationService.getDeviceAccessToken(deviceId);

                response.put("status", HttpStatus.CREATED.value());
                response.put("message", "Device assigned to existing customer");
                response.put("accessToken", token);
                return new ResponseEntity<>(response, HttpStatus.CREATED);

            } else {
                Customer newCustomer = deviceRegistrationService.createCustomer(request.getEmail());
                deviceRegistrationService.assignDeviceToCustomer(deviceId, newCustomer.getId().toString(), request.getDeviceName(), request.getEmail());
                String token = deviceRegistrationService.getDeviceAccessToken(deviceId);

                response.put("status", HttpStatus.CREATED.value());
                response.put("message", "Device assigned to new customer");
                response.put("accessToken", token);
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            }

        } catch (Exception e) {
            log.error("Error during device registration: {}", e.getMessage(), e);
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message", "An error occurred during device registration");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
