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
package org.thingsboard.server.transport.lwm2m.server.downlink.composite;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;
import org.thingsboard.server.transport.lwm2m.server.downlink.HasContentFormat;

import java.util.Optional;

public class TbLwM2MObserveCompositeRequest extends AbstractTbLwM2MTargetedDownlinkCompositeRequest<ObserveCompositeResponse> implements HasContentFormat {


    private final Optional<ContentFormat> requestContentFormatOpt;

    @Getter
    private final ContentFormat responseContentFormat;

    @Builder
    private TbLwM2MObserveCompositeRequest(String [] versionedIds, long timeout, ContentFormat requestContentFormat, ContentFormat responseContentFormat) {
        super(versionedIds, timeout);
        this.requestContentFormatOpt = Optional.ofNullable(requestContentFormat);
        this.responseContentFormat = responseContentFormat;
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.OBSERVE_COMPOSITE;
    }

    @Override
    public Optional<ContentFormat> getRequestContentFormat() {
        return this.requestContentFormatOpt;
    }
}
