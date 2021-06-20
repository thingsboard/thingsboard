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

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.List;
import java.util.Set;

public interface LwM2mDownlinkMsgHandler {

    void sendReadRequest(LwM2mClient client, TbLwM2MReadRequest request, DownlinkRequestCallback<ReadRequest, ReadResponse> callback);

    void sendObserveRequest(LwM2mClient client, TbLwM2MObserveRequest request, DownlinkRequestCallback<ObserveRequest, ObserveResponse> callback);

    void sendObserveAllRequest(LwM2mClient client, TbLwM2MObserveAllRequest request, DownlinkRequestCallback<TbLwM2MObserveAllRequest, Set<String>> callback);

    void sendExecuteRequest(LwM2mClient client, TbLwM2MExecuteRequest request, DownlinkRequestCallback<ExecuteRequest, ExecuteResponse> callback);

    void sendDeleteRequest(LwM2mClient client, TbLwM2MDeleteRequest request, DownlinkRequestCallback<DeleteRequest, DeleteResponse> callback);

    void sendCancelObserveRequest(LwM2mClient client, TbLwM2MCancelObserveRequest request, DownlinkRequestCallback<TbLwM2MCancelObserveRequest, Integer> callback);

    void sendCancelAllRequest(LwM2mClient client, TbLwM2MCancelAllRequest request, DownlinkRequestCallback<TbLwM2MCancelAllRequest, Integer> callback);

    void sendDiscoverRequest(LwM2mClient client, TbLwM2MDiscoverRequest request, DownlinkRequestCallback<DiscoverRequest, DiscoverResponse> callback);

    void sendDiscoverAllRequest(LwM2mClient client, TbLwM2MDiscoverAllRequest request, DownlinkRequestCallback<TbLwM2MDiscoverAllRequest, List<Link>> callback);

    void sendWriteAttributesRequest(LwM2mClient client, TbLwM2MWriteAttributesRequest request, DownlinkRequestCallback<WriteAttributesRequest, WriteAttributesResponse> callback);

    void sendWriteReplaceRequest(LwM2mClient client, TbLwM2MWriteReplaceRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback);

    void sendWriteUpdateRequest(LwM2mClient client, TbLwM2MWriteUpdateRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback);


}
