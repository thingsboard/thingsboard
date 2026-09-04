// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Timer;

import java.util.function.ToDoubleFunction;

public interface StatsFactory {

    StatsCounter createStatsCounter(String key, String statsName, String... otherTags);

    DefaultCounter createDefaultCounter(String key, String... tags);

    <T extends Number> T createGauge(String key, T number, String... tags);

    <T extends Number> T createGauge(String type, String name, T number, String... tags);

    <S> void createGauge(String type, String name, S stateObject, ToDoubleFunction<S> numberProvider, String... tags);

    MessagesStats createMessagesStats(String key);

    Timer createTimer(String key, String... tags);

    StatsTimer createStatsTimer(String type, String name, String... tags);

}
