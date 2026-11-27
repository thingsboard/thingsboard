// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docker;

import io.fabric8.kubernetes.api.model.Quantity;
import org.thingsboard.server.common.data.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts the Kubernetes-style resource quantity strings stored on an extension's {@code ResourceSpec}
 * into the units the Docker Engine expects: CPU into {@code NanoCPUs} ({@code HostConfig.withNanoCPUs})
 * and memory into bytes ({@code HostConfig.withMemory}).
 *
 * <p>Parsing is delegated to the fabric8 {@link Quantity#getAmountInBytes(Quantity)} parser — the SAME
 * parser {@code ExtensionDataValidator} uses to validate the spec — so a value that passes validation is
 * interpreted identically here, and both substrates agree on the Kubernetes quantity grammar (binary
 * {@code Ki/Mi/Gi/Ti/Pi}, decimal {@code k/M/G/T/P/E}, milli {@code m}, exponent forms, a bare number).
 * {@code getAmountInBytes} returns the amount in base units: <b>bytes</b> for memory and <b>cores</b> for
 * CPU (e.g. {@code "250m"} → {@code 0.25}); this class only applies the Docker unit conversion on top.
 * Docker enforces these as hard ceilings, so the spec's effective <em>limit</em> (not request) is used,
 * and a fractional result rounds up so a limit is never provisioned below the validated value.
 */
public final class ResourceQuantities {

    private static final BigDecimal NANO = BigDecimal.valueOf(1_000_000_000L);

    private ResourceQuantities() {}

    /** CPU quantity → NanoCPUs, or {@code null} when blank. */
    public static Long cpuToNanoCpus(String cpu) {
        if (StringUtils.isBlank(cpu)) {
            return null;
        }
        BigDecimal cores = Quantity.getAmountInBytes(new Quantity(cpu.trim()));
        return cores.multiply(NANO).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** Memory quantity → bytes, or {@code null} when blank. */
    public static Long memoryToBytes(String memory) {
        if (StringUtils.isBlank(memory)) {
            return null;
        }
        return Quantity.getAmountInBytes(new Quantity(memory.trim()))
                .setScale(0, RoundingMode.CEILING).longValueExact();
    }

}
