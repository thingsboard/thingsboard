// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema
public class PowerSavingConfiguration implements Serializable {

    private static final long serialVersionUID = 2905389805488525362L;

    private PowerMode powerMode;
    private Long psmActivityTimer;
    private Long edrxCycle;
    private Long pagingTransmissionWindow;
}
