/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.transport.activity.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActivityStrategyFactoryTest {

    @Test
    public void testCreateAllEventsStrategy() {
        ActivityStrategy strategy = ActivityStrategyFactory.createStrategy("ALL");
        assertInstanceOf(AllEventsActivityStrategy.class, strategy, "Should return an instance of AllEventsActivityStrategy.");
    }

    @Test
    public void testCreateFirstEventStrategy() {
        ActivityStrategy strategy = ActivityStrategyFactory.createStrategy("FIRST");
        assertInstanceOf(FirstEventActivityStrategy.class, strategy, "Should return an instance of FirstEventActivityStrategy.");
    }

    @Test
    public void testCreateLastEventStrategy() {
        ActivityStrategy strategy = ActivityStrategyFactory.createStrategy("LAST");
        assertInstanceOf(LastEventActivityStrategy.class, strategy, "Should return an instance of LastEventActivityStrategy.");
    }

    @Test
    public void testCreateFirstAndLastEventStrategy() {
        ActivityStrategy strategy = ActivityStrategyFactory.createStrategy("FIRST_AND_LAST");
        assertInstanceOf(FirstAndLastEventActivityStrategy.class, strategy, "Should return an instance of FirstAndLastEventActivityStrategy.");
    }

    @Test
    public void testCreateUnknownStrategy() {
        assertThrows(IllegalArgumentException.class, () -> ActivityStrategyFactory.createStrategy("UNKNOWN"),
                "Should throw IllegalArgumentException for unknown strategy names.");
    }

}
