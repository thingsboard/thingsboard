package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.quota.host.HostRequestsQuotaService;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by ashvayka on 04.10.18.
 */
@Slf4j
@Component
@Data
public class MqttTransportContext {

    private final ObjectMapper mapper = new ObjectMapper();

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

    @Value("${cluster.node_id:#{null}}")
    private String nodeId;

    private SslHandler sslHandler;

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(nodeId)) {
            try {
                nodeId = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                nodeId = RandomStringUtils.randomAlphabetic(10);
            }
        }
        log.info("Current NodeId: {}", nodeId);
    }

}
