// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ActivityStrategyTypeTest {

    @Test
    public void testCreateAllEventsStrategy() {
        assertThat(ActivityStrategyType.ALL.toStrategy()).isEqualTo(AllEventsActivityStrategy.getInstance());
    }

    @Test
    public void testCreateFirstEventStrategy() {
        assertThat(ActivityStrategyType.FIRST.toStrategy()).isEqualTo(new FirstEventActivityStrategy());
    }

    @Test
    public void testCreateLastEventStrategy() {
        assertThat(ActivityStrategyType.LAST.toStrategy()).isEqualTo(LastEventActivityStrategy.getInstance());
    }

    @Test
    public void testCreateFirstAndLastEventStrategy() {
        assertThat(ActivityStrategyType.FIRST_AND_LAST.toStrategy()).isEqualTo(new FirstAndLastEventActivityStrategy());
    }

}
