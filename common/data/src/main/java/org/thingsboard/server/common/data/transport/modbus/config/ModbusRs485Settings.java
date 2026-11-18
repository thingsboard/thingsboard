// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.transport.modbus.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ModbusRs485Settings implements Serializable {

    @Serial
    private static final long serialVersionUID = -5516924894048341635L;

    @Schema(description = "Whether RTS is driven high during transmit", example = "true")
    private boolean rtsActiveHigh = true;
    @Schema(description = "Enable the bus termination resistor (Linux only)", example = "false")
    private boolean termination = false;
    @Schema(description = "Keep the receiver enabled while transmitting (Linux only)", example = "false")
    private boolean rxDuringTx = false;
    @Schema(description = "Delay after asserting RTS before transmitting, in microseconds", minimum = "0", example = "0")
    private int delayBeforeMicros = 0;
    @Schema(description = "Delay after transmitting before releasing RTS, in microseconds", minimum = "0", example = "0")
    private int delayAfterMicros = 0;

    /** All values in one line, for comparing and reporting the settings of a shared serial port. */
    public String describe() {
        return "[rtsActiveHigh=" + rtsActiveHigh + ", termination=" + termination + ", rxDuringTx=" + rxDuringTx
                + ", delayBefore=" + delayBeforeMicros + "us, delayAfter=" + delayAfterMicros + "us]";
    }

    public void validate() {
        if (delayBeforeMicros < 0 || delayAfterMicros < 0) {
            throw new IllegalArgumentException("Modbus RS-485 delays must not be negative");
        }
    }
}