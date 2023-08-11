/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.transport.coap.client.CoapClientContext;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoapTransportResourceTest {

    private static final String V1 = "v1";
    private static final String API = "api";
    private static final String TELEMETRY = "telemetry";
    private static final String ATTRIBUTES = "attributes";
    private static final String RPC = "rpc";
    private static final String CLAIM = "claim";
    private static final String PROVISION = "provision";
    private static final String GET_ATTRIBUTES_URI_QUERY = "clientKeys=attribute1,attribute2&sharedKeys=shared1,shared2";

    private static final Random RANDOM = new Random();

    private CoapTransportResource coapTransportResource;

    @BeforeEach
    void setUp() {

        var ctxMock = mock(CoapTransportContext.class);
        var coapServerServiceMock = mock(CoapServerService.class);
        var transportServiceMock = mock(TransportService.class);
        var clientContextMock = mock(CoapClientContext.class);
        var schedulerComponentMock = mock(SchedulerComponent.class);

        when(ctxMock.getTransportService()).thenReturn(transportServiceMock);
        when(ctxMock.getClientContext()).thenReturn(clientContextMock);
        when(ctxMock.getSessionReportTimeout()).thenReturn(1L);
        when(ctxMock.getScheduler()).thenReturn(schedulerComponentMock);

        coapTransportResource = new CoapTransportResource(ctxMock, coapServerServiceMock, V1);
    }

    @AfterEach
    void tearDown() {
    }

    // accessToken based tests

    @Test
    void givenPostTelemetryAccessTokenRequest_whenGetFeatureType_thenFeatureTypeTelemetry() {
        // GIVEN
        var request = toAccessTokenRequest(CoAP.Code.POST, StringUtils.randomAlphanumeric(20), TELEMETRY);

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.TELEMETRY, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenPostAttributesAccessTokenRequest_whenGetFeatureType_thenFeatureTypeAttributes() {
        // GIVEN
        Request request = toAccessTokenRequest(CoAP.Code.POST, StringUtils.randomAlphanumeric(20), ATTRIBUTES);

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.ATTRIBUTES, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenGetAttributesAccessTokenRequest_whenGetFeatureType_thenFeatureTypeAttributes() {
        // GIVEN
        Request request = toGetAttributesAccessTokenRequest(StringUtils.randomAlphanumeric(20));
        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.ATTRIBUTES, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenSubscribeForAttributesUpdatesAccessTokenRequest_whenGetFeatureType_thenFeatureTypeAttributes() {
        // GIVEN
        Request request = toAccessTokenRequest(CoAP.Code.GET, StringUtils.randomAlphanumeric(20), ATTRIBUTES);
        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.ATTRIBUTES, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenSubscribeForRpcUpdatesAccessTokenRequest_whenGetFeatureType_thenFeatureTypeRpc() {
        // GIVEN
        Request request = toAccessTokenRequest(CoAP.Code.GET, StringUtils.randomAlphanumeric(20), RPC);
        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.RPC, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenRpcResponseAccessTokenRequest_whenGetFeatureType_thenFeatureTypeRpc() {
        // GIVEN
        Request request = toRpcResponseAccessTokenRequest(StringUtils.randomAlphanumeric(20), RANDOM.nextInt(100));
        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.RPC, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenClientSideRpcAccessTokenRequest_whenGetFeatureType_thenFeatureTypeRpc() {
        // GIVEN
        Request request = toAccessTokenRequest(CoAP.Code.POST, StringUtils.randomAlphanumeric(20), RPC);
        // WHEN
        var featureTypeOptional  = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.RPC, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenClaimingAccessTokenRequest_whenGetFeatureType_thenFeatureTypeClaim() {
        // GIVEN
        Request request = toAccessTokenRequest(CoAP.Code.POST, StringUtils.randomAlphanumeric(20), CLAIM);
        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.CLAIM, featureTypeOptional.get(), "Feature type is invalid");
    }

    // certificate based tests

    @Test
    void givenPostTelemetryCertificateRequest_whenGetFeatureType_thenFeatureTypeTelemetry() {
        // GIVEN
        var request = toCertificateRequest(CoAP.Code.POST, TELEMETRY);

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.TELEMETRY, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenPostAttributesCertificateRequest_whenGetFeatureType_thenFeatureTypeAttributes() {
        // GIVEN
        var request = toCertificateRequest(CoAP.Code.POST, ATTRIBUTES);

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.ATTRIBUTES, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenGetAttributesCertificateRequest_whenGetFeatureType_thenFeatureTypeAttributes() {
        // GIVEN
        var request = toGetAttributesCertificateRequest();

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.ATTRIBUTES, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenSubscribeForAttributesUpdatesCertificateRequest_whenGetFeatureType_thenFeatureTypeAttributes() {
        // GIVEN
        var request = toCertificateRequest(CoAP.Code.GET, ATTRIBUTES);

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.ATTRIBUTES, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenSubscribeForRpcUpdatesCertificateRequest_whenGetFeatureType_thenFeatureTypeRpc() {
        // GIVEN
        var request = toCertificateRequest(CoAP.Code.GET, RPC);

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.RPC, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenRpcResponseCertificateRequest_whenGetFeatureType_thenFeatureTypeRpc() {
        // GIVEN
        Request request = toRpcResponseCertificateRequest(RANDOM.nextInt(100));

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.RPC, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenClientSideRpcCertificateRequest_whenGetFeatureType_thenFeatureTypeRpc() {
        // GIVEN
        Request request = toCertificateRequest(CoAP.Code.POST, RPC);

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.RPC, featureTypeOptional.get(), "Feature type is invalid");
    }

    @Test
    void givenClaimingCertificateRequest_whenGetFeatureType_thenFeatureTypeClaim() {
        // GIVEN
        Request request = toCertificateRequest(CoAP.Code.POST, CLAIM);

        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.CLAIM, featureTypeOptional.get(), "Feature type is invalid");
    }

    // provision request

    @Test
    void givenProvisionRequest_whenGetFeatureType_thenFeatureTypeProvision() {
        // GIVEN
        Request request = toCertificateRequest(CoAP.Code.POST, PROVISION);
        // WHEN
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        // THEN
        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(FeatureType.PROVISION, featureTypeOptional.get(), "Feature type is invalid");
    }

    private Request toAccessTokenRequest(CoAP.Code method, String accessToken, String featureType) {
        return getAccessTokenRequest(method, accessToken, featureType, null, null);
    }

    private Request toGetAttributesAccessTokenRequest(String accessToken) {
        return getAccessTokenRequest(CoAP.Code.GET, accessToken, CoapTransportResourceTest.ATTRIBUTES, null, CoapTransportResourceTest.GET_ATTRIBUTES_URI_QUERY);
    }

    private Request toRpcResponseAccessTokenRequest(String accessToken, Integer requestId) {
        return getAccessTokenRequest(CoAP.Code.POST, accessToken, CoapTransportResourceTest.RPC, requestId, null);
    }

    private Request toCertificateRequest(CoAP.Code method, String featureType) {
        return getCertificateRequest(method, featureType, null, null);
    }

    private Request toGetAttributesCertificateRequest() {
        return getCertificateRequest(CoAP.Code.GET, CoapTransportResourceTest.ATTRIBUTES, null, CoapTransportResourceTest.GET_ATTRIBUTES_URI_QUERY);
    }

    private Request toRpcResponseCertificateRequest(Integer requestId) {
        return getCertificateRequest(CoAP.Code.POST, CoapTransportResourceTest.RPC, requestId, null);
    }

    private Request getAccessTokenRequest(CoAP.Code method, String accessToken, String featureType, Integer requestId, String uriQuery) {
        var request = new Request(method);
        var options = new OptionSet();
        options.addUriPath(API);
        options.addUriPath(V1);
        options.addUriPath(accessToken);
        options.addUriPath(featureType);
        if (requestId != null) {
            options.addUriPath(String.valueOf(requestId));
        }
        if (uriQuery != null) {
            options.setUriQuery(uriQuery);
        }
        request.setOptions(options);
        return request;
    }

    private Request getCertificateRequest(CoAP.Code method, String featureType, Integer requestId, String uriQuery) {
        var request = new Request(method);
        var options = new OptionSet();
        options.addUriPath(API);
        options.addUriPath(V1);
        options.addUriPath(featureType);
        if (requestId != null) {
            options.addUriPath(String.valueOf(requestId));
        }
        if (uriQuery != null) {
            options.setUriQuery(uriQuery);
        }
        request.setOptions(options);
        return request;
    }


}
