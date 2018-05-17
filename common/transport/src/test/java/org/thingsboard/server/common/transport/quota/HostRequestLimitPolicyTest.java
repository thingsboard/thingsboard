/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.quota;

import org.junit.Test;
import org.thingsboard.server.common.transport.quota.host.HostRequestLimitPolicy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
public class HostRequestLimitPolicyTest {

    private HostRequestLimitPolicy limitPolicy = new HostRequestLimitPolicy(10L);

    @Test
    public void ifCurrentValueLessThenLimitItIsValid() {
        assertTrue(limitPolicy.isValid(9));
    }

    @Test
    public void ifCurrentValueEqualsToLimitItIsValid() {
        assertTrue(limitPolicy.isValid(10));
    }

    @Test
    public void ifCurrentValueGreaterThenLimitItIsValid() {
        assertFalse(limitPolicy.isValid(11));
    }

}