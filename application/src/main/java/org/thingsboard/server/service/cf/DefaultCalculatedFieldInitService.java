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
package org.thingsboard.server.service.cf;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldStateRestoreMsg;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldLinkInitMsg;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldStateService;

@Service
@TbRuleEngineComponent
@RequiredArgsConstructor
public class DefaultCalculatedFieldInitService implements CalculatedFieldInitService {

    private final CalculatedFieldService calculatedFieldService;
    private final CalculatedFieldStateService stateService;
    private final ActorSystemContext actorSystemContext;

    @Value("${calculated_fields.init_fetch_pack_size:50000}")
    @Getter
    private int initFetchPackSize;

    @AfterStartUp(order = AfterStartUp.CF_INIT_SERVICE)
    public void initCalculatedFieldDefinitions() {
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
        cfs.forEach(cf -> actorSystemContext.tell(new CalculatedFieldInitMsg(cf.getTenantId(), cf)));
        PageDataIterable<CalculatedFieldLink> cfls = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFieldLinks, initFetchPackSize);
        cfls.forEach(link -> actorSystemContext.tell(new CalculatedFieldLinkInitMsg(link.getTenantId(), link)));
        //TODO: combine with the DefaultCalculatedFieldCache.

    }

    @AfterStartUp(order = AfterStartUp.CF_STATE_RESTORE_SERVICE)
    public void initCalculatedFieldStates() {
        stateService.restoreStates().forEach((k, v) -> actorSystemContext.tell(new CalculatedFieldStateRestoreMsg(k, v)));
    }



}
