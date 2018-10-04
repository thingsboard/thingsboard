package org.thingsboard.server.transport.mqtt;

import io.netty.handler.ssl.SslHandler;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.quota.host.HostRequestsQuotaService;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;

/**
 * Created by ashvayka on 04.10.18.
 */
@Component
@Data
public class MqttTransportContext {

    @Autowired
    @Lazy
    private TransportService transportService;

    @Autowired(required = false)
    private MqttSslHandlerProvider sslHandlerProvider;

    @Autowired(required = false)
    private HostRequestsQuotaService quotaService;

    @Autowired
    private MqttTransportAdaptor adaptor;

    @Value("${mqtt.netty.max_payload_size}")
    private Integer maxPayloadSize;

    private SslHandler sslHandler;

}
