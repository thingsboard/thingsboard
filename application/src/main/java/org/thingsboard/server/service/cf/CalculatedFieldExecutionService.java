/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.cf;

import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.cf.telemetry.CalculatedFieldTelemetryUpdateRequest;

public interface CalculatedFieldExecutionService {

    void onCalculatedFieldMsg(TransportProtos.CalculatedFieldMsgProto proto, TbCallback callback);

    void onTelemetryUpdate(CalculatedFieldTelemetryUpdateRequest calculatedFieldTelemetryUpdateRequest);

    void onCalculatedFieldStateMsg(TransportProtos.CalculatedFieldStateMsgProto proto, TbCallback callback);

    void onEntityProfileChangedMsg(TransportProtos.EntityProfileUpdateMsgProto proto, TbCallback callback);

    void onProfileEntityMsg(TransportProtos.ProfileEntityMsgProto proto, TbCallback callback);

}
