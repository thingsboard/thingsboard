// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.trendz.TrendzProxyService;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.TRENDZ_ENDPOINT_AVAILABILITY_DESCRIPTION;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping
public class TrendzProxyController extends BaseController {
    private final TrendzProxyService trendzProxyService;

    @ApiOperation(value = "Forward Authorized Requests to Trendz",
            notes = "Forwards authorized requests (/apiTrendz/**) to Trendz using the Trendz internal URL. " +
                    TRENDZ_ENDPOINT_AVAILABILITY_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @RequestMapping("/apiTrendz/**")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public ResponseEntity<byte[]> handleAuthorizedTrendzRequests(HttpServletRequest request, @RequestBody(required = false) byte[] body) throws ThingsboardException {
        return trendzProxyService.proxy(request, body);
    }

    @ApiOperation(value = "Forward Unauthorized Requests to Trendz",
            notes = "Forwards unauthorized requests (/apiTrendz/publicApi/**, /trendz/**) to Trendz using the Trendz internal URL. " +
                    TRENDZ_ENDPOINT_AVAILABILITY_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @RequestMapping({"/apiTrendz/publicApi/**", "/trendz/**"})
    public ResponseEntity<byte[]> handleUnauthorizedTrendzRequests(HttpServletRequest request, @RequestBody(required = false) byte[] body) throws ThingsboardException {
        return trendzProxyService.proxy(request, body);
    }
}
