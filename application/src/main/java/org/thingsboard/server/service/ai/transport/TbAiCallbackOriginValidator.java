// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiTokenProvider;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Optional;

/**
 * Guards the legacy HTTP paths, where TB AI calls back the origin sent with the token.
 */
@Component
@TbCoreComponent
@RequiredArgsConstructor
class TbAiCallbackOriginValidator {

    private final Optional<TbAiTokenProvider> tokenProvider;

    void validate(SecurityUser user) {
        tokenProvider.ifPresent(provider -> provider.validateCallbackOrigin(user));
    }

}
