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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Collections;
import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class QueueController extends BaseController {

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    private PartitionService partitionService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/queues", params = {"serviceType"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Queue> getTenantQueuesByServiceType(@RequestParam String serviceType) throws ThingsboardException {
        checkParameter("serviceType", serviceType);
        try {
            ServiceType type = ServiceType.valueOf(serviceType);
            switch (type) {
                case TB_RULE_ENGINE:
                    return queueService.findQueues(getTenantId());
                default:
                    return Collections.emptyList();
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/queues", params = {"serviceType"}, method = RequestMethod.POST)
    @ResponseBody
    public Queue saveQueue(@RequestBody Queue queue,
                           @RequestParam String serviceType) throws ThingsboardException {
        checkParameter("serviceType", serviceType);
        checkNotNull(queue);
        try {
            queue.setTenantId(getCurrentUser().getTenantId());
            Operation operation = queue.getId() == null ? Operation.CREATE : Operation.WRITE;

            if (operation == Operation.WRITE) {
                checkQueueId(queue.getId(), operation);
            }

            accessControlService.checkPermission(getCurrentUser(), Resource.QUEUE, operation, queue.getId(), queue);
            ServiceType type = ServiceType.valueOf(serviceType);
            switch (type) {
                case TB_RULE_ENGINE:
                    queue.setTenantId(getTenantId());
                    Queue savedQueue = queueService.createOrUpdateQueue(queue);
                    checkNotNull(savedQueue);
                    partitionService.recalculatePartitions(serviceInfoProvider.getServiceInfo(), Collections.emptyList());
                    return savedQueue;
                default:
                    return null;
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/queues/{queueId}", params = {"serviceType"}, method = RequestMethod.DELETE)
    @ResponseBody
    public boolean deleteQueue(@RequestParam String serviceType,
                               @PathVariable("queueId") String queueIdStr) throws ThingsboardException {
        checkParameter("serviceType", serviceType);
        checkParameter("queueId", queueIdStr);
        try {
            ServiceType type = ServiceType.valueOf(serviceType);
            QueueId queueId = new QueueId(toUUID(queueIdStr));
            checkQueueId(queueId, Operation.DELETE);
            switch (type) {
                case TB_RULE_ENGINE:
                    return queueService.deleteQueue(getTenantId(), queueId);
                default:
                    return false;
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
