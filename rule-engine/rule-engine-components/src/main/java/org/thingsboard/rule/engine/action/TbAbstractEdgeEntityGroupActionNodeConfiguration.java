// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import lombok.Data;

import java.util.List;

@Data
public abstract class TbAbstractEdgeEntityGroupActionNodeConfiguration {

    private EdgeSource edgeSource;
    private String edgeNamePattern;
    private List<EntityGroupPattern> entityGroups;

}
