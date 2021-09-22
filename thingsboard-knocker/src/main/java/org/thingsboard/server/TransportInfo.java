package org.thingsboard.server;

import lombok.Data;

@Data
public class TransportInfo {
    private final TransportType transportType;
    private final String information;
}
