/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
