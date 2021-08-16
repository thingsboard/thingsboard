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

    // Ids
    public static final int BINARY_APP_DATA_CONTAINER = 19;
    public static final int TEMPERATURE_SENSOR = 3303;
    protected static final int objectInstanceId_0 = 0;
    protected static final int objectInstanceId_1 = 1;
    protected static final int objectInstanceId_12 = 12;
    protected static final int resourceId_0 = 0;
    protected static final int resourceId_1 = 1;
    protected static final int resourceId_2 = 2;
    protected static final int resourceId_3 = 3;
    protected static final int resourceId_9 = 9;
    protected static final String resourceIdName_9 = "batteryLevel";
    protected static final int resourceId_11 = 11;
    protected static final int resourceId_14 = 14;
    protected static final String resourceIdName_14 = "UtfOffset";

}



