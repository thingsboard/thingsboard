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
package org.thingsboard.server.service.notification.channels;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.notification.targets.MicrosoftTeamsNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.template.MicrosoftTeamsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicrosoftTeamsNotificationChannelTest {

    private static final String GENERIC_FAILURE = "Failed to send message to Microsoft Teams";

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private SystemSecurityService systemSecurityService;

    private MicrosoftTeamsNotificationChannel channel;
    private MicrosoftTeamsNotificationTargetConfig targetConfig;
    private MicrosoftTeamsDeliveryMethodNotificationTemplate template;

    @BeforeEach
    void setUp() {
        channel = new MicrosoftTeamsNotificationChannel(systemSecurityService);
        channel.setRestTemplate(restTemplate);

        targetConfig = new MicrosoftTeamsNotificationTargetConfig();
        targetConfig.setWebhookUrl("https://example.webhook.office.com/hook");
        targetConfig.setChannelName("test");
        targetConfig.setUseOldApi(true);

        template = new MicrosoftTeamsDeliveryMethodNotificationTemplate();
        template.setBody("body");
    }

    @Test
    void successfulPostDoesNotThrow() throws Exception {
        when(restTemplate.postForEntity(any(URI.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("1", HttpStatus.OK));

        assertThatCode(() -> channel.sendNotification(targetConfig, template, null))
                .doesNotThrowAnyException();
    }

    @Test
    void adaptiveCardPathSucceeds() throws Exception {
        targetConfig.setUseOldApi(false);
        when(restTemplate.postForEntity(any(URI.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("1", HttpStatus.OK));

        assertThatCode(() -> channel.sendNotification(targetConfig, template, null))
                .doesNotThrowAnyException();
    }

    @Test
    void errorResponseStatusAndBodyAreNotLeaked() {
        HttpClientErrorException upstream = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found",
                HttpHeaders.EMPTY,
                "<html>SECRET INTERNAL 404 PAGE</html>".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        when(restTemplate.postForEntity(any(URI.class), any(), eq(String.class))).thenThrow(upstream);

        Throwable thrown = catchThrowable(() -> channel.sendNotification(targetConfig, template, null));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
        assertThat(thrown.getMessage())
                .isEqualTo(GENERIC_FAILURE)
                .doesNotContain("404")
                .doesNotContain("SECRET INTERNAL 404 PAGE");
    }

    @Test
    void connectionErrorMessageDoesNotExposeTarget() {
        when(restTemplate.postForEntity(any(URI.class), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("I/O error on POST request for \"http://172.17.0.1:8181\": Connection refused"));

        Throwable thrown = catchThrowable(() -> channel.sendNotification(targetConfig, template, null));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
        assertThat(thrown.getMessage())
                .isEqualTo(GENERIC_FAILURE)
                .doesNotContain("172.17.0.1");
    }

    @Test
    void redirectResponseIsRejectedWithoutLeak() {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("http://127.0.0.1/internal"));
        ResponseEntity<String> redirect = new ResponseEntity<>("", headers, HttpStatus.FOUND);
        when(restTemplate.postForEntity(any(URI.class), any(), eq(String.class))).thenReturn(redirect);

        Throwable thrown = catchThrowable(() -> channel.sendNotification(targetConfig, template, null));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
        assertThat(thrown.getMessage())
                .isEqualTo(GENERIC_FAILURE)
                .doesNotContain("127.0.0.1")
                .doesNotContain("302")
                .doesNotContain("redirect");
    }

    // Verifies that the RestTemplate built by the channel does not follow 3xx responses at the HTTP-client layer.
    // The mocked-RestTemplate test above only exercises the response-status check; this one guards against a
    // refactor that accidentally drops disableRedirectHandling() and reopens the SSRF bypass.
    @Test
    void disableRedirectHandlingPreventsFollowUpToInternalHost() throws Exception {
        AtomicInteger redirectTargetHits = new AtomicInteger();
        HttpServer redirectTarget = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        redirectTarget.createContext("/internal", exchange -> {
            redirectTargetHits.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        redirectTarget.start();

        HttpServer redirector = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        redirector.createContext("/hook", exchange -> {
            exchange.getResponseHeaders().add("Location",
                    "http://127.0.0.1:" + redirectTarget.getAddress().getPort() + "/internal");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        redirector.start();

        try {
            // Use the channel's own RestTemplate (default-initialized via buildHttpClient()) — do NOT call setRestTemplate.
            MicrosoftTeamsNotificationChannel realChannel = new MicrosoftTeamsNotificationChannel(systemSecurityService);
            try {
                targetConfig.setWebhookUrl("http://127.0.0.1:" + redirector.getAddress().getPort() + "/hook");

                Throwable thrown = catchThrowable(() -> realChannel.sendNotification(targetConfig, template, null));
                assertThat(thrown).isInstanceOf(RuntimeException.class);
                assertThat(thrown.getMessage()).isEqualTo(GENERIC_FAILURE);
                assertThat(redirectTargetHits.get())
                        .as("HttpClient5 must not follow 3xx — disableRedirectHandling() regression?")
                        .isZero();
            } finally {
                realChannel.shutdown();
            }
        } finally {
            redirector.stop(0);
            redirectTarget.stop(0);
        }
    }

}
