/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import org.thingsboard.server.transport.lwm2m.server.LwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE_CANCEL_ALL;

public class TbLwM2MCancelAllObserveCallback extends AbstractTbLwM2MRequestCallback<Integer> {

    public TbLwM2MCancelAllObserveCallback(LwM2mUplinkMsgHandler handler, LwM2mClient client) {
        super(handler, client);
    }

    @Override
    public void onSuccess(Integer canceledSubscriptionsCount) {
        String observeCancelMsg = String.format("%s: type operation %s paths: count: %d", LOG_LW2M_INFO, OBSERVE_CANCEL_ALL.name(), canceledSubscriptionsCount);
    }

}
