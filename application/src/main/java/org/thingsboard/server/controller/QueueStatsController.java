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

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.ArrayList;
import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class QueueStatsController extends BaseController {

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/queueStats", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<QueueStats> getTenantQueuesByServiceType(@RequestParam int pageSize,
                                                             @RequestParam int page,
                                                             @RequestParam(required = false) String textSearch,
                                                             @RequestParam(required = false) String sortProperty,
                                                             @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return queueStatsService.findQueueStats(getTenantId(), pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/queueStats/{queueStatsId}", method = RequestMethod.GET)
    @ResponseBody
    public QueueStats getRuleChainById(@PathVariable("queueStatsId") String queueStatsIdStr) throws ThingsboardException {
        checkParameter("queueStatsId", queueStatsIdStr);
        try {
            QueueStatsId queueStatsId = new QueueStatsId(toUUID(queueStatsIdStr));
            return checkQueueStatsId(queueStatsId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/queueStats", params = {"queueStatsIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<QueueStats> getDevicesByIds(
            @RequestParam("queueStatsIds") String[] queueStatsIdsStr) throws ThingsboardException {
        checkArrayParameter("queueStatsIds", queueStatsIdsStr);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<QueueStatsId> queueStatsIds = new ArrayList<>();
            for (String queueStatsId : queueStatsIdsStr) {
                queueStatsIds.add(new QueueStatsId(toUUID(queueStatsId)));
            }
            ListenableFuture<List<QueueStats>> queueStats = queueStatsService.findQueueStatsByTenantIdAndIdsAsync(tenantId, queueStatsIds);

            return checkNotNull(queueStats.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
