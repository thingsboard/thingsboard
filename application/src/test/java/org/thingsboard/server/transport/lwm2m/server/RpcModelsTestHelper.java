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
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.StringUtils;
import org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.leshan.core.model.ResourceModel.Operations.R;
import static org.eclipse.leshan.core.model.ResourceModel.Operations.RW;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;

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
    }


}
