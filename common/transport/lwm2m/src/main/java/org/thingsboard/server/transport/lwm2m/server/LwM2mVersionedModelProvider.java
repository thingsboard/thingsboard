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

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LwM2mVersionedModelProvider  implements LwM2mModelProvider {

    private LwM2mModelRepository repository;
    private Map<String, LwM2mModelRepository> repositoriesTenant;
    private LwM2mClientContext lwM2mClientContext;

    public LwM2mVersionedModelProvider(Collection<ObjectModel> objectModels, LwM2mClientContext lwM2mClientContext) {
        this.repository = new LwM2mModelRepository(objectModels);
        this.lwM2mClientContext = lwM2mClientContext;
        this.repositoriesTenant = new ConcurrentHashMap<>();
    }

    public LwM2mVersionedModelProvider(LwM2mModelRepository repository, LwM2mClientContext lwM2mClientContext) {
        this.repository = repository;
        this.lwM2mClientContext = lwM2mClientContext;
        this.repositoriesTenant = new ConcurrentHashMap<>();
    }

    public void setRepositoriesTenant (String tenantID, LwM2mModelRepository repositoryTenant) {
        this.repositoriesTenant.put(tenantID, repositoryTenant);
    }

    public LwM2mModelRepository getRepositoriesTenant (String tenantID) {
        return this.repositoriesTenant.get(tenantID);
    }

    public void setRepository (Collection<ObjectModel> objectModels) {
        this.repository = new LwM2mModelRepository(objectModels);
    }

    public LwM2mModelRepository getRepositoriesCommonTenant (String tenantID) {
        LwM2mModelRepository repository = new LwM2mModelRepository();

        return repository;
    }

    @Override
    public LwM2mModel getObjectModel(Registration registration) {
        return new DynamicModel(registration, this.lwM2mClientContext.getProfile(registration).getTenantId());
    }

    private class DynamicModel implements LwM2mModel {

        private final Registration registration;
        private final UUID tenantId;

        public DynamicModel(Registration registration, UUID tenantId) {
            this.registration = registration;
            this.tenantId = tenantId;
        }

        @Override
        public ResourceModel getResourceModel(int objectId, int resourceId) {
            ObjectModel objectModel = getObjectModel(objectId);
            if (objectModel != null)
                return objectModel.resources.get(resourceId);
            else
                return null;
        }

        @Override
        public ObjectModel getObjectModel(int objectId) {
            String version = registration.getSupportedVersion(objectId);
            if (version != null) {
                return repository.getObjectModel(objectId, version);
            }
            return null;
        }

        @Override
        public Collection<ObjectModel> getObjectModels() {
            Map<Integer, String> supportedObjects = registration.getSupportedObject();
            Collection<ObjectModel> result = new ArrayList<>(supportedObjects.size());
            for (Map.Entry<Integer, String> supportedObject : supportedObjects.entrySet()) {
                ObjectModel objectModel = repository.getObjectModel(supportedObject.getKey(),
                        supportedObject.getValue());
                if (objectModel != null)
                    result.add(objectModel);
            }
            return result;
        }
    }
}
