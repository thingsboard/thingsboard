// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import lombok.Data;

/**
 * Created by igor on 6/1/18.
 */
@Data
public abstract class TbAbstractGroupActionConfigration {

    private String groupNamePattern;
    private long groupCacheExpiration;

}
