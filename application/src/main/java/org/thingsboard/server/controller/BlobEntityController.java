// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.blob.TbBlobService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.controller.ControllerConstants.BLOB_ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.BLOB_ENTITY_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.BLOB_ENTITY_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class BlobEntityController extends BaseController {

    public static final String BLOB_ENTITY_ID = "blobEntityId";
    public static final String INVALID_BLOB_ENTITY_ID = "Referencing non-existing Blob entity Id will cause an error.";
    public static final String BLOB_ENTITY_DESCRIPTION = "The platform uses Blob(binary large object) entities in the reporting feature, in order to store Dashboard states snapshots of different content types in base64 format. ";
    public static final String BLOB_ENTITY_INFO_DESCRIPTION =
            BLOB_ENTITY_DESCRIPTION +
                    "BlobEntityInfo represents an object that contains base info about the blob entity(name, type, contentType, etc.). " +
                    "See the 'Model' tab of the Response Class for more details.";
    public static final String BLOB_ENTITY_INFO_WITH_CUSTOMER_INFO_DESCRIPTION =
            BLOB_ENTITY_DESCRIPTION +
                    "BlobEntityWithCustomerInfo represents an object that contains base info about the blob entity(name, type, contentType, etc.) " +
                    "and info about the customer(customerTitle, customerIsPublic) of the user that scheduled generation of the dashboard report. ";
    public static final String BLOB_ENTITY_QUERY_START_TIME_DESCRIPTION = "The start timestamp in milliseconds of the search time range over the BlobEntityWithCustomerInfo class field: 'createdTime'.";
    public static final String BLOB_ENTITY_QUERY_END_TIME_DESCRIPTION = "The end timestamp in milliseconds of the search time range over the BlobEntityWithCustomerInfo class field: 'createdTime'.";

    private final TbBlobService tbBlobService;

    @ApiOperation(value = "Get Blob Entity With Customer Info (getBlobEntityInfoById)",
            notes = "Fetch the BlobEntityWithCustomerInfo object based on the provided Blob entity Id. " +
                    BLOB_ENTITY_INFO_WITH_CUSTOMER_INFO_DESCRIPTION + INVALID_BLOB_ENTITY_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/blobEntity/info/{blobEntityId}")
    public BlobEntityWithCustomerInfo getBlobEntityInfoById(
            @Parameter(description = BLOB_ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        BlobEntityId blobEntityId = new BlobEntityId(toUUID(strBlobEntityId));
        return checkBlobEntityInfoId(blobEntityId, Operation.READ);
    }

    @ApiOperation(value = "Download Blob Entity By Id (downloadBlobEntity)",
            notes = "Download report file based on the provided Blob entity Id. " +
                    INVALID_BLOB_ENTITY_ID + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/blobEntity/{blobEntityId}/download")
    public ResponseEntity<ByteArrayResource> downloadBlobEntity(
            @Parameter(description = BLOB_ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        BlobEntityId blobEntityId = new BlobEntityId(toUUID(strBlobEntityId));
        BlobEntity blobEntity = checkBlobEntityId(blobEntityId, Operation.READ);
        ByteArrayResource resource = new ByteArrayResource(blobEntity.getData().array());
        ContentDisposition cd = ContentDisposition.attachment()
                .filename(blobEntity.getName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("x-filename", blobEntity.getName())
                .contentLength(resource.contentLength())
                .contentType(parseMediaType(blobEntity.getContentType()))
                .body(resource);
    }

    @ApiOperation(value = "Delete Blob Entity (deleteBlobEntity)",
            notes = "Delete Blob entity based on the provided Blob entity Id. " +
                    INVALID_BLOB_ENTITY_ID + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/blobEntity/{blobEntityId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteBlobEntity(
            @Parameter(description = BLOB_ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        BlobEntityId blobEntityId = new BlobEntityId(toUUID(strBlobEntityId));
        BlobEntityInfo blobEntityInfo = checkBlobEntityInfoId(blobEntityId, Operation.DELETE);
        tbBlobService.delete(blobEntityInfo, getCurrentUser());
    }

    @ApiOperation(value = "Get Blob Entities (getBlobEntities)",
            notes = "Returns a page of BlobEntityWithCustomerInfo object that are available for the current user. "
                    + BLOB_ENTITY_INFO_WITH_CUSTOMER_INFO_DESCRIPTION + PAGE_DATA_PARAMETERS
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/blobEntities")
    public PageData<BlobEntityWithCustomerInfo> getBlobEntities(
            @Parameter(description = PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @Parameter(description = BLOB_ENTITY_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Parameter(description = BLOB_ENTITY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "contentType", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = BLOB_ENTITY_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @Parameter(description = BLOB_ENTITY_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime
    ) throws ThingsboardException {
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        TenantId tenantId = getCurrentUser().getTenantId();
        if (!accessControlService.hasPermission(getCurrentUser(), org.thingsboard.server.common.data.permission.Resource.BLOB_ENTITY, Operation.READ)) {
            return new PageData<>();
        }
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            if (StringUtils.isNotBlank(type)) {
                return checkNotNull(blobEntityService.findBlobEntitiesByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(blobEntityService.findBlobEntitiesByTenantId(tenantId, pageLink));
            }
        } else { //CUSTOMER_USER
            CustomerId customerId = getCurrentUser().getCustomerId();
            if (StringUtils.isNotBlank(type)) {
                return checkNotNull(blobEntityService.findBlobEntitiesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(blobEntityService.findBlobEntitiesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        }
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/blobEntities", params = {"blobEntityIds"})
    public List<BlobEntityInfo> getBlobEntitiesByIdsV1(
            @RequestParam("blobEntityIds") String[] strBlobEntityIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("blobEntityIds", strBlobEntityIds);
        if (!accessControlService.hasPermission(getCurrentUser(), org.thingsboard.server.common.data.permission.Resource.BLOB_ENTITY, Operation.READ)) {
            return Collections.emptyList();
        }
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<BlobEntityId> blobEntityIds = new ArrayList<>();
        for (String strBlobEntityId : strBlobEntityIds) {
            blobEntityIds.add(new BlobEntityId(toUUID(strBlobEntityId)));
        }
        List<BlobEntityInfo> blobEntities = checkNotNull(blobEntityService.findBlobEntityInfoByIdsAsync(tenantId, blobEntityIds).get());
        return filterBlobEntitiesByReadPermission(blobEntities);
    }

    @ApiOperation(value = "Get Blob Entities By Ids (getBlobEntitiesByIds)",
            notes = "Requested blob entities must be owned by tenant or assigned to customer which user is performing the request. "
                    + BLOB_ENTITY_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/blobEntities/list")
    public List<BlobEntityInfo> getBlobEntitiesByIds(
            @Parameter(description = "A list of blob entity ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true) @RequestParam("blobEntityIds") String[] strBlobEntityIds) throws ThingsboardException, ExecutionException, InterruptedException {
        return getBlobEntitiesByIdsV1(strBlobEntityIds);
    }

    private List<BlobEntityInfo> filterBlobEntitiesByReadPermission(List<BlobEntityInfo> blobEntities) {
        return blobEntities.stream().filter(blobEntity -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), org.thingsboard.server.common.data.permission.Resource.BLOB_ENTITY,
                        Operation.READ, blobEntity.getId(), blobEntity);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

}
