// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

public interface NodeConfiguration<T extends NodeConfiguration> {

    T defaultConfiguration();

}
