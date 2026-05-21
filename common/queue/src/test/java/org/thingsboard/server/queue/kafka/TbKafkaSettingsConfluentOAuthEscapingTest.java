/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.queue.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TbKafkaSettings.class, KafkaAdmin.class})
@TestPropertySource(properties = {
        "queue.type=kafka",
        "queue.kafka.bootstrap.servers=localhost:9092",
        "queue.kafka.use_confluent_cloud=true",
        "queue.kafka.confluent.sasl.mechanism=OAUTHBEARER",
        "queue.kafka.confluent.security.protocol=SASL_SSL",
        "queue.kafka.confluent.oauth.client-id=client\"with\\\\quote",
        "queue.kafka.confluent.oauth.client-secret=secret\"with\\\\backslash;and-semi",
        "queue.kafka.confluent.oauth.endpoint-url=https://idp.example.com/oauth/token"
})
class TbKafkaSettingsConfluentOAuthEscapingTest {

    @Autowired
    TbKafkaSettings settings;

    // Locks in that double quotes and backslashes in client credentials are escaped
    // so the JAAS config string parses correctly and cannot inject extra options.
    @Test
    void givenSecretWithQuoteAndBackslash_whenToProps_thenEscapesJaasValues() {
        Properties props = settings.toProps();

        String jaas = props.getProperty("sasl.jaas.config");
        assertThat(jaas)
                .contains("clientId=\"client\\\"with\\\\quote\"")
                .contains("clientSecret=\"secret\\\"with\\\\backslash;and-semi\"")
                .endsWith("\";");
    }

}
