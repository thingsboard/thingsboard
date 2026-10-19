// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.rest;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.StringUtils;

@Data
public class TbSendRestApiCallReplyNodeConfiguration implements NodeConfiguration<TbSendRestApiCallReplyNodeConfiguration> {
    public static final String SERVICE_ID = "serviceId";
    public static final String REQUEST_UUID = "requestUUID";

    private String serviceIdMetaDataAttribute;
    private String requestIdMetaDataAttribute;

    @Override
    public TbSendRestApiCallReplyNodeConfiguration defaultConfiguration() {
        TbSendRestApiCallReplyNodeConfiguration configuration = new TbSendRestApiCallReplyNodeConfiguration();
        configuration.setRequestIdMetaDataAttribute(REQUEST_UUID);
        configuration.setServiceIdMetaDataAttribute(SERVICE_ID);
        return configuration;
    }

    public String getServiceIdMetaDataAttribute() {
        return !StringUtils.isEmpty(serviceIdMetaDataAttribute) ? serviceIdMetaDataAttribute : SERVICE_ID;
    }

    public String getRequestIdMetaDataAttribute() {
        return !StringUtils.isEmpty(requestIdMetaDataAttribute) ? requestIdMetaDataAttribute : REQUEST_UUID;
    }
}
