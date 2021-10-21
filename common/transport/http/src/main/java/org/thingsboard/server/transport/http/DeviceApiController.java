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
package org.thingsboard.server.transport.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToAttributeUpdatesMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToRPCMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Andrew Shvayka
 */
@RestController
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.http.enabled}'=='true')")
@RequestMapping("/api/v1")
@Slf4j
public class DeviceApiController implements TbTransportService {

    private static final String MARKDOWN_CODE_BLOCK_START = "```json\n";
    private static final String MARKDOWN_CODE_BLOCK_END = "\n```";

    private static final String JSON_GET_ALL_SHARED_AND_CLIENT_ATTRIBUTES_EXAMPLE = "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"client\": {\n" +
            "       \"model\": \"SMART WIFI WATER SENSOR\",\n" +
            "       \"description\": {\n" +
            "           \"material\": \"plastic\",\n" +
            "           \"start_temp\": -10,\n" +
            "           \"end_temp\":  40\n" +
            "     },\n" +
            "    \"serial_number\": \"10001KA5785O\"\n" +
            "   },\n" +
            "       \"shared\": {\n" +
            "           \"pulse\": 0.995\n" +
            "     }\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String JSON_GET_ALL_CLIENT_ATTRIBUTES_EXAMPLE = "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"client\": {\n" +
            "       \"model\": \"SMART WIFI WATER SENSOR\",\n" +
            "       \"description\": {\n" +
            "           \"material\": \"plastic\",\n" +
            "           \"start_temp\": -10,\n" +
            "           \"end_temp\":  40\n" +
            "     },\n" +
            "    \"serial_number\": \"10001KA5785O\"\n" +
            "   }\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String JSON_GET_ALL_SHARED_ATTRIBUTES_EXAMPLE = "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "       \"shared\": {\n" +
            "           \"pulse\": 0.995\n" +
            "        }\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String JSON_GET_SHARED_AND_CLIENT_ATTRIBUTES_EXAMPLE = "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"client\": {\n" +
            "       \"description\": {\n" +
            "           \"material\": \"plastic\",\n" +
            "           \"start_temp\": -10,\n" +
            "           \"end_temp\":  40\n" +
            "     }\n" +
            "   },\n" +
            "       \"shared\": {\n" +
            "           \"pulse\": 0.995\n" +
            "     }\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String JSON_DEVICE_ATTRIBUTES_EXAMPLE = "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"serial_number\": \"10001KA5785O\",\n" +
            "    \"model\": \"SMART WIFI WATER SENSOR\",\n" +
            "    \"description\": {\n" +
            "       \"material\": \"plastic\",\n" +
            "       \"start_temp\": -10,\n" +
            "       \"end_temp\":  40\n" +
            "   }\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String JSON_DEFAULT_EXAMPLE = "\n\n For example:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"stringKey\": \"value1\",\n" +
            "    \"booleanKey\": true,\n" +
            "    \"doubleKey\": 42.0,\n" +
            "    \"longKey\": 73,\n" +
            "    \"jsonKey\": {\n" +
            "       \"someNumber\": 42,\n" +
            "       \"someArray\": [1,2,3],\n" +
            "       \"someNestedObject\":  {\"key\": \"value\"},\n" +
            "   }\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n" + " See the real example in description.";

