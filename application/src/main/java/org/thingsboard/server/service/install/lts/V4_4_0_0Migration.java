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
 * Baseline migration for the {@code 4.4.0.0} LTS family. Registration-only: it has no programmatic
 * {@link #apply()} work and exists so {@link LtsMigrationService} discovers version {@code 4.4.0.0} and runs
 * its {@code data/upgrade/lts/4.4.0.0/schema_update.sql} -- the 4.4 baseline flat DDL. A directory holding a
 * {@code schema_update.sql} but lacking a matching bean would be silently skipped (the runner iterates beans,
 * not directories), so this bean is what makes the 4.4 baseline DDL run on a {@code 4.3.x -> 4.4} offline
 * upgrade. {@code 4.4.0.0} is always in {@code (4.3.x, 4.4.y]}, so it is selected on every cross-family jump
 * and correctly excluded ({@code > from}) from within-4.4 no-downtime patches.
 * <p>
 * PE inherits this bean by merge and appends its own 4.4 baseline DDL to the same
 * {@code lts/4.4.0.0/schema_update.sql}, so there is a single {@code 4.4.0.0} bean across both editions.
 */
@Component
@TbCoreComponent
public class V4_4_0_0Migration implements LtsMigration {

    @Override
    public String getVersion() {
        return "4.4.0.0";
    }
}
