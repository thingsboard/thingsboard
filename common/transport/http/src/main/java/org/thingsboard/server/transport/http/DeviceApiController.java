/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOtaPackageResponseMsg;
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

    private static final String MARKDOWN_CODE_BLOCK_START = "\n\n```json\n";
    private static final String MARKDOWN_CODE_BLOCK_END = "\n```\n\n";

    private static final String REQUIRE_ACCESS_TOKEN = "The API call is designed to be used by device firmware and requires device access token ('deviceToken'). " +
            "It is not recommended to use this API call by third-party scripts, rule-engine or platform widgets (use 'Telemetry Controller' instead).\n";

    private static final String ATTRIBUTE_PAYLOAD_EXAMPLE = "{\n" +
            " \"stringKey\":\"value1\", \n" +
            " \"booleanKey\":true, \n" +
            " \"doubleKey\":42.0, \n" +
            " \"longKey\":73, \n" +
            " \"jsonKey\": {\n" +
            "    \"someNumber\": 42,\n" +
            "    \"someArray\": [1,2,3],\n" +
            "    \"someNestedObject\": {\"key\": \"value\"}\n" +
            " }\n" +
            "}";

    protected static final String TS_PAYLOAD = "The request payload is a JSON document with three possible formats:\n\n" +
            "Simple format without timestamp. In such a case, current server time will be used: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            " \"stringKey\":\"value1\", \n" +
            " \"booleanKey\":true, \n" +
            " \"doubleKey\":42.0, \n" +
            " \"longKey\":73, \n" +
            " \"jsonKey\": {\n" +
            "    \"someNumber\": 42,\n" +
            "    \"someArray\": [1,2,3],\n" +
            "    \"someNestedObject\": {\"key\": \"value\"}\n" +
            " }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n Single JSON object with timestamp: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\"ts\":1634712287000,\"values\":{\"temperature\":26, \"humidity\":87}}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n JSON array with timestamps: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "[\n{\"ts\":1634712287000,\"values\":{\"temperature\":26, \"humidity\":87}}, \n{\"ts\":1634712588000,\"values\":{\"temperature\":25, \"humidity\":88}}\n]" +
            MARKDOWN_CODE_BLOCK_END;

    private static final String ACCESS_TOKEN_PARAM_DESCRIPTION = "Your device access token.";

    @Autowired
    private HttpTransportContext transportContext;

    @Operation(summary = "Get attributes (getDeviceAttributes)",
            description = "Returns all attributes that belong to device. "
                    + "Use optional 'clientKeys' and/or 'sharedKeys' parameter to return specific attributes. "
                    + "\n Example of the result: "
                    + MARKDOWN_CODE_BLOCK_START
                    + ATTRIBUTE_PAYLOAD_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + REQUIRE_ACCESS_TOKEN)
    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity> getDeviceAttributes(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @Parameter(description = "Comma separated key names for attribute with client scope", required = true , schema = @Schema(defaultValue = "state"))
            @RequestParam(value = "clientKeys", required = false, defaultValue = "") String clientKeys,
            @Parameter(description = "Comma separated key names for attribute with shared scope", required = true , schema = @Schema(defaultValue = "configuration"))
            @RequestParam(value = "sharedKeys", required = false, defaultValue = "") String sharedKeys) {
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

    @Operation(summary = "Post attributes (postDeviceAttributes)",
            description = "Post client attribute updates on behalf of device. "
                    + "\n Example of the request: "
                    + MARKDOWN_CODE_BLOCK_START
                    + ATTRIBUTE_PAYLOAD_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + REQUIRE_ACCESS_TOKEN)
    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postDeviceAttributes(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true , schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON with attribute key-value pairs. See API call description for example.")
            @RequestBody String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, JsonConverter.convertToAttributesProto(JsonParser.parseString(json)),
                            new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    @Operation(summary = "Post time series data (postTelemetry)",
            description = "Post time series data on behalf of device. "
                    + "\n Example of the request: "
                    + TS_PAYLOAD
                    + REQUIRE_ACCESS_TOKEN)
    @RequestMapping(value = "/{deviceToken}/telemetry", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postTelemetry(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true , schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @RequestBody String json, HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, JsonConverter.convertToTelemetryProto(JsonParser.parseString(json)),
                            new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    @Operation(summary = "Save claiming information (claimDevice)",
            description = "Saves the information required for user to claim the device. " +
                    "See more info about claiming in the corresponding 'Claiming devices' platform documentation."
                    + "\n Example of the request payload: "
                    + MARKDOWN_CODE_BLOCK_START
                    + "{\"secretKey\":\"value\", \"durationMs\":60000}"
                    + MARKDOWN_CODE_BLOCK_END
                    + "Note: both 'secretKey' and 'durationMs' is optional parameters. " +
                    "In case the secretKey is not specified, the empty string as a default value is used. In case the durationMs is not specified, the system parameter device.claim.duration is used.\n\n"
                    + REQUIRE_ACCESS_TOKEN)
    @RequestMapping(value = "/{deviceToken}/claim", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> claimDevice(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true , schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @RequestBody(required = false) String json) {
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

    @Operation(summary = "Subscribe to RPC commands (subscribeToCommands) (Deprecated)",
            description = "Subscribes to RPC commands using http long polling. " +
                    "Deprecated, since long polling is resource and network consuming. " +
                    "Consider using MQTT or CoAP protocol for light-weight real-time updates. \n\n" +
                    REQUIRE_ACCESS_TOKEN)
    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity> subscribeToCommands(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true , schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @Parameter(description = "Optional timeout of the long poll. Typically less then 60 seconds, since limited on the server side.")
            @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout) {
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

    @Operation(summary = "Reply to RPC commands (replyToCommand)",
            description = "Replies to server originated RPC command identified by 'requestId' parameter. The response is arbitrary JSON.\n\n" +
                    REQUIRE_ACCESS_TOKEN)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "RPC reply to command request was sent to Core."),
            @ApiResponse(responseCode = "400", description = "Invalid structure of the request."),
            @ApiResponse(responseCode = "413", description = "Request payload is too large."),
    })
    @RequestMapping(value = "/{deviceToken}/rpc/{requestId}", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> replyToCommand(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true , schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @Parameter(description = "RPC request id from the incoming RPC request", required = true , schema = @Schema(defaultValue = "123"))
            @PathVariable("requestId") Integer requestId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Reply to the RPC request, JSON. For example: {\"status\":\"success\"}", required = true)
            @RequestBody String json, HttpServletRequest httpServletRequest) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    TransportService transportService = transportContext.getTransportService();
                    transportService.process(sessionInfo, ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(json).build(), new HttpOkCallback(responseWriter));
                }));
        return responseWriter;
    }

    @Operation(summary = "Send the RPC command (postRpcRequest)",
            description = "Send the RPC request to server. The request payload is a JSON document that contains 'method' and 'params'. For example:" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\"method\": \"sumOnServer\", \"params\":{\"a\":2, \"b\":2}}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "The response contains arbitrary JSON with the RPC reply. For example: " +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\"result\": 4}" +
                    MARKDOWN_CODE_BLOCK_END +
                    REQUIRE_ACCESS_TOKEN)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "RPC request to server was sent to Rule Engine."),
            @ApiResponse(responseCode = "400", description = "Invalid structure of the request."),
            @ApiResponse(responseCode = "413", description = "Request payload too large."),
    })
    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postRpcRequest(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true , schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The RPC request JSON", required = true)
            @RequestBody String json, HttpServletRequest httpServletRequest) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        transportContext.getTransportService().process(DeviceTransportType.DEFAULT, ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    JsonObject request = JsonParser.parseString(json).getAsJsonObject();
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

    @Operation(summary = "Subscribe to attribute updates (subscribeToAttributes) (Deprecated)",
            description = "Subscribes to client and shared scope attribute updates using http long polling. " +
                    "Deprecated, since long polling is resource and network consuming. " +
                    "Consider using MQTT or CoAP protocol for light-weight real-time updates. \n\n" +
                    REQUIRE_ACCESS_TOKEN)
    @RequestMapping(value = "/{deviceToken}/attributes/updates", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity> subscribeToAttributes(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true , schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @Parameter(description = "Optional timeout of the long poll. Typically less then 60 seconds, since limited on the server side.")
            @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout) {
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

    @Operation(summary = "Get Device Firmware (getFirmware)",
            description = "Downloads the current firmware package." +
                    "When the platform initiates firmware update, " +
                    "it informs the device by updating the 'fw_title', 'fw_version', 'fw_checksum' and 'fw_checksum_algorithm' shared attributes." +
                    "The 'fw_title' and 'fw_version' parameters must be supplied in this request to double-check " +
                    "that the firmware that device is downloading matches the firmware it expects to download. " +
                    "This is important, since the administrator may change the firmware assignment while device is downloading the firmware. \n\n" +
                    "Optional 'chunk' and 'size' parameters may be used to download the firmware in chunks. " +
                    "For example, device may request first 16 KB of firmware using 'chunk'=0 and 'size'=16384. " +
                    "Next 16KB using 'chunk'=1 and 'size'=16384. The last chunk should have less bytes then requested using 'size' parameter. \n\n" +
                    REQUIRE_ACCESS_TOKEN)
    @RequestMapping(value = "/{deviceToken}/firmware", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity> getFirmware(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true , schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @Parameter(description = "Title of the firmware, corresponds to the value of 'fw_title' attribute.", required = true)
            @RequestParam(value = "title") String title,
            @Parameter(description = "Version of the firmware, corresponds to the value of 'fw_version' attribute.", required = true)
            @RequestParam(value = "version") String version,
            @Parameter(description = "Size of the chunk. Optional. Omit to download the entire file without chunks.")
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @Parameter(description = "Index of the chunk. Optional. Omit to download the entire file without chunks.")
            @RequestParam(value = "chunk", required = false, defaultValue = "0") int chunk) {
        return getOtaPackageCallback(deviceToken, title, version, size, chunk, OtaPackageType.FIRMWARE);
    }

    @Operation(summary = "Get Device Software (getSoftware)",
            description = "Downloads the current software package." +
                    "When the platform initiates software update, " +
                    "it informs the device by updating the 'sw_title', 'sw_version', 'sw_checksum' and 'sw_checksum_algorithm' shared attributes." +
                    "The 'sw_title' and 'sw_version' parameters must be supplied in this request to double-check " +
                    "that the software that device is downloading matches the software it expects to download. " +
                    "This is important, since the administrator may change the software assignment while device is downloading the software. \n\n" +
                    "Optional 'chunk' and 'size' parameters may be used to download the software in chunks. " +
                    "For example, device may request first 16 KB of software using 'chunk'=0 and 'size'=16384. " +
                    "Next 16KB using 'chunk'=1 and 'size'=16384. The last chunk should have less bytes then requested using 'size' parameter. \n\n" +
                    REQUIRE_ACCESS_TOKEN)
    @RequestMapping(value = "/{deviceToken}/software", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity> getSoftware(
            @Parameter(description = ACCESS_TOKEN_PARAM_DESCRIPTION, required = true , schema = @Schema(defaultValue = "YOUR_DEVICE_ACCESS_TOKEN"))
            @PathVariable("deviceToken") String deviceToken,
            @Parameter(description = "Title of the software, corresponds to the value of 'sw_title' attribute.", required = true)
            @RequestParam(value = "title") String title,
            @Parameter(description = "Version of the software, corresponds to the value of 'sw_version' attribute.", required = true)
            @RequestParam(value = "version") String version,
            @Parameter(description = "Size of the chunk. Optional. Omit to download the entire file without using  chunks.")
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @Parameter(description = "Index of the chunk. Optional. Omit to download the entire file without using chunks.")
            @RequestParam(value = "chunk", required = false, defaultValue = "0") int chunk) {
        return getOtaPackageCallback(deviceToken, title, version, size, chunk, OtaPackageType.SOFTWARE);
    }

    @Operation(summary = "Provision new device (provisionDevice)",
            description = "Exchange the provision request to the device credentials. " +
                    "See more info about provisioning in the corresponding 'Device provisioning' platform documentation." +
                    "Requires valid JSON request with the following format: " +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"deviceName\": \"NEW_DEVICE_NAME\",\n" +
                    "  \"provisionDeviceKey\": \"u7piawkboq8v32dmcmpp\",\n" +
                    "  \"provisionDeviceSecret\": \"jpmwdn8ptlswmf4m29bw\"\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "Where 'deviceName' is the name of enw or existing device which depends on the provisioning strategy. " +
                    "The 'provisionDeviceKey' and 'provisionDeviceSecret' matches info configured in one of the existing device profiles. " +
                    "The result of the successful call is the JSON object that contains new credentials:" +
                    MARKDOWN_CODE_BLOCK_START + "{\n" +
                    "  \"credentialsType\":\"ACCESS_TOKEN\",\n" +
                    "  \"credentialsValue\":\"DEVICE_ACCESS_TOKEN\",\n" +
                    "  \"status\":\"SUCCESS\"\n" +
                    "}" + MARKDOWN_CODE_BLOCK_END)
    @RequestMapping(value = "/provision", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> provisionDevice(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON with provision request. See API call description for example.")
            @RequestBody String json) {
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
                    transportContext.getTransportService().process(sessionInfo, requestMsg, new GetOtaPackageCallback(transportContext, responseWriter, title, version, size, chunk));
                }));
        return responseWriter;
    }

    @RequiredArgsConstructor
    static class DeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponse> {
        private final TransportContext transportContext;
        private final DeferredResult<ResponseEntity> responseWriter;
        private final Consumer<SessionInfoProto> onSuccess;

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
            String body = null;
            if (e instanceof HttpMessageNotReadableException || e instanceof JsonParseException) {
                body = e.getMessage();
                log.debug("Failed to process request in DeviceAuthCallback: {}", body);
            } else {
                log.warn("Failed to process request in DeviceAuthCallback", e);
            }
            responseWriter.setResult(new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @RequiredArgsConstructor
    static class DeviceProvisionCallback implements TransportServiceCallback<ProvisionDeviceResponseMsg> {
        private final DeferredResult<ResponseEntity> responseWriter;

        @Override
        public void onSuccess(ProvisionDeviceResponseMsg msg) {
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        @Override
        public void onError(Throwable e) {
            String body = null;
            if (e instanceof HttpMessageNotReadableException || e instanceof JsonParseException) {
                body = e.getMessage();
                log.debug("Failed to process request in DeviceProvisionCallback: {}", body);
            } else {
                log.warn("Failed to process request in DeviceProvisionCallback", e);
            }
            responseWriter.setResult(new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @RequiredArgsConstructor
    static class GetOtaPackageCallback implements TransportServiceCallback<GetOtaPackageResponseMsg> {
        private final TransportContext transportContext;
        private final DeferredResult<ResponseEntity> responseWriter;
        private final String title;
        private final String version;
        private final int chunkSize;
        private final int chunk;

        @Override
        public void onSuccess(TransportProtos.GetOtaPackageResponseMsg otaPackageResponseMsg) {
            if (!TransportProtos.ResponseStatus.SUCCESS.equals(otaPackageResponseMsg.getResponseStatus())) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_FOUND));
            } else if (title.equals(otaPackageResponseMsg.getTitle()) && version.equals(otaPackageResponseMsg.getVersion())) {
                String otaPackageId = new UUID(otaPackageResponseMsg.getOtaPackageIdMSB(), otaPackageResponseMsg.getOtaPackageIdLSB()).toString();
                ByteArrayResource resource = new ByteArrayResource(transportContext.getOtaPackageDataCache().get(otaPackageId, chunkSize, chunk));
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
            String body = null;
            if (e instanceof HttpMessageNotReadableException || e instanceof JsonParseException) {
                body = e.getMessage();
                log.debug("Failed to process request in GetOtaPackageCallback: {}", body);
            } else {
                log.warn("Failed to process request in GetOtaPackageCallback", e);
            }
            responseWriter.setResult(new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR));
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

        @Override
        public void onDeviceDeleted(DeviceId deviceId) {
            UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
            log.trace("[{}] Received device deleted notification for device with id: {}",sessionId, deviceId);
            responseWriter.setResult(new ResponseEntity<>("Device was deleted!", HttpStatus.FORBIDDEN));
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
