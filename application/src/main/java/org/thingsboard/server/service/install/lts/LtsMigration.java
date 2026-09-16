// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
