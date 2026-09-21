// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.shared;

public abstract class ActorTerminationMsg<T> {

    private final T id;

    public ActorTerminationMsg(T id) {
        super();
        this.id = id;
    }

    public T getId() {
        return id;
    }

}
