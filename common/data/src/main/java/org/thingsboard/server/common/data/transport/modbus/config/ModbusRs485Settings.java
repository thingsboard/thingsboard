// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.transport.modbus.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModbusRs485Settings implements Serializable {

    @Serial
    private static final long serialVersionUID = -5516924894048341635L;

    /** The Linux serial layer, which is what applies the delays, works in whole milliseconds and limits them to this. */
    public static final int MAX_DELAY_MS = 100;

    @Schema(description = "Whether RTS is driven high during transmit", example = "true")
    private boolean rtsActiveHigh = true;
    @Schema(description = "Enable the bus termination resistor (Linux only)", example = "false")
    private boolean termination = false;
    @Schema(description = "Keep the receiver enabled while transmitting (Linux only)", example = "false")
    private boolean rxDuringTx = false;
    @Schema(description = "Delay after asserting RTS before transmitting, in milliseconds (Linux only)", minimum = "0", maximum = "100", example = "0")
    private int delayBeforeMs = 0;
    @Schema(description = "Delay after transmitting before releasing RTS, in milliseconds (Linux only)", minimum = "0", maximum = "100", example = "0")
    private int delayAfterMs = 0;

    /** All values in one line, for comparing and reporting the settings of a shared serial port. */
    public String describe() {
        return "[rtsActiveHigh=" + rtsActiveHigh + ", termination=" + termination + ", rxDuringTx=" + rxDuringTx
                + ", delayBefore=" + delayBeforeMs + "ms, delayAfter=" + delayAfterMs + "ms]";
    }

    public void validate() {
        if (delayBeforeMs < 0 || delayBeforeMs > MAX_DELAY_MS || delayAfterMs < 0 || delayAfterMs > MAX_DELAY_MS) {
            throw new IllegalArgumentException("Modbus RS-485 delays must be from 0 to " + MAX_DELAY_MS + " ms");
        }
    }
}