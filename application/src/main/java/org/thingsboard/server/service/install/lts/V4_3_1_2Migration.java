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
package org.thingsboard.server.service.install.lts;

import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Registration-only migration with no {@link #apply()} (no data migration), kept intentionally.
 * <p>
 * {@link LtsMigrationService} selects migrations from the injected {@link LtsMigration} beans, not from the
 * on-disk {@code data/upgrade/lts/<version>/} directories. So this bean is what makes the runner discover
 * version {@code 4.3.1.2} and execute its {@code data/upgrade/lts/4.3.1.2/schema_update.sql} (which adds
 * {@code calculated_field.additional_info}). A directory holding a {@code schema_update.sql} but lacking a
 * matching bean would be silently skipped.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code LtsMigrationIntegrationTest}.
 */
@Component
@TbCoreComponent
public class V4_3_1_2Migration implements LtsMigration {

    @Override
    public String getVersion() {
        return "4.3.1.2";
    }
}
