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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.queue.TbQueueService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_NAME_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_QUEUE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_SERVICE_TYPE_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_SERVICE_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueueController extends BaseController {

    private final TbQueueService tbQueueService;

    @ApiOperation(value = "Get Queues (getTenantQueuesByServiceType)",
            notes = "Returns a page of queues registered in the platform. " +
                    PAGE_DATA_PARAMETERS + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/queues", params = {"serviceType", "pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Queue> getTenantQueuesByServiceType(@ApiParam(value = QUEUE_SERVICE_TYPE_DESCRIPTION, allowableValues = QUEUE_SERVICE_TYPE_ALLOWABLE_VALUES, required = true)
                                                        @RequestParam String serviceType,
                                                        @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                        @RequestParam int pageSize,
                                                        @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                        @RequestParam int page,
                                                        @ApiParam(value = QUEUE_QUEUE_TEXT_SEARCH_DESCRIPTION)
                                                        @RequestParam(required = false) String textSearch,
                                                        @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = QUEUE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                        @RequestParam(required = false) String sortProperty,
                                                        @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                        @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("serviceType", serviceType);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        ServiceType type = ServiceType.valueOf(serviceType);
        switch (type) {
            case TB_RULE_ENGINE:
                return queueService.findQueuesByTenantId(getTenantId(), pageLink);
            default:
                return new PageData<>();
        }
    }

    @ApiOperation(value = "Get Queue (getQueueById)",
            notes = "Fetch the Queue object based on the provided Queue Id. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/queues/{queueId}", method = RequestMethod.GET)
    @ResponseBody
    public Queue getQueueById(@ApiParam(value = QUEUE_ID_PARAM_DESCRIPTION)
                              @PathVariable("queueId") String queueIdStr) throws ThingsboardException {
        checkParameter("queueId", queueIdStr);
        QueueId queueId = new QueueId(UUID.fromString(queueIdStr));
        checkQueueId(queueId, Operation.READ);
        return checkNotNull(queueService.findQueueById(getTenantId(), queueId));
    }

    @ApiOperation(value = "Get Queue (getQueueByName)",
            notes = "Fetch the Queue object based on the provided Queue name. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/queues/name/{queueName}", method = RequestMethod.GET)
    @ResponseBody
    public Queue getQueueByName(@ApiParam(value = QUEUE_NAME_PARAM_DESCRIPTION)
                                @PathVariable("queueName") String queueName) throws ThingsboardException {
        checkParameter("queueName", queueName);
        return checkNotNull(queueService.findQueueByTenantIdAndName(getTenantId(), queueName));
    }

    @ApiOperation(value = "Create Or Update Queue (saveQueue)",
            notes = "Create or update the Queue. When creating queue, platform generates Queue Id as " + UUID_WIKI_LINK +
                    "Specify existing Queue id to update the queue. " +
                    "Referencing non-existing Queue Id will cause 'Not Found' error." +
                    "\n\nQueue name is unique in the scope of sysadmin. " +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Queue entity. " +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/queues", params = {"serviceType"}, method = RequestMethod.POST)
    @ResponseBody

    public Queue saveQueue(@ApiParam(value = "A JSON value representing the queue.")
                           @RequestBody Queue queue,
                           @ApiParam(value = QUEUE_SERVICE_TYPE_DESCRIPTION, allowableValues = QUEUE_SERVICE_TYPE_ALLOWABLE_VALUES, required = true)
                           @RequestParam String serviceType) throws ThingsboardException {
        checkParameter("serviceType", serviceType);
        queue.setTenantId(getCurrentUser().getTenantId());

        checkEntity(queue.getId(), queue, Resource.QUEUE);

        ServiceType type = ServiceType.valueOf(serviceType);
        switch (type) {
            case TB_RULE_ENGINE:
                queue.setTenantId(getTenantId());
                Queue savedQueue = tbQueueService.saveQueue(queue);
                checkNotNull(savedQueue);
                return savedQueue;
            default:
                return null;
        }
    }

    @ApiOperation(value = "Delete Queue (deleteQueue)", notes = "Deletes the Queue. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/queues/{queueId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteQueue(@ApiParam(value = QUEUE_ID_PARAM_DESCRIPTION)
                            @PathVariable("queueId") String queueIdStr) throws ThingsboardException {
        checkParameter("queueId", queueIdStr);
        QueueId queueId = new QueueId(toUUID(queueIdStr));
        checkQueueId(queueId, Operation.DELETE);
        tbQueueService.deleteQueue(getTenantId(), queueId);
    }
}
