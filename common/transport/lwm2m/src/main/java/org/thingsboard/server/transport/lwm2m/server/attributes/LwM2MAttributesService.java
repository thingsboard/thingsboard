// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Collection;
import java.util.List;

public interface LwM2MAttributesService {

    ListenableFuture<List<TransportProtos.TsKvProto>> getSharedAttributes(LwM2mClient client, Collection<String> keys);

    void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg getAttributesResponse, TransportProtos.SessionInfoProto sessionInfo);

    void onAttributesUpdate(TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotification, TransportProtos.SessionInfoProto sessionInfo);

    void onAttributesUpdate(LwM2mClient lwM2MClient, List<TransportProtos.TsKvProto> tsKvProtos, boolean logFailedUpdateOfNonChangedValue);
}
