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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageStateValue;

/**
 * Describes a mail send that the Edge delegates to the Cloud. On the Edge, a mail send that depends
 * on admin-configured (tenant/system) mail settings is packaged into this request and enqueued as a
 * SEND_EMAIL cloud event. On the Cloud, {@code method} selects the matching {@code MailService} call
 * so the Cloud resolves the config, renders the template and transmits via its own SMTP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EdgeMailRequest {

    public enum MailMethod {
        SEND_BASIC,        // sendEmail(to, subject, message)
        SEND_TB_EMAIL,     // send(TbEmail)
        ACTIVATION,        // sendActivationEmail(activationLink, ttlMs, to)
        ACCOUNT_ACTIVATED, // sendAccountActivatedEmail(loginLink, to)
        RESET_PASSWORD,    // sendResetPasswordEmail(passwordResetLink, ttlMs, to)
        PASSWORD_WAS_RESET,// sendPasswordWasResetEmail(loginLink, to)
        TWO_FA,            // sendTwoFaVerificationEmail(to, verificationCode, expirationTimeSeconds)
        ACCOUNT_LOCKOUT,   // sendAccountLockoutEmail(lockoutEmail, to, maxFailedLoginAttempts)
        API_USAGE_STATE,   // sendApiFeatureStateEmail(apiFeature, stateValue, to, recordState)
        TEST_MAIL          // sendTestMail(config, to)
    }

    private MailMethod method;

    private String to;
    private String subject;
    private String message;

    private TbEmail tbEmail;

    private String activationLink;
    private String loginLink;
    private String passwordResetLink;
    private Long ttlMs;

    private String verificationCode;
    private Integer expirationTimeSeconds;

    private String lockoutEmail;
    private Integer maxFailedLoginAttempts;

    private ApiFeature apiFeature;
    private ApiUsageStateValue stateValue;
    private ApiUsageRecordState recordState;

    private JsonNode testConfig;

}
