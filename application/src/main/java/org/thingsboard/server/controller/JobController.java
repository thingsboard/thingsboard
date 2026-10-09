// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class JobController extends BaseController {

    private final JobManager jobManager;

    @ApiOperation(value = "Get job by id (getJobById)",
            notes = "Fetches job info by id." + NEW_LINE +
                    "Example of a RUNNING CF_REPROCESSING job response:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"id\": {\n" +
                    "    \"entityType\": \"JOB\",\n" +
                    "    \"id\": \"475e94e0-2f2d-11f0-8240-91e99922a704\"\n" +
                    "  },\n" +
                    "  \"createdTime\": 1747053196590,\n" +
                    "  \"tenantId\": {\n" +
                    "    \"entityType\": \"TENANT\",\n" +
                    "    \"id\": \"46859a00-2f2d-11f0-8240-91e99922a704\"\n" +
                    "  },\n" +
                    "  \"type\": \"CF_REPROCESSING\",\n" +
                    "  \"key\": \"474e4130-2f2d-11f0-8240-91e99922a704\",\n" +
                    "  \"entityId\": {\n" +
                    "    \"entityType\": \"DEVICE_PROFILE\",\n" +
                    "    \"id\": \"9fd41f20-31a1-11f0-933e-27998d6db02e\"\n" +
                    "   },\n" +
                    "  \"status\": \"RUNNING\",\n" +
                    "  \"configuration\": {\n" +
                    "    \"type\": \"CF_REPROCESSING\",\n" +
                    "    \"calculatedFieldId\": {\n" +
                    "      \"entityType\": \"CALCULATED_FIELD\",\n" +
                    "      \"id\": \"474e4130-2f2d-11f0-8240-91e99922a704\"\n" +
                    "    },\n" +
                    "    \"startTs\": 1747051995760,\n" +
                    "    \"endTs\": 1747052895760,\n" +
                    "    \"tasksKey\": \"c3cdbd42-799e-4d3a-9aad-9310f767aa36\",\n" +
                    "    \"toReprocess\": null\n" +
                    "  },\n" +
                    "  \"result\": {\n" +
                    "    \"jobType\": \"CF_REPROCESSING\",\n" +
                    "    \"successfulCount\": 1,\n" +
                    "    \"failedCount\": 0,\n" +
                    "    \"discardedCount\": 0,\n" +
                    "    \"totalCount\": 2,\n" +
                    "    \"results\": [],\n" +
                    "    \"generalError\": null,\n" +
                    "    \"startTs\": 1747323069445,\n" +
                    "    \"finishTs\": 1747323070585,\n" +
                    "    \"cancellationTs\": 0\n" +
                    "  }\n" +
                    "}\n" +
                    MARKDOWN_CODE_BLOCK_END + NEW_LINE +
                    "Example of a CF_REPROCESSING job with failures:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  ...,\n" +
                    "  \"status\": \"FAILED\",\n" +
                    "  ...,\n" +
                    "  \"result\": {\n" +
                    "    \"jobType\": \"CF_REPROCESSING\",\n" +
                    "    \"successfulCount\": 0,\n" +
                    "    \"failedCount\": 2,\n" +
                    "    \"discardedCount\": 0,\n" +
                    "    \"totalCount\": 2,\n" +
                    "    \"results\": [\n" +
                    "      {\n" +
                    "        \"jobType\": \"CF_REPROCESSING\",\n" +
                    "        \"key\": \"c3cdbd42-799e-4d3a-9aad-9310f767aa36\",\n" +
                    "        \"success\": false,\n" +
                    "        \"discarded\": false,\n" +
                    "        \"failure\": {\n" +
                    "          \"error\": \"Failed to fetch temperature: Failed to fetch timeseries data\",\n" +
                    "          \"entityInfo\": {\n" +
                    "            \"id\": {\n" +
                    "              \"entityType\": \"DEVICE\",\n" +
                    "              \"id\": \"9fd41f20-31a1-11f0-933e-27998d6db02e\"\n" +
                    "            },\n" +
                    "            \"name\": \"Test device 1\"\n" +
                    "          }\n" +
                    "        }\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"jobType\": \"CF_REPROCESSING\",\n" +
                    "        \"key\": \"c3cdbd42-799e-4d3a-9aad-9310f767aa36\",\n" +
                    "        \"success\": false,\n" +
                    "        \"discarded\": false,\n" +
                    "        \"failure\": {\n" +
                    "          \"error\": \"Failed to fetch temperature: Failed to fetch timeseries data\",\n" +
                    "          \"entityInfo\": {\n" +
                    "            \"id\": {\n" +
                    "              \"entityType\": \"DEVICE\",\n" +
                    "              \"id\": \"9ffc4090-31a1-11f0-933e-27998d6db02e\"\n" +
                    "            },\n" +
                    "            \"name\": \"Test device 2\"\n" +
                    "          }\n" +
                    "        }\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"generalError\": null,\n" +
                    "    \"startTs\": 1747323069445,\n" +
                    "    \"finishTs\": 1747323070585,\n" +
                    "    \"cancellationTs\": 0\n" +
                    "  }\n" +
                    "}\n" +
                    MARKDOWN_CODE_BLOCK_END + NEW_LINE +
                    "Example of a FAILED job result with general error:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  ...,\n" +
                    "  \"status\": \"FAILED\",\n" +
                    "  ...,\n" +
                    "  \"result\": {\n" +
                    "    \"jobType\": \"CF_REPROCESSING\",\n" +
                    "    \"successfulCount\": 1,\n" +
                    "    \"failedCount\": 0,\n" +
                    "    \"discardedCount\": 0,\n" +
                    "    \"totalCount\": null,\n" +
                    "    \"results\": [],\n" +
                    "    \"generalError\": \"Timeout to find devices by profile\",\n" +
                    "    \"cancellationTs\": 0\n" +
                    "  }\n" +
                    "}\n" +
                    MARKDOWN_CODE_BLOCK_END + NEW_LINE +
                    "Example of a CANCELLED job result:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  ...,\n" +
                    "  \"status\": \"CANCELLED\",\n" +
                    "  ...,\n" +
                    "  \"result\": {\n" +
                    "    \"jobType\": \"CF_REPROCESSING\",\n" +
                    "    \"successfulCount\": 15,\n" +
                    "    \"failedCount\": 0,\n" +
                    "    \"discardedCount\": 85,\n" +
                    "    \"totalCount\": 100,\n" +
                    "    \"results\": [],\n" +
                    "    \"generalError\": null,\n" +
                    "    \"cancellationTs\": 1747065908414\n" +
                    "  }\n" +
                    "}\n" +
                    MARKDOWN_CODE_BLOCK_END +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
    )
    @GetMapping("/job/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public Job getJobById(@PathVariable UUID id) throws ThingsboardException {
        JobId jobId = new JobId(id);
        return checkJobId(jobId, Operation.READ);
    }

    @ApiOperation(value = "Get jobs (getJobs)",
            notes = "Returns the page of jobs." + NEW_LINE +
                    PAGE_DATA_PARAMETERS +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @GetMapping("/jobs")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
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
                                 @Parameter(description = "Comma-separated list of job types to include. If empty - all job types are included.", array = @ArraySchema(schema = @Schema(type = "string")), required = false)
                                 @RequestParam(required = false) List<JobType> types,
                                 @Parameter(description = "Comma-separated list of job statuses to include. If empty - all job statuses are included.", array = @ArraySchema(schema = @Schema(type = "string")))
                                 @RequestParam(required = false) List<JobStatus> statuses,
                                 @Parameter(description = "Comma-separated list of entity ids. If empty - jobs for all entities are included.", array = @ArraySchema(schema = @Schema(type = "string")))
                                 @RequestParam(required = false) List<UUID> entities,
                                 @Parameter(description = "To only include jobs created after this timestamp.")
                                 @RequestParam(required = false) Long startTime,
                                 @Parameter(description = "To only include jobs created before this timestamp.")
                                 @RequestParam(required = false) Long endTime,
                                 @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
                                 @RequestParam(required = false) Boolean includeCustomers) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        CustomerId customerId = currentUser.getCustomerId();
        accessControlService.checkPermission(currentUser, Resource.JOB, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        JobFilter filter = JobFilter.builder()
                .types(types)
                .statuses(statuses)
                .entities(entities)
                .startTime(startTime)
                .endTime(endTime)
                .customerId(customerId)
                .includeCustomers(includeCustomers != null && includeCustomers)
                .build();
        return jobService.findJobsByFilter(getTenantId(), filter, pageLink);
    }

    @ApiOperation(value = "Cancel job (cancelJob)",
            notes = "Cancels the job. The status of the job must be QUEUED, PENDING or RUNNING." + NEW_LINE +
                    "For a running job, all the tasks not yet processed will be discarded." + NEW_LINE +
                    "See the example of a cancelled job result in getJobById method description." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PostMapping("/job/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public void cancelJob(@PathVariable UUID id) throws ThingsboardException {
        JobId jobId = new JobId(id);
        checkJobId(jobId, Operation.WRITE);
        jobManager.cancelJob(getTenantId(), jobId);
    }

    @ApiOperation(value = "Reprocess job (reprocessJob)",
            notes = "Reprocesses the job. Failures are located at job.result.results list. " +
                    "Platform iterates over this list and submits new tasks for them. " +
                    "Doesn't create new job entity but updates the existing one. " +
                    "Successfully reprocessed job will look the same as completed one." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PostMapping("/job/{id}/reprocess")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public void reprocessJob(@PathVariable UUID id) throws ThingsboardException {
        JobId jobId = new JobId(id);
        checkJobId(jobId, Operation.WRITE);
        jobManager.reprocessJob(getTenantId(), jobId);
    }

    @DeleteMapping("/job/{id}")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public void deleteJob(@PathVariable UUID id) throws ThingsboardException {
        JobId jobId = new JobId(id);
        checkJobId(jobId, Operation.DELETE);
        jobService.deleteJob(getTenantId(), jobId);
    }

}
