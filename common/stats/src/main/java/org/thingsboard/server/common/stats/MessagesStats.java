// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.stats;

public interface MessagesStats {
    default void incrementTotal() {
        incrementTotal(1);
    }

    void incrementTotal(int amount);

    default void incrementSuccessful() {
        incrementSuccessful(1);
    }

    void incrementSuccessful(int amount);

    default void incrementFailed() {
        incrementFailed(1);
    }

    void incrementFailed(int amount);

    int getTotal();

    int getSuccessful();

    int getFailed();

    void reset();
}
