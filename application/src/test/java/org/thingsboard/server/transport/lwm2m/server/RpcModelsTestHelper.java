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
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.StringUtils;
import org.eclipse.leshan.core.util.datatype.ULong;
import org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.eclipse.leshan.core.model.ResourceModel.Operations.R;
import static org.eclipse.leshan.core.model.ResourceModel.Operations.RW;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.core.util.datatype.NumberUtil.numberToULong;

@Slf4j
public class RpcModelsTestHelper {

    public static final int TEST_OBJECT_SINGLE_WITH_RESOURCE_RW_ID = 2000;
    public static final String nameRwSingle = "LwM2MTestObjectModelWithResourceRwSingle";
    public static final int TEST_OBJECT_MULTI_WITH_RESOURCE_RW_ID = 2001;
    public static final String nameRwMulti = "LwM2MTestObjectModelWithResourceRwMulti";
    public static final int TEST_OBJECT_SINGLE_WITH_RESOURCE_R_ID = 2002;
    public static final String nameRSingle = "LwM2MTestObjectModelWithResourceRSingle";
    public static final int TEST_OBJECT_MULTI_WITH_RESOURCE_R_ID = 2003;
    public static final String nameRMulti = "LwM2MTestObjectModelWithResourceRMulti";
    public static final String objectVersion1_0 = "1.0";
    public static final String objectVersion1_1 = "1.1";
    public static final String resourceNameSuffixRws = "RWS";
    public static final String resourceNameSuffixRs = "RS";
    public static final String resourceNameSuffixRwm = "RWM";
    public static final String resourceNameSuffixRm = "RM";

    public List<ObjectModel> createObjectModels() {
        Map<Integer, ObjectModel> models = new TreeMap<>();
        LwM2MTestObjectModelWithResource objectModelWithResourceRWSingle = new LwM2MTestObjectModelWithResource(RW, resourceNameSuffixRws);
        ObjectModel objectModelRWSingle = objectModelWithResourceRWSingle.getObjectModel(TEST_OBJECT_SINGLE_WITH_RESOURCE_RW_ID, nameRwSingle,
                objectVersion1_0, false);
        LwM2MTestObjectModelWithResource objectModelWithResourceRWMulti = new LwM2MTestObjectModelWithResource(RW, resourceNameSuffixRwm);
        ObjectModel objectModelRWMulti = objectModelWithResourceRWMulti.getObjectModel(TEST_OBJECT_MULTI_WITH_RESOURCE_RW_ID, nameRwMulti,
                objectVersion1_1, true);
        LwM2MTestObjectModelWithResource objectModelWithResourceRSingle = new LwM2MTestObjectModelWithResource(R, resourceNameSuffixRs);
        ObjectModel objectModelRSingle = objectModelWithResourceRSingle.getObjectModel(TEST_OBJECT_SINGLE_WITH_RESOURCE_R_ID, nameRSingle,
                objectVersion1_0, false);
        LwM2MTestObjectModelWithResource objectModelWithResourceRMulti = new LwM2MTestObjectModelWithResource(R, resourceNameSuffixRm);
        ObjectModel objectModelRMulti = objectModelWithResourceRMulti.getObjectModel(TEST_OBJECT_MULTI_WITH_RESOURCE_R_ID, nameRMulti,
                objectVersion1_1, true);
        models.put(objectModelRWSingle.id, objectModelRWSingle);
        models.put(objectModelRSingle.id, objectModelRSingle);
        models.put(objectModelRWMulti.id, objectModelRWMulti);
        models.put(objectModelRMulti.id, objectModelRMulti);
        return new ArrayList<>(models.values());
    }

    public static class TestDummyInstanceEnabler extends DummyInstanceEnabler {

        public TestDummyInstanceEnabler() {
            super();
        }

