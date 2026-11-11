// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trendz;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.MultiValueMap;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@DaoSqlTest
public class TrendzProxyServiceTest extends AbstractControllerTest {
    @MockitoBean
    private TrendzClient trendzClient;

    @Autowired
    private TrendzProxyService trendzProxyService;

    @Test
    public void proxyTest() throws ThingsboardException {
        String trendzUri = "/apiTrendz/test";

        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                "trendz_response_body".getBytes(),
                MultiValueMap.fromMultiValue(Map.of("trendz_header", List.of("trendz_header_value"))),
                HttpStatusCode.valueOf(201)
        );

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("trendz_request_header", "trendz_request_header_value");
        byte[] requestBody = "trendz_request_body".getBytes();

        when(trendzClient.sendTrendzProxyRequest(trendzUri, Collections.emptyMap(), HttpMethod.POST, requestBody, requestHeaders))
                .thenReturn(expected);

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest("POST", trendzUri);
        httpServletRequest.addHeader("trendz_request_header", "trendz_request_header_value");

        ResponseEntity<byte[]> actual = trendzProxyService.proxy(httpServletRequest, requestBody);
        assertEquals(expected, actual);
    }

    @Test
    public void proxyTest_withQueryParams() throws ThingsboardException {
        ResponseEntity<byte[]> expected = new ResponseEntity<>(
                null,
                MultiValueMap.fromMultiValue(Map.of("trendz_header", List.of("trendz_header_value"))),
                HttpStatusCode.valueOf(200)
        );

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("trendz_request_header", "trendz_request_header_value");
        Map<String, String[]> expectedMap = Map.of(
                "param1", new String[]{"value1"},
                "param2", new String[]{"value2"}
        );

        when(trendzClient.sendTrendzProxyRequest(
                "/apiTrendz/test", expectedMap, HttpMethod.GET, null, requestHeaders
        )).thenReturn(expected);

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest("GET", "/apiTrendz/test");
        httpServletRequest.addHeader("trendz_request_header", "trendz_request_header_value");
        httpServletRequest.setParameters(expectedMap);

        ResponseEntity<byte[]> actual = trendzProxyService.proxy(httpServletRequest, null);
        assertEquals(expected, actual);
    }
}
