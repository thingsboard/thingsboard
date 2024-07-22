/*
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
package org.thingsboard.apigw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
public class AccessTokenAlterFilter implements WebFilter {

    private final String tbUrl;
    private final String password;

    public AccessTokenAlterFilter( String tbUrl, String password) {
        this.tbUrl = tbUrl;
        this.password = password;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.debug("IAM Access Token : {}", SecurityUtils.getTokenFromRequest(exchange));

        Mono<String> emailFromUser = SecurityUtils.getEmailFromUser(exchange);
        Mono<String> tbToken = emailFromUser.flatMap(email -> {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode requestBody = objectMapper.createObjectNode()
                    .put("password", this.password)
                    .put("username", email);

            WebClient webClient = WebClient.builder()
                    .baseUrl(this.tbUrl)  // Replace with the actual base URL of the API endpoint
                    .build();
            return webClient.post()
                    .uri("/api/auth/login")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> jsonNode.get("token").asText());
        });

       return tbToken.flatMap(authToken -> {
            // Modify request headers
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(builder -> builder.header("X-"+HttpHeaders.AUTHORIZATION, "Bearer "+ authToken))
                    .build();
            return chain.filter(modifiedExchange);
        }).switchIfEmpty(chain.filter(exchange));

    }
}
