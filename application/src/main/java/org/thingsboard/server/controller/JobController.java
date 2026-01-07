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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.JobManager;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class JobController extends BaseController {

    private final JobManager jobManager;

    @GetMapping("/job/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public Job getJobById(@PathVariable UUID id) throws ThingsboardException {
        JobId jobId = new JobId(id);
        return checkJobId(jobId, Operation.READ);
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public PageData<Job> getJobs(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                 @RequestParam int pageSize,
                                 @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                 @RequestParam int page,
                                 @Parameter(description = "Case-insensitive 'substring' filter based on job's description")
                                 @RequestParam(required = false) String textSearch,
                                 @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                 @RequestParam(required = false) String sortProperty,
                                 @Parameter(description = SORT_ORDER_DESCRIPTION)
                                 @RequestParam(required = false) String sortOrder,
                                 @RequestParam(required = false) List<JobType> types,
                                 @RequestParam(required = false) List<JobStatus> statuses,
                                 @RequestParam(required = false) List<UUID> entities,
                                 @RequestParam(required = false) Long startTime,
                                 @RequestParam(required = false) Long endTime) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        JobFilter filter = JobFilter.builder()
                .types(types)
                .statuses(statuses)
                .entities(entities)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return jobService.findJobsByFilter(getTenantId(), filter, pageLink);
    }

    @PostMapping("/job/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public void cancelJob(@PathVariable UUID id) throws ThingsboardException {
        JobId jobId = new JobId(id);
        checkJobId(jobId, Operation.WRITE);
        jobManager.cancelJob(getTenantId(), jobId);
    }

    @PostMapping("/job/{id}/reprocess")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public void reprocessJob(@PathVariable UUID id) throws ThingsboardException {
        JobId jobId = new JobId(id);
        checkJobId(jobId, Operation.WRITE);
        jobManager.reprocessJob(getTenantId(), jobId);
    }

    @DeleteMapping("/job/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public void deleteJob(@PathVariable UUID id) throws ThingsboardException {
        JobId jobId = new JobId(id);
        checkJobId(jobId, Operation.DELETE);
        jobService.deleteJob(getTenantId(), jobId);
    }

}
