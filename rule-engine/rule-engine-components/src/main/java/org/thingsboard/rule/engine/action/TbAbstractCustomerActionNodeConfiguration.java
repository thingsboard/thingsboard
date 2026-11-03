// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.action;

import lombok.Data;

@Data
public abstract class TbAbstractCustomerActionNodeConfiguration {

    private String customerNamePattern;

}
