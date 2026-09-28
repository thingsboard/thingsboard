// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public abstract class TbAbstractLatestNodeConfiguration {

    private ParentEntitiesQuery parentEntitiesQuery;

    private TimeUnit periodTimeUnit;
    private int periodValue;

    private String outMsgType;

}
