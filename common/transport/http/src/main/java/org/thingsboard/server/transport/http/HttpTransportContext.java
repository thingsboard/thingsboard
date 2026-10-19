// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.http;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;

@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.http.enabled}'=='true')")
@Component
public class HttpTransportContext extends TransportContext {

    @Getter
    @Value("${transport.http.request_timeout}")
    private long defaultTimeout;

    @Getter
    @Value("${transport.http.max_request_timeout}")
    private long maxRequestTimeout;

    @Bean
    public TomcatConnectorCustomizer tomcatAsyncTimeoutConnectorCustomizer() {
        return connector -> {
            ProtocolHandler handler = connector.getProtocolHandler();
            if (handler instanceof Http11NioProtocol) {
                log.trace("Setting async max request timeout {}", maxRequestTimeout);
                connector.setAsyncTimeout(maxRequestTimeout);
            }
        };
    }

}
