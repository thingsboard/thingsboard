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

/**
 * One LTS migration, keyed by the version it ships in (e.g. "4.2.2.3").
 * The runner runs the version's data/upgrade/lts/&lt;version&gt;/schema_update.sql by convention
 * (if present), then calls {@link #apply()} for any programmatic work beyond SQL.
 * Most migrations are SQL-only and only override {@link #getVersion()}.
 */
public interface LtsMigration {

    String getVersion();

    default void apply() {
    }
}
