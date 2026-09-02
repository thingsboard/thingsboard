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
package org.thingsboard.monitoring.data.notification;

import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class ServiceFailureNotification implements Notification {

    private final Object serviceKey;
    private final Throwable error;
    private final int failuresCount;

    public ServiceFailureNotification(Object serviceKey, Throwable error, int failuresCount) {
        this.serviceKey = serviceKey;
        this.error = error;
        this.failuresCount = failuresCount;
    }

    @Override
    public String getText() {
        String errorMsg = error.getMessage();
        if (errorMsg == null || errorMsg.equals("null")) {
            Throwable cause = ExceptionUtils.getRootCause(error);
            if (cause != null) {
                errorMsg = cause.getMessage();
            }
        }
        if (errorMsg == null) {
            errorMsg = error.getClass().getSimpleName();
        }
        errorMsg = stripResponseBody(errorMsg);
        errorMsg = linkifyRequestUrl(errorMsg);
        return String.format("%s - Failure: %s (number of subsequent failures: %s)", serviceKey, errorMsg, failuresCount);
    }

    // Spring RestClient: '... request for "<URL>"'
    private static final Pattern REQUEST_FOR_URL_PATTERN = Pattern.compile("request for \"(https?://[^\"\\s]+)\"");
    // Apache HttpClient wrapped by Spring: 'I/O error on POST request: Connect to <URL> failed: <reason>'
    private static final Pattern REQUEST_CONNECT_PATTERN = Pattern.compile("request: Connect to (https?://\\S+?) failed:");

    static String linkifyRequestUrl(String msg) {
        if (msg == null) {
            return null;
        }
        // Slack mrkdwn link: <url|label>
        Matcher m = REQUEST_FOR_URL_PATTERN.matcher(msg);
        if (m.find()) {
            return m.replaceAll("<$1|request>");
        }
        Matcher m2 = REQUEST_CONNECT_PATTERN.matcher(msg);
        if (m2.find()) {
            return m2.replaceAll("<$1|request>:");
        }
        return msg;
    }

    static String stripResponseBody(String msg) {
        if (msg == null) {
            return null;
        }
        int htmlIdx = -1;
        for (String marker : new String[]{"<html", "<HTML", "<!DOCTYPE", "<!doctype"}) {
            int idx = msg.indexOf(marker);
            if (idx >= 0 && (htmlIdx < 0 || idx < htmlIdx)) {
                htmlIdx = idx;
            }
        }
        if (htmlIdx > 0) {
            msg = msg.substring(0, htmlIdx).stripTrailing();
            if (msg.endsWith("\"")) {
                msg = msg.substring(0, msg.length() - 1).stripTrailing();
            }
            if (msg.endsWith(":")) {
                msg = msg.substring(0, msg.length() - 1).stripTrailing();
            }
        }
        return msg;
    }

    @Override
    public List<AffectedService> getAffectedServices() {
        return List.of(AffectedService.failing(shortName(serviceKey), failuresCount));
    }

    static String shortName(Object serviceKey) {
        if (serviceKey instanceof ShortNameProvider provider) {
            return provider.getShortName();
        }
        return serviceKey.toString();
    }

}
