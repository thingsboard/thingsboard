/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.id;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EntityIdTest {

    @Test
    public void givenConstantNullUuid_whenCompare_thenToStringEqualsPredefinedUuid() {
        Assertions.assertEquals("13814000-1dd2-11b2-8080-808080808080", EntityId.NULL_UUID.toString());
    }

}