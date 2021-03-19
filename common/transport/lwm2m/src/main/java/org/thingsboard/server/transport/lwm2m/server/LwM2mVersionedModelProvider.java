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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static org.thingsboard.server.common.data.ResourceType.LWM2M_MODEL;

@Slf4j
public class LwM2mVersionedModelProvider implements LwM2mModelProvider {

    /**
     * int objectId
     * String version ("1.01")
     * Key = objectId + "##" + version
     * Value = TenantId
     */
    private final LwM2mClientContext lwM2mClientContext;
    private final LwM2mTransportContextServer lwM2mTransportContextServer;

    public LwM2mVersionedModelProvider(LwM2mClientContext lwM2mClientContext, LwM2mTransportContextServer lwM2mTransportContextServer) {
        this.lwM2mClientContext = lwM2mClientContext;
        this.lwM2mTransportContextServer = lwM2mTransportContextServer;
    }
    private String getIdVer(ObjectModel objectModel) {
        return objectModel.id + "##" + ((objectModel.getVersion() == null || objectModel.getVersion().isEmpty()) ? ObjectModel.DEFAULT_VERSION : objectModel.getVersion());
    }

    private String getIdVer(Integer objectId, String version) {
        return objectId != null ? objectId + "##" + ((version == null || version.isEmpty()) ? ObjectModel.DEFAULT_VERSION : version) : null;
    }

    /**
     * Update repository if need
     *
     * @param registration
     * @return
     */
    @Override
    public LwM2mModel getObjectModel(Registration registration) {
        return new DynamicModel(registration
        );
    }

    private class DynamicModel implements LwM2mModel {

        private final Registration registration;
        private final TenantId tenantId;

        public DynamicModel(Registration registration) {
            this.registration = registration;
            this.tenantId = lwM2mClientContext.getProfile(registration).getTenantId();
        }

        @Override
        public ResourceModel getResourceModel(int objectId, int resourceId) {
            try {
                ObjectModel objectModel = getObjectModel(objectId);
                if (objectModel != null)
                    return objectModel.resources.get(resourceId);
                else
                    return null;
            } catch (Exception e) {
                log.error("", e);
                return null;
            }
        }

        @Override
        public ObjectModel getObjectModel(int objectId) {
            String version = registration.getSupportedVersion(objectId);
            if (version != null) {
                return this.getObjectModelDynamic(objectId, version);
            }
            return null;
        }

        @Override
        public Collection<ObjectModel> getObjectModels() {
            Map<Integer, String> supportedObjects = this.registration.getSupportedObject();
            Collection<ObjectModel> result = new ArrayList<>(supportedObjects.size());
            Iterator<Map.Entry<Integer, String>> i$ = supportedObjects.entrySet().iterator();

            while (i$.hasNext()) {
                Map.Entry<Integer, String> supportedObject = i$.next();
                ObjectModel objectModel = this.getObjectModelDynamic(supportedObject.getKey(), supportedObject.getValue());
                if (objectModel != null) {
                    result.add(objectModel);
                }
            }
            return result;
        }

        private ObjectModel getObjectModelDynamic(Integer objectId, String version) {
            String key = getIdVer(objectId, version);
            String xmlB64 = lwM2mTransportContextServer.getTransportResourceCache().get(
                    this.tenantId,
                    LWM2M_MODEL,
                    key).
                    getData();
            return xmlB64 != null && !xmlB64.isEmpty() ?
                    lwM2mTransportContextServer.parseFromXmlToObjectModel(
                            Base64.getDecoder().decode(xmlB64),
                            key + ".xml",
                            new DefaultDDFFileValidator()) :
                    null;
        }
    }
}
