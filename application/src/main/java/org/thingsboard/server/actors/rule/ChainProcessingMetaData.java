/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.actors.rule;

import akka.actor.ActorRef;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.extensions.api.device.DeviceMetaData;

/**
 * Immutable part of chain processing data;
 *
 * @author ashvayka
 */
public final class ChainProcessingMetaData {

    final RuleActorChain chain;
    final ToDeviceActorMsg inMsg;
    final ActorRef originator;
    final DeviceMetaData deviceMetaData;

    public ChainProcessingMetaData(RuleActorChain chain, ToDeviceActorMsg inMsg, DeviceMetaData deviceMetaData, ActorRef originator) {
        super();
        this.chain = chain;
        this.inMsg = inMsg;
        this.originator = originator;
        this.deviceMetaData = deviceMetaData;
    }
}
