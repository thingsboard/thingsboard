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
