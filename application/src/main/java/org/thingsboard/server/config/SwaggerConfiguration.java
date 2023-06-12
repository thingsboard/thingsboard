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
package org.thingsboard.server.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardCredentialsExpiredResponse;
import org.thingsboard.server.exception.ThingsboardErrorResponse;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;
import org.thingsboard.server.service.security.auth.rest.LoginResponse;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Configuration
@TbCoreComponent
@Profile("!test")
public class SwaggerConfiguration {

    public static final String LOGIN_ENDPOINT = "/api/auth/login";

    @Value("${swagger.api_path:/api/**}")
    private String apiPath;
    @Value("${swagger.non_security_path_regex}")
    private String nonSecurityPathRegex;
    @Value("${swagger.title}")
    private String title;
    @Value("${swagger.description}")
    private String description;
    @Value("${swagger.contact.name}")
    private String contactName;
    @Value("${swagger.contact.url}")
    private String contactUrl;
    @Value("${swagger.contact.email}")
    private String contactEmail;
    @Value("${swagger.license.title}")
    private String licenseTitle;
    @Value("${swagger.license.url}")
    private String licenseUrl;
    @Value("${swagger.version}")
    private String version;
    @Value("${app.version:unknown}")
    private String appVersion;

    @Bean
    public OpenAPI tbOpenAPI() {
        Contact contact = new Contact()
                .name(contactName)
                .url(contactUrl)
                .email(contactEmail);

        License license = new License()
                .name(licenseTitle)
                .url(licenseUrl);

        String apiVersion = version;
        if (StringUtils.isEmpty(apiVersion)) {
            apiVersion = appVersion;
        }

        Info info = new Info()
                .title(title)
                .description(description)
                .contact(contact)
                .license(license)
                .version(apiVersion);

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("Enter Username / Password");

        var openApi = new OpenAPI()
                .components(new Components().addSecuritySchemes("HTTP login form", securityScheme))
                .info(info);
        addLoginOperation(openApi);
        return openApi;
    }

    public void addLoginOperation(OpenAPI openAPI) {
        openAPI.getComponents()
                .addSchemas("LoginRequest", ModelConverters.getInstance().readAllAsResolvedSchema(new AnnotatedType().type(LoginRequest.class)).schema)
                .addSchemas("LoginResponse", ModelConverters.getInstance().readAllAsResolvedSchema(new AnnotatedType().type(LoginResponse.class)).schema)
                .addSchemas("ThingsboardErrorResponse", ModelConverters.getInstance().readAllAsResolvedSchema(new AnnotatedType().type(ThingsboardErrorResponse.class)).schema)
                .addSchemas("ThingsboardCredentialsExpiredResponse", ModelConverters.getInstance().readAllAsResolvedSchema(new AnnotatedType().type(ThingsboardCredentialsExpiredResponse.class)).schema);

        var operation = new Operation();
        operation.summary("Login method to get user JWT token data");
        operation.description("Login method used to authenticate user and get JWT token data.\n\nValue of the response **token** " +
                "field can be used as **X-Authorization** header value:\n\n`X-Authorization: Bearer $JWT_TOKEN_VALUE`.");
        var requestBody = new RequestBody().content(new Content().addMediaType(APPLICATION_JSON_VALUE,
                new MediaType().schema(new Schema<LoginRequest>().$ref("#/components/schemas/LoginRequest"))));
        operation.requestBody(requestBody);
        operation.responses(getResponses());
        operation.addTagsItem("login-endpoint");
        var pathItem = new PathItem().post(operation);
        openAPI.path(LOGIN_ENDPOINT, pathItem);
    }

    private ApiResponses getResponses() {
        ApiResponses apiResponses = new ApiResponses();

        apiResponses.addApiResponse("200", new ApiResponse().description("OK")
                .content(new Content().addMediaType(APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<LoginResponse>().$ref("#/components/schemas/LoginResponse")))));

        ApiResponse unauthorizedResponse = new ApiResponse().description("Unauthorized");
        Content content = new Content();
        MediaType mediaType = new MediaType().schema(new Schema<ThingsboardErrorResponse>().$ref("#/components/schemas/ThingsboardErrorResponse"));

