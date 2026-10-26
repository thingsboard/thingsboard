// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sms;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;

/**
 * Describes an SMS send that the Edge delegates to the Cloud. On the Edge, a send that depends on
 * admin-configured (tenant/system) SMS provider settings cannot be resolved locally, so the call is
 * packaged into this request and enqueued as a SEND_SMS cloud event; on the Cloud, {@code method}
 * selects the matching {@code SmsService} call so the Cloud resolves the config and transmits via its
 * own provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EdgeSmsRequest {

    public enum SmsMethod {
        SEND_SMS,      // sendSms(customerId, numbersTo, message)
        SEND_TEST_SMS  // sendTestSms(testSmsRequest)
    }

    private SmsMethod method;

    private String[] numbers;
    private String message;

    private TestSmsRequest testSmsRequest;

}
