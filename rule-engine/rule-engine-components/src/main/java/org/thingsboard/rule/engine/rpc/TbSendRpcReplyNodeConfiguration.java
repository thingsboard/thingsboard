// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.rpc;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.StringUtils;

@Data
public class TbSendRpcReplyNodeConfiguration implements NodeConfiguration<TbSendRpcReplyNodeConfiguration> {

    public static final String SERVICE_ID = "serviceId";
    public static final String SESSION_ID = "sessionId";
    public static final String REQUEST_ID = "requestId";

    private String serviceIdMetaDataAttribute;
    private String sessionIdMetaDataAttribute;
    private String requestIdMetaDataAttribute;

    @Override
    public TbSendRpcReplyNodeConfiguration defaultConfiguration() {
        TbSendRpcReplyNodeConfiguration configuration = new TbSendRpcReplyNodeConfiguration();
        configuration.setServiceIdMetaDataAttribute(SERVICE_ID);
        configuration.setSessionIdMetaDataAttribute(SESSION_ID);
        configuration.setRequestIdMetaDataAttribute(REQUEST_ID);
        return configuration;
    }

    public String getServiceIdMetaDataAttribute() {
        return !StringUtils.isEmpty(serviceIdMetaDataAttribute) ? serviceIdMetaDataAttribute : SERVICE_ID;
    }

    public String getSessionIdMetaDataAttribute() {
        return !StringUtils.isEmpty(sessionIdMetaDataAttribute) ? sessionIdMetaDataAttribute : SESSION_ID;
    }

    public String getRequestIdMetaDataAttribute() {
        return !StringUtils.isEmpty(requestIdMetaDataAttribute) ? requestIdMetaDataAttribute : REQUEST_ID;
    }
}

