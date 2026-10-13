// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data.definition;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.cf.CalculatedField;

@Data
@EqualsAndHashCode(callSuper = true)
public class CalculatedFieldDefinition extends CalculatedField {

    private Integer reprocessingOrder;

}
