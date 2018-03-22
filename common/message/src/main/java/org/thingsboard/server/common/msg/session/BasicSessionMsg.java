/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.session;

import org.thingsboard.server.common.data.id.SessionId;

public class BasicSessionMsg implements SessionMsg {

    private final SessionContext ctx;

    public BasicSessionMsg(SessionContext ctx) {
        super();
        this.ctx = ctx;
    }

    @Override
    public SessionId getSessionId() {
        return ctx.getSessionId();
    }

    @Override
    public SessionContext getSessionContext() {
        return ctx;
    }

    @Override
    public String toString() {
        return "BasicSessionMsg [ctx=" + ctx + "]";
    }

}
