// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity.strategy;

public enum ActivityStrategyType {

    ALL {
        @Override
        public ActivityStrategy toStrategy() {
            return AllEventsActivityStrategy.getInstance();
        }
    },
    FIRST {
        @Override
        public ActivityStrategy toStrategy() {
            return new FirstEventActivityStrategy();
        }
    },
    LAST {
        @Override
        public ActivityStrategy toStrategy() {
            return LastEventActivityStrategy.getInstance();
        }
    },
    FIRST_AND_LAST {
        @Override
        public ActivityStrategy toStrategy() {
            return new FirstAndLastEventActivityStrategy();
        }
    };

    public abstract ActivityStrategy toStrategy();

}
