// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

/**
 * Defines scope of the rule execution in the actor system
 * 
 * @author ashvayka
 *
 */
public enum Scope {

    SYSTEM, TENANT, CUSTOMER, DEVICE, RULE;

}