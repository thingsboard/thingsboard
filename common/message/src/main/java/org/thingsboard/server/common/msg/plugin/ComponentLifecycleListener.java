// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.plugin;

public interface ComponentLifecycleListener {
    void onComponentLifecycleMsg(ComponentLifecycleMsg componentLifecycleMsg);
}
