/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.queue.QueueStatsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_STATS_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.QUEUE_STATS_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueueStatsController extends BaseController {

    private final QueueStatsService queueStatsService;

    @ApiOperation(value = "Get Queue Stats entities (getTenantQueueStats)",
            notes = "Returns a page of queue stats objects that are designed to collect queue statistics for every service. " +
                    PAGE_DATA_PARAMETERS + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/queueStats", params = {"pageSize", "page"})
    public PageData<QueueStats> getTenantQueueStats(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                    @RequestParam int pageSize,
                                                    @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                    @RequestParam int page,
                                                    @Parameter(description = QUEUE_STATS_TEXT_SEARCH_DESCRIPTION)
                                                    @RequestParam(required = false) String textSearch,
                                                    @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime"}))
                                                    @RequestParam(required = false) String sortProperty,
                                                    @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                                    @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return queueStatsService.findByTenantId(getTenantId(), pageLink);
    }

    @ApiOperation(value = "Get Queue stats entity by id (getQueueStatsById)",
            notes = "Fetch the Queue stats object based on the provided Queue stats id. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/queueStats/{queueStatsId}")
    public QueueStats getQueueStatsById(@Parameter(description = QUEUE_STATS_ID_PARAM_DESCRIPTION)
                                        @PathVariable("queueStatsId") String queueStatsIdStr) throws ThingsboardException {
        checkParameter("queueStatsId", queueStatsIdStr);
        QueueStatsId queueStatsId = new QueueStatsId(UUID.fromString(queueStatsIdStr));
        return checkNotNull(queueStatsService.findQueueStatsById(getTenantId(), queueStatsId));
    }

    @ApiOperation(value = "Get QueueStats By Ids (getQueueStatsByIds)",
            notes = "Fetch the Queue stats objects based on the provided ids. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/queueStats", params = {"queueStatsIds"})
    public List<QueueStats> getQueueStatsByIds(
            @Parameter(description = "A list of queue stats ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("queueStatsIds") String[] strQueueStatsIds) throws ThingsboardException {
        checkArrayParameter("queueStatsIds", strQueueStatsIds);
        List<QueueStatsId> queueStatsIds = new ArrayList<>();
        for (String queueStatsId : strQueueStatsIds) {
            queueStatsIds.add(new QueueStatsId(toUUID(queueStatsId)));
        }
        return queueStatsService.findQueueStatsByIds(getTenantId(), queueStatsIds);
    }
}
