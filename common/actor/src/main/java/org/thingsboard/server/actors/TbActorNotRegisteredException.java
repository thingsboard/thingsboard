// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import lombok.Getter;

public class TbActorNotRegisteredException extends RuntimeException {

    @Getter
    private TbActorId target;

    public TbActorNotRegisteredException(TbActorId target, String message) {
        super(message);
        this.target = target;
    }
}
