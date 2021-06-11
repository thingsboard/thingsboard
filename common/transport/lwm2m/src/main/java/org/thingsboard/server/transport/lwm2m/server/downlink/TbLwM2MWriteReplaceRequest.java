/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.thingsboard.server.common.data.device.data.lwm2m.ObjectAttributes;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil;

public class TbLwM2MWriteReplaceRequest extends AbstractTbLwM2MTargetedDownlinkRequest<WriteResponse> {

    @Getter
    private final Object value;

    @Builder
    private TbLwM2MWriteReplaceRequest(String versionedId, long timeout, Object value) {
        super(versionedId, timeout);
        this.value = value;
    }

    @Override
    public LwM2mTransportUtil.LwM2mTypeOper getType() {
        return LwM2mTransportUtil.LwM2mTypeOper.WRITE_REPLACE;
    }



}
