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
    public static final int OBJLNK_RESOURCE_ID = 5;
    public static final int OPAQUE_RESOURCE_ID = 6;
    public static final int UNSIGNED_INTEGER_RESOURCE_ID = 7;
    public static final int INTEGER_MANDATORY_RESOURCE_ID = 8;
    public static final int STRING_MANDATORY_RESOURCE_ID = 9;
    public static final int STRING_MULTI_INSTANCE_RESOURCE_ID = 10;
    public static final int OBJLNK_MULTI_INSTANCE_RESOURCE_ID = 11;
    public static final int UNSIGNED_MULTI_INSTANCE_RESOURCE_ID = 12;
    public static final int OPAQUE_MULTI_INSTANCE_RESOURCE_ID = 65010;

    private final ResourceModel stringField;
    public static final String stringNamePrefix = "string_res";
    private final ResourceModel booleanField;
    public static final String booleanNamePrefix = "boolean_res";
    private final ResourceModel integerField;
    public static final String integerNamePrefix = "integer_res";
    private final ResourceModel floatField;
    public static final String floatNamePrefix = "float_res";
    private final ResourceModel timeField;
    public static final String timeNamePrefix = "time_res";
    private final ResourceModel opaqueField;
    public static final String opaqueNamePrefix = "opaque_res";
    private final ResourceModel objlnkField;
    public static final String objlnkNamePrefix = "objlnk_res";
    private final ResourceModel unsignedIntegerField;
    public static final String unsignedIntegerNamePrefix = "unsigned";
    private final ResourceModel integerMandatoryField;
    public static final String integerMandatoryPrefix = "integer_mandatory";
    private final ResourceModel stringMandatoryField;
    public static final String stringMandatoryPrefix = "string_mandatory";
    private final ResourceModel stringMultiField;
    public static final String stringMultiNamePrefix = "string_multi";
    private final ResourceModel objlnkMultiField;
    public static final String  objlnkMultiNamePrefix = "objlnk_multi";
    private final ResourceModel unsignedMultiField;
    public static final String unsignedMultiNamePrefix = "unsigned_multi";
    private final ResourceModel opaqueMultiField;
    public static final String opaqueMultiNamePrefix = "opaque_multi";

    public LwM2MTestObjectModelWithResource(Operations operationsType, String resourceNameSuffix) {
        this.stringField = new ResourceModel(STRING_RESOURCE_ID, stringNamePrefix + resourceNameSuffix, operationsType, false, false,
                Type.STRING, null, null, null);
        this.booleanField = new ResourceModel(BOOLEAN_RESOURCE_ID, booleanNamePrefix + resourceNameSuffix, operationsType, false, false,
                Type.BOOLEAN, null, null, null);
        this.integerField = new ResourceModel(INTEGER_RESOURCE_ID, integerNamePrefix + resourceNameSuffix, operationsType, false, false,
                Type.INTEGER, null, null, null);
        this.floatField = new ResourceModel(FLOAT_RESOURCE_ID, floatNamePrefix + resourceNameSuffix, operationsType, false, false,
                Type.FLOAT, null, null, null);
        this.timeField = new ResourceModel(TIME_RESOURCE_ID, timeNamePrefix + resourceNameSuffix, operationsType, false, false, Type.TIME,
                null, null, null);
        this.opaqueField = new ResourceModel(OPAQUE_RESOURCE_ID, opaqueNamePrefix + resourceNameSuffix, operationsType, false, false,
                Type.OPAQUE, null, null, null);
        this.objlnkField = new ResourceModel(OBJLNK_RESOURCE_ID, objlnkNamePrefix + resourceNameSuffix, operationsType,
                false, false, Type.OBJLNK, null, null, null);
        this.unsignedIntegerField = new ResourceModel(UNSIGNED_INTEGER_RESOURCE_ID, unsignedIntegerNamePrefix + resourceNameSuffix, operationsType,
                false, false, Type.UNSIGNED_INTEGER, null, null, null);
        this.integerMandatoryField = new ResourceModel(INTEGER_MANDATORY_RESOURCE_ID, integerMandatoryPrefix,
                operationsType, false, true, Type.INTEGER, null, null, null);
        this.stringMandatoryField = new ResourceModel(STRING_MANDATORY_RESOURCE_ID, stringMandatoryPrefix,
                operationsType, false, true, Type.STRING, null, null, null);
        this.stringMultiField = new ResourceModel(STRING_MULTI_INSTANCE_RESOURCE_ID, stringMultiNamePrefix + resourceNameSuffix, operationsType,
                true, false, Type.STRING, null, null, null);
        this.objlnkMultiField = new ResourceModel(OBJLNK_MULTI_INSTANCE_RESOURCE_ID, objlnkMultiNamePrefix + resourceNameSuffix, operationsType, true,
                false, Type.OBJLNK, null, null, null);
        this.unsignedMultiField = new ResourceModel(UNSIGNED_MULTI_INSTANCE_RESOURCE_ID, unsignedMultiNamePrefix + resourceNameSuffix, operationsType, true,
                false, Type.UNSIGNED_INTEGER, null, null, null);
        this.opaqueMultiField = new ResourceModel(OPAQUE_MULTI_INSTANCE_RESOURCE_ID, opaqueMultiNamePrefix,
                operationsType, true, false, Type.OPAQUE, null, null, null);

    }
    public ObjectModel getObjectModel(int ObjectId, String objectName, String objectVersion, boolean isMultiple) {
        return new ObjectModel(ObjectId, objectName, null, objectVersion, isMultiple, false,
                stringField, booleanField, integerField, floatField, timeField, opaqueField, objlnkMultiField,
                objlnkField, integerMandatoryField, stringMandatoryField, stringMultiField, unsignedIntegerField,
                unsignedMultiField,
                opaqueMultiField);
    }
}
