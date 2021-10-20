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
            MARKDOWN_CODE_BLOCK_END + "\n\n" + " See the real example in description.\"";

    private static final String JSON_EXAMPLE = "\n\n Let`s see example:" +
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
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    private static final String SAVE_ATTRIBUTES_STATUS_OK = "Client-side Attribute from the request was created or updated. ";
    private static final String SAVE_ATTRIBUTES_STATUS_BAD_REQUEST = "Invalid request parameters or body.";
    private static final String UNAUTHORIZED_STATUS_BAD_REQUEST = "Invalid **DEVICE_TOKEN**. Most likely, Device belongs to different Customer or Tenant.";

    private static final String ACCESS_TOKEN_DESCRIPTION = "The application needs to include deviceToken (token of required device) as a path parameter" +
            " in each HTTP request. If deviceToken is Invalid then will be return error code 401 Unauthorized. For example, 'iDr0uA1uIRRzd32xGrkG'";

    private static final String KEY_VALUE_FORMAT_DESCRIPTION = "The platform supports key-value content in JSON. Key is always " +
            "a string, while value can be either string, boolean, double, long or JSON." + JSON_DEFAULT_EXAMPLE;


    @Autowired
    private HttpTransportContext transportContext;

    @ApiOperation(value = "Get device attributes",
            notes =

                    "\n\n## Let`s see some response examples: \n\n" +
                    "\n\nExample when 'clientKeys' and 'sharedKeys' are not given returns both of them " + JSON_GET_ALL_SHARED_AND_CLIENT_ATTRIBUTES_EXAMPLE +
                    "\n\nExample when selected only 'clientKeys' (e.g. 'model,description,serial_number')" + JSON_GET_ALL_CLIENT_ATTRIBUTES_EXAMPLE +
                    "\n\nExample when selected only 'sharedKeys' (e.g. 'pulse')" + JSON_GET_ALL_SHARED_ATTRIBUTES_EXAMPLE +
                    "\n\nExample when selected 'clientKeys' (e.g. 'description') and 'sharedKeys' (e.g. 'pulse')." + JSON_GET_SHARED_AND_CLIENT_ATTRIBUTES_EXAMPLE

            ,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> getDeviceAttributes(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = "A string value representing the comma-separated list of client-side attributes keys.", example = "")
            @RequestParam(value = "clientKeys", required = false, defaultValue = "") String clientKeys,
            @ApiParam(value = "A string value representing the comma-separated list of shared-side attributes keys.", example = "pulse")
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
                    "device token and a JSON object with key-value format of attributes to create or update. " +
                    "**Key** is a unique parameter and cannot be overwritten. Only value can be overwritten for the key." +
                    "\n\nWhen creating client-side attributes, Platform generates attribute id as [time-based UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_1_(date-time_and_MAC_address)." +
                    "\n\n Let`s see example of creating some client-side attributes for the device with **deviceToken ** - 'iDr0uA1uIRRzd32xGrkG' " +
                    "and JSON object:" + JSON_DEVICE_ATTRIBUTES_EXAMPLE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SAVE_ATTRIBUTES_STATUS_OK),
            @ApiResponse(code = 400, message = SAVE_ATTRIBUTES_STATUS_BAD_REQUEST),
            @ApiResponse(code = 401, message = UNAUTHORIZED_STATUS_BAD_REQUEST),
            @ApiResponse(code = 500, message = "The exception was thrown during processing the request. "),
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

    @RequestMapping(value = "/{deviceToken}/claim", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> claimDevice(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
            @RequestBody(required = false) String json, HttpServletRequest request) {
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

    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToCommands(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
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

    @RequestMapping(value = "/{deviceToken}/rpc/{requestId}", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> replyToCommand(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
            @PathVariable("requestId") Integer requestId,
            @RequestBody String json, HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(json).build(), new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postRpcRequest(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
            @ApiParam(value = KEY_VALUE_FORMAT_DESCRIPTION, example = "")
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
            notes = "Is created a subscription to shared device attribute changes based on device token and timeout parameters," +
                    "the last one is optional. Once shared attribute will be changed by one of the server-side components " +
                    "(REST API or Rule Chain) the client will receive the following update:" +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "  {\n" +
                    "    \"pulse\": 0.995\n" +
                    "  }\n" +
                    MARKDOWN_CODE_BLOCK_END + "\n\n" +
                    "In this example, initial value of pulse was 0.999, but then it changed, so the subscribed users can get this changing."
            ,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{deviceToken}/attributes/updates", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToAttributes(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
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

    @RequestMapping(value = "/{deviceToken}/firmware", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity> getFirmware(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
            @RequestParam(value = "title") String title,
            @RequestParam(value = "version") String version,
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @RequestParam(value = "chunk", required = false, defaultValue = "0") int chunk) {
        return getOtaPackageCallback(deviceToken, title, version, size, chunk, OtaPackageType.FIRMWARE);
    }

    @RequestMapping(value = "/{deviceToken}/software", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity> getSoftware(
            @ApiParam(value = ACCESS_TOKEN_DESCRIPTION, example = "iDr0uA1uIRRzd32xGrkG")
            @PathVariable("deviceToken") String deviceToken,
            @RequestParam(value = "title") String title,
            @RequestParam(value = "version") String version,
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @RequestParam(value = "chunk", required = false, defaultValue = "0") int chunk) {
        return getOtaPackageCallback(deviceToken, title, version, size, chunk, OtaPackageType.SOFTWARE);
    }

    @RequestMapping(value = "/provision", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> provisionDevice(@RequestBody String json, HttpServletRequest httpRequest) {
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
