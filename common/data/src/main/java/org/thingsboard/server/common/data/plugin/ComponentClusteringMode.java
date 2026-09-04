// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.plugin;

/**
 * The main idea to use this - it's adding the ability to start rule nodes in singleton mode in cluster setup
 * (singleton rule node will start in only one Rule Engine instance)
 * USER_PREFERENCE - user has ability to configure clustering mode (enable/disable singleton mode in rule node config)
 * ENABLE - user doesn't have ability to configure clustering mode (singleton mode is always FALSE in rule node config)
 * SINGLETON - user doesn't have ability to configure clustering mode (singleton mode is always TRUE in rule node config)
 */
public enum ComponentClusteringMode {
    USER_PREFERENCE,
    ENABLED,
    SINGLETON
}
