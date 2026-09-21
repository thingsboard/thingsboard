// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity.strategy;

public interface ActivityStrategy {

    boolean onActivity();

    boolean onReportingPeriodEnd();

}
