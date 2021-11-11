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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.transport.lwm2m.config.LwM2mVersion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LWM2M_OBJECT_VERSION_DEFAULT;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertObjectIdToVersionedId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.equalsResourceTypeGetSimpleName;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.fromVersionedIdToObjectId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.getVerFromPathIdVerOrId;

@Slf4j
@EqualsAndHashCode(of = {"endpoint"})
public class LwM2mClient implements Serializable {

    private static final long serialVersionUID = 8793482946289222623L;

    private final String nodeId;
    @Getter
    private final String endpoint;

    private transient Lock lock;

    @Getter
    private final Map<String, ResourceValue> resources;
    @Getter
    private final Map<String, TsKvProto> sharedAttributes;
    @Getter
    private final ConcurrentMap<String, AtomicLong> keyTsLatestMap;

    @Getter
    private TenantId tenantId;
    @Getter
    private UUID profileId;
    @Getter
    private UUID deviceId;
    @Getter
    @Setter
    private LwM2MClientState state;
    @Getter
    private SessionInfoProto session;
    @Getter
    private PowerMode powerMode;
    @Getter
    private Long psmActivityTimer;
    @Getter
    private Long edrxCycle;
    @Getter
    private Long pagingTransmissionWindow;
    @Getter
    @Setter
    private Registration registration;
    @Getter
    @Setter
    private boolean asleep;
    @Getter
    private long lastUplinkTime;
    @Getter
    @Setter
    private transient Future<Void> sleepTask;

    private boolean firstEdrxDownlink = true;

    @Getter
    private final AtomicInteger retryAttempts;

    @Getter
    @Setter
    private UUID lastSentRpcId;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public LwM2mClient(String nodeId, String endpoint) {
        this.nodeId = nodeId;
        this.endpoint = endpoint;
        this.sharedAttributes = new ConcurrentHashMap<>();
        this.resources = new ConcurrentHashMap<>();
        this.keyTsLatestMap = new ConcurrentHashMap<>();
        this.state = LwM2MClientState.CREATED;
        this.lock = new ReentrantLock();
        this.retryAttempts = new AtomicInteger(0);
    }

