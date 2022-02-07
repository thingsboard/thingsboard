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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.QueueService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Set;

import static org.thingsboard.server.controller.ControllerConstants.QUEUE_SERVICE_TYPE_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_SERVICE_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueueController extends BaseController {

    private final QueueService queueService;

    @ApiOperation(value = "Get queue names (getTenantQueuesByServiceType)",
            notes = "Returns a set of unique queue names based on service type. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/queues", params = {"serviceType"}, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody()
    public Set<String> getTenantQueuesByServiceType(@ApiParam(value = QUEUE_SERVICE_TYPE_DESCRIPTION, allowableValues = QUEUE_SERVICE_TYPE_ALLOWABLE_VALUES)
                                                    @RequestParam String serviceType) throws ThingsboardException {
        checkParameter("serviceType", serviceType);
        try {
            return queueService.getQueuesByServiceType(ServiceType.valueOf(serviceType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
