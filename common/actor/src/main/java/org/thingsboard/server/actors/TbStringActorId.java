// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import org.thingsboard.server.common.data.EntityType;

import java.util.Objects;

public class TbStringActorId implements TbActorId {

    private final String id;

    public TbStringActorId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TbStringActorId that = (TbStringActorId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public EntityType getEntityType() {
        return null;
    }
}
