// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.ruleChain;

import lombok.Data;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;

/**
 * Created by ashvayka on 19.03.18.
 */
@Data
public final class RuleNodeCtx {
    private final TenantId tenantId;
    private final TbActorRef chainActor;
    private final TbActorRef selfActor;
    private RuleNode self;

    RuleNodeCtx(TenantId tenantId, TbActorCtx selfActor, RuleNode self) {
        this(tenantId, selfActor.getParentRef(), selfActor, self);
    }

    RuleNodeCtx(TenantId tenantId, TbActorRef chainActor, TbActorRef selfActor, RuleNode self) {
        this.tenantId = tenantId;
        this.chainActor = chainActor;
        this.selfActor = selfActor;
        this.self = self;
    }

}
