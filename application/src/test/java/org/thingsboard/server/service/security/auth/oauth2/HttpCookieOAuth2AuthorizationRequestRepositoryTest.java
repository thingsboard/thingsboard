/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth.oauth2;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME;

public class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private static final String SERIALIZED_ATTACK_STRING =
            "rO0ABXNyAHVvcmcudGhpbmdzYm9hcmQuc2VydmVyLnNlcnZpY2Uuc2VjdXJpdHkuYXV0aC5vYXV0aDIuSHR0cENvb2tpZU9BdXRoMkF1dGhvcml6YXRpb25SZXF1ZXN0UmVwb3NpdG9yeVRlc3QkTWFsaWNpb3VzQ2xhc3MAAAAAAAAAAAIAAHhw";

    private static int maliciousMethodInvocationCounter;

    @Before
    public void resetInvocationCounter() {
        maliciousMethodInvocationCounter = 0;
    }

    @Test
    public void whenLoadAuthorizationRequest_thenMaliciousMethodNotInvoked() {
        HttpCookieOAuth2AuthorizationRequestRepository cookieRequestRepo = new HttpCookieOAuth2AuthorizationRequestRepository();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, SERIALIZED_ATTACK_STRING);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        cookieRequestRepo.loadAuthorizationRequest(request);

        assertEquals(0, maliciousMethodInvocationCounter);
    }

    private static class MaliciousClass implements Serializable {
        private static final long serialVersionUID = 0L;

        public void maliciousMethod() {
            maliciousMethodInvocationCounter++;
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            maliciousMethod();
        }
    }
}
