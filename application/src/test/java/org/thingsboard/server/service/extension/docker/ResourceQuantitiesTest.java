// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit coverage for the Kubernetes-quantity → Docker-unit conversions ({@link ResourceQuantities}):
 * CPU into NanoCPUs and memory into bytes, across milli/whole/fractional CPU and binary/decimal/bare
 * memory suffixes.
 */
class ResourceQuantitiesTest {

    @Test
    void cpuMilliWholeAndFractional() {
        assertThat(ResourceQuantities.cpuToNanoCpus("250m")).isEqualTo(250_000_000L);
        assertThat(ResourceQuantities.cpuToNanoCpus("1")).isEqualTo(1_000_000_000L);
        assertThat(ResourceQuantities.cpuToNanoCpus("2")).isEqualTo(2_000_000_000L);
        assertThat(ResourceQuantities.cpuToNanoCpus("1.5")).isEqualTo(1_500_000_000L);
        assertThat(ResourceQuantities.cpuToNanoCpus("500m")).isEqualTo(500_000_000L);
    }

    @Test
    void cpuBlankIsNull() {
        assertThat(ResourceQuantities.cpuToNanoCpus(null)).isNull();
        assertThat(ResourceQuantities.cpuToNanoCpus("  ")).isNull();
    }

    @Test
    void cpuInvalidThrows() {
        assertThatThrownBy(() -> ResourceQuantities.cpuToNanoCpus("abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void memoryBinarySuffixes() {
        assertThat(ResourceQuantities.memoryToBytes("256Mi")).isEqualTo(256L * 1024 * 1024);
        assertThat(ResourceQuantities.memoryToBytes("8Gi")).isEqualTo(8L * 1024 * 1024 * 1024);
        assertThat(ResourceQuantities.memoryToBytes("512Ki")).isEqualTo(512L * 1024);
        assertThat(ResourceQuantities.memoryToBytes("1Ti")).isEqualTo(1024L * 1024 * 1024 * 1024);
    }

    @Test
    void memoryDecimalSuffixes() {
        assertThat(ResourceQuantities.memoryToBytes("512M")).isEqualTo(512_000_000L);
        assertThat(ResourceQuantities.memoryToBytes("1G")).isEqualTo(1_000_000_000L);
        assertThat(ResourceQuantities.memoryToBytes("1000k")).isEqualTo(1_000_000L);
    }

    @Test
    void memoryBareBytesAndFractional() {
        assertThat(ResourceQuantities.memoryToBytes("104857600")).isEqualTo(104_857_600L);
        // fractional binary rounds up so the limit is never under-provisioned
        assertThat(ResourceQuantities.memoryToBytes("1.5Gi")).isEqualTo((long) Math.ceil(1.5 * 1024 * 1024 * 1024));
    }

    @Test
    void memoryBlankIsNull() {
        assertThat(ResourceQuantities.memoryToBytes(null)).isNull();
        assertThat(ResourceQuantities.memoryToBytes("")).isNull();
    }

    @Test
    void memoryInvalidThrows() {
        assertThatThrownBy(() -> ResourceQuantities.memoryToBytes("12Xi"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourceQuantities.memoryToBytes("notanumber"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void memoryLowercaseMIsMilliNotMega() {
        // Kubernetes 'm' is milli, the SAME meaning the validator (fabric8) and kubelet use.
        // The old hand-rolled parser wrongly read 'm' as Mega (500m -> ~477 MiB); now 500m == 0.5 bytes,
        // rounded up so the limit is never under-provisioned.
        assertThat(ResourceQuantities.memoryToBytes("500m")).isEqualTo(1L);
        assertThat(ResourceQuantities.memoryToBytes("1000m")).isEqualTo(1L);
        assertThat(ResourceQuantities.memoryToBytes("3000m")).isEqualTo(3L);
    }

    @Test
    void memoryAcceptsFullK8sGrammar() {
        // Suffixes/forms the old parser threw on but the validator (fabric8) accepts — both now agree.
        assertThat(ResourceQuantities.memoryToBytes("1Pi")).isEqualTo(1024L * 1024 * 1024 * 1024 * 1024);
        assertThat(ResourceQuantities.memoryToBytes("2E")).isEqualTo(2_000_000_000_000_000_000L);
        assertThat(ResourceQuantities.memoryToBytes("1e6")).isEqualTo(1_000_000L);
    }

}
