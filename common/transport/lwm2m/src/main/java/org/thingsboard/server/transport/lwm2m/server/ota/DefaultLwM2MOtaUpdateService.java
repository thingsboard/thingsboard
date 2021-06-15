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
package org.thingsboard.server.transport.lwm2m.server.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.common.data.ota.OtaPackageUtil.getAttributeKey;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MOtaUpdateService implements LwM2MOtaUpdateService {

    private static final String FW_NAME_ID = "/5/0/6";
    private static final String FW_VER_ID = "/5/0/7";
    private static final String SW_NAME_ID = "/9/0/0";
    private static final String SW_VER_ID = "/9/0/1";

    private final Map<String, LwM2MClientOtaState> fwStates = new ConcurrentHashMap<>();
    private final Map<String, LwM2MClientOtaState> swStates = new ConcurrentHashMap<>();

    private final LwM2MAttributesService attributesService;
    private final TransportService transportService;
    private final LwM2mClientContext clientContext;

    @Override
    public void init(LwM2mClient client) {
        //TODO: check that the client supports FW and SW by checking the supported objects in the model.
        List<String> attributesToFetch = new ArrayList<>();
        if (client.isValidObjectVersion(FW_NAME_ID) || client.isValidObjectVersion(FW_VER_ID)) {
            LwM2MClientOtaState fwState = getOrInitFwSate(client);
            attributesToFetch.add(getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TITLE));
            attributesToFetch.add(getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION));
        }

        if (client.isValidObjectVersion(SW_NAME_ID) || client.isValidObjectVersion(SW_VER_ID)) {
            LwM2MClientOtaState swState = getOrInitSwSate(client);
            attributesToFetch.add(getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TITLE));
            attributesToFetch.add(getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.VERSION));
        }

        var future =  attributesService.getSharedAttributes(client, attributesToFetch);
    }

    private LwM2MClientOtaState getOrInitFwSate(LwM2mClient client) {
        //TODO: fetch state from the cache.
        return fwStates.computeIfAbsent(client.getEndpoint(), endpoint -> new LwM2MClientOtaState());
    }

    private LwM2MClientOtaState getOrInitSwSate(LwM2mClient client) {
        //TODO: fetch state from the cache.
        return swStates.computeIfAbsent(client.getEndpoint(), endpoint -> new LwM2MClientOtaState());
    }

}
