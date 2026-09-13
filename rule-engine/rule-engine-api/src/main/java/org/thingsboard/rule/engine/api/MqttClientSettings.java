// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

public interface MqttClientSettings {

    int getRetransmissionMaxAttempts();

    long getRetransmissionInitialDelayMillis();

    double getRetransmissionJitterFactor();

}
