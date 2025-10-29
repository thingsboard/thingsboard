/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

public class CustomInterval extends BaseAggInterval {

    private int multiplier; // number of base units (e.g. 2 hours, 5 days)
    private AggIntervalType internalIntervalType;

    @Override
    public AggIntervalType getType() {
        return AggIntervalType.CUSTOM;
    }

    @Override
    public long getIntervalDurationMillis() {
        return getCurrentIntervalEndTs() - getCurrentIntervalStartTs();
    }

    @Override
    public long getCurrentIntervalStartTs() {
        return super.getCurrentIntervalStartTs(internalIntervalType, multiplier);
    }

    @Override
    public long getCurrentIntervalEndTs() {
        return super.getCurrentIntervalEndTs(internalIntervalType, multiplier);
    }

    @Override
    public long getDelayUntilIntervalEnd() {
        return super.getDelayUntilIntervalEnd(internalIntervalType, multiplier);
    }

}
