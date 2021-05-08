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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.transport.lwm2m.server.DefaultLwM2MTransportMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.LwM2mQueuedRequest;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.TRANSPORT_DEFAULT_LWM2M_VERSION;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromIdVerToObjectId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromObjectIdToIdVer;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.equalsResourceTypeGetSimpleName;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.getVerFromPathIdVerOrId;

@Slf4j
public class LwM2mClient implements Cloneable {
    @Getter
    private String deviceName;
    @Getter
    private String deviceProfileName;
    @Getter
    private String endpoint;
    @Getter
    private String identity;
    @Getter
    private SecurityInfo securityInfo;
    @Getter
    private UUID deviceId;
    @Getter
    private UUID sessionId;
    @Getter
    private SessionInfoProto session;
    @Getter
    private UUID profileId;
    @Getter
    @Setter
    private volatile LwM2mFwSwUpdate fwUpdate;
    @Getter
    @Setter
    private volatile LwM2mFwSwUpdate swUpdate;
    @Getter
    @Setter
    private Registration registration;
    private ValidateDeviceCredentialsResponseMsg credentialsResponse;
    @Getter
    private final Map<String, ResourceValue> resources;
    @Getter
    private final Map<String, TsKvProto> delayedRequests;
    @Getter
    @Setter
    private final List<String> pendingReadRequests;
    @Getter
    private final Queue<LwM2mQueuedRequest> queuedRequests;
    @Getter
    private boolean init;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public LwM2mClient(String nodeId, String endpoint, String identity, SecurityInfo securityInfo, ValidateDeviceCredentialsResponseMsg credentialsResponse, UUID profileId, UUID sessionId) {
        this.endpoint = endpoint;
        this.identity = identity;
        this.securityInfo = securityInfo;
        this.credentialsResponse = credentialsResponse;
        this.delayedRequests = new ConcurrentHashMap<>();
        this.pendingReadRequests = new CopyOnWriteArrayList<>();
        this.resources = new ConcurrentHashMap<>();
        this.profileId = profileId;
        this.sessionId = sessionId;
        this.init = false;
        this.queuedRequests = new ConcurrentLinkedQueue<>();
        this.fwUpdate = new LwM2mFwSwUpdate(this, FirmwareType.FIRMWARE.name());
        this.swUpdate = new LwM2mFwSwUpdate(this, FirmwareType.SOFTWARE.name());
        if (this.credentialsResponse != null && this.credentialsResponse.hasDeviceInfo()) {
            this.session = createSession(nodeId, sessionId, credentialsResponse);
            this.deviceId = new UUID(session.getDeviceIdMSB(), session.getDeviceIdLSB());
            this.profileId = new UUID(session.getDeviceProfileIdMSB(), session.getDeviceProfileIdLSB());
            this.deviceName = session.getDeviceName();
            this.deviceProfileName = session.getDeviceType();
        }
    }

    public void onDeviceUpdate(Device device, Optional<DeviceProfile> deviceProfileOpt) {
        SessionInfoProto.Builder builder = SessionInfoProto.newBuilder().mergeFrom(session);
        this.deviceId = device.getUuidId();
        this.deviceName = device.getName();
        builder.setDeviceIdMSB(deviceId.getMostSignificantBits());
        builder.setDeviceIdLSB(deviceId.getLeastSignificantBits());
        builder.setDeviceName(deviceName);
        deviceProfileOpt.ifPresent(deviceProfile -> updateSession(deviceProfile, builder));
        this.session = builder.build();
    }

    public void onDeviceProfileUpdate(DeviceProfile deviceProfile) {
        SessionInfoProto.Builder builder = SessionInfoProto.newBuilder().mergeFrom(session);
        updateSession(deviceProfile, builder);
        this.session = builder.build();
    }

    private void updateSession(DeviceProfile deviceProfile, SessionInfoProto.Builder builder) {
        this.deviceProfileName = deviceProfile.getName();
        this.profileId = deviceProfile.getUuidId();
        builder.setDeviceProfileIdMSB(profileId.getMostSignificantBits());
        builder.setDeviceProfileIdLSB(profileId.getLeastSignificantBits());
        builder.setDeviceType(this.deviceProfileName);
    }