    public void init(ValidateDeviceCredentialsResponse credentials, UUID sessionId) {
        this.session = createSession(nodeId, sessionId, credentials);
        this.tenantId = new TenantId(new UUID(session.getTenantIdMSB(), session.getTenantIdLSB()));
        this.deviceId = new UUID(session.getDeviceIdMSB(), session.getDeviceIdLSB());
        this.profileId = new UUID(session.getDeviceProfileIdMSB(), session.getDeviceProfileIdLSB());
        this.powerMode = credentials.getDeviceInfo().getPowerMode();
        this.edrxCycle = credentials.getDeviceInfo().getEdrxCycle();
        this.psmActivityTimer = credentials.getDeviceInfo().getPsmActivityTimer();
        this.pagingTransmissionWindow = credentials.getDeviceInfo().getPagingTransmissionWindow();
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void onDeviceUpdate(Device device, Optional<DeviceProfile> deviceProfileOpt) {
        SessionInfoProto.Builder builder = SessionInfoProto.newBuilder().mergeFrom(session);
        this.deviceId = device.getUuidId();
        builder.setDeviceIdMSB(deviceId.getMostSignificantBits());
        builder.setDeviceIdLSB(deviceId.getLeastSignificantBits());
        builder.setDeviceName(device.getName());
        deviceProfileOpt.ifPresent(deviceProfile -> updateSession(deviceProfile, builder));
        this.session = builder.build();
        Lwm2mDeviceTransportConfiguration transportConfiguration = (Lwm2mDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
        this.powerMode = transportConfiguration.getPowerMode();
        this.edrxCycle = transportConfiguration.getEdrxCycle();
        this.psmActivityTimer = transportConfiguration.getPsmActivityTimer();
        this.pagingTransmissionWindow = transportConfiguration.getPagingTransmissionWindow();
    }

    public void onDeviceProfileUpdate(DeviceProfile deviceProfile) {
        SessionInfoProto.Builder builder = SessionInfoProto.newBuilder().mergeFrom(session);
        updateSession(deviceProfile, builder);
        this.session = builder.build();
    }

    private void updateSession(DeviceProfile deviceProfile, SessionInfoProto.Builder builder) {
        this.profileId = deviceProfile.getUuidId();
        builder.setDeviceProfileIdMSB(profileId.getMostSignificantBits());
        builder.setDeviceProfileIdLSB(profileId.getLeastSignificantBits());
        builder.setDeviceType(deviceProfile.getName());
    }

    public void refreshSessionId(String nodeId) {
        UUID newId = UUID.randomUUID();
        SessionInfoProto.Builder builder = SessionInfoProto.newBuilder().mergeFrom(session);
        builder.setNodeId(nodeId);
        builder.setSessionIdMSB(newId.getMostSignificantBits());
        builder.setSessionIdLSB(newId.getLeastSignificantBits());
        this.session = builder.build();
    }

    private SessionInfoProto createSession(String nodeId, UUID sessionId, ValidateDeviceCredentialsResponse msg) {
        return SessionInfoProto.newBuilder()
                .setNodeId(nodeId)
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceInfo().getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceInfo().getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(msg.getDeviceInfo().getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getDeviceInfo().getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(msg.getDeviceInfo().getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(msg.getDeviceInfo().getCustomerId().getId().getLeastSignificantBits())
                .setDeviceName(msg.getDeviceInfo().getDeviceName())
                .setDeviceType(msg.getDeviceInfo().getDeviceType())
                .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileId().getId().getLeastSignificantBits())
                .build();
    }

    public boolean saveResourceValue(String pathRezIdVer, LwM2mResource rez, LwM2mModelProvider modelProvider) {
        if (this.resources.get(pathRezIdVer) != null && this.resources.get(pathRezIdVer).getResourceModel() != null) {
            this.resources.get(pathRezIdVer).setLwM2mResource(rez);
            return true;
        } else {
            LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRezIdVer));
            ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
            if (resourceModel != null) {
                this.resources.put(pathRezIdVer, new ResourceValue(rez, resourceModel));
                return true;
            } else {
                return false;
            }
        }
    }

    public Object getResourceValue(String pathRezIdVer, String pathRezId) {
        String pathRez = pathRezIdVer == null ? convertObjectIdToVersionedId(pathRezId, this.registration) : pathRezIdVer;
        if (this.resources.get(pathRez) != null) {
            return this.resources.get(pathRez).getLwM2mResource().getValue();
        }
        return null;
    }

    public Object getResourceNameByRezId(String pathRezIdVer, String pathRezId) {
        String pathRez = pathRezIdVer == null ? convertObjectIdToVersionedId(pathRezId, this.registration) : pathRezIdVer;
        if (this.resources.get(pathRez) != null) {
            return this.resources.get(pathRez).getResourceModel().name;
        }
        return null;
    }

    public String getRezIdByResourceNameAndObjectInstanceId(String resourceName, String pathObjectInstanceIdVer, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathObjectInstanceIdVer));
        if (pathIds.isObjectInstance()) {
            Set<Integer> rezIds = modelProvider.getObjectModel(registration)
                    .getObjectModel(pathIds.getObjectId()).resources.entrySet()
                    .stream()
                    .filter(map -> resourceName.equals(map.getValue().name))
                    .map(map -> map.getKey())
                    .collect(Collectors.toSet());
            return rezIds.size() > 0 ? String.valueOf(rezIds.stream().findFirst().get()) : null;
        }
        return null;
    }

    public ResourceModel getResourceModel(String pathIdVer, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathIdVer));
        String verSupportedObject = registration.getSupportedObject().get(pathIds.getObjectId());
        String verRez = getVerFromPathIdVerOrId(pathIdVer);
        return verRez == null || verRez.equals(verSupportedObject) ? modelProvider.getObjectModel(registration)
                .getResourceModel(pathIds.getObjectId(), pathIds.getResourceId()) : null;
    }

    public boolean isResourceMultiInstances(String pathIdVer, LwM2mModelProvider modelProvider) {
        var resourceModel = getResourceModel(pathIdVer, modelProvider);
        if (resourceModel != null && resourceModel.multiple != null) {
            return resourceModel.multiple;
        } else {
            return false;
        }
    }

    public ObjectModel getObjectModel(String pathIdVer, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathIdVer));
        String verSupportedObject = registration.getSupportedObject().get(pathIds.getObjectId());
        String verRez = getVerFromPathIdVerOrId(pathIdVer);
        return verRez == null || verRez.equals(verSupportedObject) ? modelProvider.getObjectModel(registration)
                .getObjectModel(pathIds.getObjectId()) : null;
    }

    public Collection<LwM2mResource> getNewResourceForInstance(String pathRezIdVer, Object params, LwM2mModelProvider modelProvider,
                                                               LwM2mValueConverter converter) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRezIdVer));
        Collection<LwM2mResource> resources = ConcurrentHashMap.newKeySet();
        Map<Integer, ResourceModel> resourceModels = modelProvider.getObjectModel(registration)
                .getObjectModel(pathIds.getObjectId()).resources;
        resourceModels.forEach((resId, resourceModel) -> {
            if (resId.equals(pathIds.getResourceId())) {
                resources.add(LwM2mSingleResource.newResource(resId, converter.convertValue(params,
                        equalsResourceTypeGetSimpleName(params), resourceModel.type, pathIds), resourceModel.type));

            }
        });
        return resources;
    }

    public Collection<LwM2mResource> getNewResourcesForInstance(String pathRezIdVer, Object params, LwM2mModelProvider modelProvider,
                                                                LwM2mValueConverter converter) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRezIdVer));
        Collection<LwM2mResource> resources = ConcurrentHashMap.newKeySet();
        Map<Integer, ResourceModel> resourceModels = modelProvider.getObjectModel(registration)
                .getObjectModel(pathIds.getObjectId()).resources;
        resourceModels.forEach((resId, resourceModel) -> {
            if (((Map) params).containsKey(String.valueOf(resId))) {
                Object value = ((Map) params).get((String.valueOf(resId)));
                resources.add(LwM2mSingleResource.newResource(resId,
                        converter.convertValue(value, equalsResourceTypeGetSimpleName(value), resourceModel.type, pathIds), resourceModel.type));

            }
        });
        return resources;
    }

    public void isValidObjectVersion(String path) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(path));
        String verSupportedObject = registration.getSupportedObject().get(pathIds.getObjectId());
        String verRez = getVerFromPathIdVerOrId(path);
        if ((verRez != null && !verRez.equals(verSupportedObject)) ||
                (verRez == null && !LWM2M_OBJECT_VERSION_DEFAULT.equals(verSupportedObject))) {
            throw new IllegalArgumentException(String.format("Specified resource id %s is not valid version! Must be version: %s", path, verSupportedObject));
        }
    }

    /**
     * @param pathIdVer     == "3_1.0"
     * @param modelProvider -
     */
    public void deleteResources(String pathIdVer, LwM2mModelProvider modelProvider) {
        Set<String> key = getKeysEqualsIdVer(pathIdVer);
        key.forEach(pathRez -> {
            LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRez));
            ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
            if (resourceModel != null) {
                this.resources.get(pathRez).setResourceModel(resourceModel);
            } else {
                this.resources.remove(pathRez);
            }
        });
    }

    /**
     * @param idVer         -
     * @param modelProvider -
     */
    public void updateResourceModel(String idVer, LwM2mModelProvider modelProvider) {
        Set<String> key = getKeysEqualsIdVer(idVer);
        key.forEach(k -> this.saveResourceModel(k, modelProvider));
    }

    private void saveResourceModel(String pathRez, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRez));
        ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
        this.resources.get(pathRez).setResourceModel(resourceModel);
    }

    private Set<String> getKeysEqualsIdVer(String idVer) {
        return this.resources.keySet()
                .stream()
                .filter(e -> idVer.equals(e.split(LWM2M_SEPARATOR_PATH)[1]))
                .collect(Collectors.toSet());
    }

    public ContentFormat getDefaultContentFormat() {
        if (registration == null) {
            return ContentFormat.DEFAULT;
        } else {
            return LwM2mVersion.fromVersionStr(registration.getLwM2mVersion()).getContentFormat();
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.lock = new ReentrantLock();
    }

    public long updateLastUplinkTime() {
        this.lastUplinkTime = System.currentTimeMillis();
        this.firstEdrxDownlink = true;
        return lastUplinkTime;
    }

    public boolean checkFirstDownlink() {
        boolean result = firstEdrxDownlink;
        firstEdrxDownlink = false;
        return result;
    }

}

