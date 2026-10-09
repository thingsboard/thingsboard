// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
/**
 * The {@code pe} segment of this package name is load-bearing: the fully-qualified class name of a rule node is
 * persisted as {@code rule_node.type} and is also keyed on in {@code ui-ngx/src/app/shared/models/rule-node.models.ts},
 * so renaming the package would orphan every existing Twilio rule node without a data migration.
 */
package org.thingsboard.rule.engine.pe.twilio;
