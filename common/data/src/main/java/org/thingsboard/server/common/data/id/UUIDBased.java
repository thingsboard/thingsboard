// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.UUID;

@Schema
public abstract class UUIDBased implements HasUUID, Serializable {

    private static final long serialVersionUID = 1L;

    /** Cache the hash code */
    private transient int hash; // Default to 0. The hash code calculated for this object likely never be zero

    private final UUID id;

    public UUIDBased() {
        this(UUID.randomUUID());
    }

    public UUIDBased(UUID id) {
        super();
        this.id = id;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    public UUID getId() {
        return id;
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            final int prime = 31;
            int result = 1;
            hash = prime * result + ((id == null) ? 0 : id.hashCode());
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UUIDBased other = (UUIDBased) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

}