    private static final String RPC_REQUEST_EXAMPLE = "\n\n Example of the RPC request:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"method\": \"setGPIO\",\n" +
            "    \"params\": {\n" +
            "       \"pin\": 4,\n" +
            "       \"value\": 1\n" +
            "   }\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String RPC_RESPONSE_EXAMPLE = "\n\n The RPC response may be any JSON. For example:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"pin\": 4,\n" +
            "    \"value\": 1,\n" +
            "    \"changed\": true\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String JSON_TELEMETRY_EXAMPLE = "\n\n Let's review the example:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "     \"temperature\": 17.5,\n" +
            "     \"water_pulse\": 330.28000000000003,\n" +
            "     \"frequency\": 868500000\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String JSON_ARRAY_TELEMETRY_EXAMPLE = "\n\n" + MARKDOWN_CODE_BLOCK_START +
            " [\n" +
            "   {\n" +
            "       \"temperature\": 17.5,\n" +
            "       \"water_pulse\": 330.28000000000003,\n" +
            "       \"frequency\": 868500000\n" +
            "  },\n" +
            "  {\n" +
            "       \"temperature\": 17.5,\n" +
            "       \"water_pulse\": 330.28000000000003,\n" +
            "       \"frequency\": 868500000\n" +
            "  }\n" +
            " ]\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String JSON_TELEMETRY_EXAMPLE_WITH_TIMESTAMP = "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"ts\": \"1451649600512\",\n" +
            "    \"values\": {\n" +
            "       \"temperature\": 17.5,\n" +
            "       \"water_pulse\": 330.28000000000003,\n" +
            "       \"frequency\": 868500000\n" +
            "   }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String SAVE_ATTRIBUTES_STATUS_OK = "Client-side Attribute from the request was created or updated. ";
    private static final String SAVE_ATTRIBUTES_STATUS_BAD_REQUEST = "Invalid request parameters or body.";
    private static final String UNAUTHORIZED_STATUS_BAD_REQUEST = "Invalid **DEVICE_TOKEN**. Most likely, Device belongs to different Customer or Tenant.";
    private static final String INTERNAL_SERVER_BAD_REQUEST = "The exception was thrown during processing the request.";

    private static final String ACCESS_TOKEN_DESCRIPTION = "The application needs to include deviceToken (token of required device) as a path parameter" +
            " in each HTTP request. If deviceToken is Invalid then will be return error code 401 Unauthorized. For example, 'iDr0uA1uIRRzd32xGrkG'";
    private static final String TIMEOUT_DESCRIPTION = "A long value representing a detected timeout of the RPC delivery in milliseconds. The default value is 0 sec, the minimum value is 5000 (5 sec).";
    private static final String KEY_VALUE_FORMAT_DESCRIPTION = "The platform supports key-value content in JSON. Key is always " +
            "a string, while value can be either string, boolean, double, long or JSON." + JSON_DEFAULT_EXAMPLE;

    private static final String RPC_REQUEST_PARAMETERS = "\n\n**method** -  mandatory, name of the method to distinct the " +
            "RPC calls. For example, “getCurrentTime” or “getWeatherForecast”. The value of the parameter is a string." +
            "\n\n**params** - mandatory, parameters used for processing of the request. The value is a JSON. Leave empty JSON “{}” if no parameters needed." +
            "\n\n**timeout** - optional, value of the processing timeout in milliseconds. The default value is 10000 (10 seconds). The minimum value is 5000 (5 seconds)." +
            "\n\n**expirationTime** - optional, value of the epoch time (in milliseconds, UTC timezone). Overrides timeout if present." +
            "\n\n**persistent** - optional, see [persistent] vs [lightweight] RPC. The default value is \"false\"." +
            "\n\n**additionalInfo** - optional, defines metadata for the persistent RPC that will be added to the [persistent RPC events].";

