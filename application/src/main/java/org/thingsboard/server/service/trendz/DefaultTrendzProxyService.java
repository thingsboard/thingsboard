// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trendz;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrendzProxyService implements TrendzProxyService {
    private final TrendzClient trendzClient;

    @Override
    public ResponseEntity<byte[]> proxy(HttpServletRequest request, byte[] body) throws ThingsboardException {
        String path = request.getRequestURI();
        Map<String, String[]> parameterMap = request.getParameterMap();

        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());

        HttpHeaders headers = new HttpHeaders();
        request.getHeaderNames()
                .asIterator()
                .forEachRemaining(name -> request.getHeaders(name)
                        .asIterator()
                        .forEachRemaining(value -> headers.add(name, value))
                );

        return trendzClient.sendTrendzProxyRequest(path, parameterMap, httpMethod, body, headers);
    }
}
