// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Container resource request expressed as a predefined {@link ResourceSize} tier. Raw Kubernetes
 * quantities are no longer accepted on the API surface — a size is the single knob. The concrete
 * cpu/memory values live on the enum, so the effective* accessors below stay the one place every
 * deploy path (k8s renderer, docker planner, validator) reads from.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} lets a row written by an earlier build of this
 * feature — one that still carried the old {@code cpuRequest}/{@code memoryRequest}/… fields — deserialize
 * cleanly instead of failing. Such a row has no {@code size}, so {@link #effectiveSize()} falls back to
 * {@link #DEFAULT_SIZE} until the extension is saved again (which records the tier explicitly). The
 * {@code extension} table is introduced by this feature, so no released build ever persisted the old
 * fields and no data migration is required.
 */
@Data
@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final ResourceSize DEFAULT_SIZE = ResourceSize.DEFAULT;
    public static final String DEFAULT_CPU_REQUEST = DEFAULT_SIZE.getCpuRequest();
    public static final String DEFAULT_MEMORY_REQUEST = DEFAULT_SIZE.getMemoryRequest();

    // An annotation value must be a compile-time constant, so the tier name cannot be read from
    // ResourceSize.DEFAULT here and has to be spelled out. It already went stale once when the default
    // moved, and this description IS the contract clients read, so ResourceSpecTest pins the two together.
    @Schema(description = "Predefined resource tier; null defaults to SMALL.", example = "MEDIUM")
    private ResourceSize size;

    /** The tier this spec resolves to: the explicit size, or the default when none is set. */
    @JsonIgnore
    public ResourceSize effectiveSize() {
        return size != null ? size : DEFAULT_SIZE;
    }

    public String effectiveCpuRequest() {
        return effectiveSize().getCpuRequest();
    }

    public String effectiveMemoryRequest() {
        return effectiveSize().getMemoryRequest();
    }

    public String effectiveCpuLimit() {
        return effectiveSize().getCpuLimit();
    }

    public String effectiveMemoryLimit() {
        return effectiveSize().getMemoryLimit();
    }

}
