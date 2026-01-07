/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

}