    private SessionInfoProto createSession(String nodeId, UUID sessionId, ValidateDeviceCredentialsResponseMsg msg) {
        return SessionInfoProto.newBuilder()
                .setNodeId(nodeId)
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceInfo().getDeviceIdMSB())
                .setDeviceIdLSB(msg.getDeviceInfo().getDeviceIdLSB())
                .setTenantIdMSB(msg.getDeviceInfo().getTenantIdMSB())
                .setTenantIdLSB(msg.getDeviceInfo().getTenantIdLSB())
                .setCustomerIdMSB(msg.getDeviceInfo().getCustomerIdMSB())
                .setCustomerIdLSB(msg.getDeviceInfo().getCustomerIdLSB())
                .setDeviceName(msg.getDeviceInfo().getDeviceName())
                .setDeviceType(msg.getDeviceInfo().getDeviceType())
                .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileIdLSB())
                .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileIdMSB())
                .build();
    }

    public boolean saveResourceValue(String pathRezIdVer, LwM2mResource rez, LwM2mModelProvider modelProvider) {
        if (this.resources.get(pathRezIdVer) != null && this.resources.get(pathRezIdVer).getResourceModel() != null) {
            this.resources.get(pathRezIdVer).setLwM2mResource(rez);
            return true;
        } else {
            LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(pathRezIdVer));
            ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
            if (resourceModel != null) {
                this.resources.put(pathRezIdVer, new ResourceValue(rez, resourceModel));
                return true;
            } else {
                return false;
            }
        }
    }

    public Object getResourceValue (String pathRezIdVer, String pathRezId) {
        String pathRez = pathRezIdVer == null ? convertPathFromObjectIdToIdVer(pathRezId, this.registration) : pathRezIdVer;
        if (this.resources.get(pathRez) != null) {
            return  this.resources.get(pathRez).getLwM2mResource().isMultiInstances() ?
                    this.resources.get(pathRez).getLwM2mResource().getValues() :
                    this.resources.get(pathRez).getLwM2mResource().getValue();
        }
        return null;
    }

    public ResourceModel getResourceModel(String pathIdVer, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(pathIdVer));
        String verSupportedObject = registration.getSupportedObject().get(pathIds.getObjectId());
        String verRez = getVerFromPathIdVerOrId(pathIdVer);
        return verRez == null || verRez.equals(verSupportedObject) ? modelProvider.getObjectModel(registration)
                .getResourceModel(pathIds.getObjectId(), pathIds.getResourceId()) : null;
    }

    public Collection<LwM2mResource> getNewOneResourceForInstance(String pathRezIdVer, Object params, LwM2mModelProvider modelProvider,
                                                                  LwM2mValueConverterImpl converter) {
        LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(pathRezIdVer));
        Collection<LwM2mResource> resources = ConcurrentHashMap.newKeySet();
        Map<Integer, ResourceModel> resourceModels = modelProvider.getObjectModel(registration)
                .getObjectModel(pathIds.getObjectId()).resources;
        resourceModels.forEach((resId, resourceModel) -> {
            if (resId == pathIds.getResourceId()) {
                resources.add(LwM2mSingleResource.newResource(resId, converter.convertValue(params,
                        equalsResourceTypeGetSimpleName(params), resourceModel.type, pathIds), resourceModel.type));

            }});
        return resources;
    }

    public Collection<LwM2mResource> getNewManyResourcesForInstance(String pathRezIdVer, Object params, LwM2mModelProvider modelProvider,
                                                                  LwM2mValueConverterImpl converter) {
        LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(pathRezIdVer));
        Collection<LwM2mResource> resources = ConcurrentHashMap.newKeySet();
        Map<Integer, ResourceModel> resourceModels = modelProvider.getObjectModel(registration)
                .getObjectModel(pathIds.getObjectId()).resources;
        resourceModels.forEach((resId, resourceModel) -> {
            if (((ConcurrentHashMap) params).containsKey(String.valueOf(resId))) {
                Object value = ((ConcurrentHashMap) params).get((String.valueOf(resId)));
                resources.add(LwM2mSingleResource.newResource(resId,
                        converter.convertValue(value, equalsResourceTypeGetSimpleName(value), resourceModel.type, pathIds), resourceModel.type));

            }});
        return resources;
    }

    public boolean isValidObjectVersion(String path) {
        LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(path));
        String verSupportedObject = registration.getSupportedObject().get(pathIds.getObjectId());
        String verRez = getVerFromPathIdVerOrId(path);
        return verRez == null ? TRANSPORT_DEFAULT_LWM2M_VERSION.equals(verSupportedObject) : verRez.equals(verSupportedObject);
    }

    /**
     * @param pathIdVer     == "3_1.0"
     * @param modelProvider -
     */
    public void deleteResources(String pathIdVer, LwM2mModelProvider modelProvider) {
        Set<String> key = getKeysEqualsIdVer(pathIdVer);
        key.forEach(pathRez -> {
            LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(pathRez));
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
        LwM2mPath pathIds = new LwM2mPath(convertPathFromIdVerToObjectId(pathRez));
        ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
        this.resources.get(pathRez).setResourceModel(resourceModel);
    }

    private Set<String> getKeysEqualsIdVer(String idVer) {
        return this.resources.keySet()
                .stream()
                .filter(e -> idVer.equals(e.split(LWM2M_SEPARATOR_PATH)[1]))
                .collect(Collectors.toSet());
    }

    public void initReadValue(DefaultLwM2MTransportMsgHandler serviceImpl, String path) {
        if (path != null) {
            this.pendingReadRequests.remove(path);
        }
        if (this.pendingReadRequests.size() == 0) {
            this.init = true;
            serviceImpl.putDelayedUpdateResourcesThingsboard(this);
        }
    }

}

