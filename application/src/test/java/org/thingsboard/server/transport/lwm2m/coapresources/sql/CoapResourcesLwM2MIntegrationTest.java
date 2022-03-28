/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.coapresources.sql;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.thingsboard.server.transport.lwm2m.coapresources.AbstractCoapResourcesLwM2MIntegrationTest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import static org.eclipse.californium.core.config.CoapConfig.MAX_RESOURCE_BODY_SIZE;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_13;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_INSTANCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LWM2M_POST_COAP_RESOURCE;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.VALUE_SUB_LWM2M_POST_COAP_RESPONSE;

@Slf4j
public class CoapResourcesLwM2MIntegrationTest extends AbstractCoapResourcesLwM2MIntegrationTest {

    private String dataHexDec = "5b7b226e223a222f31392f302f30222c227664223a22494f55644141433554414141525234414145556541414246486741415252344141455565414141414141414141414141414141414141423948414141525234414141414141414141414141416d557741414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c3064414143394851414176523041414c306441414339485141417652304141464f3553726b414b51695f4143676376305f775f7a4650385038774150427775613378434178743651544f415041472d4e3334424f44643651496a424c427752793370384563496e6731474245614b526741726639474b5168644751646d792d6f4c79537248433853414242666f43383564416c4541672d67487851656f44436b5f71463068503668524d485f71482d6272372d5034492d78366a4476734a38557a7141304f5a5167625a2d78674330706c434150496b675137785f7a355a47714f797366763438416a37454245412d776e35512d6f42524b464642646b3847514c536f555541386736424154696b36776b45514f6f4f51414168487248555141416a78756b415137336f3849634375665f6573767143386741715439484c47305f71463034662d6f66344153456c444c50375f76774f2d7877774b3059492d777a3552656f4152616c4643746c394753795f41534d4149366c4641746b414b7744773259414d386638387065734a42614f797466762d38413737454655492d774434512d6f46524b424642646b3847514c536f45554138736d414154696b36776745514f6f4d514c336e6930494932514175415043786741416878756b414251684776656a7768375036675f45414b55625271304c41384b6541676b4a4138715341434559414c71725178756b415371666e77764567414a644141666f4338305f714630374251435436415055662d6f66346c454164513748375f76417244413737454245412d776a38512d6f425135784643646e374743795f41534541495a784641746b414b5144776c5941424f4b507244414f74737250375f76454f2d78457a416673495f455871413057735251585a66526b433071784641504b4a674145357065734d4130487141454636353848784941534c51414c36416663462d674838346b41672d675434355541412d67482d45304e4936677743542d6f5453455f71456b77662d6f5035746676";


    /**
     * ContentFormat = ContentFormat.CBOR_CODE
     * request.code = POST
     * url = coap://localhost:5685/tbpost/{endpoint}/{lwm2mPath.toString()}
     * url = coap://localhost:5685/tbpost/deviceEndpointCoapResource1//3/0/9
     * payload = (byte[]) value
     * @return response
     * @throws Exception
     */
    @Test
    public void testRequestPostValueInteger_Result_CONTENT() throws Exception {
        String path3_0_9 =  DEVICE + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_9;
        LwM2mPath lwM2mPath = new LwM2mPath(path3_0_9);
        int batteryLevel = RANDOM.nextInt(101);
        String url = this.serverURI + "/" + lwM2MTestClient.getEndpoint() + "/" + lwM2mPath.toString().trim();
        CoapResponse  response = requestPost(url, ByteBuffer.allocate(4).putInt(batteryLevel).array());
        Assert.equals( CoAP.ResponseCode.CONTENT, response.getCode());
        String payload = new String (response.getPayload());
        String expectValueStr = Integer.toString(batteryLevel);
        String actualValueStr = payload.substring(payload.indexOf(VALUE_SUB_LWM2M_POST_COAP_RESPONSE) + VALUE_SUB_LWM2M_POST_COAP_RESPONSE.length());
        Assert.equals(expectValueStr, actualValueStr);
    }

    /**
     * value: Date -> long seconds -> byte[]
     * @throws Exception
     */
    @Test
    public void testRequestPostValueDate_Result_CONTENT() throws Exception {
        String path3_0_13 =  DEVICE + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_13;
        LwM2mPath lwM2mPath = new LwM2mPath(path3_0_13);
        Date expectDate = new Date();
        long currentTime = expectDate.getTime()/1000L;
        String url = this.serverURI + "/" + lwM2MTestClient.getEndpoint() + "/" + lwM2mPath.toString().trim();
        CoapResponse  response = requestPost(url, ByteBuffer.allocate(8).putLong(currentTime).array());
        Assert.equals( CoAP.ResponseCode.CONTENT, response.getCode());
        String payload = new String (response.getPayload());
        String expectValueStr = expectDate.toString();
        String actualValueStr = payload.substring(payload.indexOf(VALUE_SUB_LWM2M_POST_COAP_RESPONSE) + VALUE_SUB_LWM2M_POST_COAP_RESPONSE.length());
        Assert.equals(expectValueStr, actualValueStr);
    }

