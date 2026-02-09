/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.rpc;

import org.thingsboard.server.dao.service.DaoSqlTest;

@DaoSqlTest
public abstract class AbstractRpcLwM2MIntegrationObserve_Ver_1_1_Test extends AbstractRpcLwM2MIntegrationTest{

    public AbstractRpcLwM2MIntegrationObserve_Ver_1_1_Test() throws Exception {
        String[] RESOURCES_RPC_VER_1_1 = new String[]{"3-1_1.xml", "5.xml", "6.xml", "9.xml", "19.xml"};
        setResources(RESOURCES_RPC_VER_1_1);
    }
}

