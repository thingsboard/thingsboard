// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.coap;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.ConvertUtil;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.common.data.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.californium.elements.DtlsEndpointContext.KEY_SESSION_ID;

@Slf4j
public class CoapIntegration extends AbstractIntegration<CoapIntegrationMsg> {

    private final CoapServerService coapServerService;

    private CoapResource integrationResource;

    private CoapResource dtlsIntegrationResource;

    public CoapIntegration(CoapServerService coapServerService) {
        this.coapServerService = coapServerService;
    }

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        try {
            CoapClientConfiguration coapClientConfiguration = getClientConfiguration(configuration, CoapClientConfiguration.class);
            setupConfiguration(coapClientConfiguration);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize CoAP integration due to: ", e);
        }
    }

    @Override
    public void destroy() {
        if (this.integrationResource != null) {
            this.integrationResource.delete();
        }
        if (this.dtlsIntegrationResource != null) {
            this.dtlsIntegrationResource.delete();
        }
    }

    @Override
    public void process(CoapIntegrationMsg msg) {
        CoapExchange exchange = msg.getExchange();
        Request request = exchange.advanced().getRequest();
        var dtlsSessionId = request.getSourceContext().get(KEY_SESSION_ID);
        if (this.dtlsIntegrationResource == null && dtlsSessionId != null && !dtlsSessionId.isEmpty()) {
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "No secure connection is not allowed");
            return;
        }
        CoAP.ResponseCode status = CoAP.ResponseCode.CREATED;
        Exception exception = null;
        try {
            List<UplinkData> uplinkDataList = getUplinkDataList(context, msg);
            processUplinkData(context, uplinkDataList);
        } catch (Exception e) {
            log.error("Failed to apply data converter function: {}", e.getMessage(), e);
            status = CoAP.ResponseCode.BAD_REQUEST;
            exception = e;
        }
        exchange.respond(status);
        boolean isOk = status.equals(CoAP.ResponseCode.CREATED);
        if (isOk) {
            integrationStatistics.incMessagesProcessed();
        } else {
            integrationStatistics.incErrorsOccurred();
        }
        persistDebug(context, "Uplink", msg.getContentType(),
                () -> ConvertUtil.toDebugMessage(msg.getContentType(), msg.getPayloadBytes()), isOk ? "OK" : "ERROR", exception);
    }

    protected void doValidateConfiguration(JsonNode configuration, boolean allowLocalNetworkHosts) {
        CoapClientConfiguration coapClientConfiguration;
        try {
            coapClientConfiguration = getClientConfiguration(configuration.get("clientConfiguration"), CoapClientConfiguration.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid CoAP Integration configuration structure!");
        }
        CoapSecurityMode securityMode = coapClientConfiguration.getSecurityMode();
        switch (securityMode) {
            case MIXED:
                checkDtlsEnabled();
                validateUri(allowLocalNetworkHosts, coapClientConfiguration.getCoapEndpoint());
                validateUri(allowLocalNetworkHosts, coapClientConfiguration.getDtlsCoapEndpoint());
                break;
            case NO_SECURE:
                validateUri(allowLocalNetworkHosts, coapClientConfiguration.getCoapEndpoint());
                break;
            case DTLS:
                checkDtlsEnabled();
                validateUri(allowLocalNetworkHosts, coapClientConfiguration.getDtlsCoapEndpoint());
                break;
            default:
                throw new IllegalArgumentException("Unsupported CoAP security mode! value: " + securityMode);
        }
    }

    private void validateUri(boolean allowLocalNetworkHosts, String strEndpoint) {
        URI uri = URI.create(strEndpoint);
        if (!allowLocalNetworkHosts && isLocalNetworkHost(uri.getHost())) {
            throw new IllegalArgumentException("Usage of local network host for CoAP endpoint connection is not allowed!");
        }
    }

    private void setupConfiguration(CoapClientConfiguration coapClientConfiguration) throws URISyntaxException, UnknownHostException {
        CoapSecurityMode securityMode = coapClientConfiguration.getSecurityMode();
        switch (securityMode) {
            case NO_SECURE:
                this.integrationResource =
                        addIntegrationResource(getEndpointUri(coapClientConfiguration.getCoapEndpoint()));
                break;
            case DTLS:
                checkDtlsEnabled();
                this.dtlsIntegrationResource =
                        addIntegrationResource(getEndpointUri(coapClientConfiguration.getDtlsCoapEndpoint()));
                break;
            case MIXED:
                checkDtlsEnabled();
                String noSecEndpointUri = getEndpointUri(coapClientConfiguration.getCoapEndpoint());
                String dtlsCoapEndpointUri = getEndpointUri(coapClientConfiguration.getDtlsCoapEndpoint());
                this.integrationResource = addIntegrationResource(noSecEndpointUri);
                if (!noSecEndpointUri.equals(dtlsCoapEndpointUri)) {
                    this.dtlsIntegrationResource = addIntegrationResource(dtlsCoapEndpointUri);
                }
                break;
        }
    }

    private String getEndpointUri(String coapEndpoint) throws URISyntaxException {
        return new URI(coapEndpoint)
                .getPath()
                .replace(this.configuration.getRoutingKey(), "");
    }

    private void checkDtlsEnabled() {
        if (!coapServerService.isDtlsEnabled()) {
            throw new RuntimeException("CoAP server doesn't have DTLS Endpoint enabled!");
        }
    }

    private CoapResource addIntegrationResource(String uriPath) throws UnknownHostException {
        List<String> resourceHierarchy = getResourceHierarchy(uriPath);
        Resource integrations = coapServerService.addResourceHierarchicallyAndReturnLast(resourceHierarchy);
        CoapResource newIntegrationResource = createNewIntegrationResource();
        integrations.add(newIntegrationResource);
        return newIntegrationResource;
    }

    private CoapResource createNewIntegrationResource() {
        return new CoapResource(configuration.getRoutingKey()) {

            @Override
            public Resource getChild(String name) {
                return this;
            }

            @Override
            public void handlePOST(CoapExchange exchange) {
                process(new CoapIntegrationMsg(exchange));
            }

            @Override
            public void handlePUT(CoapExchange exchange) {
                process(new CoapIntegrationMsg(exchange));
            }

        };
    }

    private void processUplinkData(IntegrationContext context, List<UplinkData> uplinkDataList) throws Exception {
        if (uplinkDataList != null && !uplinkDataList.isEmpty()) {
            for (UplinkData uplinkData : uplinkDataList) {
                processUplinkDataBlocking(context, uplinkData);
                log.trace("Processed uplink data: [{}]", uplinkData);
            }
        }
    }

    private List<UplinkData> getUplinkDataList(IntegrationContext context, CoapIntegrationMsg msg) throws Exception {
        Map<String, String> metadataMap = new HashMap<>(metadataTemplate.getKvMap());
        OptionSet options = msg.getExchange().getRequestOptions();
        if (options.getContentFormat() >= 0) {
            metadataMap.put("Option:content-format", Integer.toString(options.getContentFormat()));
        }
        if (options.getLocationPath() != null && !options.getLocationPath().isEmpty()) {
            metadataMap.put("Option:location-path", String.join(",", options.getLocationPath()));
        }
        if (options.getLocationQuery() != null && !options.getLocationQuery().isEmpty()) {
            metadataMap.put("Option:location-query", String.join(",", options.getLocationQuery()));
        }
        if (StringUtils.isNotEmpty(options.getUriString())) {
            metadataMap.put("Option:uri", options.getUriString());
        }
        return convertToUplinkDataList(context, msg.getPayloadBytes(), new UplinkMetaData<>(msg.getContentType(), metadataMap));
    }

    private List<String> getResourceHierarchy(String path) {
        final String slash = "/";

        // remove the leading slash
        if (path.startsWith(slash)) {
            path = path.substring(slash.length());
        }
        return Arrays.asList(path.split(slash));
    }

}
