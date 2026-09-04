// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SystemInfoData {

    @Schema(description = "Service Id.")
    private String serviceId;
    @Schema(description = "Service type.")
    private String serviceType;
    @Schema(description = "CPU usage, in percent.")
    private Long cpuUsage;
    @Schema(description = "Total CPU usage.")
    private Long cpuCount;
    @Schema(description = "Memory usage, in percent.")
    private Long memoryUsage;
    @Schema(description = "Total memory in bytes.")
    private Long totalMemory;
    @Schema(description = "Disk usage, in percent.")
    private Long discUsage;
    @Schema(description = "Total disc space in bytes.")
    private Long totalDiscSpace;

}
