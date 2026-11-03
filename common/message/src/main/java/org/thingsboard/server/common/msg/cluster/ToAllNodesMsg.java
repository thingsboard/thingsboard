// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.cluster;

import org.thingsboard.server.common.msg.TbActorMsg;

import java.io.Serializable;

/**
 * @author Andrew Shvayka
 */
public interface ToAllNodesMsg extends Serializable, TbActorMsg {
}
