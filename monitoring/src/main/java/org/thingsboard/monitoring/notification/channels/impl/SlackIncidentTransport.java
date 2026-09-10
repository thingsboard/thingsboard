// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.notification.channels.impl;

import org.thingsboard.monitoring.notification.incident.IncidentTransport;

public class SlackIncidentTransport implements IncidentTransport {

    private final SlackApiClient slackApiClient;
    private final String channelId;

    public SlackIncidentTransport(SlackApiClient slackApiClient, String channelId) {
        this.slackApiClient = slackApiClient;
        this.channelId = channelId;
    }

    @Override
    public String postIncident(String text) {
        return slackApiClient.postMessage(channelId, text);
    }

    @Override
    public void postThreadReply(String threadId, String text) {
        slackApiClient.postThreadReply(channelId, threadId, text);
    }

    @Override
    public void updateIncident(String threadId, String text) {
        slackApiClient.updateMessage(channelId, threadId, text);
    }

}
