// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.service;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCreator;

public abstract class ContextBasedCreator implements TbActorCreator {

    protected final transient ActorSystemContext context;

    public ContextBasedCreator(ActorSystemContext context) {
        super();
        this.context = context;
    }
}
