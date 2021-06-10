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
package org.thingsboard.server.transport.lwm2m.server;

import org.eclipse.leshan.core.request.ContentFormat;
import org.thingsboard.server.common.data.device.data.lwm2m.ObjectAttributes;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;

public interface LwM2mDownlinkMsgHandler {

    void sendReadRequest(LwM2mClient client, String targetId, Long timeout);

    void sendReadRequest(LwM2mClient client, String targetId, ContentFormat contentFormat, Long timeout);

    void sendObserveRequest(LwM2mClient client, String targetId, Long timeout);

    void sendObserveRequest(LwM2mClient client, String targetId, ContentFormat contentFormat, Long timeout);

    void sendExecuteRequest(LwM2mClient client, String targetId, Long timeout, DownlinkRequestCallback callback);

    void sendExecuteRequest(LwM2mClient client, String targetId, Object params, Long timeout, DownlinkRequestCallback callback);

    void sendCancelObserveRequest(LwM2mClient client, String targetId, Long timeout, DownlinkRequestCallback callback);

    void sendCancelAllRequest(LwM2mClient client, Long timeout, DownlinkRequestCallback callback);

    void sendDiscoverRequest(LwM2mClient client, String targetId, Long timeout);

    void sendWriteAttributesRequest(LwM2mClient client, String targetId, ObjectAttributes params, Long timeout);

    void sendWriteReplaceRequest(LwM2mClient client, String targetIdVer, Object newValue, Long timeout, DownlinkRequestCallback callback);

    void sendWriteUpdateRequest(LwM2mClient client, String targetIdVer, Object newValue, ContentFormat contentFormat, Long timeout, DownlinkRequestCallback callback);
}
