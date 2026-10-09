// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.converter.ConverterLibraryService;
import org.thingsboard.server.service.converter.Model;
import org.thingsboard.server.service.converter.Vendor;

import java.util.List;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/converter/library")
@Slf4j
public class ConverterLibraryController extends BaseController {

    private final ConverterLibraryService converterLibraryService;

    @ApiOperation(value = "Get vendors (getVendors)",
            notes = "Returns a list of vendors for the integration type")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/vendors")
    public List<Vendor> getVendors(@PathVariable IntegrationType integrationType,
                                   @RequestParam(required = false) String converterType,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "2147483647") int pageSize,
                                   @RequestParam(defaultValue = "true") boolean loadImages) {
        return converterLibraryService.getVendors(integrationType, converterType, page, pageSize, loadImages);
    }

    @ApiOperation(value = "Get vendor models (getVendorModels)",
            notes = "Returns a list of models for the vendor, integration type and converter type")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/models")
    public List<Model> getVendorModels(@PathVariable IntegrationType integrationType,
                                       @PathVariable String vendorName,
                                       @RequestParam(required = false) String converterType,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "2147483647") int pageSize,
                                       @RequestParam(defaultValue = "true") boolean loadImages) {
        return converterLibraryService.getVendorModels(integrationType, converterType, vendorName, page, pageSize, loadImages);
    }

    @ApiOperation(value = "Get uplink converter (getUplinkConverter)",
            notes = "Returns uplink converter body for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/uplink", produces = "text/plain")
    public String getUplinkConverter(@PathVariable IntegrationType integrationType,
                                     @PathVariable String vendorName,
                                     @PathVariable String model) {
        return converterLibraryService.getConverter(integrationType, "uplink", vendorName, model);
    }

    @ApiOperation(value = "Get uplink converter metadata (getUplinkConverterMetadata)",
            notes = "Returns uplink converter metadata for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/uplink/metadata", produces = "text/plain")
    public String getUplinkConverterMetadata(@PathVariable IntegrationType integrationType,
                                             @PathVariable String vendorName,
                                             @PathVariable String model) {
        return converterLibraryService.getConverterMetadata(integrationType, "uplink", vendorName, model);
    }

    @ApiOperation(value = "Get uplink payload (getUplinkPayload)",
            notes = "Returns payload example for the uplink converter for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/uplink/payload", produces = "text/plain")
    public String getUplinkPayload(@PathVariable IntegrationType integrationType,
                                   @PathVariable String vendorName,
                                   @PathVariable String model) {
        return converterLibraryService.getPayload(integrationType, "uplink", vendorName, model);
    }

    @ApiOperation(value = "Get downlink converter (getDownlinkConverter)",
            notes = "Returns downlink converter body for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/downlink", produces = "text/plain")
    public String getDownlinkConverter(@PathVariable IntegrationType integrationType,
                                       @PathVariable String vendorName,
                                       @PathVariable String model) {
        return converterLibraryService.getConverter(integrationType, "downlink", vendorName, model);
    }

    @ApiOperation(value = "Get downlink converter metadata (getDownlinkConverterMetadata)",
            notes = "Returns downlink converter metadata for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/downlink/metadata", produces = "text/plain")
    public String getDownlinkConverterMetadata(@PathVariable IntegrationType integrationType,
                                               @PathVariable String vendorName,
                                               @PathVariable String model) {
        return converterLibraryService.getConverterMetadata(integrationType, "downlink", vendorName, model);
    }

    @ApiOperation(value = "Get downlink payload (getDownlinkPayload)",
            notes = "Returns payload example for the downlink converter for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/downlink/payload", produces = "text/plain")
    public String getDownlinkPayload(@PathVariable IntegrationType integrationType,
                                     @PathVariable String vendorName,
                                     @PathVariable String model) {
        return converterLibraryService.getPayload(integrationType, "downlink", vendorName, model);
    }

}