        Map<String, Example> examples = Map.of(
                "bad-credentials", errorExample("Bad credentials",
                        ThingsboardErrorResponse.of("Invalid username or password", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                "token-expired", errorExample("JWT token expired",
                        ThingsboardErrorResponse.of("Token has expired", ThingsboardErrorCode.JWT_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED)),
                "account-disabled", errorExample("Disabled account",
                        ThingsboardErrorResponse.of("User account is not active", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                "account-locked", errorExample("Locked account",
                        ThingsboardErrorResponse.of("User account is locked due to security policy", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED)),
                "authentication-failed", errorExample("General authentication error",
                        ThingsboardErrorResponse.of("Authentication failed", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED))
        );

        mediaType.setExamples(examples);
        content.addMediaType(APPLICATION_JSON_VALUE, mediaType);
        unauthorizedResponse.setContent(content);
        apiResponses.addApiResponse("401", unauthorizedResponse);

        ApiResponse expiredCredentialsResponse = new ApiResponse().description("Unauthorized (**Expired credentials**)");
        Content expiredContent = new Content();
        MediaType expiredMediaType = new MediaType().schema(new Schema<ThingsboardCredentialsExpiredResponse>().$ref("#/components/schemas/ThingsboardCredentialsExpiredResponse"));
        expiredMediaType.addExamples("credentials-expired", errorExample("Expired credentials",
                ThingsboardCredentialsExpiredResponse.of("User password expired!", StringUtils.randomAlphanumeric(30))));
        expiredContent.addMediaType(APPLICATION_JSON_VALUE, expiredMediaType);
        expiredCredentialsResponse.setContent(expiredContent);
        apiResponses.addApiResponse("401 ", expiredCredentialsResponse);

        return apiResponses;
    }

    private Example errorExample(String summary, ThingsboardErrorResponse example) {
        return new Example()
                .summary(summary)
                .value(example);
    }

    @Bean
    public GroupedOpenApi thingsboardApi() {
        return GroupedOpenApi.builder()
                .group("thingsboard")
                .pathsToMatch(apiPath)
                .addOpenApiCustomizer(customOpenApiCustomizer())
                .build();
    }

    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfig(SwaggerUiConfigProperties uiProperties) {
        uiProperties.setDeepLinking(true);
        uiProperties.setDisplayOperationId(false);
        uiProperties.setDefaultModelsExpandDepth(1);
        uiProperties.setDefaultModelExpandDepth(1);
        uiProperties.setDefaultModelRendering("example");
        uiProperties.setDisplayRequestDuration(false);
        uiProperties.setDocExpansion("none");
        uiProperties.setFilter("false");
        uiProperties.setMaxDisplayedTags(null);
        uiProperties.setOperationsSorter("alpha");
        uiProperties.setTagsSorter("alpha");
        uiProperties.setShowExtensions(false);
        uiProperties.setShowCommonExtensions(false);
        uiProperties.setSupportedSubmitMethods(List.of("get", "put", "post", "delete", "options", "head", "patch", "trace"));
        uiProperties.setValidatorUrl(null);
        uiProperties.setPersistAuthorization(true);

        var syntaxHighLight = new SwaggerUiConfigProperties.SyntaxHighlight();
        syntaxHighLight.setActivated(true);
        syntaxHighLight.setTheme("agate");

        uiProperties.setSyntaxHighlight(syntaxHighLight);
        return uiProperties;
    }

    public OpenApiCustomizer customOpenApiCustomizer() {
        SecurityRequirement loginForm = new SecurityRequirement().addList("HTTP login form");

        return openAPI -> openAPI.getPaths().entrySet().stream().peek(entry -> {
            if (!(entry.getKey().matches(nonSecurityPathRegex) || entry.getKey().equals(LOGIN_ENDPOINT))) {
                entry.getValue()
                        .readOperationsMap()
                        .values()
                        .forEach(operation -> operation.addSecurityItem(loginForm));
            }

            entry.getValue().readOperationsMap().forEach(((httpMethod, operation) -> {
                operation.setResponses(getResponses(operation.getResponses(), httpMethod.equals(PathItem.HttpMethod.POST)));
            }));

        }).map(entry -> {
            String tagItem = entry.getValue().readOperationsMap().values().stream().findAny().get().getTags().get(0);
            return tagFromTagItem(tagItem);
        }).forEach(openAPI::addTagsItem);
    }

    private Tag tagFromTagItem(String tagItem) {
        String[] words = tagItem.split("-");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            sb.append(word.substring(0, 1).toUpperCase());
            sb.append(word.substring(1).toLowerCase());
            sb.append(" ");
        }

        return new Tag().name(tagItem).description(sb.toString().trim());
    }

    private ApiResponses getResponses(ApiResponses apiResponses, boolean isPost) {
        if (apiResponses == null) {
            apiResponses = new ApiResponses();
        }

        apiResponses.addApiResponse("400", new ApiResponse().description("Bad Request")
                .content(getErrorContent(ThingsboardErrorResponse.of(isPost ? "Invalid request body" : "Invalid UUID string: 123", ThingsboardErrorCode.BAD_REQUEST_PARAMS, HttpStatus.BAD_REQUEST))));

        apiResponses.addApiResponse("401", new ApiResponse().description("Unauthorized")
                .content(getErrorContent(ThingsboardErrorResponse.of("Authentication failed", ThingsboardErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED))));

        apiResponses.addApiResponse("403", new ApiResponse().description("Forbidden")
                .content(getErrorContent(ThingsboardErrorResponse.of("You don't have permission to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED, HttpStatus.FORBIDDEN))));

        apiResponses.addApiResponse("404", new ApiResponse().description("Not Found")
                .content(getErrorContent(ThingsboardErrorResponse.of("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND, HttpStatus.NOT_FOUND))));

        apiResponses.addApiResponse("429", new ApiResponse().description("Too Many Requests")
                .content(getErrorContent(ThingsboardErrorResponse.of("Too many requests for current tenant!", ThingsboardErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS))));

        return apiResponses;
    }

    private Content getErrorContent(ThingsboardErrorResponse errorResponse) {
        return new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new MediaType().schema(new Schema<ThingsboardErrorResponse>().example(errorResponse)));
    }

}
