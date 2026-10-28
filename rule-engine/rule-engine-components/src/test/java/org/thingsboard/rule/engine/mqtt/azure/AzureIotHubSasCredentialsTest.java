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
package org.thingsboard.rule.engine.mqtt.azure;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockConstruction;

@ExtendWith(MockitoExtension.class)
class AzureIotHubSasCredentialsTest {

    @BeforeEach
    void removeBcProvider() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    @AfterEach
    void removeBcProviderAfter() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    @Test
    public void initSslContextDoesNotReRegisterProviderOnRepeatedCalls() throws Exception {
        AzureIotHubSasCredentials credentials = new AzureIotHubSasCredentials();
        credentials.setCaCert(fileContent("pem/tb-cloud.pem"));

        try (MockedConstruction<BouncyCastleProvider> construction = mockConstruction(BouncyCastleProvider.class,
                (mock, context) -> given(mock.getName()).willReturn(BouncyCastleProvider.PROVIDER_NAME))) {

            assertThatNoException().isThrownBy(credentials::initSslContext);
            int afterFirstCall = construction.constructed().size();

            // A prior call already left "BC" registered; a second call must not construct another
            // instance just to have Security.addProvider() silently reject it as a duplicate name.
            assertThatNoException().isThrownBy(credentials::initSslContext);
            int afterSecondCall = construction.constructed().size();

            assertThat(afterSecondCall).isEqualTo(afterFirstCall);
        }
    }

    private String fileContent(String fileName) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(fileName).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }
}
