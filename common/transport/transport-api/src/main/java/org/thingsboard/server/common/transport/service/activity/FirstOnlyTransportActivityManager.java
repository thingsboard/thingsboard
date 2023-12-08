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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.activity.AbstractActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.service.SessionMetaData;
import org.thingsboard.server.common.transport.service.TransportActivityState;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EXPIRED_NOTIFICATION_PROTO;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "transport.activity", value = "reporting_strategy", havingValue = "first")
public class FirstOnlyTransportActivityManager extends AbstractActivityManager<UUID, TransportActivityState> {

    private final ConcurrentMap<UUID, ActivityStateWrapper> states = new ConcurrentHashMap<>();

    @Data
    private static class ActivityStateWrapper {

        volatile TransportActivityState state;
        volatile boolean alreadyBeenReported;

    }

    @Value("${transport.sessions.inactivity_timeout}")
    private long sessionInactivityTimeout;

    @Autowired
    private TransportService transportService;

    @Override
    protected void doOnActivity(UUID sessionId, Supplier<TransportActivityState> newStateSupplier) {
        long newLastRecordedTime = System.currentTimeMillis();
        SettableFuture<Pair<UUID, Long>> reportCompletedFuture = SettableFuture.create();
        states.compute(sessionId, (key, activityStateWrapper) -> {
            if (activityStateWrapper == null) {
                activityStateWrapper = new ActivityStateWrapper();
                activityStateWrapper.setState(newStateSupplier.get());
            }
            var activityState = activityStateWrapper.getState();
            if (activityState.getLastRecordedTime() < newLastRecordedTime) {
                activityState.setLastRecordedTime(newLastRecordedTime);
            }
            if (activityStateWrapper.isAlreadyBeenReported()) {
                return activityStateWrapper;
            }
            if (activityState.getLastReportedTime() < activityState.getLastRecordedTime()) {
                log.debug("[{}] Going to report first activity event for session with id: [{}]", name, sessionId);
                reporter.report(key, activityState.getLastRecordedTime(), activityState, new ActivityReportCallback<>() {
                    @Override
                    public void onSuccess(UUID key, long reportedTime) {
                        reportCompletedFuture.set(Pair.of(key, reportedTime));
                    }

                    @Override
                    public void onFailure(UUID key, Throwable t) {
                        reportCompletedFuture.setException(t);
                    }
                });
            }
            activityStateWrapper.setAlreadyBeenReported(true);
            return activityStateWrapper;
        });
        Futures.addCallback(reportCompletedFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(Pair<UUID, Long> reportResult) {
                updateLastReportedTime(reportResult.getFirst(), reportResult.getSecond());
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                log.debug("[{}] Failed to report first activity event for session with id: [{}]", name, sessionId);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void doOnReportingPeriodEnd() {
        Set<UUID> statesToRemove = new HashSet<>();
        for (Map.Entry<UUID, ActivityStateWrapper> entry : states.entrySet()) {
            var sessionId = entry.getKey();
            var activityStateWrapper = entry.getValue();
            var activityState = activityStateWrapper.getState();

            SessionMetaData sessionMetaData = transportService.getSession(sessionId);
            if (sessionMetaData != null) {
                activityState.setSessionInfoProto(sessionMetaData.getSessionInfo());
            } else {
                log.debug("[{}] Session with id: [{}] is not present. Marking it's activity state for removal.", name, sessionId);
                statesToRemove.add(sessionId);
            }

            long lastActivityTime = activityState.getLastRecordedTime();
            TransportProtos.SessionInfoProto sessionInfo = activityState.getSessionInfoProto();

            if (sessionInfo.getGwSessionIdMSB() != 0 && sessionInfo.getGwSessionIdLSB() != 0) {
                var gwSessionId = new UUID(sessionInfo.getGwSessionIdMSB(), sessionInfo.getGwSessionIdLSB());
                SessionMetaData gwSessionMetaData = transportService.getSession(gwSessionId);
                if (gwSessionMetaData != null && gwSessionMetaData.isOverwriteActivityTime()) {
                    ActivityStateWrapper gwActivityStateWrapper = states.get(gwSessionId);
                    if (gwActivityStateWrapper != null) {
                        log.debug("[{}] Session with id: [{}] has gateway session with id: [{}] with overwrite activity time enabled. Updating last activity time.", name, sessionId, gwSessionId);
                        lastActivityTime = Math.max(gwActivityStateWrapper.getState().getLastRecordedTime(), lastActivityTime);
                    }
                }
            }

            long expirationTime = System.currentTimeMillis() - sessionInactivityTimeout;
            boolean hasExpired = sessionMetaData != null && lastActivityTime < expirationTime;
            if (hasExpired) {
                log.debug("[{}] Session with id: [{}] has expired due to last activity time: [{}]. Marking it's activity state for removal.", name, sessionId, lastActivityTime);
                statesToRemove.add(sessionId);
                transportService.deregisterSession(sessionInfo);
                transportService.process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
                sessionMetaData.getListener().onRemoteSessionCloseCommand(sessionId, SESSION_EXPIRED_NOTIFICATION_PROTO);
            }
            boolean shouldReportLeftoverEvents = sessionMetaData == null || hasExpired;
            if (shouldReportLeftoverEvents && activityState.getLastReportedTime() < lastActivityTime) {
                log.debug("[{}] Going to report leftover activity event for session with id: [{}].", name, sessionId);
                reporter.report(sessionId, lastActivityTime, activityState, new ActivityReportCallback<>() {
                    @Override
                    public void onSuccess(UUID key, long reportedTime) {
                        updateLastReportedTime(key, reportedTime);
                    }

                    @Override
                    public void onFailure(UUID key, Throwable t) {
                        log.debug("[{}] Failed to report leftover activity event for session with id: [{}].", name, sessionId);
                    }
                });
            }
            activityStateWrapper.setAlreadyBeenReported(false);
        }
        statesToRemove.forEach(states::remove);
    }

    private void updateLastReportedTime(UUID key, long newLastReportedTime) {
        states.computeIfPresent(key, (__, activityStateWrapper) -> {
            var activityState = activityStateWrapper.getState();
            activityState.setLastReportedTime(Math.max(activityState.getLastReportedTime(), newLastReportedTime));
            return activityStateWrapper;
        });
    }

}
