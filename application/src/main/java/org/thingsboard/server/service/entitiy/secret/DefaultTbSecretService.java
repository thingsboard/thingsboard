// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.secret;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbSecretService extends AbstractTbEntityService implements TbSecretService {

    private final SecretService secretService;

    @Override
    public SecretInfo save(Secret secret, User user) throws ThingsboardException {
        ActionType actionType = secret.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = secret.getTenantId();
        try {
            SecretInfo savedSecret = new SecretInfo(checkNotNull(secretService.saveSecret(tenantId, secret)));
            logEntityActionService.logEntityAction(tenantId, savedSecret.getId(), savedSecret, actionType, user);
            return savedSecret;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.SECRET), secret, actionType, user, e);
            throw e;
        }
    }

    @Override
    public TbSecretDeleteResult delete(SecretInfo secretInfo, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = secretInfo.getTenantId();
        SecretId secretId = secretInfo.getId();
        try {
            TbSecretDeleteResult result = secretService.deleteSecret(tenantId, secretInfo);
            if (result.isSuccess()) {
                logEntityActionService.logEntityAction(tenantId, secretId, secretInfo, actionType, user, secretId.toString());
            }
            return result;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.SECRET), actionType, user, e, secretId.toString());
            throw e;
        }
    }

}