    @Test
    public void testRequestPostValueString_Result_CONTENT() throws Exception {
        String path3_0_14 =  DEVICE + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14;
        LwM2mPath lwM2mPath = new LwM2mPath(path3_0_14);
        String expectUtcOffset = new SimpleDateFormat("X").format(Calendar.getInstance().getTime());
        String url = this.serverURI + "/" + lwM2MTestClient.getEndpoint() + "/" + lwM2mPath.toString().trim();
        CoapResponse  response = requestPost(url, expectUtcOffset.getBytes());
        Assert.equals( CoAP.ResponseCode.CONTENT, response.getCode());
        String payload = new String (response.getPayload());
        String actualValueStr = payload.substring(payload.indexOf(VALUE_SUB_LWM2M_POST_COAP_RESPONSE) + VALUE_SUB_LWM2M_POST_COAP_RESPONSE.length());
        Assert.equals(expectUtcOffset, actualValueStr);
    }

    @Test
    public void testRequestPostValueOpaqueMultipleResourced_Result_CONTENT() throws Exception {
        String path19_1_0_2 =  BINARY_APP_DATA_CONTAINER + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0+ "/" + RESOURCE_INSTANCE_ID_2;
        LwM2mPath lwM2mPath = new LwM2mPath(path19_1_0_2);
         byte [] valueOpaque = Hex.decodeHex((dataHexDec).toCharArray());
        String url = this.serverURI + "/" + lwM2MTestClient.getEndpoint() + "/" + lwM2mPath.toString().trim();
        CoapResponse  response = requestPost(url, valueOpaque);
        Assert.equals( CoAP.ResponseCode.CONTENT, response.getCode());
        String payload = new String (response.getPayload());
        String expectOpaqueValueStr = valueOpaque.length + " Bytes, " + Hex.encodeHexString(valueOpaque);
        String actualValueStr = payload.substring(payload.indexOf(VALUE_SUB_LWM2M_POST_COAP_RESPONSE) + VALUE_SUB_LWM2M_POST_COAP_RESPONSE.length());
        Assert.isTrue(expectOpaqueValueStr.contains(actualValueStr));
    }

    @Test
    public void testRequestPostBadEndpoint_Result_UNAUTHORIZED() throws Exception {
        String path3_0_9 =  DEVICE + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_9;
        LwM2mPath lwM2mPath = new LwM2mPath(path3_0_9);
        int batteryLevel = RANDOM.nextInt(101);
        String url = this.serverURI + "/" + lwM2MTestClient.getEndpoint() + "_UNAUTHORIZED" + "/" + lwM2mPath.toString().trim();
        CoapResponse  response = requestPost(url, ByteBuffer.allocate(4).putInt(batteryLevel).array());
        Assert.equals( CoAP.ResponseCode.UNAUTHORIZED, response.getCode());
        String payload = new String (response.getPayload());
        Integer actualValue = Integer.parseInt(payload.substring(payload.indexOf(VALUE_SUB_LWM2M_POST_COAP_RESPONSE) + VALUE_SUB_LWM2M_POST_COAP_RESPONSE.length()),16);
        Assert.equals(batteryLevel, actualValue);
    }

    @Test
    public void testRequestPostInvalidPathResource_Result_BAD_REQUEST() throws Exception {
        String path3_0_9 =  DEVICE + "/" + OBJECT_INSTANCE_ID_0;
        LwM2mPath lwM2mPath = new LwM2mPath(path3_0_9);
        int batteryLevel = RANDOM.nextInt(101);
        String url = this.serverURI + "/" + lwM2MTestClient.getEndpoint() + "/" + lwM2mPath.toString().trim();
        CoapResponse  response = requestPost(url, ByteBuffer.allocate(4).putInt(batteryLevel).array());
        Assert.equals( CoAP.ResponseCode.BAD_REQUEST, response.getCode());
        String actualPayload = new String (response.getPayload());
        String expectValue = String.format("Invalid LwM2mPath resource, must be LwM2mResourceInstance or LwM2mSingleResource. Lwm2m coap Post request: [%s, %s, , %d, %d]",
                LWM2M_POST_COAP_RESOURCE, lwM2MTestClient.getEndpoint(), lwM2mPath.getObjectId(), lwM2mPath.getObjectInstanceId());
        Assert.equals(actualPayload,expectValue);
    }

    @Test
    public void testRequestPostInvalidUriPath_Result_BAD_OPTION() throws Exception {
        int batteryLevel = RANDOM.nextInt(101);
        String uriPath = this.serverURI + "/" + lwM2MTestClient.getEndpoint();
        CoapResponse  response = requestPost(uriPath, ByteBuffer.allocate(4).putInt(batteryLevel).array());
        Assert.equals( CoAP.ResponseCode.BAD_OPTION, response.getCode());
        String actualPayload = new String (response.getPayload());
        String expectValue = String.format("Invalid UriPath. Lwm2m coap Post request: [%s, %s]",
                LWM2M_POST_COAP_RESOURCE, lwM2MTestClient.getEndpoint());
        Assert.equals(actualPayload,expectValue);
    }

    public CoapResponse requestPost(String url, Object value) {
        try {
            CoapClient client = new CoapClient(url);
            Request request = new Request(CoAP.Code.POST);
            OptionSet options = new OptionSet();
            options.setContentFormat(ContentFormat.CBOR_CODE);
            options.setBlock2(6, false, 0);
            request.setOptions(options);
            if (value instanceof byte[]) {
                request.setPayload((byte[]) value);
            }
            client.useEarlyNegotiation(1024);
            request.setMaxResourceBodySize(this.lwM2MTestClient.getLeshanClient().coap().getServer().getConfig().get(MAX_RESOURCE_BODY_SIZE));
            return client.advanced(request);
        } catch (ConnectorException | IOException e) {
            log.error("Error occurred while sending request: " + e);
            return null;
        }
    }
}
