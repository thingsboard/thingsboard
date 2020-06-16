/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class QueueController extends BaseController {

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/queues", params = {"serviceType"}, method = RequestMethod.GET)
    @ResponseBody
    public List<String> getTenantQueuesByServiceType(@RequestParam String serviceType) throws ThingsboardException {
        checkParameter("serviceType", serviceType);
        try {
            ServiceType type = ServiceType.valueOf(serviceType);
            switch (type) {
                case TB_RULE_ENGINE:
                    return Arrays.asList("Main", "HighPriority", "SequentialByOriginator");
                default:
                    return Collections.emptyList();
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
