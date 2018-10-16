package org.thingsboard.server.common.transport.service;

import lombok.Data;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.gen.transport.TransportProtos;

/**
 * Created by ashvayka on 15.10.18.
 */
@Data
public class SessionMetaData {

    private final TransportProtos.SessionInfoProto sessionInfo;
    private final TransportProtos.SessionType sessionType;
    private final SessionMsgListener listener;

}
