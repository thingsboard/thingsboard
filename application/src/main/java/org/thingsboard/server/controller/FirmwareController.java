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
import org.thingsboard.server.common.data.Firmware;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

import java.nio.ByteBuffer;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class FirmwareController extends BaseController {

    public static final String FIRMWARE_ID = "firmwareId";

    @PreAuthorize("hasAnyAuthority( 'TENANT_ADMIN')")
    @RequestMapping(value = "/firmware/{firmwareId}/download", method = RequestMethod.GET)
    @ResponseBody
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

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/firmware/info/{firmwareId}", method = RequestMethod.GET)
    @ResponseBody
    public FirmwareInfo getFirmwareInfoById(@PathVariable(FIRMWARE_ID) String strFirmwareId) throws ThingsboardException {
        checkParameter(FIRMWARE_ID, strFirmwareId);
        try {
            FirmwareId firmwareId = new FirmwareId(toUUID(strFirmwareId));
            return checkFirmwareInfoId(firmwareId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/firmware/{firmwareId}", method = RequestMethod.GET)
    @ResponseBody
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
    @RequestMapping(value = "/firmware", method = RequestMethod.POST)
    @ResponseBody
    public Firmware saveFirmware(@RequestParam("title") String title,
                                 @RequestBody MultipartFile firmwareFile) throws ThingsboardException {
        checkParameter("title", title);
        try {
            checkNotNull(firmwareFile);
            Firmware firmware = new Firmware();
            firmware.setTenantId(getTenantId());
            firmware.setTitle(title);
            firmware.setFileName(firmwareFile.getOriginalFilename());
            firmware.setContentType(firmwareFile.getContentType());
            firmware.setData(ByteBuffer.wrap(firmwareFile.getBytes()));
            return firmwareService.saveFirmware(firmware);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/firmware", method = RequestMethod.GET)
    @ResponseBody
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

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/firmware/{firmwareId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteResource(@PathVariable("firmwareId") String strFirmwareId) throws ThingsboardException {
        checkParameter(FIRMWARE_ID, strFirmwareId);
        try {
            FirmwareId firmwareId = new FirmwareId(toUUID(strFirmwareId));
            checkFirmwareInfoId(firmwareId, Operation.DELETE);
            firmwareService.deleteFirmware(getTenantId(), firmwareId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}