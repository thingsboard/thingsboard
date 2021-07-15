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
package org.thingsboard.server.transport.lwm2m.client.model;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;

import static org.eclipse.leshan.core.model.ResourceModel.Operations;

public class LwM2MTestObjectModelWithResource {

    public static final int STRING_RESOURCE_ID = 0;
    public static final int BOOLEAN_RESOURCE_ID = 1;
    public static final int INTEGER_RESOURCE_ID = 2;
    public static final int FLOAT_RESOURCE_ID = 3;
    public static final int TIME_RESOURCE_ID = 4;
    public static final int OPAQUE_RESOURCE_ID = 5;
    public static final int OBJLNK_MULTI_INSTANCE_RESOURCE_ID = 6;
    public static final int OBJLNK_SINGLE_INSTANCE_RESOURCE_ID = 7;
    public static final int INTEGER_MANDATORY_RESOURCE_ID = 8;
    public static final int STRING_MANDATORY_RESOURCE_ID = 9;
    public static final int STRING_RESOURCE_INSTANCE_ID = 65010;
    public static final int UNSIGNED_INTEGER_RESOURCE_ID = 11;
    public static final int OPAQUE_MULTI_INSTANCE_RESOURCE_ID = 12;

    private final ResourceModel stringfield;
    private final ResourceModel booleanfield;
    private final ResourceModel integerfield ;
    private final ResourceModel floatfield ;
    private final ResourceModel timefield ;
    private final ResourceModel opaquefield;
    private final ResourceModel objlnkfield;
    private final ResourceModel objlnkSinglefield ;
    private final ResourceModel integermandatoryfield;
    private final ResourceModel stringmandatoryfield ;
    private final ResourceModel multiInstance;
    private final ResourceModel unsignedintegerfield ;
    private final ResourceModel opaqueMultiField ;


    public LwM2MTestObjectModelWithResource(Operations operationsType, String resourceNameSuffix) {
        this.stringfield = new ResourceModel(STRING_RESOURCE_ID, "stringres" + resourceNameSuffix, operationsType, false, false,
                Type.STRING, null, null, null);
        this.booleanfield = new ResourceModel(BOOLEAN_RESOURCE_ID, "booleanres" + resourceNameSuffix, operationsType, false, false,
                Type.BOOLEAN, null, null, null);
        this.integerfield = new ResourceModel(INTEGER_RESOURCE_ID, "integerres" + resourceNameSuffix, operationsType, false, false,
                Type.INTEGER, null, null, null);
        this.floatfield = new ResourceModel(FLOAT_RESOURCE_ID, "floatres" + resourceNameSuffix, operationsType, false, false,
                Type.FLOAT, null, null, null);
        this.timefield = new ResourceModel(TIME_RESOURCE_ID, "timeres" + resourceNameSuffix, operationsType, false, false, Type.TIME,
                null, null, null);
        this.opaquefield = new ResourceModel(OPAQUE_RESOURCE_ID, "opaque" + resourceNameSuffix, operationsType, false, false,
                Type.OPAQUE, null, null, null);
        this.objlnkfield = new ResourceModel(OBJLNK_MULTI_INSTANCE_RESOURCE_ID, "objlnk" + resourceNameSuffix, operationsType, true,
                false, Type.OBJLNK, null, null, null);
        this.objlnkSinglefield = new ResourceModel(OBJLNK_SINGLE_INSTANCE_RESOURCE_ID, "objlnk" + resourceNameSuffix, operationsType,
                false, false, Type.OBJLNK, null, null, null);
        this.integermandatoryfield = new ResourceModel(INTEGER_MANDATORY_RESOURCE_ID, "integermandatory",
                operationsType, false, true, Type.INTEGER, null, null, null);
        this.stringmandatoryfield = new ResourceModel(STRING_MANDATORY_RESOURCE_ID, "stringmandatory",
                operationsType, false, true, Type.STRING, null, null, null);
        this.multiInstance = new ResourceModel(STRING_RESOURCE_INSTANCE_ID, "multiinstance" + resourceNameSuffix, operationsType,
                true, false, Type.STRING, null, null, null);
        this.unsignedintegerfield = new ResourceModel(UNSIGNED_INTEGER_RESOURCE_ID, "unsigned" + resourceNameSuffix, operationsType,
                false, false, Type.UNSIGNED_INTEGER, null, null, null);
        this.opaqueMultiField = new ResourceModel(OPAQUE_MULTI_INSTANCE_RESOURCE_ID, "opaque_multi",
                operationsType, true, false, Type.OPAQUE, null, null, null);

    }
    public ObjectModel getObjectModel(int ObjectId, String objectName, String objectVersion, boolean isMultiple) {
        return new ObjectModel(ObjectId, objectName, null, objectVersion, isMultiple, false,
                stringfield, booleanfield, integerfield, floatfield, timefield, opaquefield, objlnkfield,
                objlnkSinglefield, integermandatoryfield, stringmandatoryfield, multiInstance, unsignedintegerfield,
                opaqueMultiField);
    }
}
