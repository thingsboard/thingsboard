package org.thingsboard.server.transport.lwm2m.client;

import org.eclipse.leshan.client.resource.BaseInstanceEnablerFactory;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TbObjectsInitializer extends ObjectsInitializer {


    public TbObjectsInitializer(LwM2mModel model) {
        super(model);
    }

    public List<LwM2mObjectEnabler> create(int... objectId) {
        List<LwM2mObjectEnabler> enablers = new ArrayList<>();
        for (int anObjectId : objectId) {
            LwM2mObjectEnabler objectEnabler = create(anObjectId);
            if (objectEnabler != null)
                enablers.add(objectEnabler);
        }
        return enablers;
    }

    public LwM2mObjectEnabler create(int objectId) {
        ObjectModel objectModel = model.getObjectModel(objectId);
        if (objectModel == null) {
            throw new IllegalArgumentException(
                    "Cannot create object for id " + objectId + " because no model is defined for this id.");
        }
        return createNodeEnabler(objectModel);
    }

    protected LwM2mObjectEnabler createNodeEnabler(ObjectModel objectModel) {
        Map<Integer, LwM2mInstanceEnabler> instances = new HashMap<>();
        LwM2mInstanceEnabler[] newInstances = createInstances(objectModel);
        for (LwM2mInstanceEnabler instance : newInstances) {
            // set id if not already set
            if (instance.getId() == null) {
                int id = BaseInstanceEnablerFactory.generateNewInstanceId(instances.keySet());
                instance.setId(id);
            }
            instance.setModel(objectModel);
            instances.put(instance.getId(), instance);
        }
        return new TbLwm2mObjectEnabler(objectModel.id, objectModel, instances, getFactoryFor(objectModel),
                getContentFormat(objectModel.id));
    }
}
