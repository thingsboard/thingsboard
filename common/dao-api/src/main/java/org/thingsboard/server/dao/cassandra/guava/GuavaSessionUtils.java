// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cassandra.guava;

public class GuavaSessionUtils {
    public static GuavaSessionBuilder builder() {
        return new GuavaSessionBuilder();
    }
}
