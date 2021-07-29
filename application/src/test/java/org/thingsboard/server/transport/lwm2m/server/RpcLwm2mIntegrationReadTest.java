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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.ObjectLink;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import static org.eclipse.leshan.core.model.ResourceModel.Type.BOOLEAN;
import static org.eclipse.leshan.core.model.ResourceModel.Type.FLOAT;
import static org.eclipse.leshan.core.model.ResourceModel.Type.INTEGER;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OBJLNK;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
import static org.eclipse.leshan.core.model.ResourceModel.Type.TIME;

import static org.eclipse.leshan.core.model.ResourceModel.Type.UNSIGNED_INTEGER;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.BOOLEAN_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.FLOAT_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.INTEGER_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.OBJLNK_MULTI_INSTANCE_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.OBJLNK_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.OPAQUE_MULTI_INSTANCE_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.OPAQUE_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.STRING_MULTI_INSTANCE_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.STRING_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.TIME_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.UNSIGNED_INTEGER_RESOURCE_ID;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.booleanNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.floatNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.integerNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.objlnkMultiNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.objlnkNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.opaqueMultiNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.opaqueNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.stringMultiNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.stringNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.timeNamePrefix;
import static org.thingsboard.server.transport.lwm2m.client.model.LwM2MTestObjectModelWithResource.unsignedIntegerNamePrefix;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.equalsResourceTypeGetSimpleName;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.fromVersionedIdToObjectId;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.TEST_OBJECT_MULTI_WITH_RESOURCE_R_ID;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.TEST_OBJECT_SINGLE_WITH_RESOURCE_RW_ID;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.resourceNameSuffixRm;
import static org.thingsboard.server.transport.lwm2m.server.RpcModelsTestHelper.resourceNameSuffixRws;

public class RpcLwm2mIntegrationReadTest extends RpcAbstractLwM2MIntegrationTest {

    /**
     * Read {"id":"/2001"}
     *
     * @throws Exception
     */
    @Test
    public void testReadObject_Ok() throws Exception {
        String actualResult = sendRead("/2001_1.1");
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mObject"));
    }

    /**
     * Read {"id":"/2000/0/"}
     *
     * @throws Exception
     */
    @Test
    public void testReadObjectInstance_Ok() throws Exception {
        String actualResult = sendRead("/2000/0");
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mObjectInstance"));
    }

