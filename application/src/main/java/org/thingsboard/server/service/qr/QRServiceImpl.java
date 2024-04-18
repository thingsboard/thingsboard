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
package org.thingsboard.server.service.qr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import static org.thingsboard.server.service.security.system.DefaultSystemSecurityService.DEFAULT_QR_SECRET_KEY_LENGTH;

@Service
@Slf4j
@RequiredArgsConstructor
public class QRServiceImpl extends AbstractCachedService<String, JwtPair, QRSecretEvictEvent> implements QRService {

    private final JwtTokenFactory tokenFactory;
    private final SystemSecurityService systemSecurityService;

    @Override
    public String generateSecret(SecurityUser securityUser) {
        log.trace("Executing generateSecret for user [{}]", securityUser.getId());
        Integer qrSecretKeyLength = systemSecurityService.getSecuritySettings().getQrSecretKeyLength();
        String secret = StringUtils.generateSafeToken(qrSecretKeyLength == null ? DEFAULT_QR_SECRET_KEY_LENGTH : qrSecretKeyLength);
        cache.put(secret, tokenFactory.createTokenPair(securityUser));
        return secret;
    }

    @Override
    public JwtPair getJwtPair(String secret) throws ThingsboardException {
        TbCacheValueWrapper<JwtPair> jwtPair = cache.get(secret);
        if (jwtPair != null) {
            return jwtPair.get();
        } else {
            throw new ThingsboardException("Jwt token not found or expired!", ThingsboardErrorCode.JWT_TOKEN_EXPIRED);
        }
    }

    @TransactionalEventListener(classes = QRSecretEvictEvent.class)
    @Override
    public void handleEvictEvent(QRSecretEvictEvent event) {
        cache.evict(event.getSecret());
    }
}
