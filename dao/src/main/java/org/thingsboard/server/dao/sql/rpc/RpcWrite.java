// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.rpc;

import org.thingsboard.server.dao.model.sql.RpcEntity;

/**
 * One queued RPC write, tagged with the statement it needs.
 */
public record RpcWrite(RpcEntity entity, RpcWrite.Op op) {

    public enum Op { INSERT, UPDATE }

    public static RpcWrite insert(RpcEntity entity) {
        return new RpcWrite(entity, Op.INSERT);
    }

    public static RpcWrite update(RpcEntity entity) {
        return new RpcWrite(entity, Op.UPDATE);
    }
}
