// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.secret;

import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;

public interface TbSecretService {

    SecretInfo save(Secret secret, User user) throws ThingsboardException;

    TbSecretDeleteResult delete(SecretInfo secretInfo, User user);

}
