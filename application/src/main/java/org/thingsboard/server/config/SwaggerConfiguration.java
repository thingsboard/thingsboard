/**
 * Copyright © 2016 The Thingsboard Authors
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
package org.thingsboard.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.common.data.security.Authority;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@Configuration
public class SwaggerConfiguration {

      @Bean
      public Docket thingsboardApi() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .groupName("thingsboard")
                    .apiInfo(apiInfo())
                    .select()
                    .paths(PathSelectors.any())
                    .build()
                    .securitySchemes(newArrayList(jwtTokenKey()))
                    .securityContexts(newArrayList(securityContext()));
      }

      private ApiKey jwtTokenKey() {
            return new ApiKey("X-Authorization", "JWT token", "header");
      }

      private SecurityContext securityContext() {
            return SecurityContext.builder()
                    .securityReferences(defaultAuth())
                    .forPaths(PathSelectors.regex("/api.*"))
                    .build();
      }

      List<SecurityReference> defaultAuth() {
            AuthorizationScope[] authorizationScopes = new AuthorizationScope[3];
            authorizationScopes[0] = new AuthorizationScope(Authority.SYS_ADMIN.name(), "System administrator");
            authorizationScopes[1] = new AuthorizationScope(Authority.TENANT_ADMIN.name(), "Tenant administrator");
            authorizationScopes[2] = new AuthorizationScope(Authority.CUSTOMER_USER.name(), "Customer");
            return newArrayList(
                    new SecurityReference("X-Authorization", authorizationScopes));
      }

      private ApiInfo apiInfo() {
            return new ApiInfoBuilder()
                .title("Thingsboard REST API")
                .description("For instructions how to authorize requests please visit <a href='http://thingsboard.io/docs/rest-auth'>Documentation page</a>")
                .contact(new Contact("Thingsboard team", "http://thingsboard.io", "info@thingsboard.io"))
                .license("Apache License Version 2.0")
                .licenseUrl("https://github.com/thingsboard/thingsboard/blob/master/LICENSE")
                .version("2.0")
                .build();
      }

}
