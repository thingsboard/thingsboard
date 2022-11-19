/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.monitoring.notification.channels;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.thingsboard.monitoring.data.notification.MonitoringFailureNotificationInfo;
import org.thingsboard.monitoring.data.notification.NotificationInfo;
import org.thingsboard.monitoring.data.notification.TransportFailureNotificationInfo;

@Slf4j
public abstract class NotificationChannel {

    public abstract void sendNotification(NotificationInfo notificationInfo);


    protected String createNotificationMessage(NotificationInfo notificationInfo) {
        String message = String.format("[%s transport (%s)]", notificationInfo.getTransportInfo().getTransportType(), notificationInfo.getTransportInfo().getUrl());

        switch (notificationInfo.getType()) {
            case TRANSPORT_FAILURE:
                TransportFailureNotificationInfo transportFailureNotificationInfo = (TransportFailureNotificationInfo) notificationInfo;
                message += " Transport failure: " + getErrorMessage(transportFailureNotificationInfo.getError());
                break;
            case MONITORING_FAILURE:
                MonitoringFailureNotificationInfo monitoringFailureNotificationInfo = (MonitoringFailureNotificationInfo) notificationInfo;
                message += " Monitoring failure: " + getErrorMessage(monitoringFailureNotificationInfo.getError());
                break;
            case TRANSPORT_RECOVERY:
                message += " Transport is now working";
                break;
            case MONITORING_RECOVERY:
                message += " Monitoring is now working";
                break;
            default:
                throw new UnsupportedOperationException("Notification type " + notificationInfo.getType() + " not supported");
        }

        return message;
    }

    protected String getErrorMessage(Exception error) {
        return ExceptionUtils.getMessage(error);
    }

}
