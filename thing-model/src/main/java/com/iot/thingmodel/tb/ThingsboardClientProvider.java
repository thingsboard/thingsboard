package com.iot.thingmodel.tb;

import com.iot.thingmodel.admin.TenantAccountService;
import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.config.ThingModelProperties;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.thingsboard.client.ThingsboardClient;

@Component
public class ThingsboardClientProvider {

    private final ThingModelProperties properties;
    private final TenantAccountService accountService;
    private final Map<UUID, ClientHolder> clients = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public ThingsboardClientProvider(ThingModelProperties properties, TenantAccountService accountService) {
        this.properties = properties;
        this.accountService = accountService;
    }

    public ThingsboardClient get(UUID tenantId) {
        ClientHolder holder = clients.get(tenantId);
        if (holder != null) {
            return holder.client();
        }
        synchronized (lock) {
            holder = clients.get(tenantId);
            if (holder != null) {
                return holder.client();
            }
            TenantAccountService.TenantCredentials credentials = accountService.credentials(tenantId);
            try {
                ThingsboardClient client = ThingsboardClient.builder()
                        .url(properties.getTb().getBaseUrl())
                        .credentials(credentials.username(), credentials.password())
                        .retryOnRateLimit(true)
                        .maxRetries(properties.getTb().getClientMaxRetries())
                        .initialRetryDelayMs(properties.getTb().getClientInitialRetryDelayMs())
                        .maxRetryDelayMs(properties.getTb().getClientMaxRetryDelayMs())
                        .build();
                clients.put(tenantId, new ClientHolder(client));
                return client;
            } catch (org.thingsboard.client.ApiException ex) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "TB_LOGIN_FAILED",
                        "无法登录 ThingsBoard 厂商服务账号: HTTP " + ex.getCode());
            }
        }
    }

    public void evict(UUID tenantId) {
        clients.remove(tenantId);
    }

    private record ClientHolder(ThingsboardClient client) {
    }
}
