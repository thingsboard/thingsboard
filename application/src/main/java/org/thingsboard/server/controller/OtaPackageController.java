/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.nio.ByteBuffer;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class OtaPackageController extends BaseController {

    public static final String OTA_PACKAGE_ID = "otaPackageId";
    public static final String CHECKSUM_ALGORITHM = "checksumAlgorithm";

    @PreAuthorize("hasAnyAuthority( 'TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> downloadOtaPackage(@PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            OtaPackage otaPackage = checkOtaPackageId(otaPackageId, Operation.READ);

            if (otaPackage.hasUrl()) {
                return ResponseEntity.badRequest().build();
            }

            ByteArrayResource resource = new ByteArrayResource(otaPackage.getData().array());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + otaPackage.getFileName())
                    .header("x-filename", otaPackage.getFileName())
                    .contentLength(resource.contentLength())
                    .contentType(parseMediaType(otaPackage.getContentType()))
                    .body(resource);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackage/info/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackageInfo getOtaPackageInfoById(@PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            return checkNotNull(otaPackageService.findOtaPackageInfoById(getTenantId(), otaPackageId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackage getOtaPackageById(@PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            return checkOtaPackageId(otaPackageId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage", method = RequestMethod.POST)
    @ResponseBody
    public OtaPackageInfo saveOtaPackageInfo(@RequestBody OtaPackageInfo otaPackageInfo) throws ThingsboardException {
        boolean created = otaPackageInfo.getId() == null;
        try {
            otaPackageInfo.setTenantId(getTenantId());
            checkEntity(otaPackageInfo.getId(), otaPackageInfo, Resource.OTA_PACKAGE);
            OtaPackageInfo savedOtaPackageInfo = otaPackageService.saveOtaPackageInfo(otaPackageInfo);
            logEntityAction(savedOtaPackageInfo.getId(), savedOtaPackageInfo,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, null);
            return savedOtaPackageInfo;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.OTA_PACKAGE), otaPackageInfo,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.POST)
    @ResponseBody
    public OtaPackage saveOtaPackageData(@PathVariable(OTA_PACKAGE_ID) String strOtaPackageId,
                                         @RequestParam(required = false) String checksum,
                                         @RequestParam(CHECKSUM_ALGORITHM) String checksumAlgorithmStr,
                                         @RequestBody MultipartFile file) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        checkParameter(CHECKSUM_ALGORITHM, checksumAlgorithmStr);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            OtaPackageInfo info = checkOtaPackageInfoId(otaPackageId, Operation.READ);

            OtaPackage otaPackage = new OtaPackage(otaPackageId);
            otaPackage.setCreatedTime(info.getCreatedTime());
            otaPackage.setTenantId(getTenantId());
            otaPackage.setDeviceProfileId(info.getDeviceProfileId());
            otaPackage.setType(info.getType());
            otaPackage.setTitle(info.getTitle());
            otaPackage.setVersion(info.getVersion());
            otaPackage.setAdditionalInfo(info.getAdditionalInfo());

            ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.valueOf(checksumAlgorithmStr.toUpperCase());

            byte[] bytes = file.getBytes();
            if (StringUtils.isEmpty(checksum)) {
                checksum = otaPackageService.generateChecksum(checksumAlgorithm, ByteBuffer.wrap(bytes));
            }

            otaPackage.setChecksumAlgorithm(checksumAlgorithm);
            otaPackage.setChecksum(checksum);
            otaPackage.setFileName(file.getOriginalFilename());
            otaPackage.setContentType(file.getContentType());
            otaPackage.setData(ByteBuffer.wrap(bytes));
            otaPackage.setDataSize((long) bytes.length);
            OtaPackage savedOtaPackage = otaPackageService.saveOtaPackage(otaPackage);
            logEntityAction(savedOtaPackage.getId(), savedOtaPackage, null, ActionType.UPDATED, null);
            return savedOtaPackage;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.OTA_PACKAGE), null, null, ActionType.UPDATED, e, strOtaPackageId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@RequestParam int pageSize,
                                                 @RequestParam int page,
                                                 @RequestParam(required = false) String textSearch,
                                                 @RequestParam(required = false) String sortProperty,
                                                 @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(otaPackageService.findTenantOtaPackagesByTenantId(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages/{deviceProfileId}/{type}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@PathVariable("deviceProfileId") String strDeviceProfileId,
                                                   @PathVariable("type") String strType,
                                                   @RequestParam int pageSize,
                                                   @RequestParam int page,
                                                   @RequestParam(required = false) String textSearch,
                                                   @RequestParam(required = false) String sortProperty,
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

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteOtaPackage(@PathVariable("otaPackageId") String strOtaPackageId) throws ThingsboardException {
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