    private static final String CLAIM_DEVICE = "Please see the corresponding article to get more information about the [Claiming devices](https://thingsboard.io/docs/user-guide/claiming-devices/?claimingscenario=deviceside) feature. " +
            "The Platform User can claim the device if the \"know\" the device Access Token and Secret Key. The Secret Key " +
            "is optional, always has an expiration time, and may also change over time." +
            "\n\n The supported data format is:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"secretKey\": \"value\"\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n" +
            "The message does not contain **durationMs** parameter. Whenever claiming is succeed the device is being assigned to the specific customer." +
            "\n\nIn addition, there is a possibility to reclaim the device, which means the device will be unassigned from the customer. " +
            "You will receive the response like the following one:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"secretKey\": {},\n" +
            "    \"setOrExpired\": true\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String SUBSCRIBE_TO_COMMANDS = "Is created a subscription to [RPC](https://thingsboard.io/docs/user-guide/rpc/#server-side-rpc-api) (remote procedure calls)" +
            "commands based from the server based on device access token and optional parameter 'timeout'. " +
            "Once subscribed, a client may receive rpc request or a timeout message if there are no requests to " +
            "a particular device. An example of RPC request body is shown below:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"id\": \"1\",\n" +
            "    \"method\": \"setGpio\",\n" +
            "    \"params\": {\n" +
            "       \"pin\": \"23\",\n" +
            "       \"value\": 1\n" +
            "   }\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n where " +
            "\n\n**id** - request id, integer request identifier" +
            "\n\n**method** - RPC method name, string" +
            "\n\n**params** - RPC method params, custom json object" +
            "\n\nand can reply to them using POST request (see 'Reply to command')";

    private static final String POST_RPC_REQUEST = "Is created a post request by [RPC(remote procedure calls)](https://thingsboard.io/docs/user-guide/rpc/#server-side-rpc-api) " +
            "commands to the server based on device access token and JSON object. A client " +
            "may receive rpc request or a timeout message and 503 error (Service Unavailable) if there are no requests to a particular device. " +
            "\n\nAn example of RPC request body is shown below:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"method\": \"getCurrentTime\",\n" +
            "    \"params\": {}\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n" +
            "\n\n**method** - name of the method to distinct the RPC calls. For example, “getCurrentTime” or “getWeatherForecast”. The value of the parameter is a string." +
            "\n\n**params** - additional parameters used for processing of the request. The value is a JSON. Leave empty JSON “{}” if no parameters needed." +
            "\n\nThe RPC response may be any number, string or JSON. For example:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"time\": \"2016 11 21 12:54:44.287\"\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String PROVISION_DEVICE = "See the corresponding article to get more information about the [Device provisioning](https://thingsboard.io/docs/user-guide/device-provisioning/) feature." +
            "The device may send a device provisioning request to the Platform. The request should always " +
            "contain a provision key and secret. The request may optionally include the device name and credentials " +
            "generated by the device. If those credentials are absent, the Server will generate an Access Token to be used by the device." +
            "\n\nProvisioning request example:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"deviceName\": \"DEVICE_NAME\",\n" +
            "    \"provisionDeviceKey\": \"YOUR_PROVISION_KEY_HERE\",\n" +
            "    \"provisionDeviceSecret\": \"YOUR_PROVISION_SECRET_HERE\"\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n" +
            "\n\nwhere" +
            "\n\n**DEVICE_NAME** - device name in the Platform" +
            "\n\n**YOUR_PROVISION_KEY_HERE** - provisioning device key, you should take it from configured device profile (\"u7piawkboq8v32dmcmpp\")" +
            "\n\n**OUR_PROVISION_SECRET_HERE** - provisioning device secret, you should take it from configured device profile (\"jpmwdn8ptlswmf4m29bw\"). " +
            "The Platform validates the request and replies with the device provisioning response. The successful " +
            "response contains device id, credentials type, and body. If the validation was not successful, the response will contain only the status." +
            "\n\nProvisioning response example:" +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "  {\n" +
            "    \"provisionDeviceStatus\": \"SUCCESS\",\n" +
            "    \"credentialsType\": \"ACCESS_TOKEN\",\n" +
            "    \"accessToken\": \"sLzc0gDAZPkGMzFVTyUY\"\n" +
            "  }\n" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    @Autowired
    private HttpTransportContext transportContext;

    @ApiOperation(value = "Get device attributes",
            notes = "Request client-side or shared device attributes to the Platform server node based on device access token and list of keys. " +
                    "The intersection of client-side and shared device attribute keys is a bad practice! However, it is " +
                    "still possible to have same keys for client, shared or even server-side attributes.\n" +
                    "\n\n## Let`s see some response examples: \n\n" +
                    "\n\nExample when 'clientKeys' and 'sharedKeys' are not given -  returns all client-side and shared-side attributes of required device: " + JSON_GET_ALL_SHARED_AND_CLIENT_ATTRIBUTES_EXAMPLE +
                    "\n\nExample when selected only 'clientKeys' (e.g. 'model,description,serial_number'): " + JSON_GET_ALL_CLIENT_ATTRIBUTES_EXAMPLE +
                    "\n\nExample when selected only 'sharedKeys' (e.g. 'pulse'): " + JSON_GET_ALL_SHARED_ATTRIBUTES_EXAMPLE +
                    "\n\nExample when selected 'clientKeys' (e.g. 'description') and 'sharedKeys' (e.g. 'pulse'): " + JSON_GET_SHARED_AND_CLIENT_ATTRIBUTES_EXAMPLE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> getDeviceAttributes(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION)
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "A string value representing the comma-separated list of client-side attributes keys. For example, 'model, description'.")
            @RequestParam(value = "clientKeys", required = false, defaultValue = "") String clientKeys,
            @ApiParam(value = "A string value representing the comma-separated list of shared-side attributes keys. For example, 'pulse'.")
            @RequestParam(value = "sharedKeys", required = false, defaultValue = "") String sharedKeys,
            HttpServletRequest httpRequest) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    GetAttributeRequestMsg.Builder request = GetAttributeRequestMsg.newBuilder().setRequestId(0);
                    List<String> clientKeySet = !StringUtils.isEmpty(clientKeys) ? Arrays.asList(clientKeys.split(",")) : null;
                    List<String> sharedKeySet = !StringUtils.isEmpty(sharedKeys) ? Arrays.asList(sharedKeys.split(",")) : null;
                    if (clientKeySet != null) {
                        request.addAllClientAttributeNames(clientKeySet);
                    }
                    if (sharedKeySet != null) {
                        request.addAllSharedAttributeNames(sharedKeySet);
                    }
                    TransportService transportService = transportContext.getTransportService();
                    transportService.registerSyncSession(sessionInfo,
                            new HttpSessionListener(responseWriter, transportContext.getTransportService(), sessionInfo),
                            transportContext.getDefaultTimeout());
                    transportService.process(sessionInfo, request.build(), new SessionCloseOnErrorCallback(transportService, sessionInfo));
                }));
        return responseWriter;
    }

    @ApiOperation(value = "Post device attributes",
            notes = "Created(published) or updated client-side device attributes to the Platform server side based on " +
                    "device token and JSON object with key-value format of attributes to create or update. " +
                    "**Key** is a unique parameter and cannot be overwritten. Only value can be overwritten for the key." +
                    "\n\nWhen creating client-side attributes, the Platform generates attribute id as [time-based UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_1_(date-time_and_MAC_address)." +
                    "\n\n Let`s see example of creating some client-side attributes for the device with **deviceToken ** - 'iDr0uA1uIRRzd32xGrkG' " +
                    "and JSON object:" + JSON_DEVICE_ATTRIBUTES_EXAMPLE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SAVE_ATTRIBUTES_STATUS_OK),
            @ApiResponse(code = 400, message = SAVE_ATTRIBUTES_STATUS_BAD_REQUEST),
            @ApiResponse(code = 401, message = UNAUTHORIZED_STATUS_BAD_REQUEST),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_BAD_REQUEST),
    })
    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postDeviceAttributes(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = KEY_VALUE_FORMAT_DESCRIPTION)
            @RequestBody String json,
            HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, JsonConverter.convertToAttributesProto(new JsonParser().parse(json)),
                            new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    @ApiOperation(value = "Post device attributes",
            notes = "Created(published) or updated telemetry data the Platform server node based on " +
                    "device access token and JSON object with key-value format of attributes to create or update. " +
                    "**Key** is a unique parameter and cannot be overwritten. Only value can be overwritten for the key." +
                    "\n\nWhen creating telemetry, the Platform generates attribute id as [time-based UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_1_(date-time_and_MAC_address)." +
                    "\n\n The simplest supported data formats are:" +
                    JSON_TELEMETRY_EXAMPLE + "\n\n or" + JSON_ARRAY_TELEMETRY_EXAMPLE +
                    "\n\n In this case, the server-side timestamp will be assigned to uploaded data!" +
                    "\n\n In case your device is able to get the client-side timestamp, you can use following format:" +
                    JSON_TELEMETRY_EXAMPLE_WITH_TIMESTAMP +
                    "In the example above, we assume that \"1451649600512\" is a [unix timestamp](https://en.wikipedia.org/wiki/Unix_time) " +
                    "with milliseconds precision. For example, the value ‘1451649600512’ corresponds to ‘Fri, 01 Jan 2016 12:00:00.512 GMT’.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SAVE_ATTRIBUTES_STATUS_OK),
            @ApiResponse(code = 400, message = SAVE_ATTRIBUTES_STATUS_BAD_REQUEST),
            @ApiResponse(code = 401, message = UNAUTHORIZED_STATUS_BAD_REQUEST),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_BAD_REQUEST),
    })
    @RequestMapping(value = "/{deviceToken}/telemetry", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postTelemetry(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = KEY_VALUE_FORMAT_DESCRIPTION, example = "")
            @RequestBody String json,
            HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, JsonConverter.convertToTelemetryProto(new JsonParser().parse(json)),
                            new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    @ApiOperation(value = "Claim device",
            notes = CLAIM_DEVICE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/claim", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> claimDevice(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION)
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = KEY_VALUE_FORMAT_DESCRIPTION)
            @RequestBody(required = false) String json,
            HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
                    transportService.process(sessionInfo, JsonConverter.convertToClaimDeviceProto(deviceId, json),
                            new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    @ApiOperation(value = "Subscribe to commands",
            notes = SUBSCRIBE_TO_COMMANDS,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToCommands(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION)
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = TIMEOUT_DESCRIPTION)
            @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout,
            HttpServletRequest httpRequest) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.registerSyncSession(sessionInfo,
                            new HttpSessionListener(responseWriter, transportContext.getTransportService(), sessionInfo),
                            timeout == 0 ? transportContext.getDefaultTimeout() : timeout);
                    transportService.process(sessionInfo, SubscribeToRPCMsg.getDefaultInstance(),
                            new SessionCloseOnErrorCallback(transportService, sessionInfo));

                }));
        return responseWriter;
    }

    @ApiOperation(value = "Reply to command",
            notes = "Returns a reply to required [RPC](https://thingsboard.io/docs/user-guide/rpc/#server-side-rpc-api) (remote procedure calls) " +
                    "request, which consists of multiple fields: " +
                    RPC_REQUEST_PARAMETERS + RPC_REQUEST_EXAMPLE + RPC_RESPONSE_EXAMPLE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/rpc/{requestId}", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> replyToCommand(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION)
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "An integer request identifier. You may use this identifier to track the state of the command. For example, 'b10bb1a0-0afd-11ec-a08f-1b3182194747'.")
            @PathVariable("requestId") Integer requestId,
            @ApiParam(value = KEY_VALUE_FORMAT_DESCRIPTION)
            @RequestBody String json,
            HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(json).build(), new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    @ApiOperation(value = "Post RPC request",
            notes = POST_RPC_REQUEST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postRpcRequest(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION)
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = KEY_VALUE_FORMAT_DESCRIPTION)
            @RequestBody String json,
            HttpServletRequest httpRequest) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    JsonObject request = new JsonParser().parse(json).getAsJsonObject();
                    TransportService transportService = transportContext.getTransportService();
                    transportService.registerSyncSession(sessionInfo,
                            new HttpSessionListener(responseWriter, transportContext.getTransportService(), sessionInfo),
                            transportContext.getDefaultTimeout());
                    transportService.process(sessionInfo, ToServerRpcRequestMsg.newBuilder().setRequestId(0)
                                    .setMethodName(request.get("method").getAsString())
                                    .setParams(request.get("params").toString()).build(),
                            new SessionCloseOnErrorCallback(transportService, sessionInfo));
                }));
        return responseWriter;
    }

    @ApiOperation(value = "Subscribe to attribute",
            notes = "Is created a subscription to shared device attributes changes based on device token and timeout parameters," +
                    "the last one is optional. Once shared attribute will be changed by one of the server-side components " +
                    "(REST API or Rule Chain) the client will receive the following update:" +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "  {\n" +
                    "    \"pulse\": 0.995\n" +
                    "  }\n" +
                    MARKDOWN_CODE_BLOCK_END + "\n\n" +
                    "In this example, initial pulse value was 0.999, but then it changed, so the subscribed users can get this changing.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/attributes/updates", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToAttributes(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION)
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = TIMEOUT_DESCRIPTION)
            @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout,
            HttpServletRequest httpRequest) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.registerSyncSession(sessionInfo,
                            new HttpSessionListener(responseWriter, transportContext.getTransportService(), sessionInfo),
                            timeout == 0 ? transportContext.getDefaultTimeout() : timeout);
                    transportService.process(sessionInfo, SubscribeToAttributeUpdatesMsg.getDefaultInstance(),
                            new SessionCloseOnErrorCallback(transportService, sessionInfo));

                }));
        return responseWriter;
    }

    @ApiOperation(value = "Get Firmware",
            notes = "See the corresponding article to get more information about the [Firmware](https://thingsboard.io/docs/user-guide/ota-updates/?remoteintegrationdockerinstall=http#overview)." +
                    "\n\nWhen the Platform initiates the firmware update over HTTP it sets the 'fw_title', 'fw_version', 'fw_checksum', " +
                    "'fw_checksum_algorithm' shared attributes. To receive the shared attribute updates, the device has to Get Firmware API call." +
                    "\n\n If all parameters except title and deviceToken are omitted - returns Error 400. " +
                    "\n\n To get the correct result first of all you should create [OTA](https://thingsboard.io/docs/user-guide/ota-updates/?remoteintegrationdockerinstall=http#provision-ota-package-to-thingsboard-repository). " +
                    "Use '/api/otaPackage' and '/api/otaPackage/{otaPackageId}{?checksum,checksumAlgorithm}' API calls from ota-package-controller for this." +
                    "For the example below was created OTA package and added to the device. So now, device has next shared attributes: " +
                    "\n\n**fw_title** - temperature-prod" +
                    "\n\n**fw_version** - 1.1" +
                    "\n\n**fw_tag** - temperature-prod 1.1" +
                    "\n\n**fw_size** - 2421" +
                    "\n\n**fw_checksum** - 679aa02f936d331d8724f38f98c90fcdca00f88dc854bf9b5408ad3939c53c13" +
                    "\n\n**fw_checksum_algorithm** - SHA256" +
                    "\n\n If title, version, size and chunk parameters are correct, then response body returns **\"Download firmware?title=temperature-prod&version=1.1&size=2421&chunk=679aa02f936d331d8724f38f98c90fcdca00f88dc854bf9b5408ad3939c53c13\"**." +
                    "When click on this link, uploaded file of required firmware will be downloaded.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/firmware", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity> getFirmware(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION)
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "A string value representing the firmware title. For example, 'temperature-prod'")
            @RequestParam(value = "title") String title,
            @ApiParam(value = "A string value representing the version of the target firmware. For example, '1.1'")
            @RequestParam(value = "version") String version,
            @ApiParam(value = "An int value representing the size of the firmware file in bytes. For example, '2421'")
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @ApiParam(value = "An int  value representing the attribute that is used to verify integrity of the received file. For example, '679aa02f936d331d8724f38f98c90fcdca00f88dc854bf9b5408ad3939c53c13'")
            @RequestParam(value = "chunk", required = false, defaultValue = "0") int chunk) {
        return getOtaPackageCallback(deviceToken, title, version, size, chunk, OtaPackageType.FIRMWARE);
    }

    @ApiOperation(value = "Get Software",
            notes = "See the corresponding article to get more information about the [Software](https://thingsboard.io/docs/user-guide/ota-updates/?remoteintegrationdockerinstall=http#overview)." +
                    "\n\nWhen the Platform initiates the software update over HTTP it sets the 'sw_title', 'sw_version', 'sw_checksum', " +
                    "'sw_checksum_algorithm' shared attributes. To receive the shared attribute updates, the device has to Get Software API call." +
                    "\n\n If all parameters except title and deviceToken are omitted - returns Error 400. " +
                    "\n\n To get the correct result first of all you should create [OTA](https://thingsboard.io/docs/user-guide/ota-updates/?remoteintegrationdockerinstall=http#provision-ota-package-to-thingsboard-repository). " +
                    "Use '/api/otaPackage' and '/api/otaPackage/{otaPackageId}{?checksum,checksumAlgorithm}' API calls from ota-package-controller for this." +
                    "For the example below was created OTA package and added to the device. So now, device has next shared attributes: " +
                    "\n\n**sw_title** - temp-prod" +
                    "\n\n**sw_version** - 1.2" +
                    "\n\n**sw_tag** - temp-prod 1.2" +
                    "\n\n**sw_size** - 3069" +
                    "\n\n**sw_checksum** - 679aa02f936d331d8724f38f98c90fcdca00f88dc854bf9b5408ad3939c53c13" +
                    "\n\n**sw_checksum_algorithm** - SHA256" +
                    "\n\n If title, version, size and chunk parameters are correct, then response body returns **\"Download software?title=temp-prod&version=1.2&size=3069&chunk=679aa02f936d331d8724f38f98c90fcdca00f88dc854bf9b5408ad3939c53c13\"**." +
                    "When click on this link, uploaded file of required firmware will be downloaded.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/software", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity> getSoftware(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION)
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "A string value representing the software title. For example, 'temperature-prod'")
            @RequestParam(value = "title") String title,
            @ApiParam(value = "A string value representing the version of the target software. For example, '1.1'")
            @RequestParam(value = "version") String version,
            @ApiParam(value = "An int value representing the size of the software file in bytes. For example, '2421'")
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @ApiParam(value = "An int  value representing the attribute that is used to verify integrity of the received file. For example, '679aa02f936d331d8724f38f98c90fcdca00f88dc854bf9b5408ad3939c53c13'")
            @RequestParam(value = "chunk", required = false, defaultValue = "0") int chunk) {
        return getOtaPackageCallback(deviceToken, title, version, size, chunk, OtaPackageType.SOFTWARE);
    }

    @ApiOperation(value = "Provision device",
            notes = PROVISION_DEVICE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/provision", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> provisionDevice(
            @ApiParam(value = KEY_VALUE_FORMAT_DESCRIPTION)
            @RequestBody String json,
            HttpServletRequest httpRequest) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(JsonConverter.convertToProvisionRequestMsg(json),
                new DeviceProvisionCallback(responseWriter));
        return responseWriter;
    }

    private DeferredResult<ResponseEntity> getOtaPackageCallback(String deviceToken, String title, String version, int size, int chunk, OtaPackageType firmwareType) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportProtos.GetOtaPackageRequestMsg requestMsg = TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                            .setTenantIdMSB(sessionInfo.getTenantIdMSB())
                            .setTenantIdLSB(sessionInfo.getTenantIdLSB())
                            .setDeviceIdMSB(sessionInfo.getDeviceIdMSB())
                            .setDeviceIdLSB(sessionInfo.getDeviceIdLSB())
                            .setType(firmwareType.name()).build();
                    transportContext.getTransportService().process(sessionInfo, requestMsg, new GetOtaPackageCallback(responseWriter, title, version, size, chunk));
                }));
        return responseWriter;
    }

    private static class DeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponse> {
        private final TransportContext transportContext;
        private final DeferredResult<ResponseEntity> responseWriter;
        private final Consumer<SessionInfoProto> onSuccess;

        DeviceAuthCallback(TransportContext transportContext, DeferredResult<ResponseEntity> responseWriter, Consumer<SessionInfoProto> onSuccess) {
            this.transportContext = transportContext;
            this.responseWriter = responseWriter;
            this.onSuccess = onSuccess;
        }

        @Override
        public void onSuccess(ValidateDeviceCredentialsResponse msg) {
            if (msg.hasDeviceInfo()) {
                onSuccess.accept(SessionInfoCreator.create(msg, transportContext, UUID.randomUUID()));
            } else {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
            }
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    private static class DeviceProvisionCallback implements TransportServiceCallback<ProvisionDeviceResponseMsg> {
        private final DeferredResult<ResponseEntity> responseWriter;

        DeviceProvisionCallback(DeferredResult<ResponseEntity> responseWriter) {
            this.responseWriter = responseWriter;
        }

        @Override
        public void onSuccess(ProvisionDeviceResponseMsg msg) {
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    private class GetOtaPackageCallback implements TransportServiceCallback<TransportProtos.GetOtaPackageResponseMsg> {
        private final DeferredResult<ResponseEntity> responseWriter;
        private final String title;
        private final String version;
        private final int chuckSize;
        private final int chuck;

        GetOtaPackageCallback(DeferredResult<ResponseEntity> responseWriter, String title, String version, int chuckSize, int chuck) {
            this.responseWriter = responseWriter;
            this.title = title;
            this.version = version;
            this.chuckSize = chuckSize;
            this.chuck = chuck;
        }

        @Override
        public void onSuccess(TransportProtos.GetOtaPackageResponseMsg otaPackageResponseMsg) {
            if (!TransportProtos.ResponseStatus.SUCCESS.equals(otaPackageResponseMsg.getResponseStatus())) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_FOUND));
            } else if (title.equals(otaPackageResponseMsg.getTitle()) && version.equals(otaPackageResponseMsg.getVersion())) {
                String otaPackageId = new UUID(otaPackageResponseMsg.getOtaPackageIdMSB(), otaPackageResponseMsg.getOtaPackageIdLSB()).toString();
                ByteArrayResource resource = new ByteArrayResource(transportContext.getOtaPackageDataCache().get(otaPackageId, chuckSize, chuck));
                ResponseEntity<ByteArrayResource> response = ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + otaPackageResponseMsg.getFileName())
                        .header("x-filename", otaPackageResponseMsg.getFileName())
                        .contentLength(resource.contentLength())
                        .contentType(parseMediaType(otaPackageResponseMsg.getContentType()))
                        .body(resource);
                responseWriter.setResult(response);
            } else {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            }
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    private static class SessionCloseOnErrorCallback implements TransportServiceCallback<Void> {
        private final TransportService transportService;
        private final SessionInfoProto sessionInfo;

        SessionCloseOnErrorCallback(TransportService transportService, SessionInfoProto sessionInfo) {
            this.transportService = transportService;
            this.sessionInfo = sessionInfo;
        }

        @Override
        public void onSuccess(Void msg) {
        }

        @Override
        public void onError(Throwable e) {
            transportService.deregisterSession(sessionInfo);
        }
    }

    private static class HttpOkCallback implements TransportServiceCallback<Void> {
        private final DeferredResult<ResponseEntity> responseWriter;

        public HttpOkCallback(DeferredResult<ResponseEntity> responseWriter) {
            this.responseWriter = responseWriter;
        }

        @Override
        public void onSuccess(Void msg) {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
        }

        @Override
        public void onError(Throwable e) {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @RequiredArgsConstructor
    private static class HttpSessionListener implements SessionMsgListener {

        private final DeferredResult<ResponseEntity> responseWriter;
        private final TransportService transportService;
        private final SessionInfoProto sessionInfo;

        @Override
        public void onGetAttributesResponse(GetAttributeResponseMsg msg) {
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        @Override
        public void onAttributeUpdate(UUID sessionId, AttributeUpdateNotificationMsg msg) {
            log.trace("[{}] Received attributes update notification to device", sessionId);
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        @Override
        public void onRemoteSessionCloseCommand(UUID sessionId, SessionCloseNotificationProto sessionCloseNotification) {
            log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT));
        }

        @Override
        public void onToDeviceRpcRequest(UUID sessionId, ToDeviceRpcRequestMsg msg) {
            log.trace("[{}] Received RPC command to device", sessionId);
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg, true).toString(), HttpStatus.OK));
            transportService.process(sessionInfo, msg, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
        }

        @Override
        public void onToServerRpcResponse(ToServerRpcResponseMsg msg) {
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

    }

    private static MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @Override
    public String getName() {
        return DataConstants.HTTP_TRANSPORT_NAME;
    }

}
