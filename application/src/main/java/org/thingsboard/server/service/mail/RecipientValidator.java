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
package org.thingsboard.server.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipientValidator {

    private final TbTenantProfileCache tenantProfileCache;
    private final UserService userService;
    private final UserAuthDetailsCache userAuthDetailsCache;

    @Value("${security.user_login_case_sensitive:true}")
    private boolean userLoginCaseSensitive;

    public void validateRecipients(TenantId tenantId, TbEmail tbEmail) throws ThingsboardException {
        if (!tenantProfileCache.isRestricted(tenantId)) {
            return;
        }
        for (String recipient : collectRecipients(tbEmail)) {
            if (!isActivatedTenantUser(tenantId, recipient)) {
                throw new ThingsboardException("Recipient '" + recipient + "' is not an activated user of this tenant",
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
        }
    }

    // The 'from' address is tenant-authored in the to-email rule node, so a restricted tenant could
    // otherwise send under our DKIM as anyone.
    public String resolveFrom(TenantId tenantId, TbEmail tbEmail, String systemMailFrom) {
        if (tenantProfileCache.isRestricted(tenantId)) {
            return systemMailFrom;
        }
        return StringUtils.isBlank(tbEmail.getFrom()) ? systemMailFrom : tbEmail.getFrom();
    }

    private List<String> collectRecipients(TbEmail tbEmail) {
        List<String> recipients = new ArrayList<>();
        addAll(recipients, tbEmail.getTo());
        addAll(recipients, tbEmail.getCc());
        addAll(recipients, tbEmail.getBcc());
        return recipients;
    }

    private void addAll(List<String> recipients, String addresses) {
        if (StringUtils.isBlank(addresses)) {
            return;
        }
        for (String address : addresses.split("\\s*,\\s*")) {
            if (StringUtils.isNotBlank(address)) {
                recipients.add(address);
            }
        }
    }

    // Both lookups are cache-backed, so a warm send performs no database call.
    private boolean isActivatedTenantUser(TenantId tenantId, String recipient) throws ThingsboardException {
        // Validated up front so a malformed address is reported as a rejected recipient rather than
        // being swallowed by the fail-closed catch below as an internal error.
        try {
            DataValidator.validateEmail(recipient);
        } catch (Exception e) {
            return false;
        }
        try {
            User user = userService.findUserByTenantIdAndEmail(tenantId, normalize(recipient));
            if (user == null) {
                return false;
            }
            UserAuthDetails authDetails = userAuthDetailsCache.getUserAuthDetails(tenantId, user.getId());
            return authDetails != null && authDetails.credentialsEnabled();
        } catch (Exception e) {
            // Fail closed: an unverifiable recipient must not become a bypass.
            log.warn("[{}] Failed to verify email recipient", tenantId, e);
            throw new ThingsboardException("Unable to verify email recipient", ThingsboardErrorCode.GENERAL);
        }
    }

    // findUserByTenantIdAndEmail does no normalisation of its own, unlike findUserByEmail.
    private String normalize(String email) {
        return userLoginCaseSensitive ? email : email.toLowerCase();
    }

}
