package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;

/**
 * REST Controller for device ping operations
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/device")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Device Ping Controller", description = "Check device reachability and last seen status")
public class DevicePingController extends BaseController {

    private final DevicePingService devicePingService;

    /**
     * Ping a device to check its reachability status
     *
     * @param deviceIdStr Device ID as string
     * @return DevicePingResponse with reachability status and last seen timestamp
     * @throws ThingsboardException if device not found or access denied
     */
    @Operation(
            summary = "Ping Device",
            description = "Returns device reachability status and last seen timestamp. " +
                    "Device is considered reachable if it sent data within the last 5 minutes.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful ping response",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = DevicePingResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid device ID format"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Device not found")
            }
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/ping/{deviceId}")
    @ResponseStatus(HttpStatus.OK)
    public DevicePingResponse pingDevice(
            @Parameter(description = DEVICE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("deviceId") String deviceIdStr
    ) throws ThingsboardException {

        log.debug("REST request to ping device [{}]", deviceIdStr);

        try {
            // Parse and validate device ID
            DeviceId deviceId = new DeviceId(UUID.fromString(deviceIdStr));


            // Ping the device
            return devicePingService.pingDevice(getCurrentUser().getTenantId(), deviceId);

        } catch (IllegalArgumentException e) {
            log.error("Invalid device ID format: {}", deviceIdStr, e);
            throw new ThingsboardException("Invalid device ID format", ThingsboardErrorCode.BAD_REQUEST_PARAMS);

        } catch (Exception e) {
            log.error("Error pinging device [{}]", deviceIdStr, e);
            throw handleException(e);
        }
    }
}
