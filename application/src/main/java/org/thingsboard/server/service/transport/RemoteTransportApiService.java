package org.thingsboard.server.service.transport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Created by ashvayka on 05.10.18.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "quota.rule.tenant", value = "enabled", havingValue = "true", matchIfMissing = false)
public class RemoteTransportApiService implements TransportApiService {
}
