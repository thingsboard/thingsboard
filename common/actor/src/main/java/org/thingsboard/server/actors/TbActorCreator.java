// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

public interface TbActorCreator {

    TbActorId createActorId();

    TbActor createActor();

}
