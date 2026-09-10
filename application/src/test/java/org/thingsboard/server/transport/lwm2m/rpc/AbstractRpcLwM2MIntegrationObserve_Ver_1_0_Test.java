// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.rpc;

import org.thingsboard.server.dao.service.DaoSqlTest;

@DaoSqlTest
public abstract class AbstractRpcLwM2MIntegrationObserve_Ver_1_0_Test extends AbstractRpcLwM2MIntegrationTest{

    public AbstractRpcLwM2MIntegrationObserve_Ver_1_0_Test() throws Exception {
        String[] RESOURCES_RPC_VER_1_0 = new String[]{"3-1_0.xml", "5.xml", "6.xml", "9.xml", "19.xml"};
        setResources(RESOURCES_RPC_VER_1_0);
    }
}
