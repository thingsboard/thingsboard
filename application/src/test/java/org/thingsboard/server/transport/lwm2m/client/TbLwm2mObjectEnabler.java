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
package org.thingsboard.server.transport.lwm2m.client;

import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.resource.BaseObjectEnabler;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnablerFactory;
import org.eclipse.leshan.client.resource.listener.ResourceListener;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapReadRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TbLwm2mObjectEnabler extends BaseObjectEnabler implements Destroyable, Startable, Stoppable {

    private static Logger LOG = LoggerFactory.getLogger(DummyInstanceEnabler.class);

    protected Map<Integer, LwM2mInstanceEnabler> instances;

    protected LwM2mInstanceEnablerFactory instanceFactory;
    protected ContentFormat defaultContentFormat;

    private LinkFormatHelper tbLinkFormatHelper;
    protected Map<LwM2mPath, LwM2mAttributeSet> lwM2mAttributes;
    public TbLwm2mObjectEnabler(int id, ObjectModel objectModel, Map<Integer, LwM2mInstanceEnabler> instances,
                                LwM2mInstanceEnablerFactory instanceFactory, ContentFormat defaultContentFormat) {
        super(id, objectModel);
        this.instances = new HashMap<>(instances);
        ;
        this.instanceFactory = instanceFactory;
        this.defaultContentFormat = defaultContentFormat;
        for (Entry<Integer, LwM2mInstanceEnabler> entry : this.instances.entrySet()) {
            instances.put(entry.getKey(), entry.getValue());
            listenInstance(entry.getValue(), entry.getKey());
        }
        this.lwM2mAttributes = new HashMap<>();
    }

    public TbLwm2mObjectEnabler(int id, ObjectModel objectModel) {
        super(id, objectModel);
    }

    @Override
    public synchronized List<Integer> getAvailableInstanceIds() {
        List<Integer> ids = new ArrayList<>(instances.keySet());
        Collections.sort(ids);
        return ids;
    }

    @Override
    public synchronized List<Integer> getAvailableResourceIds(int instanceId) {
        LwM2mInstanceEnabler instanceEnabler = instances.get(instanceId);
        if (instanceEnabler != null) {
            return instanceEnabler.getAvailableResourceIds(getObjectModel());
        } else {
            return Collections.emptyList();
        }
    }

    public synchronized void addInstance(int instanceId, LwM2mInstanceEnabler newInstance) {
        instances.put(instanceId, newInstance);
        listenInstance(newInstance, instanceId);
        fireInstancesAdded(instanceId);
    }

    public synchronized LwM2mInstanceEnabler getInstance(int instanceId) {
        return instances.get(instanceId);
    }

    public synchronized LwM2mInstanceEnabler removeInstance(int instanceId) {
        LwM2mInstanceEnabler removedInstance = instances.remove(instanceId);
        if (removedInstance != null) {
            fireInstancesRemoved(removedInstance.getId());
        }
        return removedInstance;
    }

    @Override
    protected CreateResponse doCreate(LwM2mServer server, CreateRequest request) {
        if (!getObjectModel().multiple && instances.size() > 0) {
            return CreateResponse.badRequest("an instance already exist for this single instance object");
        }

        if (request.unknownObjectInstanceId()) {
            // create instance
            LwM2mInstanceEnabler newInstance = createInstance(server, getObjectModel().multiple ? null : 0,
                    request.getResources());

            // add new instance to this object
            instances.put(newInstance.getId(), newInstance);
            listenInstance(newInstance, newInstance.getId());
            fireInstancesAdded(newInstance.getId());

            return CreateResponse
                    .success(new LwM2mPath(request.getPath().getObjectId(), newInstance.getId()).toString());
        } else {
            List<LwM2mObjectInstance> instanceNodes = request.getObjectInstances();

            // checks single object instances
            if (!getObjectModel().multiple) {
                if (request.getObjectInstances().size() > 1) {
                    return CreateResponse.badRequest("can not create several instances on this single instance object");
                }
                if (request.getObjectInstances().get(0).getId() != 0) {
                    return CreateResponse.badRequest("single instance object must use 0 as ID");
                }
            }
            // ensure instance does not already exists
            for (LwM2mObjectInstance instance : instanceNodes) {
                if (instances.containsKey(instance.getId())) {
                    return CreateResponse.badRequest(String.format("instance %d already exists", instance.getId()));
                }
            }

            // create the new instances
            int[] instanceIds = new int[request.getObjectInstances().size()];
            int i = 0;
            for (LwM2mObjectInstance instance : request.getObjectInstances()) {
                // create instance
                LwM2mInstanceEnabler newInstance = createInstance(server, instance.getId(),
                        instance.getResources().values());

                // add new instance to this object
                instances.put(newInstance.getId(), newInstance);
                listenInstance(newInstance, newInstance.getId());

                // store instance ids
                instanceIds[i] = newInstance.getId();
                i++;
            }
            fireInstancesAdded(instanceIds);
            return CreateResponse.success();
        }
    }

    protected LwM2mInstanceEnabler createInstance(LwM2mServer server, Integer instanceId,
                                                  Collection<LwM2mResource> resources) {
        // create the new instance
        LwM2mInstanceEnabler newInstance = instanceFactory.create(getObjectModel(), instanceId, instances.keySet());
        newInstance.setLwM2mClient(getLwm2mClient());

        // add/write resource
        for (LwM2mResource resource : resources) {
            newInstance.write(server, true, resource.getId(), resource);
        }

        return newInstance;
    }

    @Override
    protected ReadResponse doRead(LwM2mServer server, ReadRequest request) {
        LwM2mPath path = request.getPath();

        // Manage Object case
        if (path.isObject()) {
            List<LwM2mObjectInstance> lwM2mObjectInstances = new ArrayList<>();
            for (LwM2mInstanceEnabler instance : instances.values()) {
                ReadResponse response = instance.read(server);
                if (response.isSuccess()) {
                    lwM2mObjectInstances.add((LwM2mObjectInstance) response.getContent());
                }
            }
            return ReadResponse.success(new LwM2mObject(getId(), lwM2mObjectInstances));
        }

        // Manage Instance case
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null)
            return ReadResponse.notFound();

        if (path.getResourceId() == null) {
            return instance.read(server);
        }

        // Manage Resource case
        if (path.getResourceInstanceId() == null) {
            return instance.read(server, path.getResourceId());
        }

        // Manage Resource Instance case
        return instance.read(server, path.getResourceId(), path.getResourceInstanceId());
    }

    @Override
    protected BootstrapReadResponse doRead(LwM2mServer server, BootstrapReadRequest request) {
        // Basic implementation we delegate to classic Read Request
        ReadResponse response = doRead(server,
                new ReadRequest(request.getContentFormat(), request.getPath(), request.getCoapRequest()));
        return new BootstrapReadResponse(response.getCode(), response.getContent(), response.getErrorMessage());
    }

    @Override
    protected ObserveResponse doObserve(final LwM2mServer server, final ObserveRequest request) {
        final LwM2mPath path = request.getPath();

        // Manage Object case
        if (path.isObject()) {
            List<LwM2mObjectInstance> lwM2mObjectInstances = new ArrayList<>();
            for (LwM2mInstanceEnabler instance : instances.values()) {
                ReadResponse response = instance.observe(server);
                if (response.isSuccess()) {
                    lwM2mObjectInstances.add((LwM2mObjectInstance) response.getContent());
                }
            }
            return ObserveResponse.success(new LwM2mObject(getId(), lwM2mObjectInstances));
        }

        // Manage Instance case
        final LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null)
            return ObserveResponse.notFound();

        if (path.getResourceId() == null) {
            return instance.observe(server);
        }

        // Manage Resource case
        if (path.getResourceInstanceId() == null) {
            return instance.observe(server, path.getResourceId());
        }

        // Manage Resource Instance case
        return instance.observe(server, path.getResourceId(), path.getResourceInstanceId());
    }

    @Override
    protected WriteResponse doWrite(LwM2mServer server, WriteRequest request) {
        LwM2mPath path = request.getPath();

        // Manage Instance case
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null)
            return WriteResponse.notFound();

        if (path.isObjectInstance()) {
            return instance.write(server, request.isReplaceRequest(), (LwM2mObjectInstance) request.getNode());
        }

        // Manage Resource case
        if (path.getResourceInstanceId() == null) {
            return instance.write(server, request.isReplaceRequest(), path.getResourceId(),
                    (LwM2mResource) request.getNode());
        }

        // Manage Resource Instance case
        return instance.write(server, false, path.getResourceId(), path.getResourceInstanceId(),
                ((LwM2mResourceInstance) request.getNode()));
    }

    @Override
    protected BootstrapWriteResponse doWrite(LwM2mServer server, BootstrapWriteRequest request) {
        LwM2mPath path = request.getPath();

        // Manage Object case
        if (path.isObject()) {
            for (LwM2mObjectInstance instanceNode : ((LwM2mObject) request.getNode()).getInstances().values()) {
                LwM2mInstanceEnabler instanceEnabler = instances.get(instanceNode.getId());
                if (instanceEnabler == null) {
                    doCreate(server, new CreateRequest(path.getObjectId(), instanceNode));
                } else {
                    doWrite(server, new WriteRequest(Mode.REPLACE, path.getObjectId(), instanceEnabler.getId(),
                            instanceNode.getResources().values()));
                }
            }
            return BootstrapWriteResponse.success();
        }

        // Manage Instance case
        if (path.isObjectInstance()) {
            LwM2mObjectInstance instanceNode = (LwM2mObjectInstance) request.getNode();
            LwM2mInstanceEnabler instanceEnabler = instances.get(path.getObjectInstanceId());
            if (instanceEnabler == null) {
                doCreate(server, new CreateRequest(path.getObjectId(), instanceNode));
            } else {
                doWrite(server, new WriteRequest(Mode.REPLACE, request.getContentFormat(), path.getObjectId(),
                        path.getObjectInstanceId(), instanceNode.getResources().values()));
            }
            return BootstrapWriteResponse.success();
        }

        // Manage resource case
        LwM2mResource resource = (LwM2mResource) request.getNode();
        LwM2mInstanceEnabler instanceEnabler = instances.get(path.getObjectInstanceId());
        if (instanceEnabler == null) {
            doCreate(server, new CreateRequest(path.getObjectId(),
                    new LwM2mObjectInstance(path.getObjectInstanceId(), resource)));
        } else {
            instanceEnabler.write(server, true, path.getResourceId(), resource);
        }
        return BootstrapWriteResponse.success();
    }

    @Override
    protected ExecuteResponse doExecute(LwM2mServer server, ExecuteRequest request) {
        LwM2mPath path = request.getPath();
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        if (instance == null) {
            return ExecuteResponse.notFound();
        }
        return instance.execute(server, path.getResourceId(), request.getArguments());
    }

    @Override
    protected DeleteResponse doDelete(LwM2mServer server, DeleteRequest request) {
        LwM2mInstanceEnabler deletedInstance = instances.remove(request.getPath().getObjectInstanceId());
        if (deletedInstance != null) {
            deletedInstance.onDelete(server);
            fireInstancesRemoved(deletedInstance.getId());
            return DeleteResponse.success();
        }
        return DeleteResponse.notFound();
    }

    @Override
    public BootstrapDeleteResponse doDelete(LwM2mServer server, BootstrapDeleteRequest request) {
        if (request.getPath().isRoot() || request.getPath().isObject()) {
            if (id == LwM2mId.SECURITY) {
                // For security object, we clean everything except bootstrap Server account.

                // Get bootstrap account and store removed instances ids
                Entry<Integer, LwM2mInstanceEnabler> bootstrapServerAccount = null;
                int[] instanceIds = new int[instances.size()];
                int i = 0;
                for (Entry<Integer, LwM2mInstanceEnabler> instance : instances.entrySet()) {
                    if (ServersInfoExtractor.isBootstrapServer(instance.getValue())) {
                        bootstrapServerAccount = instance;
                    } else {
                        // Store instance ids
                        instanceIds[i] = instance.getKey();
                        i++;
                    }
                }
                // Clear everything
                instances.clear();

                // Put bootstrap account again
                if (bootstrapServerAccount != null) {
                    instances.put(bootstrapServerAccount.getKey(), bootstrapServerAccount.getValue());
                }

                fireInstancesRemoved(instanceIds);
                return BootstrapDeleteResponse.success();
            } else if (id == LwM2mId.OSCORE) {
                // For OSCORE object, we clean everything except OSCORE object link to bootstrap Server account.

                // Get bootstrap account
                LwM2mObjectInstance bootstrapInstance = ServersInfoExtractor.getBootstrapSecurityInstance(
                        getLwm2mClient().getObjectTree().getObjectEnabler(LwM2mId.SECURITY));
                // Get OSCORE instance ID associated to it
                Integer bootstrapOscoreInstanceId = bootstrapInstance != null
                        ? ServersInfoExtractor.getOscoreSecurityMode(bootstrapInstance)
                        : null;

                // if bootstrap server use OSCORE,
                // search the OSCORE instance for this ID and store removed instances ids
                if (bootstrapOscoreInstanceId != null) {
                    Entry<Integer, LwM2mInstanceEnabler> bootstrapServerOscore = null;
                    int[] instanceIds = new int[instances.size()];
                    int i = 0;
                    for (Entry<Integer, LwM2mInstanceEnabler> instance : instances.entrySet()) {
                        if (bootstrapOscoreInstanceId.equals(instance.getKey())) {
                            bootstrapServerOscore = instance;
                        } else {
                            // Store instance ids
                            instanceIds[i] = instance.getKey();
                            i++;
                        }
                    }

                    // Clear everything
                    instances.clear();

                    // Put bootstrap OSCORE instance again
                    if (bootstrapServerOscore != null) {
                        instances.put(bootstrapServerOscore.getKey(), bootstrapServerOscore.getValue());
                    }
                    fireInstancesRemoved(instanceIds);
                    return BootstrapDeleteResponse.success();
                }
                // else delete everything.
            }

            // In all other cases, just delete everything
            instances.clear();
            // fired instances removed
            int[] instanceIds = new int[instances.size()];
            int i = 0;
            for (Entry<Integer, LwM2mInstanceEnabler> instance : instances.entrySet()) {
                instanceIds[i] = instance.getKey();
                i++;
            }
            fireInstancesRemoved(instanceIds);

            return BootstrapDeleteResponse.success();
        } else if (request.getPath().isObjectInstance()) {
            if (id == LwM2mId.SECURITY) {
                // For security object, deleting bootstrap Server account is not allowed
                LwM2mInstanceEnabler instance = instances.get(request.getPath().getObjectInstanceId());
                if (instance == null) {
                    return BootstrapDeleteResponse
                            .badRequest(String.format("Instance %s not found", request.getPath()));
                } else if (ServersInfoExtractor.isBootstrapServer(instance)) {
                    return BootstrapDeleteResponse.badRequest("bootstrap server can not be deleted");
                }
            } else if (id == LwM2mId.OSCORE) {
                // For OSCORE object, deleting instance linked to Bootstrap account is not allowed

                // Get bootstrap instance
                LwM2mObjectInstance bootstrapInstance = ServersInfoExtractor.getBootstrapSecurityInstance(
                        getLwm2mClient().getObjectTree().getObjectEnabler(LwM2mId.SECURITY));
                // Get OSCORE instance ID associated to it
                Integer bootstrapOscoreInstanceId = bootstrapInstance != null
                        ? ServersInfoExtractor.getOscoreSecurityMode(bootstrapInstance)
                        : null;

                if (bootstrapOscoreInstanceId != null
                        && bootstrapOscoreInstanceId.equals(request.getPath().getObjectInstanceId())) {
                    return BootstrapDeleteResponse
                            .badRequest("OSCORE instance linked to bootstrap server can not be deleted");
                }
            }
            if (null != instances.remove(request.getPath().getObjectInstanceId())) {
                fireInstancesRemoved(request.getPath().getObjectInstanceId());
                return BootstrapDeleteResponse.success();
            } else {
                return BootstrapDeleteResponse.badRequest(String.format("Instance %s not found", request.getPath()));
            }
        }
        return BootstrapDeleteResponse.badRequest(String.format("unexcepted path %s", request.getPath()));
    }

    protected void listenInstance(LwM2mInstanceEnabler instance, final int instanceId) {
        instance.addResourceListener(new ResourceListener() {
            @Override
            public void resourceChanged(LwM2mPath... paths) {
                for (LwM2mPath path : paths) {
                    if (!isValid(instanceId, path)) {
                        LOG.warn("InstanceEnabler ({}) of object ({}) try to raise a change of {} which seems invalid.",
                                instanceId, getId(), path);
                    }
                }
                fireResourcesChanged(paths);
            }
        });
    }

    protected boolean isValid(int instanceId, LwM2mPath pathToValidate) {
        if (!(pathToValidate.isResource() || pathToValidate.isResourceInstance()))
            return false;

        if (pathToValidate.getObjectId() != getId()) {
            return false;
        }

        if (pathToValidate.getObjectInstanceId() != instanceId) {
            return false;
        }

        return true;
    }

    @Override
    public ContentFormat getDefaultEncodingFormat(DownlinkRequest<?> request) {
        return defaultContentFormat;
    }

    @Override
    public void init(LwM2mClient client, LinkFormatHelper linkFormatHelper) {
        super.init(client, linkFormatHelper);
        this.tbLinkFormatHelper = linkFormatHelper;
        for (LwM2mInstanceEnabler instanceEnabler : instances.values()) {
            instanceEnabler.setLwM2mClient(client);
        }
    }

    @Override
    public void destroy() {
        for (LwM2mInstanceEnabler instanceEnabler : instances.values()) {
            if (instanceEnabler instanceof Destroyable) {
                ((Destroyable) instanceEnabler).destroy();
            } else if (instanceEnabler instanceof Stoppable) {
                ((Stoppable) instanceEnabler).stop();
            }
        }
    }

    @Override
    public void start() {
        for (LwM2mInstanceEnabler instanceEnabler : instances.values()) {
            if (instanceEnabler instanceof Startable) {
                ((Startable) instanceEnabler).start();
            }
        }
    }

    @Override
    public void stop() {
        for (LwM2mInstanceEnabler instanceEnabler : instances.values()) {
            if (instanceEnabler instanceof Stoppable) {
                ((Stoppable) instanceEnabler).stop();
            }
        }
    }

    @Override
    public synchronized WriteAttributesResponse writeAttributes(LwM2mServer server, WriteAttributesRequest request) {
        // execute is not supported for bootstrap
        if (server.isLwm2mBootstrapServer()) {
            return WriteAttributesResponse.methodNotAllowed();
        }
//        return WriteAttributesResponse.internalServerError("not implemented");
        return doWriteAttributes(server, request);
    }

    /**
     *  <NOTIFICATION> Class Attributes
     * - pmin             (def = 0(sec)) Integer Resource/Object Instance/Object Readable Resource
     * - pmax             (def = -- )    Integer Resource/Object Instance/Object Readable Resource
     * - Greater Than  gt (def = -- )    Float   Resource                        Numerical&Readable Resource
     * - Less Than     lt (def = -- )    Float   Resource                        Numerical&Readable Resource
     * - Step          st (def = -- )    Float   Resource                        Numerical&Readable Resource
     */
    public  WriteAttributesResponse doWriteAttributes(LwM2mServer server, WriteAttributesRequest request) {
        LwM2mPath lwM2mPath = request.getPath();
        LwM2mAttributeSet attributeSet = lwM2mAttributes.get(lwM2mPath);
        Map <String, LwM2mAttribute<?>> attributes = new HashMap<>();

        for (LwM2mAttribute attr : request.getAttributes().getLwM2mAttributes()) {
            if (attr.getName().equals("pmax") || attr.getName().equals("pmin")) {
                if (lwM2mPath.isObject() || lwM2mPath.isObjectInstance() || lwM2mPath.isResource()) {
                    attributes.put(attr.getName(), attr);
                } else {
                    return WriteAttributesResponse.badRequest("Attribute " + attr.getName() + " can be used for only Resource/Object Instance/Object.");
                }
            } else if (attr.getName().equals("gt") || attr.getName().equals("lt") || attr.getName().equals("st")) {
                if (lwM2mPath.isResource()) {
                    attributes.put(attr.getName(), attr);
                } else {
                    return WriteAttributesResponse.badRequest("Attribute " + attr.getName() + " can be used for only Resource.");
                }
            }
        }
        if (attributes.size()>0){
            if (attributeSet == null) {
                attributeSet = new LwM2mAttributeSet(attributes.values());
            } else {
                Iterable<LwM2mAttribute<?>> lwM2mAttributeIterable = attributeSet.getLwM2mAttributes();
              Map <String, LwM2mAttribute<?>> attributesOld = new HashMap<>();
                for (LwM2mAttribute<?> attr : lwM2mAttributeIterable) {
                    attributesOld.put(attr.getName(), attr);
                }
                attributesOld.putAll(attributes);
                attributeSet = new LwM2mAttributeSet(attributesOld.values());
            }
            lwM2mAttributes.put(lwM2mPath, attributeSet);
            return WriteAttributesResponse.success();
        }
        return WriteAttributesResponse.internalServerError("not implemented");
    }

    @Override
    public synchronized DiscoverResponse discover(LwM2mServer server, DiscoverRequest request) {

        if (server.isLwm2mBootstrapServer()) {
            // discover is not supported for bootstrap
            return DiscoverResponse.methodNotAllowed();
        }

        if (id == LwM2mId.SECURITY || id == LwM2mId.OSCORE) {
            return DiscoverResponse.notFound();
        }
        return doDiscover(server, request);

    }

    protected DiscoverResponse doDiscover(LwM2mServer server, DiscoverRequest request) {

        LwM2mPath path = request.getPath();
        if (path.isObject()) {
            LwM2mLink[] ObjectLinks = linkUpdateAttributes(this.tbLinkFormatHelper.getObjectDescription(this, null), server);
            return DiscoverResponse.success(ObjectLinks);

        } else if (path.isObjectInstance()) {
            // Manage discover on instance
            if (!getAvailableInstanceIds().contains(path.getObjectInstanceId()))
                return DiscoverResponse.notFound();

            LwM2mLink[] instanceLink =  linkUpdateAttributes(this.tbLinkFormatHelper.getInstanceDescription(this, path.getObjectInstanceId(), null), server);
            return DiscoverResponse.success(instanceLink);

        } else if (path.isResource()) {
            // Manage discover on resource
            if (!getAvailableInstanceIds().contains(path.getObjectInstanceId()))
                return DiscoverResponse.notFound();

            ResourceModel resourceModel = getObjectModel().resources.get(path.getResourceId());
            if (resourceModel == null)
                return DiscoverResponse.notFound();

            if (!getAvailableResourceIds(path.getObjectInstanceId()).contains(path.getResourceId()))
                return DiscoverResponse.notFound();

            LwM2mLink resourceLink  = linkAddAttribute(
                    this.tbLinkFormatHelper.getResourceDescription(this, path.getObjectInstanceId(), path.getResourceId(), null),
                    server);
            return DiscoverResponse.success(new LwM2mLink[] { resourceLink });
        }
        return DiscoverResponse.badRequest(null);
    }

    private LwM2mLink[] linkUpdateAttributes(LwM2mLink[] links, LwM2mServer server) {
        return  Arrays.stream(links)
                .map(link -> linkAddAttribute(link, server))
                .toArray(LwM2mLink[]::new);
    }

    private LwM2mLink linkAddAttribute(LwM2mLink link, LwM2mServer server) {

        LwM2mAttributeSet lwM2mAttributeSetDop = null;
        if (this.lwM2mAttributes.get(link.getPath())!= null){
            lwM2mAttributeSetDop = this.lwM2mAttributes.get(link.getPath());
        }
        LwM2mAttribute resourceAttributeDim = getResourceAttributes (server, link.getPath());

        Map <String, LwM2mAttribute<?>> attributes = new HashMap<>();
        if (link.getAttributes() != null) {
            for (LwM2mAttribute attr : link.getAttributes().getLwM2mAttributes()) {
                attributes.put(attr.getName(), attr);
            }
        }
        if (lwM2mAttributeSetDop != null) {
            for (LwM2mAttribute attr : lwM2mAttributeSetDop.getLwM2mAttributes()) {
                attributes.put(attr.getName(), attr);
            }
        }
        if (resourceAttributeDim != null) {
            attributes.put(resourceAttributeDim.getName(), resourceAttributeDim);
        }
        return new LwM2mLink(link.getRootPath(), link.getPath(), attributes.values());
    }

    protected LwM2mAttribute getResourceAttributes (LwM2mServer server, LwM2mPath path)    {
        ResourceModel resourceModel = getObjectModel().resources.get(path.getResourceId());
        if (path.isResource() && resourceModel.multiple) {
            return getResourceAttributeDim(path, server);
        }
        return null;
    }

    protected LwM2mAttribute getResourceAttributeDim(LwM2mPath path, LwM2mServer server) {
        LwM2mInstanceEnabler instance = instances.get(path.getObjectInstanceId());
        try {
            ReadResponse readResponse = instance.read(server, path.getResourceId());
            if (readResponse.getCode().getCode()==205 && readResponse.getContent() instanceof LwM2mMultipleResource) {
                long valueDim = ((LwM2mMultipleResource)readResponse.getContent()).getInstances().size();
                return LwM2mAttributes.create(LwM2mAttributes.DIMENSION, valueDim);
            } else {
                return null;
            }
        } catch (Exception e ){
            return null;
        }
    }

}

