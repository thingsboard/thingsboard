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

import java.nio.ByteBuffer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Firmware;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.firmware.ChecksumAlgorithm;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class FirmwareController extends BaseController {

    public static final String FIRMWARE_ID = "firmwareId";
    public static final String CHECKSUM_ALGORITHM = "checksumAlgorithm";

    @PreAuthorize("hasAnyAuthority( 'TENANT_ADMIN')")
    @GetMapping(value = "/firmware/{firmwareId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFirmware(@PathVariable(FIRMWARE_ID) String strFirmwareId) throws ThingsboardException {
        checkParameter(FIRMWARE_ID, strFirmwareId);
        try {
            FirmwareId firmwareId = new FirmwareId(toUUID(strFirmwareId));
            Firmware firmware = checkFirmwareId(firmwareId, Operation.READ);

            ByteArrayResource resource = new ByteArrayResource(firmware.getData().array());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + firmware.getFileName())
                    .header("x-filename", firmware.getFileName())
                    .contentLength(resource.contentLength())
                    .contentType(parseMediaType(firmware.getContentType()))
                    .body(resource);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/firmware/info/{firmwareId}")
    public FirmwareInfo getFirmwareInfoById(@PathVariable(FIRMWARE_ID) String strFirmwareId) throws ThingsboardException {
        checkParameter(FIRMWARE_ID, strFirmwareId);
        try {
            FirmwareId firmwareId = new FirmwareId(toUUID(strFirmwareId));
            return checkNotNull(firmwareService.findFirmwareInfoById(getTenantId(), firmwareId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/firmware/{firmwareId}")
    public Firmware getFirmwareById(@PathVariable(FIRMWARE_ID) String strFirmwareId) throws ThingsboardException {
        checkParameter(FIRMWARE_ID, strFirmwareId);
        try {
            FirmwareId firmwareId = new FirmwareId(toUUID(strFirmwareId));
            return checkFirmwareId(firmwareId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/firmware")
    public FirmwareInfo saveFirmwareInfo(@RequestBody FirmwareInfo firmwareInfo) throws ThingsboardException {
        boolean created = firmwareInfo.getId() == null;
        try {
            firmwareInfo.setTenantId(getTenantId());
            checkEntity(firmwareInfo.getId(), firmwareInfo, Resource.FIRMWARE);
            FirmwareInfo savedFirmwareInfo = firmwareService.saveFirmwareInfo(firmwareInfo);
            logEntityAction(savedFirmwareInfo.getId(), savedFirmwareInfo,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, null);
            return savedFirmwareInfo;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.FIRMWARE), firmwareInfo,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/firmware/{firmwareId}")
    public Firmware saveFirmwareData(@PathVariable(FIRMWARE_ID) String strFirmwareId,
                                     @RequestParam(required = false) String checksum,
                                     @RequestParam(CHECKSUM_ALGORITHM) String checksumAlgorithmStr,
                                     @RequestBody MultipartFile file) throws ThingsboardException {
        checkParameter(FIRMWARE_ID, strFirmwareId);
        checkParameter(CHECKSUM_ALGORITHM, checksumAlgorithmStr);
        try {
            FirmwareId firmwareId = new FirmwareId(toUUID(strFirmwareId));
            FirmwareInfo info = checkFirmwareInfoId(firmwareId, Operation.READ);

            Firmware firmware = new Firmware(firmwareId);
            firmware.setCreatedTime(info.getCreatedTime());
            firmware.setTenantId(getTenantId());
            firmware.setDeviceProfileId(info.getDeviceProfileId());
            firmware.setType(info.getType());
            firmware.setTitle(info.getTitle());
            firmware.setVersion(info.getVersion());
            firmware.setAdditionalInfo(info.getAdditionalInfo());

            ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.valueOf(checksumAlgorithmStr.toUpperCase());

            byte[] bytes = file.getBytes();
            if (StringUtils.isEmpty(checksum)) {
                checksum = firmwareService.generateChecksum(checksumAlgorithm, ByteBuffer.wrap(bytes));
            }

            firmware.setChecksumAlgorithm(checksumAlgorithm);
            firmware.setChecksum(checksum);
            firmware.setFileName(file.getOriginalFilename());
            firmware.setContentType(file.getContentType());
            firmware.setData(ByteBuffer.wrap(bytes));
            firmware.setDataSize((long) bytes.length);
            Firmware savedFirmware = firmwareService.saveFirmware(firmware);
            logEntityAction(savedFirmware.getId(), savedFirmware, null, ActionType.UPDATED, null);
            return savedFirmware;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.FIRMWARE), null, null, ActionType.UPDATED, e, strFirmwareId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/firmwares")
    public PageData<FirmwareInfo> getFirmwares(@RequestParam int pageSize,
                                               @RequestParam int page,
                                               @RequestParam(required = false) String textSearch,
                                               @RequestParam(required = false) String sortProperty,
                                               @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(firmwareService.findTenantFirmwaresByTenantId(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/firmwares/{deviceProfileId}/{type}/{hasData}")
    public PageData<FirmwareInfo> getFirmwares(@PathVariable("deviceProfileId") String strDeviceProfileId,
                                               @PathVariable("type") String strType,
                                               @PathVariable("hasData") boolean hasData,
                                               @RequestParam int pageSize,
                                               @RequestParam int page,
                                               @RequestParam(required = false) String textSearch,
                                               @RequestParam(required = false) String sortProperty,
                                               @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("deviceProfileId", strDeviceProfileId);
        checkParameter("type", strType);
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(firmwareService.findTenantFirmwaresByTenantIdAndDeviceProfileIdAndTypeAndHasData(getTenantId(),
                    new DeviceProfileId(toUUID(strDeviceProfileId)), FirmwareType.valueOf(strType), hasData, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/firmware/{firmwareId}")
    public void deleteFirmware(@PathVariable("firmwareId") String strFirmwareId) throws ThingsboardException {
        checkParameter(FIRMWARE_ID, strFirmwareId);
        try {
            FirmwareId firmwareId = new FirmwareId(toUUID(strFirmwareId));
            FirmwareInfo info = checkFirmwareInfoId(firmwareId, Operation.DELETE);
            firmwareService.deleteFirmware(getTenantId(), firmwareId);
            logEntityAction(firmwareId, info, null, ActionType.DELETED, null, strFirmwareId);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.FIRMWARE), null, null, ActionType.DELETED, e, strFirmwareId);
            throw handleException(e);
        }
    }

}
