package org.thingsboard.server.common.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.transport.quota.host.HostRequestsQuotaService;
import org.thingsboard.server.kafka.TbNodeIdProvider;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ashvayka on 15.10.18.
 */
@Slf4j
@Data
public class TransportContext {

    protected final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TransportService transportService;

    @Autowired
    private TbNodeIdProvider nodeIdProvider;

    @Autowired(required = false)
    private HostRequestsQuotaService quotaService;

    @Getter
    private ExecutorService executor;

    @PostConstruct
    public void init() {
        executor = Executors.newCachedThreadPool();
    }

    @PreDestroy
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public String getNodeId() {
        return nodeIdProvider.getNodeId();
    }

}
