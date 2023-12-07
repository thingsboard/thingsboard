/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.transport.service.activity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.activity.AbstractActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.service.SessionMetaData;
import org.thingsboard.server.common.transport.service.TransportActivityState;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EXPIRED_NOTIFICATION_PROTO;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "transport.activity", value = "reporting_strategy", havingValue = "last")
public class LastOnlyTransportActivityManager extends AbstractActivityManager<UUID, TransportActivityState> {

    private final ConcurrentMap<UUID, TransportActivityState> states = new ConcurrentHashMap<>();

    @Value("${transport.sessions.inactivity_timeout}")
    private long sessionInactivityTimeout;

    @Autowired
    private TransportService transportService;

    @Override
    protected void doOnActivity(UUID activityKey, Supplier<TransportActivityState> newStateSupplier) {
        long newLastRecordedTime = System.currentTimeMillis();
        states.compute(activityKey, (__, activityState) -> {
            if (activityState == null) {
                activityState = newStateSupplier.get();
                activityState.setLastRecordedTime(newLastRecordedTime);
                activityState.setLastReportedTime(0L);
            } else {
                activityState.setLastRecordedTime(newLastRecordedTime);
            }
            return activityState;
        });
    }

    @Override
    protected void onReportingPeriodEnd() {
        long expirationTime = System.currentTimeMillis() - sessionInactivityTimeout;
        for (Map.Entry<UUID, TransportActivityState> entry : states.entrySet()) {
            var sessionId = entry.getKey();
            var activityState = entry.getValue();

            SessionMetaData sessionMetaData = transportService.getSession(sessionId);
            if (sessionMetaData != null) {
                activityState.setSessionInfoProto(sessionMetaData.getSessionInfo());
            } else {
                states.remove(sessionId);
            }

            long lastActivityTime = activityState.getLastRecordedTime();
            TransportProtos.SessionInfoProto sessionInfo = activityState.getSessionInfoProto();

            if (sessionInfo.getGwSessionIdMSB() != 0 && sessionInfo.getGwSessionIdLSB() != 0) {
                var gwSessionId = new UUID(sessionInfo.getGwSessionIdMSB(), sessionInfo.getGwSessionIdLSB());
                SessionMetaData gwSessionMetaData = transportService.getSession(gwSessionId);
                if (gwSessionMetaData != null && gwSessionMetaData.isOverwriteActivityTime()) {
                    TransportActivityState gwActivityState = states.get(gwSessionId);
                    if (gwActivityState != null) {
                        lastActivityTime = Math.max(gwActivityState.getLastRecordedTime(), lastActivityTime);
                    }
                }
            }

            boolean hasExpired = sessionMetaData != null && lastActivityTime < expirationTime;
            if (hasExpired) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Session has expired due to last activity time: [{}]!", sessionId, lastActivityTime);
                }
                transportService.deregisterSession(sessionInfo);
                transportService.process(sessionInfo, SESSION_EVENT_MSG_CLOSED, new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(Void msgAcknowledged) {
                        states.remove(sessionId);
                    }

                    @Override
                    public void onError(Throwable e) {
                        states.remove(sessionId);
                    }
                });
                sessionMetaData.getListener().onRemoteSessionCloseCommand(sessionId, SESSION_EXPIRED_NOTIFICATION_PROTO);
            } else if (activityState.getLastReportedTime() < lastActivityTime) {
                reporter.report(sessionId, lastActivityTime, activityState, new ActivityReportCallback<>() {
                    @Override
                    public void onSuccess(UUID key, long reportedTime) {
                        updateLastReportedTime(key, reportedTime);
                    }

                    @Override
                    public void onFailure(UUID key, Throwable t) {
                        log.debug("[{}] Failed to report last activity event in a period for session.", sessionId);
                    }
                });
            }
        }
    }

    private void updateLastReportedTime(UUID key, long newLastReportedTime) {
        states.computeIfPresent(key, (__, activityState) -> {
            activityState.setLastReportedTime(Math.max(activityState.getLastReportedTime(), newLastReportedTime));
            return activityState;
        });
    }

}
