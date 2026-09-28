// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.MultiValueMap;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.trendz.TrendzProxyService;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DaoSqlTest
public class TrendzProxyControllerTest extends AbstractControllerTest {
    @MockitoBean
    private TrendzProxyService trendzProxyService;

    @Test
    public void handleAuthorizedTrendzRequest_withBody() throws Exception {
        loginCustomerUser();

        String trendzUri = "/apiTrendz/test";
        String expectedBody = "trendz_response_body";
        String expectedHeaderName = "trendz_header";
        List<String> expectedHeaderValue = List.of("trendz_header_value");
        int expectedStatusCode = 201;

        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                expectedBody.getBytes(),
                MultiValueMap.fromMultiValue(Map.of(expectedHeaderName, expectedHeaderValue)),
                HttpStatusCode.valueOf(expectedStatusCode)
        );

        String requestBody = "trendz_request_body";
        when(trendzProxyService.proxy(any(), eq(requestBody.getBytes())))
                .thenReturn(expected);

        doPost(trendzUri, (Object) requestBody)
                .andExpectAll(
                        r -> assertEquals(expectedStatusCode, r.getResponse().getStatus()),
                        r -> assertEquals(expectedHeaderValue, r.getResponse().getHeaders(expectedHeaderName)),
                        r -> assertEquals(expectedBody, r.getResponse().getContentAsString())
                );
    }

    @Test
    public void handleAuthorizedTrendzRequest_withoutBody() throws Exception {
        loginCustomerUser();

        String trendzUri = "/apiTrendz/test";
        String expectedHeaderName = "trendz_header";
        List<String> expectedHeaderValue = List.of("trendz_header_value");
        int expectedStatusCode = 201;

        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                null,
                MultiValueMap.fromMultiValue(Map.of(expectedHeaderName, expectedHeaderValue)),
                HttpStatusCode.valueOf(expectedStatusCode)
        );

        String requestBody = "trendz_request_body";
        when(trendzProxyService.proxy(any(), eq(requestBody.getBytes())))
                .thenReturn(expected);

        doPost(trendzUri, (Object) requestBody)
                .andExpectAll(
                        r -> assertEquals(expectedStatusCode, r.getResponse().getStatus()),
                        r -> assertEquals(expectedHeaderValue, r.getResponse().getHeaders(expectedHeaderName)),
                        r -> assertTrue(r.getResponse().getContentAsString().isBlank())
                );
    }

    @Test
    public void handleAuthorizedTrendzRequest_withoutLogin_returnUnauthorized() throws Exception {
        String trendzUri = "/apiTrendz/test";
        doPost(trendzUri, (Object) "trendz_request_body")
                .andExpect(r -> assertEquals(401, r.getResponse().getStatus()));
    }

    @Test
    public void handleUnauthorizedTrendzRequest_trendz() throws Exception {
        String trendzUri = "/trendz/test";
        String expectedHeaderName = "trendz_header";
        List<String> expectedHeaderValue = List.of("trendz_header_value");
        int expectedStatusCode = 201;

        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                null,
                MultiValueMap.fromMultiValue(Map.of(expectedHeaderName, expectedHeaderValue)),
                HttpStatusCode.valueOf(expectedStatusCode)
        );

        String requestBody = "trendz_request_body";
        when(trendzProxyService.proxy(any(), eq(requestBody.getBytes())))
                .thenReturn(expected);

        doPost(trendzUri, (Object) requestBody)
                .andExpectAll(
                        r -> assertEquals(expectedStatusCode, r.getResponse().getStatus()),
                        r -> assertEquals(expectedHeaderValue, r.getResponse().getHeaders(expectedHeaderName)),
                        r -> assertTrue(r.getResponse().getContentAsString().isBlank())
                );
    }

    @Test
    public void handleUnauthorizedTrendzRequest_apiTrendzPublicApi() throws Exception {
        String trendzUri = "/apiTrendz/publicApi/test";
        String expectedHeaderName = "trendz_header";
        List<String> expectedHeaderValue = List.of("trendz_header_value");
        int expectedStatusCode = 201;

        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                null,
                MultiValueMap.fromMultiValue(Map.of(expectedHeaderName, expectedHeaderValue)),
                HttpStatusCode.valueOf(expectedStatusCode)
        );

        String requestBody = "trendz_request_body";
        when(trendzProxyService.proxy(any(), eq(requestBody.getBytes())))
                .thenReturn(expected);

        doPost(trendzUri, (Object) requestBody)
                .andExpectAll(
                        r -> assertEquals(expectedStatusCode, r.getResponse().getStatus()),
                        r -> assertEquals(expectedHeaderValue, r.getResponse().getHeaders(expectedHeaderName)),
                        r -> assertTrue(r.getResponse().getContentAsString().isBlank())
                );
    }
}
