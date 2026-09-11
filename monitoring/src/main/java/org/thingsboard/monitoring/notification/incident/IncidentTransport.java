// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.notification.incident;

public interface IncidentTransport {

    String postIncident(String text);

    void postThreadReply(String threadId, String text);

    void updateIncident(String threadId, String text);

}
