package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
public class ResultUpdateResource {
    LwM2mClient lwM2MClient;
    Set<String> paths;

    public ResultUpdateResource(LwM2mClient lwM2MClient) {
        this.lwM2MClient = lwM2MClient;
        this.paths = new HashSet<>();
    }
}
