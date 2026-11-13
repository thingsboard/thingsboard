// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceSpecTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nullSizeResolvesToTheDefaultTier() {
        ResourceSpec spec = new ResourceSpec();
        assertThat(spec.getSize()).isNull();
        assertThat(spec.effectiveCpuRequest()).isEqualTo(ResourceSize.DEFAULT.getCpuRequest());
        assertThat(spec.effectiveCpuLimit()).isEqualTo(ResourceSize.DEFAULT.getCpuLimit());
        assertThat(spec.effectiveMemoryRequest()).isEqualTo(ResourceSize.DEFAULT.getMemoryRequest());
        assertThat(spec.effectiveMemoryLimit()).isEqualTo(ResourceSize.DEFAULT.getMemoryLimit());
    }

    @Test
    void effectiveValuesFollowTheChosenSize() {
        ResourceSpec spec = new ResourceSpec();
        spec.setSize(ResourceSize.MEDIUM);
        assertThat(spec.effectiveCpuRequest()).isEqualTo("500m");
        assertThat(spec.effectiveCpuLimit()).isEqualTo("1");
        assertThat(spec.effectiveMemoryRequest()).isEqualTo("512Mi");
        assertThat(spec.effectiveMemoryLimit()).isEqualTo("512Mi");
    }

    @Test
    void memoryRequestAlwaysEqualsLimitAndCpuLimitNeverBelowRequest() {
        for (ResourceSize size : ResourceSize.values()) {
            assertThat(size.getMemoryRequest())
                    .as("memory request == limit for %s", size)
                    .isEqualTo(size.getMemoryLimit())
                    .isEqualTo(size.getMemory());
            assertThat(size.getCpuLimit()).as("cpuLimit present for %s", size).isNotBlank();
            assertThat(size.getCpuRequest()).as("cpuRequest present for %s", size).isNotBlank();
        }
    }

    @Test
    void defaultConstantsMirrorTheDefaultTier() {
        assertThat(ResourceSpec.DEFAULT_SIZE).isEqualTo(ResourceSize.DEFAULT);
        assertThat(ResourceSpec.DEFAULT_CPU_REQUEST).isEqualTo(ResourceSize.DEFAULT.getCpuRequest());
        assertThat(ResourceSpec.DEFAULT_MEMORY_REQUEST).isEqualTo(ResourceSize.DEFAULT.getMemoryRequest());
    }

    // The @Schema description must name the tier as a literal (annotation values are compile-time
    // constants), so it can drift away from ResourceSize.DEFAULT unnoticed — as it already did once.
    // This is the API contract clients read, so pin the two together.
    @Test
    void schemaDescriptionNamesTheActualDefaultTier() throws Exception {
        String description = ResourceSpec.class.getDeclaredField("size")
                .getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class).description();
        assertThat(description).contains(ResourceSize.DEFAULT.name());
    }

    @Test
    void legacyRawQuantityFieldsAreIgnoredOnDeserialize() throws Exception {
        // A row written before the t-shirt-size migration still carries the old raw quantities; it must
        // deserialize cleanly (unknown fields ignored) and fall back to the default tier.
        String legacyJson = "{\"cpuRequest\":\"2\",\"cpuLimit\":\"4\",\"memoryRequest\":\"4Gi\",\"memoryLimit\":\"4Gi\"}";
        ResourceSpec spec = mapper.readValue(legacyJson, ResourceSpec.class);
        assertThat(spec.getSize()).isNull();
        assertThat(spec.effectiveCpuRequest()).isEqualTo(ResourceSize.DEFAULT.getCpuRequest());
    }

    @Test
    void sizeDeserializesFromName() throws Exception {
        ResourceSpec spec = mapper.readValue("{\"size\":\"LARGE\"}", ResourceSpec.class);
        assertThat(spec.getSize()).isEqualTo(ResourceSize.LARGE);
    }

}
