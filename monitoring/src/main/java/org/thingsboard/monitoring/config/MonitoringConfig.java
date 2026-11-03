// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.config;

import java.util.List;

public interface MonitoringConfig<T extends MonitoringTarget> {

    List<T> getTargets();

}
