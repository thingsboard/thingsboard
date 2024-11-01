/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public abstract class AbstractRpcLwM2MIntegrationObserveTest extends AbstractRpcLwM2MIntegrationTest{
    private final String[] RESOURCES_RPC_MULTIPLE_19 = new String[]{"0.xml", "1.xml", "2.xml", "3.xml", "5.xml", "6.xml", "9.xml", "19.xml", "3303.xml"};

    public AbstractRpcLwM2MIntegrationObserveTest() {
        setResources(this.RESOURCES_RPC_MULTIPLE_19);
    }

    protected void sendRpcObserveWithContainsLwM2mSingleResource(String params) throws Exception {
        String rpcActualResult = sendRpcObserveOkWithResultValue("Observe", params);
        assertTrue(rpcActualResult.contains("LwM2mSingleResource") || rpcActualResult.contains("LwM2mMultipleResource"));
    }
}