    /**
     * Read {"id":"/2003/1/12"}
     *
     * @throws Exception
     */
    @Test
    public void testReadStringMultipleResource_Ok() throws Exception {
        String expectedValue = stringMultiNamePrefix + resourceNameSuffixRm;
        String expectedTypeStr = "type=STRING";
        String resourcePath = "/2003_1.1/0/" + STRING_MULTI_INSTANCE_RESOURCE_ID;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        isMultipleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedTypeStr, STRING, resourcePath);
    }

    /**
     * Read {"id":"/2003/1/12"}
     *
     * @throws Exception
     */
    @Test
    public void testReadObjlnkMultipleResource_Ok() throws Exception {
        int instanceId = 1;
        String resourcePath = "/" + TEST_OBJECT_MULTI_WITH_RESOURCE_R_ID + "_1.1/" + instanceId +"/" + OBJLNK_MULTI_INSTANCE_RESOURCE_ID;
        String expectedValue = new ObjectLink(TEST_OBJECT_MULTI_WITH_RESOURCE_R_ID, instanceId).encodeToString();
        String actualResult = sendRead(resourcePath);
        String expectedTypeStr = "type=STRING";
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mMultipleResource"));
        isMultipleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedTypeStr, OBJLNK, resourcePath);
    }

    /**
     * Read {"id":"/2003/1/12"}
     *
     * @throws Exception
     */
    @Test
    public void testReadOpaqueMultipleResource_Ok() throws Exception {
        String expectedValue = "Default " + opaqueMultiNamePrefix + resourceNameSuffixRm;
        String expectedTypeStr = "type=STRING";
        String resourcePath = "/2003_1.1/1/" + OPAQUE_MULTI_INSTANCE_RESOURCE_ID;
        String actualResult = sendRead("/2003_1.1/1/" + OPAQUE_MULTI_INSTANCE_RESOURCE_ID);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        isMultipleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedTypeStr, OPAQUE, resourcePath);
    }

    /**
     *     Test by type value:
     *     public static final int STRING_RESOURCE_ID = 0;
     *     public static final int BOOLEAN_RESOURCE_ID = 1;
     *     public static final int INTEGER_RESOURCE_ID = 2;
     *     public static final int FLOAT_RESOURCE_ID = 3;
     *     public static final int TIME_RESOURCE_ID = 4;
     *     public static final int OBJLNK_RESOURCE_ID = 5;
     *     public static final int OPAQUE_RESOURCE_ID = 6;
     *     public static final int UNSIGNED_INTEGER_RESOURCE_ID = 7;
     *     public static final int INTEGER_MANDATORY_RESOURCE_ID = 8;
     *     public static final int STRING_MANDATORY_RESOURCE_ID = 9;
     *     public static final int STRING_MULTI_INSTANCE_RESOURCE_ID = 10;
     *     public static final int OBJLNK_MULTI_INSTANCE_RESOURCE_ID = 11;
     *     public static final int UNSIGNED_MULTI_INSTANCE_RESOURCE_ID = 12; // 30/07/2021 -> org.eclipse.leshan.core.node.newInstance => LwM2mNodeException: Type UNSIGNED_INTEGER is not supported
     *     public static final int OPAQUE_MULTI_INSTANCE_RESOURCE_ID = 65010;
     *
     *         private void setResourceValue(Object value, Type type, JsonArrayEntry jsonResource, LwM2mPath resourcePath) {
     *             switch (type) {
     *             case STRING:
     *                 jsonResource.setStringValue((String) value);
     *                 break;
     *             case INTEGER:
     *             case UNSIGNED_INTEGER:
     *             case FLOAT:
     *                 jsonResource.setFloatValue((Number) value);
     *                 break;
     *             case BOOLEAN:
     *                 jsonResource.setBooleanValue((Boolean) value);
     *                 break;
     *             case TIME:
     *                 // Specs device object example page 44, rec 13 is Time
     *                 // represented as float?
     *                 jsonResource.setFloatValue((((Date) value).getTime() / 1000L));
     *                 break;
     *             case OPAQUE:
     *                 jsonResource.setStringValue(Base64.encodeBase64String((byte[]) value));
     *                 break;
     *             case OBJLNK:
     *                 try {
     *                     jsonResource.setStringValue(((ObjectLink) value).encodeToString());
     *                 } catch (IllegalArgumentException e) {
     *                     throw new CodecException(e, "Invalid value [%s] for objectLink resource [%s] ", value,
     *                             resourcePath);
     *                 }
     *                 break;
     *             default:
     *                 throw new CodecException("Invalid value type %s for %s", type, resourcePath);
     *             }
     *         }
     *     }
     */

    /**
     * Read {"id":"/2000/0/0"}
     *
     * @throws Exception
     */
    @Test
    public void testReadStringSingleResource_Ok() throws Exception {
        String expectedValue = stringNamePrefix + resourceNameSuffixRws;
        ResourceModel.Type expectedType = STRING;
        String resourcePath = "/2000/0/" + STRING_RESOURCE_ID;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }

    /**
     * Read {"id":"/2000/0/1"}
     *
     * @throws Exception
     */
    @Test
    public void testReadBooleanSingleResource_Ok() throws Exception {
        String expectedValue = booleanNamePrefix + resourceNameSuffixRws;
        ResourceModel.Type expectedType = BOOLEAN;
        String resourcePath = "/2000/0/" + BOOLEAN_RESOURCE_ID;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }

    /**
     * Read {"id":"/2000/0/2"}
     *
     * @throws Exception
     */
    @Test
    public void testReadIntegerSingleResource_Ok() throws Exception {
        String expectedValue = integerNamePrefix + resourceNameSuffixRws;
        ResourceModel.Type expectedType = INTEGER;
        String resourcePath = "/2000/0/" + INTEGER_RESOURCE_ID;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }

    /**
     * Read {"id":"/2000/0/3"}
     *
     * @throws Exception
     */
    @Test
    public void testReadFloatSingleResource_Ok() throws Exception {
        String expectedValue = floatNamePrefix + resourceNameSuffixRws;
        ResourceModel.Type expectedType = FLOAT;
        String resourcePath = "/2000/0/" + FLOAT_RESOURCE_ID;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }

    /**
     * Read {"id":"/2000/0/4"}
     *
     * @throws Exception
     */
    @Test
    public void testReadTimeSingleResource_Ok() throws Exception {
        String expectedValue = timeNamePrefix + resourceNameSuffixRws;
        ResourceModel.Type expectedType = TIME;
        String resourcePath = "/2000/0/" + TIME_RESOURCE_ID;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }

    /**
     * Read {"id":"/2000/0/5"}
     *
     * @throws Exception
     */
    @Test
    public void testReadObjlnkSingleResource_Ok() throws Exception {
        int instanceId = 0;
        String resourcePath = "/" + TEST_OBJECT_SINGLE_WITH_RESOURCE_RW_ID + "/" + instanceId +"/" + OBJLNK_RESOURCE_ID;
        String expectedValue = new ObjectLink(TEST_OBJECT_SINGLE_WITH_RESOURCE_RW_ID, instanceId).encodeToString();
        ResourceModel.Type expectedType = OBJLNK;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }

    /**
     * Read {"id":"/2000/0/6"}
     *
     * @throws Exception
     */
    @Test
    public void testReadOpaqueSingleResource_Ok() throws Exception {
        String expectedValue = "Default " + opaqueNamePrefix + resourceNameSuffixRws;
        ResourceModel.Type expectedType = OPAQUE;
        String resourcePath = "/2000/0/" + OPAQUE_RESOURCE_ID;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }

    /**
     * Read {"id":"/2000/0/7"}
     *
     * @throws Exception
     */
    @Test
    public void testReadUnsignedIntegerSingleResource_Ok() throws Exception {
        String expectedValue = "Default " + unsignedIntegerNamePrefix + resourceNameSuffixRws;
        ResourceModel.Type expectedType = UNSIGNED_INTEGER;
        String resourcePath = "/2000/0/" + UNSIGNED_INTEGER_RESOURCE_ID;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }


    /**
     * Read {"id":"/2003_1.1/1/10/0"}
     *
     * @throws Exception
     */
    @Test
    public void testReadStringResourceInstance_Ok() throws Exception {
        String expectedValue = stringMultiNamePrefix + resourceNameSuffixRm;
        ResourceModel.Type expectedType = STRING;
        String resourcePath = "/2003_1.1/1/" + STRING_MULTI_INSTANCE_RESOURCE_ID + "/0";
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mResourceInstance"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }

    /**
     * Read {"id":"/2003_1.1/1/11/0"}
     *
     * @throws Exception
     */
    @Test
    public void testReadObjlnkResourceInstance_Ok() throws Exception {
        int instanceId = 1;
        String resourcePath = "/" + TEST_OBJECT_MULTI_WITH_RESOURCE_R_ID + "_1.1/" + instanceId +"/" + OBJLNK_MULTI_INSTANCE_RESOURCE_ID + "/0";
        String expectedValue = new ObjectLink(TEST_OBJECT_MULTI_WITH_RESOURCE_R_ID, instanceId).encodeToString();
        ResourceModel.Type expectedType = OBJLNK;
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mResourceInstance"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
    }

    /**
     * Read {"id":"/2003_1.1/1/12/0"}
     *
     * @throws Exception
     */
//    @Test
//    public void testReadUnsignedIntegerResourceInstance_Ok() throws Exception {
//        String expectedValue = unsignedMultiNamePrefix + resourceNameSuffixRm;
//        ResourceModel.Type expectedType = UNSIGNED_INTEGER;
//        String resourcePath = "/2003_1.1/1/" + UNSIGNED_MULTI_INSTANCE_RESOURCE_ID + "/0";
//        String actualResult = sendRead(resourcePath);
//        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
//        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
//        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mResourceInstance"));
//        isValueTypeResource_Ok(rpcActualResult.get("value").asText(), expectedValue, expectedType, resourcePath);
//    }

    /**
     * Read {"id":"/2003_1.1/1/65010/0"}
     *
     * @throws Exception
     */
    @Test
    public void testReadOpaqueResourceInstance_Ok() throws Exception {
        String expectedValue = "Default " + opaqueMultiNamePrefix + resourceNameSuffixRm;
        String resourcePath = "/2003_1.1/1/" + OPAQUE_MULTI_INSTANCE_RESOURCE_ID + "/0";
        String actualResult = sendRead(resourcePath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mResourceInstance"));
        isSingleResourceValueType_Ok(rpcActualResult.get("value").asText(), expectedValue, OPAQUE, resourcePath);
    }

    private String sendRead(String path) throws Exception {
        String method = "Read";
        String setRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String readValue(String contentValueStr) {
        String searchTextValue = "value=";
        if (contentValueStr.contains(searchTextValue)) {
            int startPos = contentValueStr.indexOf(searchTextValue) + searchTextValue.length();
            if (startPos >= 0) {
                int endPos = contentValueStr.indexOf(",", startPos);
                endPos = endPos > 0 ? endPos : contentValueStr.indexOf("type", startPos) - 1;
                if (endPos > 0) {
                    return contentValueStr.substring(startPos, endPos);
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private String readType(String contentValueStr) {
        String searchTextType = "type=";
        if (contentValueStr.contains(searchTextType)) {
            int startPos = contentValueStr.indexOf(searchTextType) + searchTextType.length();
            if (startPos >= 0) {
                int endPos = contentValueStr.indexOf("]", startPos);
                endPos = endPos > 0 ? endPos : contentValueStr.length();
                return contentValueStr.substring(startPos, endPos);
            }
            return null;
        } else {
            return null;
        }
    }

    private void isSingleResourceValueType_Ok(String contentValueStr, String expectedValue, ResourceModel.Type expectedType, String pathIdVer) {
        String actualType = readType(contentValueStr);
        String actualValue = readValue(contentValueStr);
        if (expectedType.equals(OPAQUE)) {
            assertEquals(STRING.name(), actualType);
        } else if (expectedType.equals(OBJLNK)) {
            assertEquals(STRING.name(), actualType);
        } else if (expectedType.equals(TIME)) {
            assertEquals(FLOAT.name(), actualType);
            actualValue = actualValue.substring(0, actualValue.indexOf("."));
        } else if (expectedType.equals(INTEGER) || expectedType.equals(UNSIGNED_INTEGER)) {
            assertEquals(FLOAT.name(), actualType);
            actualValue = actualValue.substring(0, actualValue.indexOf("."));
        } else {
            assertEquals(expectedType.name(), actualType);
        }
        Object value = LwM2mValueConverterImpl.getInstance().convertValue(actualValue, STRING, expectedType,
                new LwM2mPath(fromVersionedIdToObjectId(pathIdVer)));
        ResourceModel.Type valueType = equalsResourceTypeGetSimpleName(value);
        valueType = (UNSIGNED_INTEGER.equals(expectedType) && INTEGER.equals(valueType)) ? UNSIGNED_INTEGER : valueType;
        assertTrue(expectedType.equals(valueType));
        if (actualValue != null) {
            switch (expectedType) {
                case STRING:
                case OBJLNK:
                    assertEquals(expectedValue, actualValue);
                    break;
                case OPAQUE:
                    assertEquals(expectedValue, new String((byte[]) value));
            }
        }
    }

    private void isMultipleResourceValueType_Ok(String contentValueStr, String expectedValue, String expectedTypeStr, ResourceModel.Type expectedType, String pathIdVer) {
        String actualValue = readValue(contentValueStr);
        Object value = LwM2mValueConverterImpl.getInstance().convertValue(actualValue, STRING, expectedType,
                new LwM2mPath(fromVersionedIdToObjectId(pathIdVer)));
        int countActual = StringUtils.countMatches(contentValueStr, "LwM2mMultipleResource");
        assertEquals (1, countActual);
        countActual = StringUtils.countMatches(contentValueStr, "LwM2mResourceInstance");
        assertEquals (2, countActual);
        countActual = StringUtils.countMatches(contentValueStr, expectedTypeStr);
        assertEquals (3, countActual);
        if (actualValue != null) {
            switch (expectedType) {
                case STRING:
                case OBJLNK:
                    assertEquals(expectedValue, actualValue);
                    break;
                case OPAQUE:
                    assertEquals(expectedValue, new String((byte[]) value));
            }
        }
        countActual = StringUtils.countMatches(contentValueStr, actualValue);
        assertEquals (2, countActual);
    }
}
