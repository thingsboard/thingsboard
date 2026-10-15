// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.queue;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProcessingStrategy implements Serializable {

    private static final long serialVersionUID = 293598327194813L;

    private ProcessingStrategyType type;
    private int retries;
    private double failurePercentage;
    private long pauseBetweenRetries;
    private long maxPauseBetweenRetries;
}
