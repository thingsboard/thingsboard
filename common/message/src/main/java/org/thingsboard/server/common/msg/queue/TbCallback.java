// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.queue;

import com.google.common.util.concurrent.SettableFuture;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.UUID;

public interface TbCallback {

    TbCallback EMPTY = new TbCallback() {

        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailure(Throwable t) {

        }
    };

    default UUID getId() {
        return EntityId.NULL_UUID;
    }

    void onSuccess();

    void onFailure(Throwable t);

    static <V> TbCallback wrap(SettableFuture<V> future) {
        return new TbCallback() {
            @Override
            public void onSuccess() {
                future.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                future.setException(t);
            }
        };
    }

}
