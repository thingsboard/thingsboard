/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.activity.AbstractActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategyType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class TransportActivityManager extends AbstractActivityManager<UUID, TransportProtos.SessionInfoProto> implements TransportService {

    public static final String SESSION_EXPIRED_MESSAGE = "Session has expired due to last activity time!";

    public static final TransportProtos.SessionEventMsg SESSION_EVENT_MSG_CLOSED = TransportProtos.SessionEventMsg.newBuilder()
            .setSessionType(TransportProtos.SessionType.ASYNC)
            .setEvent(TransportProtos.SessionEvent.CLOSED).build();
    public static final TransportProtos.SessionCloseNotificationProto SESSION_EXPIRED_NOTIFICATION_PROTO = TransportProtos.SessionCloseNotificationProto.newBuilder()
            .setMessage(SESSION_EXPIRED_MESSAGE).build();

    public final ConcurrentMap<UUID, SessionMetaData> sessions = new ConcurrentHashMap<>();

    @Value("${transport.sessions.report_timeout}")
    protected long sessionReportTimeout;

    @Value("${transport.sessions.inactivity_timeout}")
    protected long sessionInactivityTimeout;

    @Value("${transport.activity.reporting_strategy:LAST}")
    private ActivityStrategyType reportingStrategyType;

    @Override
    protected long getReportingPeriodMillis() {
        return sessionReportTimeout;
    }

    @Override
    protected ActivityState<TransportProtos.SessionInfoProto> createNewState(UUID sessionId) {
        SessionMetaData session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setMetadata(session.getSessionInfo());
        return state;
    }

    @Override
    protected ActivityStrategy getStrategy() {
        return reportingStrategyType.toStrategy();
    }

    @Override
    protected ActivityState<TransportProtos.SessionInfoProto> updateState(UUID sessionId, ActivityState<TransportProtos.SessionInfoProto> state) {
        SessionMetaData session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }

        state.setMetadata(session.getSessionInfo());
        var sessionInfo = state.getMetadata();

        if (sessionInfo.getGwSessionIdMSB() == 0L || sessionInfo.getGwSessionIdLSB() == 0L) {
            return state;
        }

        var gwSessionId = new UUID(sessionInfo.getGwSessionIdMSB(), sessionInfo.getGwSessionIdLSB());
        SessionMetaData gwSession = sessions.get(gwSessionId);
        if (gwSession == null || !gwSession.isOverwriteActivityTime()) {
            return state;
        }

        long lastRecordedTime = state.getLastRecordedTime();
        long gwLastRecordedTime = getLastRecordedTime(gwSessionId);
        log.debug("Session with id: [{}] has gateway session with id: [{}] with overwrite activity time enabled. " +
                        "Updating last activity time. Session last recorded time: [{}], gateway session last recorded time: [{}].",
                sessionId, gwSessionId, lastRecordedTime, gwLastRecordedTime);
        state.setLastRecordedTime(Math.max(lastRecordedTime, gwLastRecordedTime));
        return state;
    }

    @Override
    protected boolean hasExpired(long lastRecordedTime) {
        return (getCurrentTimeMillis() - sessionInactivityTimeout) > lastRecordedTime;
    }

    @Override
    protected void onStateExpiry(UUID sessionId, TransportProtos.SessionInfoProto sessionInfo) {
        log.debug("Session with id: [{}] has expired due to last activity time.", sessionId);
        SessionMetaData expiredSession = sessions.remove(sessionId);
        if (expiredSession != null) {
            deregisterSession(sessionInfo);
            process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
            expiredSession.getListener().onRemoteSessionCloseCommand(sessionId, SESSION_EXPIRED_NOTIFICATION_PROTO);
        }
    }

    @Override
    protected void reportActivity(UUID sessionId, TransportProtos.SessionInfoProto currentSessionInfo, long timeToReport, ActivityReportCallback<UUID> callback) {
        log.debug("Reporting activity state for session with id: [{}]. Time to report: [{}].", sessionId, timeToReport);
        SessionMetaData session = sessions.get(sessionId);
        TransportProtos.SubscriptionInfoProto subscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(session != null && session.isSubscribedToAttributes())
                .setRpcSubscription(session != null && session.isSubscribedToRPC())
                .setLastActivityTime(timeToReport)
                .build();
        TransportProtos.SessionInfoProto sessionInfo = session != null ? session.getSessionInfo() : currentSessionInfo;
        process(sessionInfo, subscriptionInfo, new TransportServiceCallback<>() {
            @Override
            public void onSuccess(Void msgAcknowledged) {
                callback.onSuccess(sessionId, timeToReport);

            }

            @Override
            public void onError(Throwable e) {
                callback.onFailure(sessionId, e);
            }
        });
    }

    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

}
