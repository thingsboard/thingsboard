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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.ota.util.ChecksumUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.transaction.Transactional;
import java.io.IOException;

import static org.thingsboard.server.controller.ControllerConstants.*;

@Slf4j
@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class OtaPackageController extends BaseController {

    public static final String OTA_PACKAGE_ID = "otaPackageId";
    public static final String CHECKSUM_ALGORITHM = "checksumAlgorithm";


    @ApiOperation(value = "Download OTA Package (downloadOtaPackage)", notes = "Download OTA Package based on the provided OTA Package Id." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority( 'TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}/download", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public ResponseEntity<FileSystemResource> downloadOtaPackage(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                                                 @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            OtaPackage otaPackage = checkOtaPackageId(otaPackageId, Operation.READ);
            if (otaPackage.hasUrl()) {
                return ResponseEntity.badRequest().build();
            }
            FileSystemResource resource = new FileSystemResource(otaPackageService.getOtaDataFile(getTenantId(), otaPackageId));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + otaPackage.getFileName())
                    .header("x-filename", otaPackage.getFileName())
                    .contentLength(otaPackage.getDataSize())
                    .contentType(parseMediaType(otaPackage.getContentType()))
                    .body(resource);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get OTA Package Info (getOtaPackageInfoById)",
            notes = "Fetch the OTA Package Info object based on the provided OTA Package Id. " +
                    OTA_PACKAGE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackage/info/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackageInfo getOtaPackageInfoById(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                                @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            return checkNotNull(otaPackageService.findOtaPackageInfoById(getTenantId(), otaPackageId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get OTA Package (getOtaPackageById)",
            notes = "Fetch the OTA Package object based on the provided OTA Package Id. " +
                    "The server checks that the OTA Package is owned by the same tenant. " + OTA_PACKAGE_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackage getOtaPackageById(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                        @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            return checkOtaPackageId(otaPackageId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update OTA Package Info (saveOtaPackageInfo)",
            notes = "Create or update the OTA Package Info. When creating OTA Package Info, platform generates OTA Package id as " + UUID_WIKI_LINK +
                    "The newly created OTA Package id will be present in the response. " +
                    "Specify existing OTA Package id to update the OTA Package Info. " +
                    "Referencing non-existing OTA Package Id will cause 'Not Found' error. " +
                    "\n\nOTA Package combination of the title with the version is unique in the scope of tenant. " + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json",
            consumes = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage", method = RequestMethod.POST)
    @ResponseBody
    public OtaPackageInfo saveOtaPackageInfo(@ApiParam(value = "A JSON value representing the OTA Package.")
                                             @RequestBody SaveOtaPackageInfoRequest otaPackageInfo) throws ThingsboardException {
        boolean created = otaPackageInfo.getId() == null;
        try {
            otaPackageInfo.setTenantId(getTenantId());
            checkEntity(otaPackageInfo.getId(), otaPackageInfo, Resource.OTA_PACKAGE);
            OtaPackageInfo savedOtaPackageInfo = otaPackageService.saveOtaPackageInfo(new OtaPackageInfo(otaPackageInfo), otaPackageInfo.isUsesUrl());
            logEntityAction(savedOtaPackageInfo.getId(), savedOtaPackageInfo,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, null);
            return savedOtaPackageInfo;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.OTA_PACKAGE), otaPackageInfo,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save OTA Package data (saveOtaPackageData)",
            notes = "Update the OTA Package. Adds the date to the existing OTA Package Info" + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.POST)
    @ResponseBody
    public OtaPackageInfo saveOtaPackageData(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                             @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId,
                                             @ApiParam(value = "OTA Package checksum. For example, '0xd87f7e0c'")
                                             @RequestParam(required = false) String checksum,
                                             @ApiParam(value = "OTA Package checksum algorithm.", allowableValues = OTA_PACKAGE_CHECKSUM_ALGORITHM_ALLOWABLE_VALUES)
                                             @RequestParam(CHECKSUM_ALGORITHM) String checksumAlgorithmStr,
                                             @ApiParam(value = "OTA Package data.")
                                             @RequestBody MultipartFile file) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        checkParameter(CHECKSUM_ALGORITHM, checksumAlgorithmStr);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.valueOf(checksumAlgorithmStr.toUpperCase());

            if (StringUtils.isEmpty(checksum)) {
                checksum = ChecksumUtil.generateChecksum(checksumAlgorithm, file.getInputStream());
            }
            OtaPackageInfo savedOtaPackage = saveOtaPackageWithData(otaPackageId, file, checksum, checksumAlgorithm);
            logEntityAction(savedOtaPackage.getId(), savedOtaPackage, null, ActionType.UPDATED, null);
            return savedOtaPackage;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.OTA_PACKAGE), null, null, ActionType.UPDATED, e, strOtaPackageId);
            throw handleException(e);
        }
    }

    private OtaPackage saveOtaPackageWithData(OtaPackageId otaPackageId, MultipartFile file, String checksum, ChecksumAlgorithm checksumAlgorithm) throws ThingsboardException, IOException {
        OtaPackageInfo info = checkOtaPackageInfoId(otaPackageId, Operation.READ);
        OtaPackage otaPackage = new OtaPackage(otaPackageId);
        otaPackage.setCreatedTime(info.getCreatedTime());
        otaPackage.setTenantId(info.getTenantId());
        otaPackage.setDeviceProfileId(info.getDeviceProfileId());
        otaPackage.setType(info.getType());
        otaPackage.setTitle(info.getTitle());
        otaPackage.setVersion(info.getVersion());
        otaPackage.setTag(info.getTag());
        otaPackage.setAdditionalInfo(info.getAdditionalInfo());
        otaPackage.setChecksumAlgorithm(checksumAlgorithm);
        otaPackage.setChecksum(checksum);
        otaPackage.setFileName(file.getOriginalFilename());
        otaPackage.setContentType(file.getContentType());
        otaPackage.setDataSize(file.getSize());
        otaPackage.setData(file.getInputStream());
        return otaPackageService.saveOtaPackage(otaPackage);
    }

    @ApiOperation(value = "Get OTA Package Infos (getOtaPackages)",
            notes = "Returns a page of OTA Package Info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + OTA_PACKAGE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                   @RequestParam int pageSize,
                                                   @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                   @RequestParam int page,
                                                   @ApiParam(value = OTA_PACKAGE_TEXT_SEARCH_DESCRIPTION)
                                                   @RequestParam(required = false) String textSearch,
                                                   @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = OTA_PACKAGE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortProperty,
                                                   @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(otaPackageService.findTenantOtaPackagesByTenantId(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get OTA Package Infos (getOtaPackages)",
            notes = "Returns a page of OTA Package Info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + OTA_PACKAGE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages/{deviceProfileId}/{type}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
                                                   @PathVariable("deviceProfileId") String strDeviceProfileId,
                                                   @ApiParam(value = "OTA Package type.", allowableValues = "FIRMWARE, SOFTWARE")
                                                   @PathVariable("type") String strType,
                                                   @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                   @RequestParam int pageSize,
                                                   @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                   @RequestParam int page,
                                                   @ApiParam(value = OTA_PACKAGE_TEXT_SEARCH_DESCRIPTION)
                                                   @RequestParam(required = false) String textSearch,
                                                   @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = OTA_PACKAGE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortProperty,
                                                   @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("deviceProfileId", strDeviceProfileId);
        checkParameter("type", strType);
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(otaPackageService.findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(getTenantId(),
                    new DeviceProfileId(toUUID(strDeviceProfileId)), OtaPackageType.valueOf(strType), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete OTA Package (deleteOtaPackage)",
            notes = "Deletes the OTA Package. Referencing non-existing OTA Package Id will cause an error. " +
                    "Can't delete the OTA Package if it is referenced by existing devices or device profile." + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteOtaPackage(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                 @PathVariable("otaPackageId") String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            OtaPackageInfo info = checkOtaPackageInfoId(otaPackageId, Operation.DELETE);
            otaPackageService.deleteOtaPackage(getTenantId(), otaPackageId);
            logEntityAction(otaPackageId, info, null, ActionType.DELETED, null, strOtaPackageId);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.OTA_PACKAGE), null, null, ActionType.DELETED, e, strOtaPackageId);
            throw handleException(e);
        }
    }

}