        public TestDummyInstanceEnabler(ScheduledExecutorService executorService, Integer id) {
            super(id);
            try {
                executorService.scheduleWithFixedDelay(() ->
                        fireResourcesChange(3), 10000, 10000, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                log.error("[{}]Throwable", e.toString());
                e.printStackTrace();
            }
        }


//        @Override
//        public ExecuteResponse execute(ServerIdentity identity, int resourceid, String params) {
//            if (resourceid == 4) {
//                return ExecuteResponse.success();
//            } else {
//                return super.execute(identity, resourceid, params);
//            }
//        }

        @Override
        protected LwM2mMultipleResource initializeMultipleResource(ObjectModel objectModel, ResourceModel resourceModel) {
            Map<Integer, Object> values = new HashMap<>();
            switch (resourceModel.type) {
                case STRING:
                    values.put(0, createDefaultStringValueFor(objectModel, resourceModel));
                    values.put(1, createDefaultStringValueFor(objectModel, resourceModel));
                    break;
                case BOOLEAN:
                    values.put(0, createDefaultBooleanValueFor(objectModel, resourceModel));
                    values.put(1, createDefaultBooleanValueFor(objectModel, resourceModel));
                    break;
                case INTEGER:
                    values.put(0, createDefaultIntegerValueFor(objectModel, resourceModel));
                    values.put(1, createDefaultIntegerValueFor(objectModel, resourceModel));
                    break;
                case UNSIGNED_INTEGER:
                    values.put(0, createDefaultUnsignedIntegerValueFor(objectModel, resourceModel));
                    values.put(1, createDefaultUnsignedIntegerValueFor(objectModel, resourceModel));
                    break;
                case FLOAT:
                    values.put(0, createDefaultFloatValueFor(objectModel, resourceModel));
                    values.put(1, createDefaultFloatValueFor(objectModel, resourceModel));
                    break;
                case TIME:
                    values.put(0, createDefaultDateValueFor(objectModel, resourceModel));
                    break;
                case OPAQUE:
                    values.put(0, createDefaultOpaqueValueFor(objectModel, resourceModel));
                    values.put(4, createDefaultOpaqueValueFor(objectModel, resourceModel));
                    break;
                case OBJLNK:
                    values.put(0, createDefaultObjectLinkValueFor(objectModel, resourceModel));
                    values.put(11, createDefaultObjectLinkValueFor(objectModel, resourceModel));
                    break;
                default:
                    // this should not happened
                    values = null;
                    break;
            }
            if (values != null)
                return LwM2mMultipleResource.newResource(resourceModel.id, values, resourceModel.type);
            else
                return null;
        }

        @Override
        protected LwM2mSingleResource initializeSingleResource(ObjectModel objectModel, ResourceModel resourceModel) {
            if (initialValues != null) {
                Object initialValue = initialValues.get(resourceModel.id);
                if (initialValue == null)
                    return null;
                return LwM2mSingleResource.newResource(resourceModel.id, initialValue, resourceModel.type);
            } else {
                switch (resourceModel.type) {
                    case STRING:
                        return LwM2mSingleResource.newStringResource(resourceModel.id,
                                createDefaultStringValueFor(objectModel, resourceModel));
                    case BOOLEAN:
                        return LwM2mSingleResource.newBooleanResource(resourceModel.id,
                                createDefaultBooleanValueFor(objectModel, resourceModel));
                    case INTEGER:
                        return LwM2mSingleResource.newIntegerResource(resourceModel.id,
                                createDefaultIntegerValueFor(objectModel, resourceModel));
                    case FLOAT:
                        return LwM2mSingleResource.newFloatResource(resourceModel.id,
                                createDefaultFloatValueFor(objectModel, resourceModel));
                    case TIME:
                        return LwM2mSingleResource.newDateResource(resourceModel.id,
                                createDefaultDateValueFor(objectModel, resourceModel));
                    case OPAQUE:
                        return LwM2mSingleResource.newBinaryResource(resourceModel.id,
                                createDefaultOpaqueValueFor(objectModel, resourceModel));
                    case UNSIGNED_INTEGER:
                        return LwM2mSingleResource.newUnsignedIntegerResource(resourceModel.id,
                                createDefaultUnsignedIntegerValueFor(objectModel, resourceModel));
                    case OBJLNK:
                        return LwM2mSingleResource.newObjectLinkResource(resourceModel.id,
                                createDefaultObjectLinkValueFor(objectModel, resourceModel));
                    default:
                        // this should not happened
                        return null;
                }
            }
        }

        /**
         * longValue >= 0
         */
        protected ULong createDefaultUnsignedIntegerValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
            return numberToULong(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE));
        }

        /**
         * "/2001/0"
         */
        protected ObjectLink createDefaultObjectLinkValueFor(ObjectModel objectModel, ResourceModel resourceModel) {
            return new ObjectLink(objectModel.id, this.id);
        }

    }
}



