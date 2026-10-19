// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import org.thingsboard.rule.engine.util.TbMsgSource;

@Data
public abstract class TbAbstractFetchToNodeConfiguration {

    private TbMsgSource fetchTo;

}
