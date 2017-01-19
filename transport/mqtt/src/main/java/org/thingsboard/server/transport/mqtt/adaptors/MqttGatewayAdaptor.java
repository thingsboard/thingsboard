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
package org.thingsboard.server.transport.mqtt.adaptors;

import io.netty.handler.codec.mqtt.MqttMessage;
import org.thingsboard.server.common.msg.session.AdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.MsgType;
import org.thingsboard.server.common.msg.session.SessionActorToAdaptorMsg;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.transport.mqtt.session.GatewaySessionCtx;

import java.util.Optional;

/**
 * Created by ashvayka on 19.01.17.
 */
public interface MqttGatewayAdaptor  {

    AdaptorToSessionActorMsg convertToActorMsg(GatewaySessionCtx ctx, MsgType type, MqttMessage inbound) throws AdaptorException;

    Optional<MqttMessage> convertToAdaptorMsg(GatewaySessionCtx ctx, SessionActorToAdaptorMsg msg) throws AdaptorException;

}
